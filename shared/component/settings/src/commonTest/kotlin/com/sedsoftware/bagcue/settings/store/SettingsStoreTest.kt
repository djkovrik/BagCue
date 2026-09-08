package com.sedsoftware.bagcue.settings.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.domain.reminder.*
import com.sedsoftware.bagcue.settings.*
import com.sedsoftware.bagcue.settings.domain.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun immediateSaveKeepsCoreSettingsAvailableWhenPermissionIsDenied() = runTest(dispatcher) {
        val preferences = FakeReminderPreferencesRepository()
        val scheduler = FakeReminderScheduler(NotificationPermissionState.Denied)
        val manager = SettingsManager(
            preferences, FakeReminderSessionRepository(listOf(plannedSession("session", LocalDate(2026, 9, 10)))), scheduler,
            FakeAnalyticsPreferenceRepository(), FakeAnalyticsController(), FakeAdvertisingPrivacyRepository(),
            FakePrivacyRegionApi(Result.success(protectedPrivacyResponse())), FakeSettingsInlineAdController(),
            { Instant.parse("2026-09-08T10:00:00Z") }, { TimeZone.UTC },
        )
        val store = SettingsStoreProvider(DefaultStoreFactory(), manager, null, null, "test").provide()
        store.init()
        advanceUntilIdle()
        store.accept(SettingsStore.Intent.SetEnabled(ReminderKind.Morning, true))
        advanceUntilIdle()
        assertTrue(store.state.preferences.morning.enabled)
        assertEquals(NotificationPermissionState.Denied, store.state.permission)
        assertNull(store.state.error)
        store.dispose()
    }

    @Test
    fun failedSaveKeepsRequestedSettingVisibleWhileDurableValueStaysAuthoritativeAndRetryCommits() = runTest(dispatcher) {
        val preferences = FakeReminderPreferencesRepository()
        val manager = SettingsManager(
            preferences, FakeReminderSessionRepository(emptyList()), FakeReminderScheduler(),
            FakeAnalyticsPreferenceRepository(), FakeAnalyticsController(), FakeAdvertisingPrivacyRepository(),
            FakePrivacyRegionApi(Result.success(protectedPrivacyResponse())), FakeSettingsInlineAdController(),
            { Instant.parse("2026-09-08T10:00:00Z") }, { TimeZone.UTC },
        )
        val store = SettingsStoreProvider(DefaultStoreFactory(), manager, null, null, "test").provide()
        try {
            store.init()
            advanceUntilIdle()
            preferences.globalUpdateFailure = IllegalStateException("forced settings failure")

            store.accept(SettingsStore.Intent.SetEnabled(ReminderKind.Morning, true))
            advanceUntilIdle()

            assertEquals(SettingsStore.Error.SaveFailed, store.state.error)
            assertTrue(store.state.preferences.morning.enabled)
            assertFalse(preferences.global.value.morning.enabled)

            preferences.globalUpdateFailure = null
            store.accept(SettingsStore.Intent.SetEnabled(ReminderKind.Morning, true))
            advanceUntilIdle()

            assertNull(store.state.error)
            assertTrue(store.state.preferences.morning.enabled)
            assertTrue(preferences.global.value.morning.enabled)
        } finally {
            store.dispose()
        }
    }
}
