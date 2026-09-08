package com.sedsoftware.bagcue.domain.reminder

import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlin.jvm.JvmInline

@JvmInline
value class ReminderLocalTime private constructor(val minuteOfDay: Int) {
    val hour: Int get() = minuteOfDay / MINUTES_PER_HOUR
    val minute: Int get() = minuteOfDay % MINUTES_PER_HOUR

    override fun toString(): String = hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')

    companion object {
        fun of(hour: Int, minute: Int): ReminderLocalTime {
            require(hour in 0 until HOURS_PER_DAY) { "Hour must be between 0 and 23" }
            require(minute in 0 until MINUTES_PER_HOUR) { "Minute must be between 0 and 59" }
            return ReminderLocalTime(hour * MINUTES_PER_HOUR + minute)
        }

        fun parse(value: String): ReminderLocalTime {
            val parts = value.split(':')
            require(parts.size == 2) { "Reminder time must use HH:mm" }
            return of(parts[0].toInt(), parts[1].toInt())
        }

        const val HOURS_PER_DAY = 24
        const val MINUTES_PER_HOUR = 60
    }
}

data class ReminderSetting(
    val enabled: Boolean,
    val time: ReminderLocalTime,
)

data class ReminderPreferences(
    val evening: ReminderSetting = ReminderSetting(false, ReminderLocalTime.of(DEFAULT_EVENING_HOUR, 0)),
    val morning: ReminderSetting = ReminderSetting(
        false,
        ReminderLocalTime.of(DEFAULT_MORNING_HOUR, DEFAULT_MORNING_MINUTE),
    ),
)

private const val DEFAULT_EVENING_HOUR = 20
private const val DEFAULT_MORNING_HOUR = 7
private const val DEFAULT_MORNING_MINUTE = 30

data class SessionReminderPreferences(
    val sessionId: PackingSessionId,
    val evening: ReminderSetting,
    val morning: ReminderSetting,
)

enum class ReminderKind { Evening, Morning }

@JvmInline
value class ReminderNotificationId(val value: String) {
    init { require(value.isNotBlank()) { "Reminder notification ID must not be blank" } }
}

data class ReminderOccurrence(
    val id: ReminderNotificationId,
    val sessionId: PackingSessionId,
    val kind: ReminderKind,
    val scheduledAtEpochMillis: Long,
)

enum class NotificationPermissionState { Unknown, Granted, Denied, Unavailable }

enum class ReminderRescheduleReason {
    PreferencesChanged,
    AppResumed,
    TimeZoneChanged,
    BootCompleted,
    PackageReplaced,
    PermissionChanged,
}

sealed interface ReminderScheduleResult {
    data class Synchronized(val scheduledIds: Set<ReminderNotificationId>) : ReminderScheduleResult
    data object PermissionDenied : ReminderScheduleResult
    data object Unavailable : ReminderScheduleResult
}
