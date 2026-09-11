package com.sedsoftware.bagcue.session.domain

import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.OneOffSessionItemCommand
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.PackingSessionRepository
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SaveSessionItemToTemplatesCommand
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.SessionMergeInput
import com.sedsoftware.bagcue.domain.session.SessionMutation
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionTemplateSnapshot
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.session.completeAllPacked
import com.sedsoftware.bagcue.domain.session.completeWithSkipped
import com.sedsoftware.bagcue.domain.session.editSessionItem
import com.sedsoftware.bagcue.domain.session.mergeSessionItems
import com.sedsoftware.bagcue.domain.session.reapplySessionTemplates
import com.sedsoftware.bagcue.domain.session.requireNonEmptySession
import com.sedsoftware.bagcue.domain.session.setSessionItemPacked
import com.sedsoftware.bagcue.domain.session.validateSessionDate
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

internal data class SessionReferenceData(
    val templates: List<KitTemplate>,
    val catalogItems: List<PackingItem>,
    val resolvedNames: Map<String, String>,
)

@Suppress("TooManyFunctions")
internal class SessionManager(
    private val sessionRepository: PackingSessionRepository,
    private val templateRepository: KitTemplateRepository,
    private val catalogRepository: CatalogRepository,
    private val sessionIdGenerator: PackingSessionIdGenerator,
    private val sessionItemIdGenerator: SessionPackingItemIdGenerator,
    private val catalogItemIdGenerator: PackingItemIdGenerator,
    private val templatePositionIdGenerator: TemplatePositionIdGenerator,
    private val today: () -> LocalDate,
    private val currentTimeMillis: () -> Long,
    private val resolveResourceKey: suspend (ResourceKey) -> String,
    private val analyticsController: AnalyticsController,
    private val historyRepository: SessionHistoryRepository? = null,
) {
    val currentDate: LocalDate get() = today()

    fun observeReferenceData(): Flow<SessionReferenceData> = combine(
        templateRepository.observeTemplates(),
        catalogRepository.observeItems(),
    ) { templates, items ->
        val keys = buildSet {
            templates.mapNotNullTo(this) { it.seedNameKey }
            items.mapNotNullTo(this) { it.seedNameKey }
            templates.flatMap(KitTemplate::positions).mapNotNullTo(this) { it.bagLabel.seedNameKey }
        }
        SessionReferenceData(
            templates = templates,
            catalogItems = items,
            resolvedNames = keys.associate { it.value to resolveResourceKey(it) },
        )
    }

    fun observeSession(id: PackingSessionId): Flow<PackingSession?> = sessionRepository.observeSession(id)

    suspend fun loadToday(): Result<TodayOverview> = captureResult {
        val currentDate = today()
        val current = sessionRepository.findSessionByDate(currentDate).getOrThrow()
        val history = historyRepository?.readHistory()?.getOrThrow()
        TodayOverview(
            session = current,
            nextSession = history?.planned.orEmpty()
                .asSequence()
                .filter { it.localDate > currentDate }
                .minByOrNull(PackingSession::localDate),
            isFirstRun = history?.let { it.planned.isEmpty() && it.completed.isEmpty() } ?: (current == null),
        )
    }

    suspend fun findByDate(date: LocalDate): Result<PackingSession?> = captureResult {
        sessionRepository.findSessionByDate(date).getOrThrow()
    }

    suspend fun create(date: LocalDate, templateIds: Set<KitTemplateId>): Result<CreateSessionResult> = captureResult {
        validateSessionDate(date, today())
        val data = readReferenceData()
        val session = composeSession(
            id = sessionIdGenerator.nextId(),
            date = date,
            templates = data.templates.filter { it.id in templateIds },
            catalogItems = data.catalogItems,
        )
        sessionRepository.createSession(session).getOrThrow().also { result ->
            if (result is CreateSessionResult.Created) recordSafely(AnalyticsEventName.PackingSessionCreated)
        }
    }

    suspend fun replace(existing: PackingSession, templateIds: Set<KitTemplateId>): Result<SessionMutation> = captureResult {
        val data = readReferenceData()
        val templates = data.templates.filter { it.id in templateIds }
        val recomposed = merge(templates, data.catalogItems)
        val mutation = reapplySessionTemplates(existing, snapshots(templates), recomposed)
        val persisted = sessionRepository.replaceActiveSnapshot(mutation.session).getOrThrow()
        mutation.copy(session = persisted)
    }

    suspend fun setPacked(session: PackingSession, itemId: SessionPackingItemId, packed: Boolean): Result<PackingSession> =
        captureResult {
            sessionRepository.updateItem(
                session.id,
                setSessionItemPacked(session, itemId, packed).items.first { it.id == itemId },
            ).getOrThrow()
        }

    suspend fun editItem(
        session: PackingSession,
        itemId: SessionPackingItemId,
        bag: SessionBagAssignment,
        source: String?,
    ): Result<PackingSession> = captureResult {
        sessionRepository.updateItem(
            session.id,
            editSessionItem(session, itemId, bag, source).items.first { it.id == itemId },
        ).getOrThrow()
    }

    suspend fun addOneOff(sessionId: PackingSessionId, name: String, location: String?): Result<PackingSession> =
        captureResult {
            sessionRepository.addOneOffItem(
                OneOffSessionItemCommand(
                    sessionId = sessionId,
                    catalogItemId = catalogItemIdGenerator.nextId(),
                    sessionItemId = sessionItemIdGenerator.nextId(),
                    userName = validatedCatalogName(name),
                    usualLocation = location?.takeUnless(String::isBlank),
                ),
            ).getOrThrow()
        }

    suspend fun remove(sessionId: PackingSessionId, itemId: SessionPackingItemId): Result<SessionMutation> =
        captureResult { sessionRepository.removeItem(sessionId, itemId).getOrThrow() }

    suspend fun undo(snapshot: com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot): Result<PackingSession> =
        captureResult { sessionRepository.restoreUndo(snapshot).getOrThrow() }

    suspend fun completeAll(session: PackingSession): Result<PackingSession> = captureResult {
        sessionRepository.completeSession(completeAllPacked(session, currentTimeMillis())).getOrThrow().also {
            recordSafely(AnalyticsEventName.PackingSessionCompleted)
        }
    }

    suspend fun completeSkipped(session: PackingSession, count: Int): Result<PackingSession> = captureResult {
        sessionRepository.completeSession(completeWithSkipped(session, count, currentTimeMillis())).getOrThrow().also {
            recordSafely(AnalyticsEventName.PackingSessionCompleted)
        }
    }

    suspend fun reopen(id: PackingSessionId): Result<PackingSession> = captureResult {
        sessionRepository.reopenSession(id).getOrThrow().also {
            recordSafely(AnalyticsEventName.PackingSessionReopened)
        }
    }

    suspend fun recordHistoryRepeat(): Result<Unit> = captureResult {
        recordSafely(AnalyticsEventName.HistoryRepeatUsed)
    }

    private suspend fun recordSafely(name: AnalyticsEventName) {
        analyticsController.record(AnalyticsEvent(name)).exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
        }
    }

    suspend fun saveItemToTemplates(
        sessionId: PackingSessionId,
        itemId: SessionPackingItemId,
        templateIds: Set<KitTemplateId>,
    ): Result<List<KitTemplate>> = captureResult {
        sessionRepository.saveItemToTemplates(
            SaveSessionItemToTemplatesCommand(
                sessionId = sessionId,
                sessionItemId = itemId,
                templatePositionIds = templateIds.associateWith { templatePositionIdGenerator.nextId() },
            ),
        ).getOrThrow()
    }

    private suspend fun readReferenceData(): SessionReferenceData {
        val templates = templateRepository.readTemplates().getOrThrow()
        val items = catalogRepository.readItems().getOrThrow()
        return SessionReferenceData(templates, items, emptyMap())
    }

    private fun composeSession(
        id: PackingSessionId,
        date: LocalDate,
        templates: List<KitTemplate>,
        catalogItems: List<PackingItem>,
    ): PackingSession {
        val items = merge(templates, catalogItems)
        requireNonEmptySession(items)
        return PackingSession(
            id = id,
            localDate = date,
            status = PackingSessionStatus.Active,
            completionMode = null,
            createdAtMillis = currentTimeMillis(),
            completedAtMillis = null,
            revision = 0,
            selectedTemplates = snapshots(templates),
            items = items,
        )
    }

    private fun merge(templates: List<KitTemplate>, catalogItems: List<PackingItem>): List<SessionPackingItem> {
        val itemsById = catalogItems.associateBy(PackingItem::id)
        return mergeSessionItems(
            inputs = templates.flatMap(KitTemplate::positions).map { position ->
                SessionMergeInput(
                    item = requireNotNull(itemsById[position.itemId]),
                    quantity = position.quantity,
                    bagLabel = position.bagLabel,
                    sourceHintOverride = position.sourceHintOverride,
                    sortOrder = position.sortOrder,
                )
            },
            idGenerator = sessionItemIdGenerator,
        )
    }

    private fun snapshots(templates: List<KitTemplate>) = templates.map { template ->
        SessionTemplateSnapshot(
            id = template.id,
            name = SnapshotText(template.seedNameKey, template.userNameOverride),
            sortOrder = template.sortOrder,
        )
    }
}

internal fun mergedItemCount(templateIds: Set<KitTemplateId>, data: SessionReferenceData): Int = data.templates
    .filter { it.id in templateIds }
    .flatMap(KitTemplate::positions)
    .map { it.itemId }
    .distinct()
    .size

internal data class TodayOverview(
    val session: PackingSession?,
    val nextSession: PackingSession?,
    val isFirstRun: Boolean,
)

private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
