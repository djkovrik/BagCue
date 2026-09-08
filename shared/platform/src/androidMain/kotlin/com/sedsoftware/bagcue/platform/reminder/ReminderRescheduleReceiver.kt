package com.sedsoftware.bagcue.platform.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ReminderRescheduleReason.BootCompleted
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> ReminderRescheduleReason.TimeZoneChanged
            Intent.ACTION_MY_PACKAGE_REPLACED -> ReminderRescheduleReason.PackageReplaced
            else -> return
        }
        val state = context.getSharedPreferences(AndroidReminderNotificationScheduler.STATE_NAME, Context.MODE_PRIVATE)
        val updated = state.getStringSet(AndroidReminderNotificationScheduler.KEY_PENDING_REASONS, emptySet())
            .orEmpty() + reason.name
        state.edit().putStringSet(AndroidReminderNotificationScheduler.KEY_PENDING_REASONS, updated).apply()
    }
}
