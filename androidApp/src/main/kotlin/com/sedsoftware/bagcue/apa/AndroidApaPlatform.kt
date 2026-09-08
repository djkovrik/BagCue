package com.sedsoftware.bagcue.apa

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.sedsoftware.bagcue.domain.apa.AdPlatform
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.InlineAdState
import com.sedsoftware.bagcue.platform.apa.AnalyticsEventSink
import com.sedsoftware.bagcue.platform.apa.InlineAdSdkGateway
import com.sedsoftware.bagcue.platform.apa.PrivacyFirstInlineAdController
import com.sedsoftware.bagcue.platform.apa.SafeAnalyticsController
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class AndroidApaPlatform(
    val analyticsController: AnalyticsController,
    val inlineAdController: InlineAdController,
    val inlineAdGateway: AndroidYandexInlineAdGateway,
)

fun AndroidApaPlatform(context: Context, adUnitId: String?): AndroidApaPlatform {
    val appContext = context.applicationContext
    val gateway = AndroidYandexInlineAdGateway(appContext)
    val analyticsSink = AndroidAppOwnedAnalyticsSink(appContext)
    return AndroidApaPlatform(
        analyticsController = SafeAnalyticsController(analyticsSink, analyticsSink.isEnabled()),
        inlineAdController = PrivacyFirstInlineAdController(AdPlatform.Android, adUnitId, gateway),
        inlineAdGateway = gateway,
    )
}

class AndroidYandexInlineAdGateway(
    private val context: Context,
) : InlineAdSdkGateway {
    var bannerAdView: BannerAdView? = null
        private set

    override suspend fun setUserConsent(value: Boolean) {
        YandexAds.setUserConsent(value)
    }

    override suspend fun initialize() = suspendCancellableCoroutine { continuation ->
        YandexAds.initialize(context) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

    override suspend fun requestInlineAd(adUnitId: String): InlineAdState = suspendCancellableCoroutine { continuation ->
        bannerAdView?.destroy()
        bannerAdView = null
        val width = (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).toInt()
        val candidate = BannerAdView(context)
        candidate.setAdSize(BannerAdSize.inline(context, width, MAX_HEIGHT_DP))
        candidate.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {
                candidate.setBannerAdEventListener(null)
                if (continuation.isActive) {
                    bannerAdView = candidate
                    continuation.resume(InlineAdState.Ready)
                } else {
                    candidate.destroy()
                }
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                candidate.setBannerAdEventListener(null)
                candidate.destroy()
                if (continuation.isActive) continuation.resume(InlineAdState.Unavailable)
            }

            override fun onAdClicked() = Unit
            override fun onImpression(impressionData: ImpressionData?) = Unit
        })
        continuation.invokeOnCancellation {
            candidate.setBannerAdEventListener(null)
            candidate.destroy()
        }
        candidate.loadAd(AdRequest.Builder(adUnitId).build())
    }

    override suspend fun dispose() {
        bannerAdView?.destroy()
        bannerAdView = null
    }

    private companion object { const val MAX_HEIGHT_DP = 100 }
}

private class AndroidAppOwnedAnalyticsSink(context: Context) : AnalyticsEventSink {
    private val settings = context.getSharedPreferences("bagcue_analytics", Context.MODE_PRIVATE)
    private val analytics by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Firebase.analytics }

    override suspend fun setEnabled(enabled: Boolean) {
        val storageConsent = if (enabled) {
            FirebaseAnalytics.ConsentStatus.GRANTED
        } else {
            FirebaseAnalytics.ConsentStatus.DENIED
        }
        analytics.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to storageConsent,
                FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
            ),
        )
        analytics.setAnalyticsCollectionEnabled(enabled)
        settings.edit().putBoolean(ANALYTICS_ENABLED, enabled).apply()
    }

    override suspend fun reset() {
        analytics.resetAnalyticsData()
    }

    override suspend fun appInstanceId(): String? {
        if (!isEnabled()) return null
        return suspendCancellableCoroutine { continuation ->
            analytics.appInstanceId.addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase app-instance ID lookup failed"),
                    )
                }
            }
        }
    }

    override suspend fun sendAllowlistedEvent(name: String) {
        analytics.logEvent(name, null)
    }

    fun isEnabled(): Boolean = settings.getBoolean(ANALYTICS_ENABLED, false)

    private companion object {
        const val ANALYTICS_ENABLED = "collection_enabled"
    }
}
