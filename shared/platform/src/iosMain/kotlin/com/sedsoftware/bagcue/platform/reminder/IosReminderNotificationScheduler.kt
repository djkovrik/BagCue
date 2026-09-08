package com.sedsoftware.bagcue.platform.reminder

import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationScheduler
import com.sedsoftware.bagcue.domain.reminder.ReminderOccurrence
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.reminder.ReminderScheduleResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosReminderNotificationScheduler(
    private val currentTimeMillis: () -> Long,
) : ReminderNotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun permissionState(): Result<NotificationPermissionState> = captureResult {
        readPermissionState()
    }

    override suspend fun requestPermission(): Result<NotificationPermissionState> = captureResult {
        suspendCancellableCoroutine { continuation ->
            center.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
            ) { granted, error ->
                when {
                    error != null -> continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                    granted -> continuation.resume(NotificationPermissionState.Granted)
                    else -> continuation.resume(NotificationPermissionState.Denied)
                }
            }
        }
    }

    override suspend fun consumePendingRescheduleReasons(): Result<Set<ReminderRescheduleReason>> =
        Result.success(emptySet())

    override suspend fun synchronize(
        occurrences: List<ReminderOccurrence>,
        reason: ReminderRescheduleReason,
    ): Result<ReminderScheduleResult> = captureResult {
        when (readPermissionState()) {
            NotificationPermissionState.Denied, NotificationPermissionState.Unknown -> {
                removePendingBagCueRequests()
                ReminderScheduleResult.PermissionDenied
            }
            NotificationPermissionState.Unavailable -> ReminderScheduleResult.Unavailable
            NotificationPermissionState.Granted -> {
                removePendingBagCueRequests()
                val future = occurrences.filter { it.scheduledAtEpochMillis > currentTimeMillis() }
                future.forEach { add(it) }
                ReminderScheduleResult.Synchronized(future.map { it.id }.toSet())
            }
        }
    }

    private suspend fun readPermissionState(): NotificationPermissionState = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val state = when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional,
                UNAuthorizationStatusEphemeral,
                -> NotificationPermissionState.Granted
                UNAuthorizationStatusDenied -> NotificationPermissionState.Denied
                UNAuthorizationStatusNotDetermined -> NotificationPermissionState.Unknown
                else -> NotificationPermissionState.Unavailable
            }
            continuation.resume(state)
        }
    }

    private suspend fun removePendingBagCueRequests() = suspendCancellableCoroutine { continuation ->
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ids = requests.orEmpty().mapNotNull { request ->
                (request as? UNNotificationRequest)?.identifier?.takeIf { it.startsWith(ID_PREFIX) }
            }
            if (ids.isNotEmpty()) center.removePendingNotificationRequestsWithIdentifiers(ids)
            continuation.resume(Unit)
        }
    }

    private suspend fun add(occurrence: ReminderOccurrence) = suspendCancellableCoroutine { continuation ->
        val seconds = ((occurrence.scheduledAtEpochMillis - currentTimeMillis()) / 1000.0).coerceAtLeast(1.0)
        val content = UNMutableNotificationContent().apply {
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger: UNNotificationTrigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = seconds,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(occurrence.id.value, content, trigger)
        center.addNotificationRequest(request) { error ->
            if (error == null) continuation.resume(Unit)
            else continuation.resumeWithException(IllegalStateException(error.localizedDescription))
        }
    }

    private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
    }

    private companion object {
        const val ID_PREFIX = "session:"
    }
}
