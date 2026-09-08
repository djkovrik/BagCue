package com.sedsoftware.bagcue.platform.reminder

import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationId
import com.sedsoftware.bagcue.domain.reminder.ReminderOccurrence

data class ReminderScheduleDelta(
    val cancelIds: Set<ReminderNotificationId>,
    val schedule: List<ReminderOccurrence>,
)

private const val FNV_OFFSET_BASIS = 0x811c9dc5u
private const val FNV_PRIME = 0x01000193u

fun stableReminderRequestCode(id: ReminderNotificationId): Int {
    var hash = FNV_OFFSET_BASIS
    id.value.encodeToByteArray().forEach { byte ->
        hash = (hash xor byte.toUByte().toUInt()) * FNV_PRIME
    }
    return (hash and Int.MAX_VALUE.toUInt()).toInt()
}

fun reminderScheduleDelta(
    registeredIds: Set<ReminderNotificationId>,
    occurrences: List<ReminderOccurrence>,
    currentTimeMillis: Long,
): ReminderScheduleDelta = ReminderScheduleDelta(
    cancelIds = registeredIds,
    schedule = occurrences
        .filter { it.scheduledAtEpochMillis > currentTimeMillis }
        .associateBy { it.id }
        .values
        .sortedBy { it.scheduledAtEpochMillis },
)
