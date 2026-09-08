package com.sedsoftware.bagcue.data.apa

import com.russhwolf.settings.MapSettings
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreference
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApaSettingsTest {
    @Test
    fun analyticsDefaultsOffAndPersistsImmediateUpdatesAcrossRestart() = runTest {
        val settings = MapSettings()
        val first = SettingsAnalyticsPreferenceRepository(settings, Dispatchers.Unconfined)
        assertFalse(first.read().getOrThrow().enabled)
        first.update(AnalyticsPreference(true)).getOrThrow()
        assertTrue(SettingsAnalyticsPreferenceRepository(settings, Dispatchers.Unconfined).read().getOrThrow().enabled)
    }

    @Test
    fun privacyCacheClampsFreshnessAndInvalidatesChoiceOnPolicyChange() = runTest {
        val settings = MapSettings()
        val repository = SettingsAdvertisingPrivacyRepository(settings, Dispatchers.Unconfined)
        val first = repository.storeResponse(PrivacyRegionResponse(1, true, "p1", Long.MAX_VALUE), 100).getOrThrow()
        assertEquals(100 + 72L * 60 * 60 * 1000, first.response.expiresAtEpochMillis)
        repository.storeChoice(AdvertisingConsentChoice.Allowed, 101).getOrThrow()

        val changed = repository.storeResponse(PrivacyRegionResponse(1, true, "p2", 1_000_000_000), 102).getOrThrow()
        assertNull(changed.choice)
        assertNull(changed.choicePolicyVersion)
    }

    @Test
    fun expiredOrMalformedPrivacyPayloadFailsClosedAcrossRestart() = runTest {
        val settings = MapSettings()
        val repository = SettingsAdvertisingPrivacyRepository(settings, Dispatchers.Unconfined)
        repository.storeResponse(PrivacyRegionResponse(1, false, "p1", 200), 100).getOrThrow()
        assertNull(SettingsAdvertisingPrivacyRepository(settings, Dispatchers.Unconfined).readFresh(200).getOrThrow())

        settings.putString("advertising.privacy.state", "country|RU|ip|identifier")
        assertNull(SettingsAdvertisingPrivacyRepository(settings, Dispatchers.Unconfined).readFresh(100).getOrThrow())
    }
}
