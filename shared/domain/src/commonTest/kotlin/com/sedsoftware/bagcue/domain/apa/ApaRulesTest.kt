package com.sedsoftware.bagcue.domain.apa

import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApaRulesTest {
    @Test
    fun analyticsContractContainsExactlySixContentFreeEventNames() {
        assertEquals(
            setOf(
                "template_created",
                "packing_session_created",
                "packing_session_completed",
                "packing_session_reopened",
                "history_repeat_used",
                "reminder_configured",
            ),
            AnalyticsEventName.entries.map { it.wireName }.toSet(),
        )
        assertTrue(AnalyticsEvent(AnalyticsEventName.TemplateCreated).name.wireName.isNotBlank())
    }

    @Test
    fun privacyIsFailClosedForMissingExpiredDeclinedAndPolicyMismatchStates() {
        val fresh = response(expiresAt = 200)
        assertIs<AdvertisingPrivacyResolution.Unresolved>(resolveAdvertisingPrivacy(null, 100))
        assertIs<AdvertisingPrivacyResolution.Unresolved>(
            resolveAdvertisingPrivacy(AdvertisingPrivacyState(response(100), null, null), 100),
        )
        assertIs<AdvertisingPrivacyResolution.ConsentRequired>(
            resolveAdvertisingPrivacy(AdvertisingPrivacyState(fresh, AdvertisingConsentChoice.Allowed, "old"), 100),
        )
        assertEquals(
            "policy-v1",
            assertIs<AdvertisingPrivacyResolution.ConsentRequired>(
                resolveAdvertisingPrivacy(AdvertisingPrivacyState(fresh, null, "policy-v1"), 100),
            ).policyVersion,
        )
        assertIs<AdvertisingPrivacyResolution.Declined>(
            resolveAdvertisingPrivacy(AdvertisingPrivacyState(fresh, AdvertisingConsentChoice.Declined, "policy-v1"), 100),
        )
    }

    @Test
    fun onlyAndroidFullCompletionWithEligiblePrivacyCanRequestAnAd() {
        val eligible = input()
        assertIs<AdEligibility.Eligible>(evaluateAdEligibility(eligible))
        assertEquals(
            AdSuppressionReason.WrongPlatform,
            assertIs<AdEligibility.Suppressed>(evaluateAdEligibility(eligible.copy(platform = AdPlatform.Ios))).reason,
        )
        assertEquals(
            AdSuppressionReason.SkippedCompletion,
            assertIs<AdEligibility.Suppressed>(
                evaluateAdEligibility(eligible.copy(completionMode = SessionCompletionMode.WithSkipped)),
            ).reason,
        )
        assertEquals(
            AdSuppressionReason.DisallowedSurface,
            assertIs<AdEligibility.Suppressed>(evaluateAdEligibility(eligible.copy(surface = AdSurface.Packing))).reason,
        )
        assertEquals(
            AdSuppressionReason.AlreadyRequested,
            assertIs<AdEligibility.Suppressed>(evaluateAdEligibility(eligible.copy(alreadyRequested = true))).reason,
        )
    }

    private fun response(expiresAt: Long) = PrivacyRegionResponse(1, true, "policy-v1", expiresAt)

    private fun input() = AdEligibilityInput(
        platform = AdPlatform.Android,
        surface = AdSurface.CompletedSessionResult,
        sessionId = PackingSessionId("session"),
        completionMode = SessionCompletionMode.AllPacked,
        privacy = AdvertisingPrivacyResolution.Eligible(true),
        hasProductionAdUnit = true,
        alreadyRequested = false,
    )
}
