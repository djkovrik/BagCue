package com.sedsoftware.bagcue.settings.integration

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.settings.SettingsComponent

class SettingsComponentPreview(initialModel: SettingsComponent.Model) : SettingsComponent {
    override val model: Value<SettingsComponent.Model> = MutableValue(initialModel)
    override fun refresh() = Unit
    override fun setEnabled(kind: ReminderKind, enabled: Boolean) = Unit
    override fun setTime(kind: ReminderKind, time: ReminderLocalTime) = Unit
    override fun disableAll() = Unit
    override fun requestNotificationPermission() = Unit
    override fun reschedule(reason: ReminderRescheduleReason) = Unit
    override fun setAnalyticsEnabled(enabled: Boolean) = Unit
    override fun refreshAdvertisingPrivacy() = Unit
    override fun chooseAdvertisingConsent(choice: AdvertisingConsentChoice) = Unit
    override fun openPrivacyPolicy() = Unit
    override fun clearError() = Unit
}
