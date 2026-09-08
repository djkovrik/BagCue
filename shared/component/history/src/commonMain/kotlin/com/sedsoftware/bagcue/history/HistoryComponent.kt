package com.sedsoftware.bagcue.history

import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import kotlinx.datetime.LocalDate

interface HistoryComponent {
    val model: Value<Model>

    fun refresh()
    fun toggleCalendar()
    fun selectDate(date: LocalDate)
    fun clearDateFilter()
    fun openSession(id: PackingSessionId)
    fun reopenSession(id: PackingSessionId)
    fun repeatSession(id: PackingSessionId)
    fun requestDelete(id: PackingSessionId)
    fun confirmDelete()
    fun dismissDelete()
    fun undoDelete()
    fun openToday()
    fun clearError()

    data class Model(
        val planned: List<SessionRow>,
        val completed: List<SessionRow>,
        val selectedDate: LocalDate?,
        val isCalendarExpanded: Boolean,
        val isLoading: Boolean,
        val isEmpty: Boolean,
        val isFilteredEmpty: Boolean,
        val deleteConfirmation: SessionRow?,
        val undoAvailable: Boolean,
        val error: ErrorKey?,
    )

    data class SessionRow(
        val id: PackingSessionId,
        val date: LocalDate,
        val templateNames: List<UserText>,
        val packedCount: Int,
        val totalCount: Int,
        val isCompleted: Boolean,
        val skippedCount: Int,
    )

    sealed interface UserText {
        data class Resource(val key: String) : UserText
        data class Authored(val value: String) : UserText
    }

    enum class ErrorKey {
        LoadFailed,
        ReopenFailed,
        RepeatFailed,
        DeleteFailed,
        UndoFailed,
        DateOccupied,
        SessionNoLongerExists,
    }
}
