package com.sedsoftware.bagcue.settings.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreferenceRepository
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionApi
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationScheduler
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferencesRepository
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.settings.domain.SettingsManager
import com.sedsoftware.bagcue.settings.store.SettingsStore
import com.sedsoftware.bagcue.settings.store.SettingsStoreProvider
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class SettingsComponentDefault(
    componentContext: ComponentContext,
    preferencesRepository: ReminderPreferencesRepository,
    sessionRepository: SessionHistoryRepository,
    scheduler: ReminderNotificationScheduler,
    analyticsRepository: AnalyticsPreferenceRepository,
    analyticsController: AnalyticsController,
    privacyRepository: AdvertisingPrivacyRepository,
    privacyApi: PrivacyRegionApi,
    inlineAdController: InlineAdController,
    storeFactory: StoreFactory,
    sessionId: PackingSessionId? = null,
    now: () -> Instant,
    timeZone: () -> TimeZone,
    private val privacyPolicyUrl: String?,
    versionName: String,
    private val onOpenPrivacyPolicy: (String) -> Unit,
) : SettingsComponent, ComponentContext by componentContext {
    private val holder = instanceKeeper.getOrCreate(key = STORE_KEY + (sessionId?.value ?: "global")) {
        val manager = SettingsManager(
            preferencesRepository, sessionRepository, scheduler,
            analyticsRepository, analyticsController, privacyRepository, privacyApi, inlineAdController,
            now, timeZone,
        )
        StoreHolder(SettingsStoreProvider(storeFactory, manager, sessionId, privacyPolicyUrl, versionName).provide().also { it.init() })
    }
    private val store = holder.store

    override val model: Value<SettingsComponent.Model> = store.asValue().map(SettingsStore.State::toComponentModel)
    override fun refresh() = store.accept(SettingsStore.Intent.Refresh)
    override fun setEnabled(kind: ReminderKind, enabled: Boolean) = store.accept(SettingsStore.Intent.SetEnabled(kind, enabled))
    override fun setTime(kind: ReminderKind, time: ReminderLocalTime) = store.accept(SettingsStore.Intent.SetTime(kind, time))
    override fun disableAll() = store.accept(SettingsStore.Intent.DisableAll)
    override fun requestNotificationPermission() = store.accept(SettingsStore.Intent.RequestPermission)
    override fun reschedule(reason: ReminderRescheduleReason) = store.accept(SettingsStore.Intent.Reschedule(reason))
    override fun setAnalyticsEnabled(enabled: Boolean) = store.accept(SettingsStore.Intent.SetAnalyticsEnabled(enabled))
    override fun refreshAdvertisingPrivacy() = store.accept(SettingsStore.Intent.RefreshPrivacy)
    override fun chooseAdvertisingConsent(choice: AdvertisingConsentChoice) = store.accept(SettingsStore.Intent.ChooseConsent(choice))
    override fun openPrivacyPolicy() = privacyPolicyUrl?.let(onOpenPrivacyPolicy) ?: Unit
    override fun clearError() = store.accept(SettingsStore.Intent.ClearError)

    private class StoreHolder(val store: SettingsStore) : InstanceKeeper.Instance {
        override fun onDestroy() = store.dispose()
    }

    private companion object { const val STORE_KEY = "SettingsStore:" }
}
