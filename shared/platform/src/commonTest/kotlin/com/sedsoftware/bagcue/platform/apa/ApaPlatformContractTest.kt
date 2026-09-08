package com.sedsoftware.bagcue.platform.apa

import com.sedsoftware.bagcue.domain.apa.AdPlatform
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.apa.InlineAdState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApaPlatformContractTest {
    @Test
    fun analyticsIsOffUntilEnabledAndResetRemovesTheAppInstanceId() = runTest {
        val sink = RecordingAnalyticsSink()
        val controller = SafeAnalyticsController(sink)
        controller.record(AnalyticsEvent(AnalyticsEventName.TemplateCreated)).getOrThrow()
        assertTrue(sink.events.isEmpty())

        controller.setCollectionEnabled(true).getOrThrow()
        controller.record(AnalyticsEvent(AnalyticsEventName.TemplateCreated)).getOrThrow()
        assertEquals(listOf("template_created"), sink.events)
        controller.resetAnalyticsData().getOrThrow()
        assertNull(controller.readAppInstanceId().getOrThrow())
    }

    @Test
    fun persistedEnabledStateAllowsTheFirstEventAfterRestart() = runTest {
        val sink = RecordingAnalyticsSink().apply { enabled = true }
        val controller = SafeAnalyticsController(sink, initiallyEnabled = true)

        controller.record(AnalyticsEvent(AnalyticsEventName.PackingSessionReopened)).getOrThrow()

        assertEquals(listOf("packing_session_reopened"), sink.events)
    }

    @Test
    fun privacyConsentIsAppliedBeforeSdkInitializationAndEveryAdRequest() = runTest {
        val gateway = RecordingAdGateway()
        val controller = PrivacyFirstInlineAdController(AdPlatform.Android, "production-id", gateway)

        assertTrue(controller.requestInlineAd().isFailure)
        controller.initialize(true).getOrThrow()
        assertEquals(InlineAdState.Ready, controller.requestInlineAd().getOrThrow())
        assertEquals(listOf("consent:true", "initialize", "request:production-id"), gateway.calls)
    }

    @Test
    fun nullIosConfigurationNeverInitializesOrRequestsTheSdk() = runTest {
        val gateway = RecordingAdGateway()
        val controller = PrivacyFirstInlineAdController(AdPlatform.Ios, null, gateway)

        controller.initialize(true).getOrThrow()
        assertEquals(InlineAdState.Unavailable, controller.requestInlineAd().getOrThrow())
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun withdrawalIsForwardedBeforeSdkResourcesAreDisposed() = runTest {
        val gateway = RecordingAdGateway()
        val controller = PrivacyFirstInlineAdController(AdPlatform.Android, "production-id", gateway)

        controller.initialize(true).getOrThrow()
        controller.dispose().getOrThrow()

        assertEquals(listOf("consent:true", "initialize", "consent:false", "dispose"), gateway.calls)
        assertTrue(controller.requestInlineAd().isFailure)
    }

    private class RecordingAnalyticsSink : AnalyticsEventSink {
        var enabled = false
        var id: String? = "instance"
        val events = mutableListOf<String>()
        override suspend fun setEnabled(enabled: Boolean) { this.enabled = enabled }
        override suspend fun reset() { id = null }
        override suspend fun appInstanceId(): String? = id
        override suspend fun sendAllowlistedEvent(name: String) { events += name }
    }

    private class RecordingAdGateway : InlineAdSdkGateway {
        val calls = mutableListOf<String>()
        override suspend fun setUserConsent(value: Boolean) { calls += "consent:$value" }
        override suspend fun initialize() { calls += "initialize" }
        override suspend fun requestInlineAd(adUnitId: String): InlineAdState {
            calls += "request:$adUnitId"
            return InlineAdState.Ready
        }
        override suspend fun dispose() { calls += "dispose" }
    }
}
