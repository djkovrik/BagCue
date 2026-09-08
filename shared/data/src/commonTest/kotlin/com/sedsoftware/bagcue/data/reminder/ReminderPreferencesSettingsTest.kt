package com.sedsoftware.bagcue.data.reminder

import com.russhwolf.settings.MapSettings
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.ReminderSetting
import com.sedsoftware.bagcue.domain.reminder.SessionReminderPreferences
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderPreferencesSettingsTest {
    @Test
    fun missingSettingsUseDisabledEveningAndMorningDefaults() = runTest {
        val repository = repository(MapSettings())

        val defaults = repository.readGlobalPreferences().getOrThrow()

        assertFalse(defaults.evening.enabled)
        assertEquals(ReminderLocalTime.of(20, 0), defaults.evening.time)
        assertFalse(defaults.morning.enabled)
        assertEquals(ReminderLocalTime.of(7, 30), defaults.morning.time)
    }

    @Test
    fun sessionCopiesGlobalValuesThenOverridesIndependentlyAcrossRestart() = runTest {
        val settings = MapSettings()
        val sessionId = PackingSessionId("session-reminder")
        val first = repository(settings)
        val global = ReminderPreferences(
            evening = ReminderSetting(true, ReminderLocalTime.of(19, 45)),
            morning = ReminderSetting(false, ReminderLocalTime.of(8, 0)),
        )
        first.updateGlobalPreferences(global).getOrThrow()
        assertEquals(global, first.copyGlobalPreferencesToSession(sessionId).getOrThrow().toGlobal())

        val override = SessionReminderPreferences(
            sessionId,
            ReminderSetting(false, ReminderLocalTime.of(21, 0)),
            ReminderSetting(true, ReminderLocalTime.of(6, 50)),
        )
        first.updateSessionPreferences(override).getOrThrow()
        first.updateGlobalPreferences(ReminderPreferences()).getOrThrow()

        val restarted = repository(settings)
        assertEquals(ReminderPreferences(), restarted.readGlobalPreferences().getOrThrow())
        assertEquals(override, restarted.readSessionPreferences(sessionId).getOrThrow())
        assertEquals(override, restarted.observeSessionPreferences(sessionId).first())
        assertNull(restarted.readSessionPreferences(PackingSessionId("missing")).getOrThrow())
    }

    @Test
    fun malformedPayloadFallsBackWithoutInventingAnEnabledReminder() = runTest {
        val settings = MapSettings().apply {
            putString("reminder.preferences.global", "v1|1|9999|1|450")
        }

        val value = repository(settings).readGlobalPreferences().getOrThrow()

        assertFalse(value.evening.enabled)
        assertFalse(value.morning.enabled)
        assertTrue(value == ReminderPreferences())
    }

    private fun repository(settings: MapSettings) =
        SettingsReminderPreferencesRepository(settings, Dispatchers.Unconfined)

    private fun SessionReminderPreferences.toGlobal() = ReminderPreferences(evening, morning)
}
