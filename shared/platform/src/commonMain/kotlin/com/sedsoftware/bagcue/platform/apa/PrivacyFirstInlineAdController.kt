package com.sedsoftware.bagcue.platform.apa

import com.sedsoftware.bagcue.domain.apa.AdPlatform
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.InlineAdState
import kotlinx.coroutines.CancellationException

interface InlineAdSdkGateway {
    suspend fun setUserConsent(value: Boolean)
    suspend fun initialize()
    suspend fun requestInlineAd(adUnitId: String): InlineAdState
    suspend fun dispose()
}

class PrivacyFirstInlineAdController(
    override val platform: AdPlatform,
    private val adUnitId: String?,
    private val gateway: InlineAdSdkGateway,
) : InlineAdController {
    override val hasProductionAdUnit: Boolean = !adUnitId.isNullOrBlank()
    private var initialized = false

    override suspend fun initialize(userConsent: Boolean): Result<Unit> = capture {
        if (!hasProductionAdUnit) return@capture
        gateway.setUserConsent(userConsent)
        gateway.initialize()
        initialized = true
    }

    override suspend fun requestInlineAd(): Result<InlineAdState> = capture {
        if (!hasProductionAdUnit || platform != AdPlatform.Android) return@capture InlineAdState.Unavailable
        check(initialized) { "Advertising must be privacy-configured before requesting an ad" }
        gateway.requestInlineAd(requireNotNull(adUnitId))
    }

    override suspend fun dispose(): Result<Unit> = capture {
        if (initialized) gateway.setUserConsent(false)
        gateway.dispose()
        initialized = false
    }

    private suspend inline fun <T> capture(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
    }
}
