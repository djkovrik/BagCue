package com.sedsoftware.bagcue.session.domain

import com.sedsoftware.bagcue.domain.apa.*
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.session.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResultAdvertisingManagerTest {
    @Test
    fun iosAndSkippedResultsSuppressBeforePrivacyOrSdkWork() = runTest {
        val api = FakeSessionPrivacyApi(Result.failure(IllegalStateException("must not fetch")))
        val iosController = FakeInlineAdController(AdPlatform.Ios)
        val ios = ResultAdvertisingManager(FakeSessionPrivacyRepository(), api, iosController) { 1L }
        assertIs<ResultAdvertisingDecision.Suppressed>(ios.evaluate(completedSession("ios"), false).getOrThrow())
        assertEquals(0, api.fetchCount)
        assertEquals(0, iosController.initializeCount)

        val androidController = FakeInlineAdController(AdPlatform.Android)
        val skipped = ResultAdvertisingManager(FakeSessionPrivacyRepository(), api, androidController) { 1L }
        assertIs<ResultAdvertisingDecision.Suppressed>(skipped.evaluate(completedSession("skipped", SessionCompletionMode.WithSkipped), false).getOrThrow())
        assertEquals(0, api.fetchCount)
        assertEquals(0, androidController.initializeCount)
    }

    @Test
    fun unresolvedAndDeclinedPrivacyFailClosedWithoutInitialization() = runTest {
        val controller = FakeInlineAdController(AdPlatform.Android)
        val unresolved = ResultAdvertisingManager(
            FakeSessionPrivacyRepository(), FakeSessionPrivacyApi(Result.failure(IllegalStateException("offline"))), controller,
        ) { 1L }
        assertIs<ResultAdvertisingDecision.Suppressed>(unresolved.evaluate(completedSession("offline"), false).getOrThrow())

        val declined = ResultAdvertisingManager(
            FakeSessionPrivacyRepository(protectedSessionPrivacy(AdvertisingConsentChoice.Declined)),
            FakeSessionPrivacyApi(Result.failure(IllegalStateException("unused"))), controller,
        ) { 1L }
        assertIs<ResultAdvertisingDecision.Suppressed>(declined.evaluate(completedSession("declined"), false).getOrThrow())
        assertEquals(0, controller.initializeCount)
        assertEquals(0, controller.requestCount)
    }

    @Test
    fun eligibleAndroidResultInitializesOnlyWhenExplicitlyRequested() = runTest {
        val controller = FakeInlineAdController(AdPlatform.Android)
        val manager = ResultAdvertisingManager(
            FakeSessionPrivacyRepository(protectedSessionPrivacy(AdvertisingConsentChoice.Allowed)),
            FakeSessionPrivacyApi(Result.failure(IllegalStateException("unused"))), controller,
        ) { 1L }
        val decision = assertIs<ResultAdvertisingDecision.Eligible>(manager.evaluate(completedSession("eligible"), false).getOrThrow())
        assertEquals(0, controller.initializeCount)
        assertIs<InlineAdState.Ready>(manager.request(decision.eligibility).getOrThrow())
        assertEquals(1, controller.initializeCount)
        assertEquals(1, controller.requestCount)
    }
}
