package com.sedsoftware.bagcue.session.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.session.FakeCatalogRepository
import com.sedsoftware.bagcue.session.FakeSessionRepository
import com.sedsoftware.bagcue.session.FakeTemplateRepository
import com.sedsoftware.bagcue.session.FakeSessionAnalyticsController
import com.sedsoftware.bagcue.session.FakeSessionPrivacyApi
import com.sedsoftware.bagcue.session.FakeSessionPrivacyRepository
import com.sedsoftware.bagcue.session.FakeInlineAdController
import com.sedsoftware.bagcue.domain.apa.AdPlatform
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionResponse
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.session.domain.ResultAdvertisingManager
import com.sedsoftware.bagcue.session.domain.SessionManager
import com.sedsoftware.bagcue.session.testItem
import com.sedsoftware.bagcue.session.testPosition
import com.sedsoftware.bagcue.session.testTemplate
import com.sedsoftware.bagcue.session.completedSession
import com.sedsoftware.bagcue.session.protectedSessionPrivacy
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate(2026, 9, 8)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun createAndFullRowTogglePersistThroughStore() = runTest(dispatcher) {
        val template = testTemplate("office", testPosition("position", "laptop"))
        val sessions = FakeSessionRepository()
        val manager = SessionManager(
            sessionRepository = sessions,
            templateRepository = FakeTemplateRepository(listOf(template)),
            catalogRepository = FakeCatalogRepository(listOf(testItem("laptop"))),
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("session") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("session-item") },
            catalogItemIdGenerator = PackingItemIdGenerator { PackingItemId("one-off") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("saved") },
            today = { date },
            currentTimeMillis = { 1L },
            resolveResourceKey = { it.value },
            analyticsController = FakeSessionAnalyticsController(),
        )
        val store = SessionStoreProvider(
            DefaultStoreFactory(), manager,
            ResultAdvertisingManager(
                FakeSessionPrivacyRepository(),
                FakeSessionPrivacyApi(Result.success(PrivacyRegionResponse(1, false, "policy", Long.MAX_VALUE))),
                FakeInlineAdController(AdPlatform.Ios),
                currentTimeMillis = { 1L },
            ),
            privacyPolicyUrl = null,
        ).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(SessionStore.Intent.StartCreate(date))
            store.accept(SessionStore.Intent.ToggleTemplate(template.id))
            store.accept(SessionStore.Intent.CreateSession)
            advanceUntilIdle()

            val created = assertNotNull(store.state.session)
            store.accept(SessionStore.Intent.TogglePacked(created.items.single().id))
            advanceUntilIdle()

            assertEquals(SessionStore.Route.Active, store.state.route)
            assertEquals(SessionItemState.Packed, store.state.session!!.items.single().state)
            assertEquals(SessionItemState.Packed, sessions.sessions.value.single().items.single().state)
        } finally {
            store.dispose()
        }
    }

    @Test
    fun completedResultRequestsAtMostOnceAndHidesOnExit() = runTest(dispatcher) {
        val completed = completedSession("completed")
        val sessions = FakeSessionRepository(listOf(completed))
        val analytics = FakeSessionAnalyticsController()
        val manager = SessionManager(
            sessionRepository = sessions,
            templateRepository = FakeTemplateRepository(emptyList()),
            catalogRepository = FakeCatalogRepository(emptyList()),
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("new") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("item") },
            catalogItemIdGenerator = PackingItemIdGenerator { PackingItemId("catalog") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("position") },
            today = { date }, currentTimeMillis = { 1L }, resolveResourceKey = { it.value },
            analyticsController = analytics,
        )
        val controller = FakeInlineAdController(AdPlatform.Android)
        val store = SessionStoreProvider(
            DefaultStoreFactory(), manager,
            ResultAdvertisingManager(
                FakeSessionPrivacyRepository(protectedSessionPrivacy(AdvertisingConsentChoice.Allowed)),
                FakeSessionPrivacyApi(Result.failure(IllegalStateException("unused"))), controller,
            ) { 1L },
            privacyPolicyUrl = null,
        ).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(SessionStore.Intent.OpenSession(completed.id))
            advanceUntilIdle()
            assertEquals(SessionStore.ResultAdvertisingStatus.Ready, store.state.resultAdvertisingStatus)
            store.accept(SessionStore.Intent.RefreshResultAdvertising)
            advanceUntilIdle()
            assertEquals(1, controller.requestCount)
            store.accept(SessionStore.Intent.BackToToday)
            advanceUntilIdle()
            assertEquals(SessionStore.ResultAdvertisingStatus.Hidden, store.state.resultAdvertisingStatus)
            assertEquals(1, controller.disposeCount)
        } finally {
            store.dispose()
        }
    }

    @Test
    fun completedResultReopensTheSameSessionAsAnActiveChecklist() = runTest(dispatcher) {
        val completed = completedSession("completed")
        val sessions = FakeSessionRepository(listOf(completed))
        val analytics = FakeSessionAnalyticsController()
        val manager = SessionManager(
            sessionRepository = sessions,
            templateRepository = FakeTemplateRepository(emptyList()),
            catalogRepository = FakeCatalogRepository(emptyList()),
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("new") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("item") },
            catalogItemIdGenerator = PackingItemIdGenerator { PackingItemId("catalog") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("position") },
            today = { date }, currentTimeMillis = { 1L }, resolveResourceKey = { it.value },
            analyticsController = analytics,
        )
        val store = SessionStoreProvider(
            DefaultStoreFactory(), manager,
            ResultAdvertisingManager(
                FakeSessionPrivacyRepository(),
                FakeSessionPrivacyApi(Result.failure(IllegalStateException("unused"))),
                FakeInlineAdController(AdPlatform.Ios),
            ) { 1L },
            privacyPolicyUrl = null,
        ).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(SessionStore.Intent.OpenSession(completed.id))
            advanceUntilIdle()
            assertEquals(SessionStore.Route.Result, store.state.route)

            store.accept(SessionStore.Intent.ReopenSession(completed.id))
            advanceUntilIdle()

            assertEquals(SessionStore.Route.Active, store.state.route)
            assertEquals(PackingSessionStatus.Active, store.state.session?.status)
            assertNull(store.state.session?.completionMode)
            assertNull(store.state.session?.completedAtMillis)
            assertEquals(completed.items, store.state.session?.items)
            assertEquals(completed.id, sessions.sessions.value.single().id)
            assertEquals(listOf(AnalyticsEventName.PackingSessionReopened), analytics.events.map { it.name })
        } finally {
            store.dispose()
        }
    }

    @Test
    fun failedReopenKeepsTheResultAndExposesARecoverableError() = runTest(dispatcher) {
        val completed = completedSession("completed")
        val sessions = FakeSessionRepository(
            initial = listOf(completed),
            reopenFailure = IllegalStateException("forced reopen failure"),
        )
        val manager = SessionManager(
            sessionRepository = sessions,
            templateRepository = FakeTemplateRepository(emptyList()),
            catalogRepository = FakeCatalogRepository(emptyList()),
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("new") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("item") },
            catalogItemIdGenerator = PackingItemIdGenerator { PackingItemId("catalog") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("position") },
            today = { date }, currentTimeMillis = { 1L }, resolveResourceKey = { it.value },
            analyticsController = FakeSessionAnalyticsController(),
        )
        val store = SessionStoreProvider(
            DefaultStoreFactory(), manager,
            ResultAdvertisingManager(
                FakeSessionPrivacyRepository(),
                FakeSessionPrivacyApi(Result.failure(IllegalStateException("unused"))),
                FakeInlineAdController(AdPlatform.Ios),
            ) { 1L },
            privacyPolicyUrl = null,
        ).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(SessionStore.Intent.OpenSession(completed.id))
            advanceUntilIdle()

            store.accept(SessionStore.Intent.ReopenSession(completed.id))
            advanceUntilIdle()

            assertEquals(SessionStore.Route.Result, store.state.route)
            assertEquals(SessionStore.Error.ReopenFailed, store.state.error)
            assertEquals(completed, sessions.sessions.value.single())
        } finally {
            store.dispose()
        }
    }

    @Test
    fun failedOneOffSaveRetainsVerbatimDraftAndRetryCommitsTheSameInput() = runTest(dispatcher) {
        val active = completedSession("active").copy(
            status = com.sedsoftware.bagcue.domain.session.PackingSessionStatus.Active,
            completionMode = null,
            completedAtMillis = null,
        )
        val sessions = FakeSessionRepository(
            initial = listOf(active),
            addOneOffFailure = IllegalStateException("forced save failure"),
        )
        val manager = SessionManager(
            sessionRepository = sessions,
            templateRepository = FakeTemplateRepository(emptyList()),
            catalogRepository = FakeCatalogRepository(emptyList()),
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("new") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("one-off-item") },
            catalogItemIdGenerator = PackingItemIdGenerator { PackingItemId("one-off-catalog") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("position") },
            today = { date },
            currentTimeMillis = { 1L },
            resolveResourceKey = { it.value },
            analyticsController = FakeSessionAnalyticsController(),
        )
        val store = SessionStoreProvider(
            DefaultStoreFactory(),
            manager,
            ResultAdvertisingManager(
                FakeSessionPrivacyRepository(),
                FakeSessionPrivacyApi(Result.failure(IllegalStateException("unused"))),
                FakeInlineAdController(AdPlatform.Ios),
            ) { 1L },
            privacyPolicyUrl = null,
        ).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(SessionStore.Intent.OpenSession(active.id))
            advanceUntilIdle()
            store.accept(SessionStore.Intent.StartAddOneOff)
            store.accept(SessionStore.Intent.ChangeOneOffName("  Passport 🎒  "))
            store.accept(SessionStore.Intent.ChangeOneOffLocation("  Desk drawer  "))
            store.accept(SessionStore.Intent.AddOneOffItem)
            advanceUntilIdle()

            assertEquals(SessionStore.Error.SaveFailed, store.state.error)
            assertEquals("  Passport 🎒  ", store.state.oneOffEditor?.name)
            assertEquals("  Desk drawer  ", store.state.oneOffEditor?.location)
            assertEquals(active, sessions.sessions.value.single())

            sessions.addOneOffFailure = null
            store.accept(SessionStore.Intent.AddOneOffItem)
            advanceUntilIdle()

            assertNull(store.state.oneOffEditor)
            assertEquals("  Passport 🎒  ", sessions.sessions.value.single().items.single().name.userText)
            assertEquals("  Desk drawer  ", sessions.sessions.value.single().items.single().sourceHint)
        } finally {
            store.dispose()
        }
    }
}
