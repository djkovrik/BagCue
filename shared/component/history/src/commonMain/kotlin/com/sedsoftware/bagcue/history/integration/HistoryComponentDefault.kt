package com.sedsoftware.bagcue.history.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.history.domain.HistoryManager
import com.sedsoftware.bagcue.history.store.HistoryStore
import com.sedsoftware.bagcue.history.store.HistoryStoreProvider
import kotlinx.datetime.LocalDate

class HistoryComponentDefault(
    componentContext: ComponentContext,
    repository: SessionHistoryRepository,
    storeFactory: StoreFactory,
    analyticsController: AnalyticsController,
    private val onOpenSession: (PackingSessionId) -> Unit,
    private val onRepeatSession: (List<KitTemplateId>) -> Unit,
    private val onOpenToday: () -> Unit,
) : HistoryComponent, ComponentContext by componentContext {
    private val holder = instanceKeeper.getOrCreate(key = STORE_KEY) {
        val store = HistoryStoreProvider(
            storeFactory,
            HistoryManager(repository, analyticsController),
        ).provide()
        val labels = store.labels(observer(onNext = ::handleLabel))
        store.init()
        StoreHolder(store, labels::dispose)
    }
    private val store = holder.store

    override val model: Value<HistoryComponent.Model> = store.asValue().map(HistoryStore.State::toComponentModel)
    override fun refresh() = store.accept(HistoryStore.Intent.Refresh)
    override fun toggleCalendar() = store.accept(HistoryStore.Intent.ToggleCalendar)
    override fun selectDate(date: LocalDate) = store.accept(HistoryStore.Intent.SelectDate(date))
    override fun clearDateFilter() = store.accept(HistoryStore.Intent.ClearDateFilter)
    override fun openSession(id: PackingSessionId) = store.accept(HistoryStore.Intent.OpenSession(id))
    override fun reopenSession(id: PackingSessionId) = store.accept(HistoryStore.Intent.ReopenSession(id))
    override fun repeatSession(id: PackingSessionId) = store.accept(HistoryStore.Intent.RepeatSession(id))
    override fun requestDelete(id: PackingSessionId) = store.accept(HistoryStore.Intent.RequestDelete(id))
    override fun confirmDelete() = store.accept(HistoryStore.Intent.ConfirmDelete)
    override fun dismissDelete() = store.accept(HistoryStore.Intent.DismissDelete)
    override fun undoDelete() = store.accept(HistoryStore.Intent.UndoDelete)
    override fun openToday() = onOpenToday()
    override fun clearError() = store.accept(HistoryStore.Intent.ClearError)

    private fun handleLabel(label: HistoryStore.Label) = when (label) {
        is HistoryStore.Label.OpenSession -> onOpenSession(label.id)
        is HistoryStore.Label.RepeatSession -> onRepeatSession(label.templateIds)
    }

    private class StoreHolder(val store: HistoryStore, private val disposeLabels: () -> Unit) : InstanceKeeper.Instance {
        override fun onDestroy() { disposeLabels(); store.dispose() }
    }

    private companion object { const val STORE_KEY = "HistoryStore" }
}
