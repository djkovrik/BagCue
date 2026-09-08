package com.sedsoftware.bagcue.settings.integration

import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyResolution
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.settings.store.SettingsStore

internal fun SettingsStore.State.toComponentModel() = SettingsComponent.Model(
    scope = sessionId?.let(SettingsComponent.Scope::Session) ?: SettingsComponent.Scope.Global,
    evening = SettingsComponent.Reminder(
        kind = ReminderKind.Evening,
        enabled = preferences.evening.enabled,
        time = preferences.evening.time,
        source = source(),
    ),
    morning = SettingsComponent.Reminder(
        kind = ReminderKind.Morning,
        enabled = preferences.morning.enabled,
        time = preferences.morning.time,
        source = source(),
    ),
    notificationCapability = when (permission) {
        NotificationPermissionState.Unknown -> SettingsComponent.NotificationCapability.Unknown
        NotificationPermissionState.Granted -> SettingsComponent.NotificationCapability.Available
        NotificationPermissionState.Denied -> SettingsComponent.NotificationCapability.Denied
        NotificationPermissionState.Unavailable -> SettingsComponent.NotificationCapability.Unavailable
    },
    analytics = SettingsComponent.Analytics(
        enabled = analytics.enabled,
        hasAppInstanceId = hasAppInstanceId,
    ),
    advertisingPrivacy = SettingsComponent.AdvertisingPrivacy(
        status = when (privacyResolution) {
            AdvertisingPrivacyResolution.Unresolved -> SettingsComponent.AdvertisingPrivacyStatus.Unresolved
            is AdvertisingPrivacyResolution.ConsentRequired -> SettingsComponent.AdvertisingPrivacyStatus.ConsentRequired
            is AdvertisingPrivacyResolution.Eligible -> if (privacyState?.response?.consentRequired == true) {
                SettingsComponent.AdvertisingPrivacyStatus.Allowed
            } else {
                SettingsComponent.AdvertisingPrivacyStatus.NotRequired
            }
            AdvertisingPrivacyResolution.Declined -> SettingsComponent.AdvertisingPrivacyStatus.Declined
        },
        choice = privacyState?.choice,
        choiceAvailable = privacyState?.response?.consentRequired == true && privacyResolution !is AdvertisingPrivacyResolution.Unresolved,
    ),
    privacyPolicyUrl = privacyPolicyUrl,
    about = SettingsComponent.About(versionName),
    isLoading = isLoading,
    isSaving = isSaving,
    error = error?.let {
        when (it) {
            SettingsStore.Error.LoadFailed -> SettingsComponent.Error.LoadFailed
            SettingsStore.Error.SaveFailed -> SettingsComponent.Error.SaveFailed
            SettingsStore.Error.PermissionFailed -> SettingsComponent.Error.PermissionFailed
            SettingsStore.Error.RescheduleFailed -> SettingsComponent.Error.RescheduleFailed
            SettingsStore.Error.AnalyticsSaveFailed -> SettingsComponent.Error.AnalyticsSaveFailed
            SettingsStore.Error.AnalyticsResetFailed -> SettingsComponent.Error.AnalyticsResetFailed
            SettingsStore.Error.PrivacyRefreshFailed -> SettingsComponent.Error.PrivacyRefreshFailed
            SettingsStore.Error.ConsentSaveFailed -> SettingsComponent.Error.ConsentSaveFailed
        }
    },
)

private fun SettingsStore.State.source(): SettingsComponent.Source =
    if (sessionId != null && isSessionOverride) SettingsComponent.Source.SessionOverride else SettingsComponent.Source.GlobalDefault
