package com.sedsoftware.bagcue.data.reminder

import com.russhwolf.settings.Settings
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferencesRepository
import com.sedsoftware.bagcue.domain.reminder.ReminderSetting
import com.sedsoftware.bagcue.domain.reminder.SessionReminderPreferences
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SettingsReminderPreferencesRepository(
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : ReminderPreferencesRepository {
    private val mutex = Mutex()
    private val global = MutableStateFlow(readGlobalDirect())
    private val sessions = mutableMapOf<PackingSessionId, MutableStateFlow<SessionReminderPreferences?>>()

    override fun observeGlobalPreferences(): Flow<ReminderPreferences> = global

    override fun observeSessionPreferences(sessionId: PackingSessionId): Flow<SessionReminderPreferences?> =
        sessionFlow(sessionId)

    override suspend fun readGlobalPreferences(): Result<ReminderPreferences> = onSettings {
        readGlobalDirect().also { global.value = it }
    }

    override suspend fun updateGlobalPreferences(preferences: ReminderPreferences): Result<ReminderPreferences> = onSettings {
        mutex.withLock {
            settings.putString(GLOBAL_KEY, encode(preferences.evening, preferences.morning))
            preferences.also { global.value = it }
        }
    }

    override suspend fun readSessionPreferences(
        sessionId: PackingSessionId,
    ): Result<SessionReminderPreferences?> = onSettings {
        readSessionDirect(sessionId).also { sessionFlow(sessionId).value = it }
    }

    override suspend fun copyGlobalPreferencesToSession(
        sessionId: PackingSessionId,
    ): Result<SessionReminderPreferences> = onSettings {
        mutex.withLock {
            val source = readGlobalDirect()
            val copied = SessionReminderPreferences(sessionId, source.evening, source.morning)
            settings.putString(sessionKey(sessionId), encode(copied.evening, copied.morning))
            sessionFlow(sessionId).value = copied
            copied
        }
    }

    override suspend fun updateSessionPreferences(
        preferences: SessionReminderPreferences,
    ): Result<SessionReminderPreferences> = onSettings {
        mutex.withLock {
            settings.putString(sessionKey(preferences.sessionId), encode(preferences.evening, preferences.morning))
            sessionFlow(preferences.sessionId).value = preferences
            preferences
        }
    }

    private fun sessionFlow(sessionId: PackingSessionId): MutableStateFlow<SessionReminderPreferences?> =
        sessions.getOrPut(sessionId) { MutableStateFlow(readSessionDirect(sessionId)) }

    private fun readGlobalDirect(): ReminderPreferences = settings.getStringOrNull(GLOBAL_KEY)
        ?.let(::decode)
        ?.let { (evening, morning) -> ReminderPreferences(evening, morning) }
        ?: ReminderPreferences()

    private fun readSessionDirect(sessionId: PackingSessionId): SessionReminderPreferences? =
        settings.getStringOrNull(sessionKey(sessionId))
            ?.let(::decode)
            ?.let { (evening, morning) -> SessionReminderPreferences(sessionId, evening, morning) }

    private suspend fun <T> onSettings(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
        }
    }

    private companion object {
        const val GLOBAL_KEY = "reminder.preferences.global"
        const val VERSION = "v1"

        fun sessionKey(sessionId: PackingSessionId) = "reminder.preferences.session.${sessionId.value}"

        fun encode(evening: ReminderSetting, morning: ReminderSetting): String = listOf(
            VERSION,
            if (evening.enabled) "1" else "0",
            evening.time.minuteOfDay.toString(),
            if (morning.enabled) "1" else "0",
            morning.time.minuteOfDay.toString(),
        ).joinToString("|")

        fun decode(value: String): Pair<ReminderSetting, ReminderSetting>? = runCatching {
            val parts = value.split('|')
            require(parts.size == ENCODED_PART_COUNT && parts[VERSION_INDEX] == VERSION)
            require(parts[EVENING_ENABLED_INDEX] == "0" || parts[EVENING_ENABLED_INDEX] == "1")
            require(parts[MORNING_ENABLED_INDEX] == "0" || parts[MORNING_ENABLED_INDEX] == "1")
            ReminderSetting(
                parts[EVENING_ENABLED_INDEX] == "1",
                minuteOfDay(parts[EVENING_TIME_INDEX].toInt()),
            ) to ReminderSetting(
                parts[MORNING_ENABLED_INDEX] == "1",
                minuteOfDay(parts[MORNING_TIME_INDEX].toInt()),
            )
        }.getOrNull()

        fun minuteOfDay(value: Int): ReminderLocalTime {
            require(value in 0 until ReminderLocalTime.HOURS_PER_DAY * ReminderLocalTime.MINUTES_PER_HOUR)
            return ReminderLocalTime.of(
                value / ReminderLocalTime.MINUTES_PER_HOUR,
                value % ReminderLocalTime.MINUTES_PER_HOUR,
            )
        }

        const val ENCODED_PART_COUNT = 5
        const val VERSION_INDEX = 0
        const val EVENING_ENABLED_INDEX = 1
        const val EVENING_TIME_INDEX = 2
        const val MORNING_ENABLED_INDEX = 3
        const val MORNING_TIME_INDEX = 4
    }
}
