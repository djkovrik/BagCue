package com.sedsoftware.bagcue.settings

import com.sedsoftware.bagcue.domain.apa.*
import com.sedsoftware.bagcue.domain.reminder.*
import com.sedsoftware.bagcue.domain.session.*
import com.sedsoftware.bagcue.domain.template.KitTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class FakeReminderPreferencesRepository : ReminderPreferencesRepository {
    val global = MutableStateFlow(ReminderPreferences())
    val sessions = MutableStateFlow<Map<PackingSessionId, SessionReminderPreferences>>(emptyMap())
    var globalUpdateFailure: Throwable? = null
    override fun observeGlobalPreferences(): Flow<ReminderPreferences> = global
    override fun observeSessionPreferences(sessionId: PackingSessionId) = sessions.map { it[sessionId] }
    override suspend fun readGlobalPreferences() = Result.success(global.value)
    override suspend fun updateGlobalPreferences(preferences: ReminderPreferences): Result<ReminderPreferences> {
        globalUpdateFailure?.let { return Result.failure(it) }
        return Result.success(preferences.also { global.value = it })
    }
    override suspend fun readSessionPreferences(sessionId: PackingSessionId) = Result.success(sessions.value[sessionId])
    override suspend fun copyGlobalPreferencesToSession(sessionId: PackingSessionId): Result<SessionReminderPreferences> {
        val value = SessionReminderPreferences(sessionId, global.value.evening, global.value.morning)
        sessions.value += sessionId to value
        return Result.success(value)
    }
    override suspend fun updateSessionPreferences(preferences: SessionReminderPreferences) =
        Result.success(preferences.also { sessions.value += it.sessionId to it })
}

internal class FakeReminderScheduler(
    var permission: NotificationPermissionState = NotificationPermissionState.Granted,
) : ReminderNotificationScheduler {
    val synchronized = mutableListOf<Pair<List<ReminderOccurrence>, ReminderRescheduleReason>>()
    val pending = mutableSetOf<ReminderRescheduleReason>()
    override suspend fun permissionState() = Result.success(permission)
    override suspend fun requestPermission() = Result.success(permission)
    override suspend fun consumePendingRescheduleReasons() = Result.success(pending.toSet().also { pending.clear() })
    override suspend fun synchronize(occurrences: List<ReminderOccurrence>, reason: ReminderRescheduleReason): Result<ReminderScheduleResult> {
        synchronized += occurrences to reason
        return Result.success(
            when (permission) {
                NotificationPermissionState.Denied -> ReminderScheduleResult.PermissionDenied
                NotificationPermissionState.Unavailable -> ReminderScheduleResult.Unavailable
                else -> ReminderScheduleResult.Synchronized(occurrences.map { it.id }.toSet())
            },
        )
    }
}

internal class FakeReminderSessionRepository(initial: List<PackingSession>) : SessionHistoryRepository {
    private val sessions = MutableStateFlow(initial)
    override fun observeSession(id: PackingSessionId) = sessions.map { values -> values.firstOrNull { it.id == id } }
    override fun observeHistory(localDate: LocalDate?) = sessions.map { values -> SessionHistory(values.filter { it.status == PackingSessionStatus.Active && (localDate == null || it.localDate == localDate) }, emptyList()) }
    override suspend fun readSession(id: PackingSessionId) = Result.success(sessions.value.firstOrNull { it.id == id })
    override suspend fun findSessionByDate(date: LocalDate) = Result.success(sessions.value.firstOrNull { it.localDate == date })
    override suspend fun readHistory(localDate: LocalDate?) = Result.success(SessionHistory(sessions.value.filter { it.status == PackingSessionStatus.Active && (localDate == null || it.localDate == localDate) }, emptyList()))
    override suspend fun createSession(session: PackingSession) = Result.success<CreateSessionResult>(CreateSessionResult.Created(session))
    override suspend fun replaceActiveSnapshot(session: PackingSession) = Result.success(session)
    override suspend fun restoreUndo(snapshot: SessionUndoSnapshot) = Result.success(snapshot.before)
    override suspend fun addOneOffItem(command: OneOffSessionItemCommand): Result<PackingSession> = error("Not used")
    override suspend fun removeItem(sessionId: PackingSessionId, itemId: SessionPackingItemId): Result<SessionMutation> = error("Not used")
    override suspend fun updateItem(sessionId: PackingSessionId, item: SessionPackingItem): Result<PackingSession> = error("Not used")
    override suspend fun completeSession(session: PackingSession) = Result.success(session)
    override suspend fun saveItemToTemplates(command: SaveSessionItemToTemplatesCommand) = Result.success(emptyList<KitTemplate>())
    override suspend fun reopenSession(id: PackingSessionId): Result<PackingSession> = error("Not used")
    override suspend fun prepareRepeat(id: PackingSessionId): Result<RepeatSessionSelection> = error("Not used")
    override suspend fun deleteSession(id: PackingSessionId): Result<SessionDeletion> = error("Not used")
    override suspend fun restoreDeletedSession(undo: DeletedSessionUndo): Result<CreateSessionResult> = error("Not used")
}

internal fun plannedSession(id: String, date: LocalDate) = PackingSession(
    id = PackingSessionId(id), localDate = date, status = PackingSessionStatus.Active,
    completionMode = null, createdAtMillis = 1L, completedAtMillis = null, revision = 0L,
    selectedTemplates = emptyList(), items = emptyList(),
)

internal class FakeAnalyticsPreferenceRepository : AnalyticsPreferenceRepository {
    val value = MutableStateFlow(AnalyticsPreference())
    override fun observe(): Flow<AnalyticsPreference> = value
    override suspend fun read() = Result.success(value.value)
    override suspend fun update(preference: AnalyticsPreference) = Result.success(preference.also { value.value = it })
}

internal class FakeAnalyticsController : AnalyticsController {
    var enabled = false
    var appInstanceId: String? = null
    var resetFails = false
    val events = mutableListOf<AnalyticsEvent>()
    override suspend fun setCollectionEnabled(enabled: Boolean) = Result.success(Unit).also { this.enabled = enabled }
    override suspend fun resetAnalyticsData() = if (resetFails) Result.failure(IllegalStateException("reset")) else Result.success(Unit).also { appInstanceId = null }
    override suspend fun readAppInstanceId() = Result.success(appInstanceId)
    override suspend fun record(event: AnalyticsEvent) = Result.success(Unit).also { events += event }
}

internal class FakeAdvertisingPrivacyRepository : AdvertisingPrivacyRepository {
    val value = MutableStateFlow<AdvertisingPrivacyState?>(null)
    override fun observe(): Flow<AdvertisingPrivacyState?> = value
    override suspend fun readFresh(nowEpochMillis: Long) = Result.success(value.value?.takeIf { it.response.expiresAtEpochMillis > nowEpochMillis })
    override suspend fun storeResponse(response: PrivacyRegionResponse, nowEpochMillis: Long) = Result.success(AdvertisingPrivacyState(response, null, null).also { value.value = it })
    override suspend fun storeChoice(choice: AdvertisingConsentChoice, nowEpochMillis: Long): Result<AdvertisingPrivacyState> {
        val current = requireNotNull(value.value)
        return Result.success(current.copy(choice = choice, choicePolicyVersion = current.response.policyVersion).also { value.value = it })
    }
    override suspend fun clear() = Result.success(Unit).also { value.value = null }
}

internal class FakePrivacyRegionApi(var response: Result<PrivacyRegionResponse>) : PrivacyRegionApi {
    override suspend fun fetch() = response
}

internal fun protectedPrivacyResponse() = PrivacyRegionResponse(1, true, "policy", Long.MAX_VALUE)

internal class FakeSettingsInlineAdController : InlineAdController {
    override val platform = AdPlatform.Android
    override val hasProductionAdUnit = true
    var disposeCount = 0
    override suspend fun initialize(userConsent: Boolean) = Result.success(Unit)
    override suspend fun requestInlineAd() = Result.success<InlineAdState>(InlineAdState.Ready)
    override suspend fun dispose() = Result.success(Unit).also { disposeCount += 1 }
}
