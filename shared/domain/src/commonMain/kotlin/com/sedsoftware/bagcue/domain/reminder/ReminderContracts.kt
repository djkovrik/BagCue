package com.sedsoftware.bagcue.domain.reminder

import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlinx.coroutines.flow.Flow

interface ReminderPreferencesRepository {
    fun observeGlobalPreferences(): Flow<ReminderPreferences>

    fun observeSessionPreferences(sessionId: PackingSessionId): Flow<SessionReminderPreferences?>

    suspend fun readGlobalPreferences(): Result<ReminderPreferences>

    suspend fun updateGlobalPreferences(preferences: ReminderPreferences): Result<ReminderPreferences>

    suspend fun readSessionPreferences(sessionId: PackingSessionId): Result<SessionReminderPreferences?>

    suspend fun copyGlobalPreferencesToSession(sessionId: PackingSessionId): Result<SessionReminderPreferences>

    suspend fun updateSessionPreferences(preferences: SessionReminderPreferences): Result<SessionReminderPreferences>
}

interface ReminderNotificationScheduler {
    suspend fun permissionState(): Result<NotificationPermissionState>

    suspend fun requestPermission(): Result<NotificationPermissionState>

    suspend fun consumePendingRescheduleReasons(): Result<Set<ReminderRescheduleReason>>

    suspend fun synchronize(
        occurrences: List<ReminderOccurrence>,
        reason: ReminderRescheduleReason,
    ): Result<ReminderScheduleResult>
}
