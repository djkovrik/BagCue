package com.sedsoftware.bagcue.domain.reminder

import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderRulesTest {
    private val sessionId = PackingSessionId("session-2026-09-10")
    private val preferences = SessionReminderPreferences(
        sessionId = sessionId,
        evening = ReminderSetting(true, ReminderLocalTime.of(20, 0)),
        morning = ReminderSetting(true, ReminderLocalTime.of(7, 30)),
    )

    @Test
    fun defaultsAreDisabledWithSuggestedLocalTimes() {
        val defaults = ReminderPreferences()

        assertEquals(ReminderSetting(false, ReminderLocalTime.of(20, 0)), defaults.evening)
        assertEquals(ReminderSetting(false, ReminderLocalTime.of(7, 30)), defaults.morning)
    }

    @Test
    fun occurrencesUsePreviousEveningAndSessionMorningInTheCurrentZone() {
        val zone = TimeZone.of("Europe/Moscow")
        val occurrences = calculateReminderOccurrences(
            sessionId,
            LocalDate(2026, 9, 10),
            preferences,
            Instant.parse("2026-09-08T00:00:00Z"),
            zone,
        )

        val local = occurrences.map { Instant.fromEpochMilliseconds(it.scheduledAtEpochMillis).toLocalDateTime(zone) }
        assertEquals(LocalDate(2026, 9, 9), local[0].date)
        assertEquals(20, local[0].hour)
        assertEquals(LocalDate(2026, 9, 10), local[1].date)
        assertEquals(7, local[1].hour)
        assertEquals(30, local[1].minute)
    }

    @Test
    fun zoneChangeRecomputesInstantsButRetainsStableNotificationIdsAndWallTimes() {
        val date = LocalDate(2026, 11, 2)
        val now = Instant.parse("2026-10-30T00:00:00Z")
        val utc = calculateReminderOccurrences(sessionId, date, preferences, now, TimeZone.UTC)
        val newYork = calculateReminderOccurrences(sessionId, date, preferences, now, TimeZone.of("America/New_York"))

        assertEquals(utc.map { it.id }, newYork.map { it.id })
        assertTrue(utc.map { it.scheduledAtEpochMillis } != newYork.map { it.scheduledAtEpochMillis })
        assertEquals(
            ReminderNotificationId("session:session-2026-09-10:evening"),
            stableReminderNotificationId(sessionId, ReminderKind.Evening),
        )
    }

    @Test
    fun pastOccurrencesAreSkippedWithoutDisablingFutureOnes() {
        val occurrences = calculateReminderOccurrences(
            sessionId,
            LocalDate(2026, 9, 10),
            preferences,
            Instant.parse("2026-09-09T18:00:00Z"),
            TimeZone.of("Europe/Moscow"),
        )

        assertEquals(listOf(ReminderKind.Morning), occurrences.map { it.kind })
    }
}
