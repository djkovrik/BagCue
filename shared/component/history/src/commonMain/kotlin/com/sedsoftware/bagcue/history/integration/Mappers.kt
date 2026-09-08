package com.sedsoftware.bagcue.history.integration

import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.history.store.HistoryStore

internal fun HistoryStore.State.toComponentModel(): HistoryComponent.Model {
    val allRows = history.planned + history.completed
    return HistoryComponent.Model(
        planned = history.planned.map(PackingSession::toRow),
        completed = history.completed.map(PackingSession::toRow),
        selectedDate = selectedDate,
        isCalendarExpanded = isCalendarExpanded,
        isLoading = isLoading,
        isEmpty = selectedDate == null && allRows.isEmpty(),
        isFilteredEmpty = selectedDate != null && allRows.isEmpty(),
        deleteConfirmation = deleteSessionId?.let { id -> allRows.firstOrNull { it.id == id }?.toRow() },
        undoAvailable = deletedUndo != null,
        error = error?.let { HistoryComponent.ErrorKey.valueOf(it.name) },
    )
}

private fun PackingSession.toRow() = HistoryComponent.SessionRow(
    id = id,
    date = localDate,
    templateNames = selectedTemplates.map { it.name.toText() },
    packedCount = items.count { it.state == SessionItemState.Packed },
    totalCount = items.size,
    isCompleted = status == PackingSessionStatus.Completed,
    skippedCount = items.count { it.skipped },
)

private fun SnapshotText.toText(): HistoryComponent.UserText = userText
    ?.let(HistoryComponent.UserText::Authored)
    ?: HistoryComponent.UserText.Resource(requireNotNull(seedNameKey).value)
