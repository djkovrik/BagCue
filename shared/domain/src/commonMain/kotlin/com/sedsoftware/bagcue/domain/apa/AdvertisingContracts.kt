package com.sedsoftware.bagcue.domain.apa

import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode

enum class AdPlatform { Android, Ios }
enum class AdSurface { CompletedSessionResult, Packing, History, Settings, Templates, Catalog, Other }
enum class AdSuppressionReason { WrongPlatform, DisallowedSurface, SkippedCompletion, PrivacyNotEligible, MissingAdUnit, AlreadyRequested }

data class AdEligibilityInput(
    val platform: AdPlatform,
    val surface: AdSurface,
    val sessionId: PackingSessionId?,
    val completionMode: SessionCompletionMode?,
    val privacy: AdvertisingPrivacyResolution,
    val hasProductionAdUnit: Boolean,
    val alreadyRequested: Boolean,
)

sealed interface AdEligibility {
    data class Eligible(val sessionId: PackingSessionId, val userConsent: Boolean) : AdEligibility
    data class Suppressed(val reason: AdSuppressionReason) : AdEligibility
}

fun evaluateAdEligibility(input: AdEligibilityInput): AdEligibility = when {
    input.platform != AdPlatform.Android -> AdEligibility.Suppressed(AdSuppressionReason.WrongPlatform)
    input.surface != AdSurface.CompletedSessionResult -> AdEligibility.Suppressed(AdSuppressionReason.DisallowedSurface)
    input.completionMode != SessionCompletionMode.AllPacked -> AdEligibility.Suppressed(AdSuppressionReason.SkippedCompletion)
    input.privacy !is AdvertisingPrivacyResolution.Eligible -> AdEligibility.Suppressed(AdSuppressionReason.PrivacyNotEligible)
    !input.hasProductionAdUnit -> AdEligibility.Suppressed(AdSuppressionReason.MissingAdUnit)
    input.alreadyRequested -> AdEligibility.Suppressed(AdSuppressionReason.AlreadyRequested)
    else -> AdEligibility.Eligible(requireNotNull(input.sessionId), input.privacy.userConsent)
}

sealed interface InlineAdState {
    data object Unavailable : InlineAdState
    data object Suppressed : InlineAdState
    data object Loading : InlineAdState
    data object Ready : InlineAdState
    data object Failed : InlineAdState
}

interface InlineAdController {
    val platform: AdPlatform
    val hasProductionAdUnit: Boolean
    suspend fun initialize(userConsent: Boolean): Result<Unit>
    suspend fun requestInlineAd(): Result<InlineAdState>
    suspend fun dispose(): Result<Unit>
}
