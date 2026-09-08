package com.sedsoftware.bagcue.session

import com.sedsoftware.bagcue.domain.apa.*
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.OneOffSessionItemCommand
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionRepository
import com.sedsoftware.bagcue.domain.session.SaveSessionItemToTemplatesCommand
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.domain.session.SessionMutation
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.session.removeSessionItem
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.StarterKitTemplate
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class FakeSessionRepository(
    initial: List<PackingSession> = emptyList(),
    var addOneOffFailure: Throwable? = null,
) : PackingSessionRepository {
    val sessions = MutableStateFlow(initial)
    override fun observeSession(id: PackingSessionId): Flow<PackingSession?> = sessions.map { values -> values.firstOrNull { it.id == id } }
    override suspend fun readSession(id: PackingSessionId) = Result.success(sessions.value.firstOrNull { it.id == id })
    override suspend fun findSessionByDate(date: LocalDate) = Result.success(sessions.value.firstOrNull { it.localDate == date })
    override suspend fun createSession(session: PackingSession): Result<CreateSessionResult> {
        val occupied = sessions.value.firstOrNull { it.localDate == session.localDate }
        if (occupied != null) return Result.success(CreateSessionResult.DateOccupied(occupied))
        sessions.value += session
        return Result.success(CreateSessionResult.Created(session))
    }
    override suspend fun replaceActiveSnapshot(session: PackingSession): Result<PackingSession> = save(session)
    override suspend fun restoreUndo(snapshot: SessionUndoSnapshot): Result<PackingSession> = save(snapshot.before)
    override suspend fun addOneOffItem(command: OneOffSessionItemCommand): Result<PackingSession> {
        addOneOffFailure?.let { return Result.failure(it) }
        val current = sessions.value.first { it.id == command.sessionId }
        val updated = current.copy(revision = current.revision + 1, items = current.items + SessionPackingItem(
            id = command.sessionItemId,
            catalogItemId = command.catalogItemId,
            name = SnapshotText(null, command.userName),
            quantity = command.quantity,
            bagAssignment = command.bagAssignment,
            sourceHint = command.usualLocation,
            state = SessionItemState.NotPacked,
            skipped = false,
            sortOrder = current.items.size.toLong(),
        ))
        return save(updated)
    }
    override suspend fun removeItem(sessionId: PackingSessionId, itemId: SessionPackingItemId): Result<SessionMutation> {
        val mutation = removeSessionItem(sessions.value.first { it.id == sessionId }, itemId)
        save(mutation.session)
        return Result.success(mutation)
    }
    override suspend fun updateItem(sessionId: PackingSessionId, item: SessionPackingItem): Result<PackingSession> {
        val current = sessions.value.first { it.id == sessionId }
        return save(current.copy(revision = current.revision + 1, items = current.items.map { if (it.id == item.id) item else it }))
    }
    override suspend fun completeSession(session: PackingSession): Result<PackingSession> = save(session)
    override suspend fun saveItemToTemplates(command: SaveSessionItemToTemplatesCommand): Result<List<KitTemplate>> = Result.success(emptyList())
    private fun save(session: PackingSession): Result<PackingSession> {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
        return Result.success(session)
    }
}

internal class FakeTemplateRepository(initial: List<KitTemplate>) : KitTemplateRepository {
    val templates = MutableStateFlow(initial)
    override fun observeTemplates(): Flow<List<KitTemplate>> = templates
    override suspend fun readTemplates() = Result.success(templates.value)
    override suspend fun findTemplate(id: KitTemplateId) = Result.success(templates.value.firstOrNull { it.id == id })
    override suspend fun installStarterTemplates(templates: List<StarterKitTemplate>) = Result.success(Unit)
    override suspend fun createTemplate(template: KitTemplate) = Result.success(template)
    override suspend fun updateTemplate(template: KitTemplate) = Result.success(template)
    override suspend fun deleteTemplate(id: KitTemplateId) = Result.success(Unit)
}

internal class FakeCatalogRepository(initial: List<PackingItem>) : CatalogRepository {
    val items = MutableStateFlow(initial)
    override fun observeItems(): Flow<List<PackingItem>> = items
    override suspend fun readItems() = Result.success(items.value)
    override suspend fun findItem(id: PackingItemId) = Result.success(items.value.firstOrNull { it.id == id })
    override suspend fun installStarterItems(items: List<StarterPackingItem>) = Result.success(Unit)
    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary> = error("Not used")
    override suspend fun deleteItemAtomically(id: PackingItemId) = Result.success(Unit)
}

internal fun testTemplate(id: String, vararg positions: TemplatePosition) = KitTemplate(
    id = KitTemplateId(id), seedNameKey = null, userNameOverride = id, sortOrder = 0, positions = positions.toList(),
)

internal fun testPosition(id: String, itemId: String, quantity: Int = 1, sortOrder: Long = 0) = TemplatePosition(
    id = TemplatePositionId(id), itemId = PackingItemId(itemId), quantity = PositionQuantity(quantity),
    bagLabel = com.sedsoftware.bagcue.domain.template.TemplateBagLabel.None, sourceHintOverride = null, sortOrder = sortOrder,
)

internal fun testItem(id: String) = PackingItem(PackingItemId(id), null, id, null, 0)

internal class FakeSessionAnalyticsController : AnalyticsController {
    val events = mutableListOf<AnalyticsEvent>()
    override suspend fun setCollectionEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun resetAnalyticsData() = Result.success(Unit)
    override suspend fun readAppInstanceId() = Result.success<String?>(null)
    override suspend fun record(event: AnalyticsEvent) = Result.success(Unit).also { events += event }
}

internal class FakeSessionPrivacyRepository(initial: AdvertisingPrivacyState? = null) : AdvertisingPrivacyRepository {
    val value = MutableStateFlow(initial)
    override fun observe(): Flow<AdvertisingPrivacyState?> = value
    override suspend fun readFresh(nowEpochMillis: Long) = Result.success(value.value?.takeIf { it.response.expiresAtEpochMillis > nowEpochMillis })
    override suspend fun storeResponse(response: PrivacyRegionResponse, nowEpochMillis: Long) = Result.success(AdvertisingPrivacyState(response, null, null).also { value.value = it })
    override suspend fun storeChoice(choice: AdvertisingConsentChoice, nowEpochMillis: Long): Result<AdvertisingPrivacyState> {
        val current = requireNotNull(value.value)
        return Result.success(current.copy(choice = choice, choicePolicyVersion = current.response.policyVersion).also { value.value = it })
    }
    override suspend fun clear() = Result.success(Unit).also { value.value = null }
}

internal class FakeSessionPrivacyApi(var response: Result<PrivacyRegionResponse>) : PrivacyRegionApi {
    var fetchCount = 0
    override suspend fun fetch(): Result<PrivacyRegionResponse> { fetchCount += 1; return response }
}

internal class FakeInlineAdController(
    override val platform: AdPlatform,
    override val hasProductionAdUnit: Boolean = true,
) : InlineAdController {
    var initializeCount = 0
    var requestCount = 0
    var disposeCount = 0
    override suspend fun initialize(userConsent: Boolean) = Result.success(Unit).also { initializeCount += 1 }
    override suspend fun requestInlineAd() = Result.success<InlineAdState>(InlineAdState.Ready).also { requestCount += 1 }
    override suspend fun dispose() = Result.success(Unit).also { disposeCount += 1 }
}

internal fun protectedSessionPrivacy(choice: AdvertisingConsentChoice? = null) = AdvertisingPrivacyState(
    response = PrivacyRegionResponse(1, true, "policy", Long.MAX_VALUE),
    choice = choice,
    choicePolicyVersion = choice?.let { "policy" },
)

internal fun completedSession(id: String, mode: SessionCompletionMode = SessionCompletionMode.AllPacked) = PackingSession(
    id = PackingSessionId(id),
    localDate = LocalDate(2026, 9, 8),
    status = PackingSessionStatus.Completed,
    completionMode = mode,
    createdAtMillis = 1L,
    completedAtMillis = 2L,
    revision = 1L,
    selectedTemplates = emptyList(),
    items = emptyList(),
)
