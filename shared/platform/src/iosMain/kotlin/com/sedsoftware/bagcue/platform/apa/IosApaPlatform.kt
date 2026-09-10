package com.sedsoftware.bagcue.platform.apa

import cocoapods.FirebaseAnalytics.FIRAnalytics
import com.sedsoftware.bagcue.domain.apa.AdPlatform
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.InlineAdState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
fun IosAnalyticsController(): AnalyticsController {
    val sink = IosAppOwnedAnalyticsSink()
    return SafeAnalyticsController(sink, sink.isEnabled())
}

fun IosInlineAdController(): InlineAdController = PrivacyFirstInlineAdController(
    platform = AdPlatform.Ios,
    adUnitId = null,
    gateway = object : InlineAdSdkGateway {
        override suspend fun setUserConsent(value: Boolean) = Unit
        override suspend fun initialize() = Unit
        override suspend fun requestInlineAd(adUnitId: String) = InlineAdState.Unavailable
        override suspend fun dispose() = Unit
    },
)

@OptIn(ExperimentalForeignApi::class)
private class IosAppOwnedAnalyticsSink : AnalyticsEventSink {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun setEnabled(enabled: Boolean) {
        FIRAnalytics.setAnalyticsCollectionEnabled(enabled)
        defaults.setBool(enabled, ANALYTICS_ENABLED)
    }

    override suspend fun reset() {
        FIRAnalytics.resetAnalyticsData()
    }

    override suspend fun appInstanceId(): String? = if (isEnabled()) FIRAnalytics.appInstanceID() else null

    override suspend fun sendAllowlistedEvent(name: String) {
        FIRAnalytics.logEventWithName(name, parameters = null)
    }

    fun isEnabled(): Boolean = defaults.boolForKey(ANALYTICS_ENABLED)

    private companion object {
        const val ANALYTICS_ENABLED = "analytics.collection.enabled.platform"
    }
}
