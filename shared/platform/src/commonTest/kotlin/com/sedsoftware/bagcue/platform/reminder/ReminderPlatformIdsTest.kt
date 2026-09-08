package com.sedsoftware.bagcue.platform.reminder

import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationId
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderOccurrence
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReminderPlatformIdsTest {
    @Test
    fun requestCodeIsStableForTheSameLanguageNeutralNotificationId() {
        val id = ReminderNotificationId("session:stable-id:morning")

        assertEquals(stableReminderRequestCode(id), stableReminderRequestCode(id))
        assertNotEquals(
            stableReminderRequestCode(id),
            stableReminderRequestCode(ReminderNotificationId("session:stable-id:evening")),
        )
    }

    @Test
    fun synchronizationReplacesRegisteredIdsSkipsPastAndDeduplicatesRequests() {
        val id = ReminderNotificationId("session:stable-id:morning")
        val old = ReminderNotificationId("session:old-id:morning")
        val occurrence = ReminderOccurrence(id, PackingSessionId("stable-id"), ReminderKind.Morning, 200)

        val delta = reminderScheduleDelta(
            registeredIds = setOf(old, id),
            occurrences = listOf(
                occurrence.copy(scheduledAtEpochMillis = 50),
                occurrence,
                occurrence,
            ),
            currentTimeMillis = 100,
        )

        assertEquals(setOf(old, id), delta.cancelIds)
        assertEquals(listOf(occurrence), delta.schedule)
    }
}
