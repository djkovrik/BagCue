package com.sedsoftware.bagcue.domain.reminder

import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant

fun calculateReminderOccurrences(
    sessionId: PackingSessionId,
    sessionDate: LocalDate,
    preferences: SessionReminderPreferences,
    now: Instant,
    timeZone: TimeZone,
): List<ReminderOccurrence> {
    require(preferences.sessionId == sessionId)
    return listOfNotNull(
        preferences.evening.takeIf { it.enabled }?.toOccurrence(
            sessionId = sessionId,
            kind = ReminderKind.Evening,
            date = sessionDate.minus(1, DateTimeUnit.DAY),
            now = now,
            timeZone = timeZone,
        ),
        preferences.morning.takeIf { it.enabled }?.toOccurrence(
            sessionId = sessionId,
            kind = ReminderKind.Morning,
            date = sessionDate,
            now = now,
            timeZone = timeZone,
        ),
    )
}

fun stableReminderNotificationId(
    sessionId: PackingSessionId,
    kind: ReminderKind,
): ReminderNotificationId = ReminderNotificationId(
    "session:${sessionId.value}:${if (kind == ReminderKind.Evening) "evening" else "morning"}",
)

private fun ReminderSetting.toOccurrence(
    sessionId: PackingSessionId,
    kind: ReminderKind,
    date: LocalDate,
    now: Instant,
    timeZone: TimeZone,
): ReminderOccurrence? {
    val scheduledAt = LocalDateTime(date, LocalTime(time.hour, time.minute)).toInstant(timeZone)
    if (scheduledAt <= now) return null
    return ReminderOccurrence(
        id = stableReminderNotificationId(sessionId, kind),
        sessionId = sessionId,
        kind = kind,
        scheduledAtEpochMillis = scheduledAt.toEpochMilliseconds(),
    )
}
