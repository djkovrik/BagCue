package com.sedsoftware.bagcue.history

import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.DeletedSessionUndo
import com.sedsoftware.bagcue.domain.session.OneOffSessionItemCommand
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.RepeatSessionSelection
import com.sedsoftware.bagcue.domain.session.SaveSessionItemToTemplatesCommand
import com.sedsoftware.bagcue.domain.session.SessionDeletion
import com.sedsoftware.bagcue.domain.session.SessionHistory
import com.sedsoftware.bagcue.domain.session.SessionMutation
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot
import com.sedsoftware.bagcue.domain.session.SessionTemplateSnapshot
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.session.reopenCompletedSession
import com.sedsoftware.bagcue.domain.session.repeatSessionSelection
import com.sedsoftware.bagcue.domain.session.sessionHistory
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.CancellationException

internal class FakeHistoryRepository(
    initial: List<PackingSession>,
    private val failOnReopen: Boolean = false,
    private val cancelOnReopen: Boolean = false,
) : SessionHistoryRepository {
    val sessions = MutableStateFlow(initial)
    override fun observeSession(id: PackingSessionId): Flow<PackingSession?> = sessions.map { all -> all.firstOrNull { it.id == id } }
    override fun observeHistory(localDate: LocalDate?): Flow<SessionHistory> = sessions.map { sessionHistory(it, localDate) }
    override suspend fun readSession(id: PackingSessionId) = Result.success(sessions.value.firstOrNull { it.id == id })
    override suspend fun findSessionByDate(date: LocalDate) = Result.success(sessions.value.firstOrNull { it.localDate == date })
    override suspend fun readHistory(localDate: LocalDate?) = Result.success(sessionHistory(sessions.value, localDate))
    override suspend fun createSession(session: PackingSession): Result<CreateSessionResult> {
        val occupied = sessions.value.firstOrNull { it.localDate == session.localDate }
        if (occupied != null) return Result.success(CreateSessionResult.DateOccupied(occupied))
        sessions.value += session
        return Result.success(CreateSessionResult.Created(session))
    }
    override suspend fun replaceActiveSnapshot(session: PackingSession) = save(session)
    override suspend fun restoreUndo(snapshot: SessionUndoSnapshot) = save(snapshot.before)
    override suspend fun addOneOffItem(command: OneOffSessionItemCommand): Result<PackingSession> = error("Not used")
    override suspend fun removeItem(sessionId: PackingSessionId, itemId: SessionPackingItemId): Result<SessionMutation> = error("Not used")
    override suspend fun updateItem(sessionId: PackingSessionId, item: SessionPackingItem): Result<PackingSession> = error("Not used")
    override suspend fun completeSession(session: PackingSession) = save(session)
    override suspend fun reopenSession(id: PackingSessionId): Result<PackingSession> {
        if (cancelOnReopen) throw CancellationException("cancel reopen")
        if (failOnReopen) return Result.failure(IllegalStateException("reopen failed"))
        return save(reopenCompletedSession(sessions.value.first { it.id == id }))
    }
    override suspend fun prepareRepeat(id: PackingSessionId): Result<RepeatSessionSelection> = Result.success(repeatSessionSelection(sessions.value.first { it.id == id }))
    override suspend fun deleteSession(id: PackingSessionId): Result<SessionDeletion> {
        val deleted = sessions.value.first { it.id == id }
        sessions.value = sessions.value.filterNot { it.id == id }
        return Result.success(SessionDeletion(deleted, DeletedSessionUndo(deleted)))
    }
    override suspend fun restoreDeletedSession(undo: DeletedSessionUndo): Result<CreateSessionResult> = createSession(undo.session)
    override suspend fun saveItemToTemplates(command: SaveSessionItemToTemplatesCommand) = Result.success(emptyList<KitTemplate>())
    private fun save(session: PackingSession): Result<PackingSession> {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
        return Result.success(session)
    }
}

internal class RecordingHistoryAnalyticsController : AnalyticsController {
    val events = mutableListOf<AnalyticsEvent>()

    override suspend fun setCollectionEnabled(enabled: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun resetAnalyticsData(): Result<Unit> = Result.success(Unit)
    override suspend fun readAppInstanceId(): Result<String?> = Result.success(null)
    override suspend fun record(event: AnalyticsEvent): Result<Unit> = Result.success(Unit).also {
        events += event
    }
}

internal fun historySession(
    id: String,
    date: LocalDate,
    completed: Boolean,
    templateIds: List<String> = listOf("template"),
) = PackingSession(
    id = PackingSessionId(id),
    localDate = date,
    status = if (completed) PackingSessionStatus.Completed else PackingSessionStatus.Active,
    completionMode = SessionCompletionMode.AllPacked.takeIf { completed },
    createdAtMillis = 1,
    completedAtMillis = 2L.takeIf { completed },
    revision = 0,
    selectedTemplates = templateIds.mapIndexed { index, templateId ->
        SessionTemplateSnapshot(
            id = KitTemplateId(templateId),
            name = SnapshotText(ResourceKey("starter_$templateId"), null),
            sortOrder = index.toLong(),
        )
    },
    items = emptyList(),
)
