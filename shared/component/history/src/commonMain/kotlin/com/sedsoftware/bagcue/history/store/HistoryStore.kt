package com.sedsoftware.bagcue.history.store

import com.arkivanov.mvikotlin.core.store.Store
import com.sedsoftware.bagcue.domain.session.DeletedSessionUndo
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionHistory
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import kotlinx.datetime.LocalDate

internal interface HistoryStore : Store<HistoryStore.Intent, HistoryStore.State, HistoryStore.Label> {
    sealed interface Intent {
        data object Refresh : Intent
        data object ToggleCalendar : Intent
        data class SelectDate(val date: LocalDate) : Intent
        data object ClearDateFilter : Intent
        data class OpenSession(val id: PackingSessionId) : Intent
        data class ReopenSession(val id: PackingSessionId) : Intent
        data class RepeatSession(val id: PackingSessionId) : Intent
        data class RequestDelete(val id: PackingSessionId) : Intent
        data object ConfirmDelete : Intent
        data object DismissDelete : Intent
        data object UndoDelete : Intent
        data object ClearError : Intent
    }

    data class State(
        val history: SessionHistory = SessionHistory(emptyList(), emptyList()),
        val selectedDate: LocalDate? = null,
        val isCalendarExpanded: Boolean = false,
        val isLoading: Boolean = true,
        val deleteSessionId: PackingSessionId? = null,
        val deletedUndo: DeletedSessionUndo? = null,
        val error: Error? = null,
    )

    sealed interface Label {
        data class OpenSession(val id: PackingSessionId) : Label
        data class RepeatSession(val templateIds: List<KitTemplateId>) : Label
    }

    enum class Error { LoadFailed, ReopenFailed, RepeatFailed, DeleteFailed, UndoFailed, DateOccupied, SessionNoLongerExists }
}
