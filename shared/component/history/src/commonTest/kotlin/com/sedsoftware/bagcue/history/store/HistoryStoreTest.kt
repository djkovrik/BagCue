package com.sedsoftware.bagcue.history.store

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.history.FakeHistoryRepository
import com.sedsoftware.bagcue.history.RecordingHistoryAnalyticsController
import com.sedsoftware.bagcue.history.domain.HistoryManager
import com.sedsoftware.bagcue.history.historySession
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryStoreTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun filterReopenAndDeleteUndoAreStoreOwned() = runTest(dispatcher) {
        val firstDate = LocalDate(2026, 9, 8)
        val secondDate = LocalDate(2026, 9, 9)
        val completed = historySession("completed", firstDate, completed = true)
        val planned = historySession("planned", secondDate, completed = false)
        val repository = FakeHistoryRepository(listOf(completed, planned))
        val store = HistoryStoreProvider(
            DefaultStoreFactory(),
            HistoryManager(repository, RecordingHistoryAnalyticsController()),
        ).provide()
        val labels = mutableListOf<HistoryStore.Label>()
        val disposable = store.labels(observer(onNext = labels::add))
        try {
            store.init()
            advanceUntilIdle()
            store.accept(HistoryStore.Intent.SelectDate(firstDate))
            advanceUntilIdle()
            assertEquals(listOf(completed.id), store.state.history.completed.map { it.id })

            store.accept(HistoryStore.Intent.ReopenSession(completed.id))
            advanceUntilIdle()
            assertEquals(completed.id, assertIs<HistoryStore.Label.OpenSession>(labels.single()).id)

            store.accept(HistoryStore.Intent.RequestDelete(completed.id))
            store.accept(HistoryStore.Intent.ConfirmDelete)
            advanceUntilIdle()
            assertTrue(store.state.deletedUndo != null)
            store.accept(HistoryStore.Intent.UndoDelete)
            advanceUntilIdle()
            assertEquals(null, store.state.deletedUndo)
            assertTrue(repository.sessions.value.any { it.id == completed.id })
        } finally {
            disposable.dispose()
            store.dispose()
        }
    }
}
