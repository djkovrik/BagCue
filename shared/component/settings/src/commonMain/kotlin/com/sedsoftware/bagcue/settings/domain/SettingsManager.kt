package com.sedsoftware.bagcue.settings.domain

import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyResolution
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyState
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreference
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreferenceRepository
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionApi
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.resolveAdvertisingPrivacy
import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationScheduler
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferencesRepository
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.reminder.ReminderScheduleResult
import com.sedsoftware.bagcue.domain.reminder.ReminderSetting
import com.sedsoftware.bagcue.domain.reminder.SessionReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.calculateReminderOccurrences
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

internal data class SettingsSnapshot(
    val preferences: ReminderPreferences,
    val isSessionOverride: Boolean,
    val analytics: AnalyticsPreference,
    val privacyState: AdvertisingPrivacyState?,
    val privacyResolution: AdvertisingPrivacyResolution,
)

internal data class SettingsSaveResult(
    val scheduleResult: ReminderScheduleResult,
)

internal data class AnalyticsStatus(val preference: AnalyticsPreference, val hasAppInstanceId: Boolean)
internal data class AnalyticsUpdateResult(
    val preference: AnalyticsPreference,
    val hasAppInstanceId: Boolean,
    val resetFailed: Boolean,
)

internal class SettingsManager(
    private val preferencesRepository: ReminderPreferencesRepository,
    private val sessionRepository: SessionHistoryRepository,
    private val scheduler: ReminderNotificationScheduler,
    private val analyticsRepository: AnalyticsPreferenceRepository,
    private val analyticsController: AnalyticsController,
    private val privacyRepository: AdvertisingPrivacyRepository,
    private val privacyApi: PrivacyRegionApi,
    private val inlineAdController: InlineAdController,
    private val now: () -> Instant,
    private val timeZone: () -> TimeZone,
) {
    fun observe(sessionId: PackingSessionId?): Flow<SettingsSnapshot> {
        val reminders = if (sessionId == null) {
            preferencesRepository.observeGlobalPreferences().map { it to false }
        } else {
            combine(
                preferencesRepository.observeGlobalPreferences(),
                preferencesRepository.observeSessionPreferences(sessionId),
            ) { global, session -> (session?.toPreferences() ?: global) to (session != null) }
        }
        return combine(reminders, analyticsRepository.observe(), privacyRepository.observe()) { reminder, analytics, privacy ->
            SettingsSnapshot(
                preferences = reminder.first,
                isSessionOverride = reminder.second,
                analytics = analytics,
                privacyState = privacy,
                privacyResolution = resolveAdvertisingPrivacy(privacy, now().toEpochMilliseconds()),
            )
        }
    }

    suspend fun analyticsStatus(): Result<AnalyticsStatus> = captureResult {
        val preference = analyticsRepository.read().getOrThrow()
        analyticsController.setCollectionEnabled(preference.enabled).getOrThrow()
        AnalyticsStatus(preference, analyticsController.readAppInstanceId().getOrThrow() != null)
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean): Result<AnalyticsUpdateResult> = captureResult {
        val saved = analyticsRepository.update(AnalyticsPreference(enabled)).getOrThrow()
        analyticsController.setCollectionEnabled(enabled).getOrElse { error ->
            if (enabled) analyticsRepository.update(AnalyticsPreference(false)).getOrThrow()
            throw error
        }
        val resetFailed = !enabled && analyticsController.resetAnalyticsData().isFailure
        AnalyticsUpdateResult(saved, analyticsController.readAppInstanceId().getOrNull() != null, resetFailed)
    }

    suspend fun refreshPrivacy(): Result<AdvertisingPrivacyResolution> = captureResult {
        val current = now().toEpochMilliseconds()
        val state = privacyRepository.readFresh(current).getOrThrow()
            ?: privacyRepository.storeResponse(privacyApi.fetch().getOrThrow(), current).getOrThrow()
        resolveAdvertisingPrivacy(state, current)
    }

    suspend fun chooseConsent(choice: AdvertisingConsentChoice): Result<AdvertisingPrivacyResolution> = captureResult {
        val current = now().toEpochMilliseconds()
        val fresh = privacyRepository.readFresh(current).getOrThrow()
            ?: throw IllegalStateException("Fresh advertising privacy state is required")
        if (!fresh.response.consentRequired) return@captureResult AdvertisingPrivacyResolution.Eligible(true)
        val stored = privacyRepository.storeChoice(choice, current).getOrThrow()
        if (choice == AdvertisingConsentChoice.Declined) inlineAdController.dispose().getOrThrow()
        resolveAdvertisingPrivacy(stored, current)
    }

    suspend fun permissionState(): Result<NotificationPermissionState> = captureResult {
        scheduler.permissionState().getOrThrow()
    }

    suspend fun setEnabled(sessionId: PackingSessionId?, kind: ReminderKind, enabled: Boolean): Result<SettingsSaveResult> =
        update(sessionId) { preferences -> preferences.with(kind, preferences.setting(kind).copy(enabled = enabled)) }

    suspend fun setTime(sessionId: PackingSessionId?, kind: ReminderKind, time: ReminderLocalTime): Result<SettingsSaveResult> =
        update(sessionId) { preferences -> preferences.with(kind, preferences.setting(kind).copy(time = time)) }

    suspend fun disableAll(sessionId: PackingSessionId?): Result<SettingsSaveResult> = update(sessionId) { preferences ->
        preferences.copy(
            evening = preferences.evening.copy(enabled = false),
            morning = preferences.morning.copy(enabled = false),
        )
    }

    suspend fun requestPermission(): Result<NotificationPermissionState> = captureResult {
        val state = scheduler.requestPermission().getOrThrow()
        if (state == NotificationPermissionState.Granted) reschedule(ReminderRescheduleReason.PermissionChanged).getOrThrow()
        state
    }

    suspend fun reschedule(reason: ReminderRescheduleReason): Result<ReminderScheduleResult> = captureResult {
        val global = preferencesRepository.readGlobalPreferences().getOrThrow()
        val planned = sessionRepository.readHistory().getOrThrow().planned
        val occurrences = planned.flatMap { session ->
            val preferences = preferencesRepository.readSessionPreferences(session.id).getOrThrow()
                ?: SessionReminderPreferences(session.id, global.evening, global.morning)
            calculateReminderOccurrences(session.id, session.localDate, preferences, now(), timeZone())
        }
        val reasons = if (reason == ReminderRescheduleReason.AppResumed) {
            (scheduler.consumePendingRescheduleReasons().getOrThrow() + reason).sortedBy(ReminderRescheduleReason::ordinal)
        } else {
            listOf(reason)
        }
        reasons.fold<ReminderRescheduleReason, ReminderScheduleResult>(ReminderScheduleResult.Synchronized(emptySet())) { _, current ->
            scheduler.synchronize(occurrences, current).getOrThrow()
        }
    }

    private suspend fun update(
        sessionId: PackingSessionId?,
        transform: (ReminderPreferences) -> ReminderPreferences,
    ): Result<SettingsSaveResult> = captureResult {
        if (sessionId == null) {
            val current = preferencesRepository.readGlobalPreferences().getOrThrow()
            preferencesRepository.updateGlobalPreferences(transform(current)).getOrThrow()
        } else {
            val current = preferencesRepository.readSessionPreferences(sessionId).getOrThrow()
                ?: preferencesRepository.copyGlobalPreferencesToSession(sessionId).getOrThrow()
            val changed = transform(current.toPreferences())
            preferencesRepository.updateSessionPreferences(
                SessionReminderPreferences(sessionId, changed.evening, changed.morning),
            ).getOrThrow()
        }
        SettingsSaveResult(reschedule(ReminderRescheduleReason.PreferencesChanged).getOrThrow()).also {
            analyticsController.record(AnalyticsEvent(AnalyticsEventName.ReminderConfigured)).exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
            }
        }
    }
}

private fun SessionReminderPreferences.toPreferences() = ReminderPreferences(evening, morning)
private fun ReminderPreferences.setting(kind: ReminderKind): ReminderSetting = if (kind == ReminderKind.Evening) evening else morning
private fun ReminderPreferences.with(kind: ReminderKind, setting: ReminderSetting): ReminderPreferences =
    if (kind == ReminderKind.Evening) copy(evening = setting) else copy(morning = setting)

private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
