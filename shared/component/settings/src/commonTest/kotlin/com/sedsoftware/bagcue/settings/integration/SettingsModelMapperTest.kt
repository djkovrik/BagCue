package com.sedsoftware.bagcue.settings.integration

import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.apa.*
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.settings.store.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsModelMapperTest {
    @Test
    fun mapsGlobalDefaultsAndPermissionDenialAsExplanatoryState() {
        val model = SettingsStore.State(
            sessionId = null,
            preferences = ReminderPreferences(),
            permission = NotificationPermissionState.Denied,
            isLoading = false,
        ).toComponentModel()
        assertEquals(SettingsComponent.Scope.Global, model.scope)
        assertEquals(SettingsComponent.NotificationCapability.Denied, model.notificationCapability)
        assertFalse(model.evening.enabled)
        assertEquals(ReminderKind.Morning, model.morning.kind)
    }

    @Test
    fun mapsPerSessionOverrideSource() {
        val model = SettingsStore.State(
            sessionId = PackingSessionId("session"),
            isSessionOverride = true,
            isLoading = false,
        ).toComponentModel()
        assertEquals(SettingsComponent.Source.SessionOverride, model.evening.source)
        assertEquals(SettingsComponent.Scope.Session(PackingSessionId("session")), model.scope)
    }

    @Test
    fun exposesConsentChoiceOnlyForFreshProtectedPrivacy() {
        val state = AdvertisingPrivacyState(
            PrivacyRegionResponse(1, true, "policy", Long.MAX_VALUE),
            AdvertisingConsentChoice.Declined,
            "policy",
        )
        val model = SettingsStore.State(
            sessionId = null,
            privacyState = state,
            privacyResolution = AdvertisingPrivacyResolution.Declined,
        ).toComponentModel()
        assertEquals(SettingsComponent.AdvertisingPrivacyStatus.Declined, model.advertisingPrivacy.status)
        assertEquals(true, model.advertisingPrivacy.choiceAvailable)
    }

    @Test
    fun nonProtectedPrivacyHasNoConsentChoice() {
        val state = AdvertisingPrivacyState(PrivacyRegionResponse(1, false, "policy", Long.MAX_VALUE), null, null)
        val model = SettingsStore.State(
            sessionId = null,
            privacyState = state,
            privacyResolution = AdvertisingPrivacyResolution.Eligible(true),
        ).toComponentModel()
        assertEquals(SettingsComponent.AdvertisingPrivacyStatus.NotRequired, model.advertisingPrivacy.status)
        assertEquals(false, model.advertisingPrivacy.choiceAvailable)
    }
}
