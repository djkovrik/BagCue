package com.sedsoftware.bagcue.history.domain

import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.history.FakeHistoryRepository
import com.sedsoftware.bagcue.history.RecordingHistoryAnalyticsController
import com.sedsoftware.bagcue.history.historySession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException

class HistoryManagerTest {
    private val firstDate = LocalDate(2026, 9, 8)
    private val secondDate = LocalDate(2026, 9, 9)

    @Test
    fun browseAndFilterPreserveStoredLocalDatesAndSections() = runTest {
        val planned = historySession("planned", secondDate, completed = false)
        val completed = historySession("completed", firstDate, completed = true)
        val manager = manager(FakeHistoryRepository(listOf(completed, planned)))

        val all = manager.observeHistory(null).first()
        val filtered = manager.observeHistory(firstDate).first()

        assertEquals(listOf(planned), all.planned)
        assertEquals(listOf(completed), all.completed)
        assertEquals(firstDate, filtered.completed.single().localDate)
        assertEquals(emptyList(), filtered.planned)
    }

    @Test
    fun reopenKeepsSameIdentityDateAndSnapshots() = runTest {
        val completed = historySession("completed", firstDate, completed = true)
        val reopened = manager(FakeHistoryRepository(listOf(completed))).reopen(completed.id).getOrThrow()

        assertEquals(completed.id, reopened.id)
        assertEquals(completed.localDate, reopened.localDate)
        assertEquals(completed.selectedTemplates, reopened.selectedTemplates)
        assertEquals(PackingSessionStatus.Active, reopened.status)
    }

    @Test
    fun repeatReturnsHistoricalStableTemplateIdsOnly() = runTest {
        val completed = historySession("completed", firstDate, completed = true, templateIds = listOf("office", "pool"))

        val repeat = manager(FakeHistoryRepository(listOf(completed))).prepareRepeat(completed.id).getOrThrow()

        assertEquals(completed.id, repeat.sourceSessionId)
        assertEquals(completed.selectedTemplates.map { it.id }, repeat.selectedTemplateIds)
    }

    @Test
    fun deleteAndUndoRestoreTheExactDatedAggregate() = runTest {
        val completed = historySession("completed", firstDate, completed = true)
        val repository = FakeHistoryRepository(listOf(completed))
        val manager = manager(repository)

        val deletion = manager.delete(completed.id).getOrThrow()
        val restored = assertIs<CreateSessionResult.Created>(manager.undo(deletion.undo).getOrThrow()).session

        assertEquals(emptyList(), repository.sessions.value.filterNot { it == restored })
        assertEquals(completed, restored)
    }

    @Test
    fun successfulReopenRecordsOnlyContentFreePackingSessionReopenedEvent() = runTest {
        val completed = historySession("private-session-id", firstDate, completed = true)
        val analytics = RecordingHistoryAnalyticsController()

        manager(FakeHistoryRepository(listOf(completed)), analytics).reopen(completed.id).getOrThrow()

        assertEquals(
            listOf(AnalyticsEvent(AnalyticsEventName.PackingSessionReopened)),
            analytics.events,
        )
    }

    @Test
    fun failedReopenDoesNotRecordAnalyticsEvent() = runTest {
        val completed = historySession("private-session-id", firstDate, completed = true)
        val analytics = RecordingHistoryAnalyticsController()
        val result = manager(
            FakeHistoryRepository(listOf(completed), failOnReopen = true),
            analytics,
        ).reopen(completed.id)

        assertTrue(result.isFailure)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun cancelledReopenDoesNotRecordAnalyticsEvent() = runTest {
        val completed = historySession("private-session-id", firstDate, completed = true)
        val analytics = RecordingHistoryAnalyticsController()
        val manager = manager(
            FakeHistoryRepository(listOf(completed), cancelOnReopen = true),
            analytics,
        )

        assertFailsWith<CancellationException> { manager.reopen(completed.id) }
        assertTrue(analytics.events.isEmpty())
    }

    private fun manager(
        repository: FakeHistoryRepository,
        analyticsController: RecordingHistoryAnalyticsController = RecordingHistoryAnalyticsController(),
    ) = HistoryManager(repository, analyticsController)
}
