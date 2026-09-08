package com.sedsoftware.bagcue.settings.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyResolution
import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.ReminderScheduleResult
import com.sedsoftware.bagcue.settings.domain.SettingsManager
import com.sedsoftware.bagcue.settings.domain.AnalyticsStatus
import com.sedsoftware.bagcue.settings.domain.SettingsSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SettingsStoreProvider(
    private val storeFactory: StoreFactory,
    private val manager: SettingsManager,
    private val sessionId: com.sedsoftware.bagcue.domain.session.PackingSessionId?,
    private val privacyPolicyUrl: String?,
    private val versionName: String,
) {
    fun provide(): SettingsStore = object : SettingsStore,
        com.arkivanov.mvikotlin.core.store.Store<SettingsStore.Intent, SettingsStore.State, Nothing> by storeFactory.create(
            name = "SettingsStore",
            initialState = SettingsStore.State(sessionId = sessionId, privacyPolicyUrl = privacyPolicyUrl, versionName = versionName),
            bootstrapper = SimpleBootstrapper(Action.Initialize),
            executorFactory = { ExecutorImpl(manager) },
            reducer = ReducerImpl,
            autoInit = false,
        ) {}

    private sealed interface Action { data object Initialize : Action }
    private sealed interface Msg {
        data class Loaded(val snapshot: SettingsSnapshot) : Msg
        data class PermissionChanged(val permission: NotificationPermissionState) : Msg
        data class AnalyticsChanged(val status: AnalyticsStatus) : Msg
        data class PrivacyChanged(val resolution: AdvertisingPrivacyResolution) : Msg
        data class PreferencesDrafted(val preferences: ReminderPreferences) : Msg
        data class AnalyticsDrafted(val enabled: Boolean) : Msg
        data class Failed(val error: SettingsStore.Error) : Msg
        data object Loading : Msg
        data object Saving : Msg
        data object Saved : Msg
        data object ErrorCleared : Msg
    }

    private class ExecutorImpl(private val manager: SettingsManager) :
        CoroutineExecutor<SettingsStore.Intent, Action, SettingsStore.State, Msg, Nothing>() {
        private var observation: Job? = null
        private val saveMutex = Mutex()

        override fun executeAction(action: Action) { if (action == Action.Initialize) refresh() }

        override fun executeIntent(intent: SettingsStore.Intent) = when (intent) {
            SettingsStore.Intent.Refresh -> refresh()
            is SettingsStore.Intent.SetEnabled -> {
                val current = state().preferences
                dispatch(Msg.PreferencesDrafted(if (intent.kind == ReminderKind.Evening) {
                    current.copy(evening = current.evening.copy(enabled = intent.enabled))
                } else {
                    current.copy(morning = current.morning.copy(enabled = intent.enabled))
                }))
                save { manager.setEnabled(state().sessionId, intent.kind, intent.enabled) }
            }
            is SettingsStore.Intent.SetTime -> {
                val current = state().preferences
                dispatch(Msg.PreferencesDrafted(if (intent.kind == ReminderKind.Evening) {
                    current.copy(evening = current.evening.copy(time = intent.time))
                } else {
                    current.copy(morning = current.morning.copy(time = intent.time))
                }))
                save { manager.setTime(state().sessionId, intent.kind, intent.time) }
            }
            SettingsStore.Intent.DisableAll -> {
                dispatch(Msg.PreferencesDrafted(state().preferences.copy(
                    evening = state().preferences.evening.copy(enabled = false),
                    morning = state().preferences.morning.copy(enabled = false),
                )))
                save { manager.disableAll(state().sessionId) }
            }
            SettingsStore.Intent.RequestPermission -> requestPermission()
            is SettingsStore.Intent.Reschedule -> reschedule(intent.reason)
            is SettingsStore.Intent.SetAnalyticsEnabled -> setAnalytics(intent.enabled)
            SettingsStore.Intent.RefreshPrivacy -> refreshPrivacy()
            is SettingsStore.Intent.ChooseConsent -> chooseConsent(intent.choice)
            SettingsStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)
        }

        private fun refresh() {
            observation?.cancel()
            dispatch(Msg.Loading)
            observation = scope.launch {
                launch {
                    manager.permissionState().unwrap(
                        onSuccess = { dispatch(Msg.PermissionChanged(it)) },
                        onFailure = { dispatch(Msg.Failed(SettingsStore.Error.LoadFailed)) },
                    )
                }
                launch {
                    manager.analyticsStatus().unwrap(
                        onSuccess = { dispatch(Msg.AnalyticsChanged(it)) },
                        onFailure = { dispatch(Msg.Failed(SettingsStore.Error.LoadFailed)) },
                    )
                }
                launch { refreshPrivacy() }
                try {
                    manager.observe(state().sessionId).collectLatest { dispatch(Msg.Loaded(it)) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    dispatch(Msg.Failed(SettingsStore.Error.LoadFailed))
                }
            }
        }

        private fun setAnalytics(enabled: Boolean) {
            dispatch(Msg.AnalyticsDrafted(enabled))
            scope.launch {
                dispatch(Msg.Saving)
                manager.setAnalyticsEnabled(enabled).unwrap(
                    onSuccess = { result ->
                        dispatch(Msg.AnalyticsChanged(AnalyticsStatus(result.preference, result.hasAppInstanceId)))
                        if (result.resetFailed) dispatch(Msg.Failed(SettingsStore.Error.AnalyticsResetFailed)) else dispatch(Msg.Saved)
                    },
                    onFailure = { dispatch(Msg.Failed(SettingsStore.Error.AnalyticsSaveFailed)) },
                )
            }
        }

        private fun refreshPrivacy() {
            scope.launch {
                manager.refreshPrivacy().unwrap(
                    onSuccess = { dispatch(Msg.PrivacyChanged(it)) },
                    onFailure = { dispatch(Msg.Failed(SettingsStore.Error.PrivacyRefreshFailed)) },
                )
            }
        }

        private fun chooseConsent(choice: com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice) {
            scope.launch {
                dispatch(Msg.Saving)
                manager.chooseConsent(choice).unwrap(
                    onSuccess = { dispatch(Msg.PrivacyChanged(it)); dispatch(Msg.Saved) },
                    onFailure = { dispatch(Msg.Failed(SettingsStore.Error.ConsentSaveFailed)) },
                )
            }
        }

        private fun save(operation: suspend () -> Result<com.sedsoftware.bagcue.settings.domain.SettingsSaveResult>) {
            scope.launch {
                saveMutex.withLock {
                    dispatch(Msg.Saving)
                    operation().unwrap(
                        onSuccess = { result -> dispatchScheduleResult(result.scheduleResult); dispatch(Msg.Saved) },
                        onFailure = { dispatch(Msg.Failed(SettingsStore.Error.SaveFailed)) },
                    )
                }
            }
        }

        private fun requestPermission() {
            scope.launch {
                manager.requestPermission().unwrap(
                    onSuccess = { dispatch(Msg.PermissionChanged(it)) },
                    onFailure = { dispatch(Msg.Failed(SettingsStore.Error.PermissionFailed)) },
                )
            }
        }

        private fun reschedule(reason: com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason) {
            scope.launch {
                manager.reschedule(reason).unwrap(
                    onSuccess = { dispatchScheduleResult(it) },
                    onFailure = { dispatch(Msg.Failed(SettingsStore.Error.RescheduleFailed)) },
                )
            }
        }

        private fun dispatchScheduleResult(result: ReminderScheduleResult) {
            when (result) {
                is ReminderScheduleResult.Synchronized -> Unit
                ReminderScheduleResult.PermissionDenied -> dispatch(Msg.PermissionChanged(NotificationPermissionState.Denied))
                ReminderScheduleResult.Unavailable -> dispatch(Msg.PermissionChanged(NotificationPermissionState.Unavailable))
            }
        }
    }

    private object ReducerImpl : Reducer<SettingsStore.State, Msg> {
        override fun SettingsStore.State.reduce(msg: Msg): SettingsStore.State = when (msg) {
            is Msg.Loaded -> copy(
                preferences = msg.snapshot.preferences,
                isSessionOverride = msg.snapshot.isSessionOverride,
                analytics = msg.snapshot.analytics,
                privacyState = msg.snapshot.privacyState,
                privacyResolution = msg.snapshot.privacyResolution,
                isLoading = false,
                error = null,
            )
            is Msg.PermissionChanged -> copy(permission = msg.permission, isLoading = false)
            is Msg.AnalyticsChanged -> copy(
                analytics = msg.status.preference,
                hasAppInstanceId = msg.status.hasAppInstanceId,
                isLoading = false,
            )
            is Msg.PrivacyChanged -> copy(privacyResolution = msg.resolution, isLoading = false)
            is Msg.PreferencesDrafted -> copy(preferences = msg.preferences, error = null)
            is Msg.AnalyticsDrafted -> copy(analytics = analytics.copy(enabled = msg.enabled), error = null)
            is Msg.Failed -> copy(isLoading = false, isSaving = false, error = msg.error)
            Msg.Loading -> copy(isLoading = true, error = null)
            Msg.Saving -> copy(isSaving = true, error = null)
            Msg.Saved -> copy(isSaving = false, error = null)
            Msg.ErrorCleared -> copy(error = null)
        }
    }
}

private inline fun <T> Result<T>.unwrap(onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    fold(onSuccess, onFailure = { error ->
        if (error is CancellationException) throw error
        onFailure(error)
    })
}
