package com.sedsoftware.bagcue.settings

import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice

interface SettingsComponent {
    val model: Value<Model>

    fun refresh()
    fun setEnabled(kind: ReminderKind, enabled: Boolean)
    fun setTime(kind: ReminderKind, time: ReminderLocalTime)
    fun disableAll()
    fun requestNotificationPermission()
    fun reschedule(reason: ReminderRescheduleReason)
    fun setAnalyticsEnabled(enabled: Boolean)
    fun refreshAdvertisingPrivacy()
    fun chooseAdvertisingConsent(choice: AdvertisingConsentChoice)
    fun openPrivacyPolicy()
    fun clearError()

    data class Model(
        val scope: Scope,
        val evening: Reminder,
        val morning: Reminder,
        val notificationCapability: NotificationCapability,
        val analytics: Analytics = Analytics(false, false),
        val advertisingPrivacy: AdvertisingPrivacy = AdvertisingPrivacy(
            AdvertisingPrivacyStatus.Unresolved,
            choice = null,
            choiceAvailable = false,
        ),
        val privacyPolicyUrl: String? = null,
        val about: About = About(""),
        val isLoading: Boolean,
        val isSaving: Boolean,
        val error: Error?,
    )

    sealed interface Scope {
        data object Global : Scope
        data class Session(val sessionId: PackingSessionId) : Scope
    }

    data class Reminder(
        val kind: ReminderKind,
        val enabled: Boolean,
        val time: ReminderLocalTime,
        val source: Source,
    )

    data class Analytics(
        val enabled: Boolean,
        val hasAppInstanceId: Boolean,
    )

    data class AdvertisingPrivacy(
        val status: AdvertisingPrivacyStatus,
        val choice: AdvertisingConsentChoice?,
        val choiceAvailable: Boolean,
    )

    data class About(val versionName: String)

    enum class Source { GlobalDefault, SessionOverride }
    enum class NotificationCapability { Unknown, Available, Denied, Unavailable }
    enum class AdvertisingPrivacyStatus { Unresolved, NotRequired, ConsentRequired, Allowed, Declined }
    enum class Error {
        LoadFailed, SaveFailed, PermissionFailed, RescheduleFailed,
        AnalyticsSaveFailed, AnalyticsResetFailed, PrivacyRefreshFailed, ConsentSaveFailed,
    }
}
