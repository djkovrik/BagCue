package com.sedsoftware.bagcue.settings.domain

import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyResolution
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.apa.resolveAdvertisingPrivacy
import com.sedsoftware.bagcue.domain.reminder.*
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.settings.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SettingsManagerTest {
    private val preferences = FakeReminderPreferencesRepository()
    private val scheduler = FakeReminderScheduler()
    private val analyticsRepository = FakeAnalyticsPreferenceRepository()
    private val analyticsController = FakeAnalyticsController()
    private val privacyRepository = FakeAdvertisingPrivacyRepository()
    private val privacyApi = FakePrivacyRegionApi(Result.success(protectedPrivacyResponse()))
    private val inlineAdController = FakeSettingsInlineAdController()
    private val session = plannedSession("session", LocalDate(2026, 9, 10))
    private val manager = SettingsManager(
        preferences, FakeReminderSessionRepository(listOf(session)), scheduler,
        analyticsRepository, analyticsController, privacyRepository, privacyApi, inlineAdController,
        { Instant.parse("2026-09-08T10:00:00Z") }, { TimeZone.UTC },
    )

    @Test
    fun defaultsAreDisabledWithSuggestedLocalTimes() = runTest {
        val snapshot = manager.observe(null).first()
        assertFalse(snapshot.preferences.evening.enabled)
        assertEquals(ReminderLocalTime.of(20, 0), snapshot.preferences.evening.time)
        assertFalse(snapshot.preferences.morning.enabled)
        assertEquals(ReminderLocalTime.of(7, 30), snapshot.preferences.morning.time)
    }

    @Test
    fun sessionFallsBackThenCreatesItsOwnOverride() = runTest {
        assertFalse(manager.observe(session.id).first().isSessionOverride)
        manager.setEnabled(session.id, ReminderKind.Morning, true).getOrThrow()
        assertTrue(manager.observe(session.id).first().isSessionOverride)
        assertTrue(preferences.sessions.value.getValue(session.id).morning.enabled)
        assertEquals(AnalyticsEventName.ReminderConfigured, analyticsController.events.single().name)
    }

    @Test
    fun saveReschedulesFutureOccurrencesWithStableIds() = runTest {
        manager.setEnabled(null, ReminderKind.Morning, true).getOrThrow()
        val occurrences = scheduler.synchronized.last().first
        assertEquals(1, occurrences.size)
        assertEquals("session:session:morning", occurrences.single().id.value)
    }

    @Test
    fun deniedPermissionIsSuccessfulNonBlockingState() = runTest {
        scheduler.permission = NotificationPermissionState.Denied
        val result = manager.setEnabled(null, ReminderKind.Evening, true).getOrThrow().scheduleResult
        assertIs<ReminderScheduleResult.PermissionDenied>(result)
        assertTrue(preferences.global.value.evening.enabled)
    }

    @Test
    fun appResumeConsumesPendingRescheduleReasonsWithoutDuplicateIds() = runTest {
        preferences.updateGlobalPreferences(ReminderPreferences(morning = ReminderSetting(true, ReminderLocalTime.of(7, 30)))).getOrThrow()
        scheduler.pending += ReminderRescheduleReason.TimeZoneChanged
        manager.reschedule(ReminderRescheduleReason.AppResumed).getOrThrow()
        assertEquals(listOf(ReminderRescheduleReason.AppResumed, ReminderRescheduleReason.TimeZoneChanged), scheduler.synchronized.takeLast(2).map { it.second }.sortedBy { it.ordinal })
        assertEquals(1, scheduler.synchronized.last().first.map { it.id }.distinct().size)
    }

    @Test
    fun analyticsDefaultsDisabledAndResetFailureDoesNotReenableCollection() = runTest {
        assertFalse(manager.analyticsStatus().getOrThrow().preference.enabled)
        analyticsController.appInstanceId = "instance"
        analyticsController.resetFails = true
        val result = manager.setAnalyticsEnabled(false).getOrThrow()
        assertTrue(result.resetFailed)
        assertFalse(analyticsRepository.value.value.enabled)
        assertFalse(analyticsController.enabled)
    }

    @Test
    fun protectedPrivacyChoiceIsVersionBoundAndDeclineRemainsIneligible() = runTest {
        manager.refreshPrivacy().getOrThrow()
        assertIs<AdvertisingPrivacyResolution.Declined>(manager.chooseConsent(AdvertisingConsentChoice.Declined).getOrThrow())
        assertIs<AdvertisingPrivacyResolution.Declined>(
            resolveAdvertisingPrivacy(privacyRepository.value.value, Instant.parse("2026-09-08T10:00:00Z").toEpochMilliseconds()),
        )
        assertEquals("policy", privacyRepository.value.value?.choicePolicyVersion)
        assertEquals(1, inlineAdController.disposeCount)
    }

    @Test
    fun privacyEndpointFailureLeavesAdvertisingUnresolved() = runTest {
        privacyApi.response = Result.failure(IllegalStateException("offline"))
        assertTrue(manager.refreshPrivacy().isFailure)
        assertIs<AdvertisingPrivacyResolution.Unresolved>(
            resolveAdvertisingPrivacy(privacyRepository.value.value, Instant.parse("2026-09-08T10:00:00Z").toEpochMilliseconds()),
        )
    }

    @Test
    fun reminderSaveFailurePreservesOriginalCauseAndPriorDurablePreference() = runTest {
        val before = preferences.global.value
        val failure = IllegalStateException("settings write failed")
        preferences.globalUpdateFailure = failure

        val result = manager.setTime(null, ReminderKind.Morning, ReminderLocalTime.of(9, 45))

        assertSame(failure, result.exceptionOrNull())
        assertEquals(before, preferences.global.value)
    }

    @Test
    fun reminderSaveCancellationFromRepositoryResultIsRethrown() = runTest {
        val before = preferences.global.value
        val cancellation = CancellationException("cancel settings write")
        preferences.globalUpdateFailure = cancellation

        assertSame(
            cancellation,
            assertFailsWith<CancellationException> {
                manager.setEnabled(null, ReminderKind.Evening, true)
            },
        )
        assertEquals(before, preferences.global.value)
    }
}
