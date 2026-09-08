package com.sedsoftware.bagcue.platform.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationId
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationScheduler
import com.sedsoftware.bagcue.domain.reminder.ReminderOccurrence
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.reminder.ReminderScheduleResult
import kotlinx.coroutines.CancellationException

class AndroidReminderNotificationScheduler(
    context: Context,
    private val permissionRequester: suspend () -> NotificationPermissionState,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ReminderNotificationScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val state = appContext.getSharedPreferences(STATE_NAME, Context.MODE_PRIVATE)

    override suspend fun permissionState(): Result<NotificationPermissionState> = captureResult {
        permissionStateDirect()
    }

    override suspend fun requestPermission(): Result<NotificationPermissionState> = captureResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) NotificationPermissionState.Granted
        else permissionRequester()
    }

    override suspend fun consumePendingRescheduleReasons(): Result<Set<ReminderRescheduleReason>> = captureResult {
        val reasons = state.getStringSet(KEY_PENDING_REASONS, emptySet()).orEmpty().mapNotNull { encoded ->
            ReminderRescheduleReason.entries.firstOrNull { it.name == encoded }
        }.toSet()
        state.edit().remove(KEY_PENDING_REASONS).apply()
        reasons
    }

    override suspend fun synchronize(
        occurrences: List<ReminderOccurrence>,
        reason: ReminderRescheduleReason,
    ): Result<ReminderScheduleResult> = captureResult {
        val manager = alarmManager ?: return@captureResult ReminderScheduleResult.Unavailable
        val permission = permissionStateDirect()
        if (permission != NotificationPermissionState.Granted) {
            cancelRegistered(manager)
            return@captureResult if (permission == NotificationPermissionState.Unavailable) {
                ReminderScheduleResult.Unavailable
            } else {
                ReminderScheduleResult.PermissionDenied
            }
        }

        val registered = state.getStringSet(KEY_REGISTERED_IDS, emptySet()).orEmpty()
            .mapTo(mutableSetOf()) { ReminderNotificationId(it) }
        val delta = reminderScheduleDelta(registered, occurrences, currentTimeMillis())
        cancelRegistered(manager)
        delta.schedule.forEach { occurrence ->
            val pendingIntent = pendingIntent(occurrence.id, occurrence.kind.name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.scheduledAtEpochMillis, pendingIntent)
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, occurrence.scheduledAtEpochMillis, pendingIntent)
            }
        }
        state.edit().putStringSet(KEY_REGISTERED_IDS, delta.schedule.map { it.id.value }.toSet()).apply()
        ReminderScheduleResult.Synchronized(delta.schedule.map { it.id }.toSet())
    }

    private fun permissionStateDirect(): NotificationPermissionState = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> NotificationPermissionState.Granted
        appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
            NotificationPermissionState.Granted
        else -> NotificationPermissionState.Denied
    }

    private fun cancelRegistered(manager: AlarmManager) {
        state.getStringSet(KEY_REGISTERED_IDS, emptySet()).orEmpty().forEach { value ->
            manager.cancel(pendingIntent(ReminderNotificationId(value), null))
        }
        state.edit().remove(KEY_REGISTERED_IDS).apply()
    }

    private fun pendingIntent(id: ReminderNotificationId, kind: String?): PendingIntent {
        val intent = Intent(appContext, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_DELIVER
            putExtra(EXTRA_NOTIFICATION_ID, id.value)
            kind?.let { putExtra(EXTRA_REMINDER_KIND, it) }
        }
        return PendingIntent.getBroadcast(
            appContext,
            stableReminderRequestCode(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
    }

    companion object {
        internal const val STATE_NAME = "bagcue_reminder_scheduler"
        internal const val KEY_PENDING_REASONS = "pending_reschedule_reasons"
        private const val KEY_REGISTERED_IDS = "registered_notification_ids"
        internal const val ACTION_DELIVER = "com.sedsoftware.bagcue.action.DELIVER_REMINDER"
        internal const val EXTRA_NOTIFICATION_ID = "notification_id"
        internal const val EXTRA_REMINDER_KIND = "reminder_kind"
    }
}
