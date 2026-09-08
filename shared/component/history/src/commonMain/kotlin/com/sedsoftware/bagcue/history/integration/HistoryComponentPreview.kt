package com.sedsoftware.bagcue.history.integration

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.history.HistoryComponent
import kotlinx.datetime.LocalDate

class HistoryComponentPreview(initialModel: HistoryComponent.Model) : HistoryComponent {
    private val mutableModel = MutableValue(initialModel)
    override val model: Value<HistoryComponent.Model> = mutableModel
    override fun refresh() = Unit
    override fun toggleCalendar() = Unit
    override fun selectDate(date: LocalDate) = Unit
    override fun clearDateFilter() = Unit
    override fun openSession(id: PackingSessionId) = Unit
    override fun reopenSession(id: PackingSessionId) = Unit
    override fun repeatSession(id: PackingSessionId) = Unit
    override fun requestDelete(id: PackingSessionId) = Unit
    override fun confirmDelete() = Unit
    override fun dismissDelete() = Unit
    override fun undoDelete() = Unit
    override fun openToday() = Unit
    override fun clearError() = Unit
}
