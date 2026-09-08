package com.sedsoftware.bagcue.settings.store

import com.arkivanov.mvikotlin.core.store.Store
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyResolution
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyState
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreference
import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.session.PackingSessionId

internal interface SettingsStore : Store<SettingsStore.Intent, SettingsStore.State, Nothing> {
    sealed interface Intent {
        data object Refresh : Intent
        data class SetEnabled(val kind: ReminderKind, val enabled: Boolean) : Intent
        data class SetTime(val kind: ReminderKind, val time: ReminderLocalTime) : Intent
        data object DisableAll : Intent
        data object RequestPermission : Intent
        data class Reschedule(val reason: ReminderRescheduleReason) : Intent
        data class SetAnalyticsEnabled(val enabled: Boolean) : Intent
        data object RefreshPrivacy : Intent
        data class ChooseConsent(val choice: AdvertisingConsentChoice) : Intent
        data object ClearError : Intent
    }

    data class State(
        val sessionId: PackingSessionId?,
        val preferences: ReminderPreferences = ReminderPreferences(),
        val isSessionOverride: Boolean = false,
        val permission: NotificationPermissionState = NotificationPermissionState.Unknown,
        val analytics: AnalyticsPreference = AnalyticsPreference(),
        val hasAppInstanceId: Boolean = false,
        val privacyState: AdvertisingPrivacyState? = null,
        val privacyResolution: AdvertisingPrivacyResolution = AdvertisingPrivacyResolution.Unresolved,
        val privacyPolicyUrl: String? = null,
        val versionName: String = "",
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val error: Error? = null,
    )

    enum class Error {
        LoadFailed, SaveFailed, PermissionFailed, RescheduleFailed,
        AnalyticsSaveFailed, AnalyticsResetFailed, PrivacyRefreshFailed, ConsentSaveFailed,
    }
}
