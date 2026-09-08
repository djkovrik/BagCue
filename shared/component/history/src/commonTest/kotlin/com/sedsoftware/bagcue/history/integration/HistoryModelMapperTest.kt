package com.sedsoftware.bagcue.history.integration

import com.sedsoftware.bagcue.domain.session.SessionHistory
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.history.historySession
import com.sedsoftware.bagcue.history.store.HistoryStore
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryModelMapperTest {
    @Test
    fun mapsPlannedCompletedAndLocaleNeutralTemplateNames() {
        val planned = historySession("planned", LocalDate(2026, 9, 9), completed = false)
        val completed = historySession("completed", LocalDate(2026, 9, 8), completed = true)

        val model = HistoryStore.State(history = SessionHistory(listOf(planned), listOf(completed)), isLoading = false)
            .toComponentModel()

        assertEquals(listOf(planned.id), model.planned.map { it.id })
        assertEquals(listOf(completed.id), model.completed.map { it.id })
        assertEquals(HistoryComponent.UserText.Resource("starter_template"), model.completed.single().templateNames.single())
        assertFalse(model.isEmpty)
    }

    @Test
    fun distinguishesGlobalEmptyFromFilteredEmptyAndMapsDeleteTarget() {
        val date = LocalDate(2026, 9, 8)
        val filteredEmpty = HistoryStore.State(selectedDate = date, isLoading = false).toComponentModel()
        assertTrue(filteredEmpty.isFilteredEmpty)
        assertFalse(filteredEmpty.isEmpty)

        val session = historySession("session", date, completed = false)
        val confirmation = HistoryStore.State(
            history = SessionHistory(listOf(session), emptyList()),
            deleteSessionId = session.id,
            isLoading = false,
        ).toComponentModel()
        assertEquals(session.id, confirmation.deleteConfirmation?.id)
    }
}
