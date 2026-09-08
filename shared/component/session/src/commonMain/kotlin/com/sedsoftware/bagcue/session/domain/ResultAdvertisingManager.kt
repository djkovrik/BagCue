package com.sedsoftware.bagcue.session.domain

import com.sedsoftware.bagcue.domain.apa.AdEligibility
import com.sedsoftware.bagcue.domain.apa.AdEligibilityInput
import com.sedsoftware.bagcue.domain.apa.AdSurface
import com.sedsoftware.bagcue.domain.apa.AdSuppressionReason
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyResolution
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.InlineAdState
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionApi
import com.sedsoftware.bagcue.domain.apa.evaluateAdEligibility
import com.sedsoftware.bagcue.domain.apa.resolveAdvertisingPrivacy
import com.sedsoftware.bagcue.domain.session.PackingSession
import kotlinx.coroutines.CancellationException

internal sealed interface ResultAdvertisingDecision {
    data object Suppressed : ResultAdvertisingDecision
    data object ConsentRequired : ResultAdvertisingDecision
    data class Eligible(val eligibility: AdEligibility.Eligible) : ResultAdvertisingDecision
}

internal class ResultAdvertisingManager(
    private val privacyRepository: AdvertisingPrivacyRepository,
    private val privacyApi: PrivacyRegionApi,
    private val controller: InlineAdController,
    private val currentTimeMillis: () -> Long,
) {
    suspend fun evaluate(session: PackingSession, alreadyRequested: Boolean): Result<ResultAdvertisingDecision> = captureAdResult {
        val preliminary = evaluateAdEligibility(
            AdEligibilityInput(
                platform = controller.platform,
                surface = AdSurface.CompletedSessionResult,
                sessionId = session.id,
                completionMode = session.completionMode,
                privacy = AdvertisingPrivacyResolution.Unresolved,
                hasProductionAdUnit = controller.hasProductionAdUnit,
                alreadyRequested = alreadyRequested,
            ),
        )
        if (preliminary is AdEligibility.Suppressed && preliminary.reason != AdSuppressionReason.PrivacyNotEligible) {
            return@captureAdResult ResultAdvertisingDecision.Suppressed
        }
        val now = currentTimeMillis()
        val privacyState = privacyRepository.readFresh(now).getOrThrow() ?: privacyApi.fetch().fold(
            onSuccess = { privacyRepository.storeResponse(it, now).getOrThrow() },
            onFailure = { error -> if (error is CancellationException) throw error else null },
        )
            ?: return@captureAdResult ResultAdvertisingDecision.Suppressed
        when (val privacy = resolveAdvertisingPrivacy(privacyState, now)) {
            is AdvertisingPrivacyResolution.ConsentRequired -> ResultAdvertisingDecision.ConsentRequired
            else -> when (val eligibility = evaluateAdEligibility(
                AdEligibilityInput(
                    platform = controller.platform,
                    surface = AdSurface.CompletedSessionResult,
                    sessionId = session.id,
                    completionMode = session.completionMode,
                    privacy = privacy,
                    hasProductionAdUnit = controller.hasProductionAdUnit,
                    alreadyRequested = alreadyRequested,
                ),
            )) {
                is AdEligibility.Eligible -> ResultAdvertisingDecision.Eligible(eligibility)
                is AdEligibility.Suppressed -> ResultAdvertisingDecision.Suppressed
            }
        }
    }

    suspend fun chooseConsent(choice: AdvertisingConsentChoice): Result<Unit> = captureAdResult {
        val now = currentTimeMillis()
        val fresh = privacyRepository.readFresh(now).getOrThrow()
            ?: throw IllegalStateException("Fresh advertising privacy state is required")
        if (fresh.response.consentRequired) privacyRepository.storeChoice(choice, now).getOrThrow()
    }

    suspend fun request(eligibility: AdEligibility.Eligible): Result<InlineAdState> = captureAdResult {
        controller.initialize(eligibility.userConsent).getOrThrow()
        controller.requestInlineAd().getOrThrow()
    }

    suspend fun dispose(): Result<Unit> = captureAdResult { controller.dispose().getOrThrow() }
}

private suspend inline fun <T> captureAdResult(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
