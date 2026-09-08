package com.sedsoftware.bagcue.session.domain

import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.session.FakeCatalogRepository
import com.sedsoftware.bagcue.session.FakeSessionRepository
import com.sedsoftware.bagcue.session.FakeTemplateRepository
import com.sedsoftware.bagcue.session.testItem
import com.sedsoftware.bagcue.session.testPosition
import com.sedsoftware.bagcue.session.testTemplate
import com.sedsoftware.bagcue.session.FakeSessionAnalyticsController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class SessionManagerTest {
    private val date = LocalDate(2026, 9, 8)

    @Test
    fun createProducesOneMergedPersistedSnapshotForDate() = runTest {
        val item = testItem("laptop")
        val first = testTemplate("office", testPosition("one", "laptop", 1))
        val second = testTemplate("travel", testPosition("two", "laptop", 4))
        val sessions = FakeSessionRepository()
        val manager = manager(sessions, FakeTemplateRepository(listOf(first, second)), FakeCatalogRepository(listOf(item)))

        val created = assertIs<CreateSessionResult.Created>(manager.create(date, setOf(first.id, second.id)).getOrThrow()).session

        assertEquals(1, created.items.size)
        assertEquals(4, created.items.single().quantity.value)
        assertEquals(created, sessions.sessions.value.single())
    }

    @Test
    fun occupiedDateReturnsExistingWithoutReplacingIt() = runTest {
        val template = testTemplate("office", testPosition("one", "laptop"))
        val sessions = FakeSessionRepository()
        val manager = manager(sessions, FakeTemplateRepository(listOf(template)), FakeCatalogRepository(listOf(testItem("laptop"))))
        val existing = assertIs<CreateSessionResult.Created>(manager.create(date, setOf(template.id)).getOrThrow()).session

        val second = manager.create(date, setOf(template.id)).getOrThrow()

        assertEquals(existing, assertIs<CreateSessionResult.DateOccupied>(second).existing)
        assertEquals(1, sessions.sessions.value.size)
    }

    @Test
    fun replacementPreservesPackedIdentityAndProvidesWholeUndo() = runTest {
        val first = testTemplate("office", testPosition("one", "laptop"))
        val second = testTemplate(
            "travel",
            testPosition("two", "laptop", quantity = 8),
            testPosition("three", "keys", sortOrder = 1),
        )
        val sessions = FakeSessionRepository()
        val manager = manager(sessions, FakeTemplateRepository(listOf(first, second)), FakeCatalogRepository(listOf(testItem("laptop"), testItem("keys"))))
        val original = assertIs<CreateSessionResult.Created>(manager.create(date, setOf(first.id)).getOrThrow()).session
        val packed = manager.setPacked(original, original.items.single().id, true).getOrThrow()

        val mutation = manager.replace(packed, setOf(second.id)).getOrThrow()

        assertEquals(packed, mutation.undo.before)
        assertEquals(SessionItemState.Packed, mutation.session.items.first { it.catalogItemId == PackingItemId("laptop") }.state)
        assertEquals(8, mutation.session.items.first { it.catalogItemId == PackingItemId("laptop") }.quantity.value)
        assertEquals(SessionItemState.NotPacked, mutation.session.items.first { it.catalogItemId == PackingItemId("keys") }.state)
    }

    @Test
    fun oneOffThenRemoveAndUndoRestoresExactSnapshot() = runTest {
        val template = testTemplate("office", testPosition("one", "laptop"))
        val sessions = FakeSessionRepository()
        val manager = manager(sessions, FakeTemplateRepository(listOf(template)), FakeCatalogRepository(listOf(testItem("laptop"))))
        val created = assertIs<CreateSessionResult.Created>(manager.create(date, setOf(template.id)).getOrThrow()).session
        val withOneOff = manager.addOneOff(created.id, "Passport", "Desk").getOrThrow()
        val oneOff = withOneOff.items.first { it.catalogItemId != PackingItemId("laptop") }

        val removed = manager.remove(withOneOff.id, oneOff.id).getOrThrow()
        val restored = manager.undo(removed.undo).getOrThrow()

        assertEquals(withOneOff, restored)
        assertNotEquals(created.items, restored.items)
    }

    @Test
    fun successfulSessionActionsRecordOnlyApprovedContentFreeEvents() = runTest {
        val analytics = FakeSessionAnalyticsController()
        val template = testTemplate("office", testPosition("one", "laptop"))
        val manager = manager(
            FakeSessionRepository(),
            FakeTemplateRepository(listOf(template)),
            FakeCatalogRepository(listOf(testItem("laptop"))),
            analytics,
        )
        val created = assertIs<CreateSessionResult.Created>(manager.create(date, setOf(template.id)).getOrThrow()).session
        val packed = manager.setPacked(created, created.items.single().id, true).getOrThrow()
        manager.completeAll(packed).getOrThrow()
        manager.recordHistoryRepeat().getOrThrow()
        assertEquals(
            listOf(AnalyticsEventName.PackingSessionCreated, AnalyticsEventName.PackingSessionCompleted, AnalyticsEventName.HistoryRepeatUsed),
            analytics.events.map { it.name },
        )
    }

    @Test
    fun failedOneOffSavePreservesOriginalCauseAndPriorDurableSession() = runTest {
        val original = com.sedsoftware.bagcue.session.completedSession("active").copy(
            status = com.sedsoftware.bagcue.domain.session.PackingSessionStatus.Active,
            completionMode = null,
            completedAtMillis = null,
        )
        val failure = IllegalStateException("database unavailable")
        val sessions = FakeSessionRepository(listOf(original), addOneOffFailure = failure)

        val result = manager(sessions, FakeTemplateRepository(emptyList()), FakeCatalogRepository(emptyList()))
            .addOneOff(original.id, "  Passport 🎒  ", "  Desk drawer  ")

        assertSame(failure, result.exceptionOrNull())
        assertEquals(original, sessions.sessions.value.single())
    }

    @Test
    fun oneOffSaveCancellationFromRepositoryResultIsRethrown() = runTest {
        val original = com.sedsoftware.bagcue.session.completedSession("active").copy(
            status = com.sedsoftware.bagcue.domain.session.PackingSessionStatus.Active,
            completionMode = null,
            completedAtMillis = null,
        )
        val cancellation = CancellationException("cancel one-off")
        val sessions = FakeSessionRepository(listOf(original), addOneOffFailure = cancellation)

        assertSame(
            cancellation,
            assertFailsWith<CancellationException> {
                manager(sessions, FakeTemplateRepository(emptyList()), FakeCatalogRepository(emptyList()))
                    .addOneOff(original.id, "Passport", null)
            },
        )
        assertEquals(original, sessions.sessions.value.single())
    }

    private fun manager(
        sessions: FakeSessionRepository,
        templates: FakeTemplateRepository,
        catalog: FakeCatalogRepository,
        analytics: FakeSessionAnalyticsController = FakeSessionAnalyticsController(),
    ): SessionManager {
        var itemIndex = 0
        return SessionManager(
            sessionRepository = sessions,
            templateRepository = templates,
            catalogRepository = catalog,
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("session-${sessions.sessions.value.size}") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("session-item-${itemIndex++}") },
            catalogItemIdGenerator = com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator { PackingItemId("one-off") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("saved-position") },
            today = { date },
            currentTimeMillis = { 1000L },
            resolveResourceKey = { it.value },
            analyticsController = analytics,
        )
    }
}
