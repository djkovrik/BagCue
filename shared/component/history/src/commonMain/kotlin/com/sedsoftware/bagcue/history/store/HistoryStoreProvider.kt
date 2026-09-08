package com.sedsoftware.bagcue.history.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.PackingSessionNotFoundException
import com.sedsoftware.bagcue.domain.session.SessionHistory
import com.sedsoftware.bagcue.history.domain.HistoryManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

internal class HistoryStoreProvider(
    private val storeFactory: StoreFactory,
    private val manager: HistoryManager,
) {
    fun provide(): HistoryStore = object : HistoryStore,
        com.arkivanov.mvikotlin.core.store.Store<HistoryStore.Intent, HistoryStore.State, HistoryStore.Label> by storeFactory.create(
            name = "HistoryStore",
            initialState = HistoryStore.State(),
            bootstrapper = SimpleBootstrapper(Action.Initialize),
            executorFactory = { ExecutorImpl(manager) },
            reducer = ReducerImpl,
            autoInit = false,
        ) {}

    private sealed interface Action { data object Initialize : Action }
    private sealed interface Msg {
        data class Loaded(val history: SessionHistory) : Msg
        data class FilterChanged(val date: LocalDate?) : Msg
        data class CalendarChanged(val expanded: Boolean) : Msg
        data class DeleteRequested(val id: com.sedsoftware.bagcue.domain.session.PackingSessionId?) : Msg
        data class UndoChanged(val value: com.sedsoftware.bagcue.domain.session.DeletedSessionUndo?) : Msg
        data class Failed(val error: HistoryStore.Error) : Msg
        data object Loading : Msg
        data object ErrorCleared : Msg
    }

    private class ExecutorImpl(private val manager: HistoryManager) :
        CoroutineExecutor<HistoryStore.Intent, Action, HistoryStore.State, Msg, HistoryStore.Label>() {
        private var observation: Job? = null

        override fun executeAction(action: Action) { if (action == Action.Initialize) observe(null) }

        override fun executeIntent(intent: HistoryStore.Intent) = when (intent) {
            HistoryStore.Intent.Refresh -> observe(state().selectedDate)
            HistoryStore.Intent.ToggleCalendar -> dispatch(Msg.CalendarChanged(!state().isCalendarExpanded))
            is HistoryStore.Intent.SelectDate -> { dispatch(Msg.FilterChanged(intent.date)); observe(intent.date) }
            HistoryStore.Intent.ClearDateFilter -> { dispatch(Msg.FilterChanged(null)); observe(null) }
            is HistoryStore.Intent.OpenSession -> publish(HistoryStore.Label.OpenSession(intent.id))
            is HistoryStore.Intent.ReopenSession -> reopen(intent.id)
            is HistoryStore.Intent.RepeatSession -> repeat(intent.id)
            is HistoryStore.Intent.RequestDelete -> dispatch(Msg.DeleteRequested(intent.id))
            HistoryStore.Intent.ConfirmDelete -> delete()
            HistoryStore.Intent.DismissDelete -> dispatch(Msg.DeleteRequested(null))
            HistoryStore.Intent.UndoDelete -> undo()
            HistoryStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)
        }

        private fun observe(date: LocalDate?) {
            observation?.cancel()
            dispatch(Msg.Loading)
            observation = scope.launch {
                try {
                    manager.observeHistory(date).collectLatest { dispatch(Msg.Loaded(it)) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    dispatch(Msg.Failed(HistoryStore.Error.LoadFailed))
                }
            }
        }

        private fun reopen(id: com.sedsoftware.bagcue.domain.session.PackingSessionId) {
            scope.launch {
                manager.reopen(id).unwrap(
                    onSuccess = { publish(HistoryStore.Label.OpenSession(it.id)) },
                    onFailure = { error ->
                        val failure = if (error is PackingSessionNotFoundException) {
                            HistoryStore.Error.SessionNoLongerExists
                        } else {
                            HistoryStore.Error.ReopenFailed
                        }
                        dispatch(Msg.Failed(failure))
                    },
                )
            }
        }

        private fun repeat(id: com.sedsoftware.bagcue.domain.session.PackingSessionId) {
            scope.launch {
                manager.prepareRepeat(id).unwrap(
                    onSuccess = { publish(HistoryStore.Label.RepeatSession(it.selectedTemplateIds)) },
                    onFailure = { error ->
                        val failure = if (error is PackingSessionNotFoundException) {
                            HistoryStore.Error.SessionNoLongerExists
                        } else {
                            HistoryStore.Error.RepeatFailed
                        }
                        dispatch(Msg.Failed(failure))
                    },
                )
            }
        }

        private fun delete() {
            val id = state().deleteSessionId ?: return
            scope.launch {
                manager.delete(id).unwrap(
                    onSuccess = { dispatch(Msg.DeleteRequested(null)); dispatch(Msg.UndoChanged(it.undo)) },
                    onFailure = { error ->
                        val failure = if (error is PackingSessionNotFoundException) {
                            HistoryStore.Error.SessionNoLongerExists
                        } else {
                            HistoryStore.Error.DeleteFailed
                        }
                        dispatch(Msg.Failed(failure))
                    },
                )
            }
        }

        private fun undo() {
            val value = state().deletedUndo ?: return
            scope.launch {
                manager.undo(value).unwrap(
                    onSuccess = { result ->
                        when (result) {
                            is CreateSessionResult.Created -> dispatch(Msg.UndoChanged(null))
                            is CreateSessionResult.DateOccupied -> dispatch(Msg.Failed(HistoryStore.Error.DateOccupied))
                        }
                    },
                    onFailure = { dispatch(Msg.Failed(HistoryStore.Error.UndoFailed)) },
                )
            }
        }
    }

    private object ReducerImpl : Reducer<HistoryStore.State, Msg> {
        override fun HistoryStore.State.reduce(msg: Msg): HistoryStore.State = when (msg) {
            is Msg.Loaded -> copy(history = msg.history, isLoading = false, error = null)
            is Msg.FilterChanged -> copy(selectedDate = msg.date, isCalendarExpanded = false, error = null)
            is Msg.CalendarChanged -> copy(isCalendarExpanded = msg.expanded)
            is Msg.DeleteRequested -> copy(deleteSessionId = msg.id, error = null)
            is Msg.UndoChanged -> copy(deletedUndo = msg.value, error = null)
            is Msg.Failed -> copy(isLoading = false, error = msg.error)
            Msg.Loading -> copy(isLoading = true, error = null)
            Msg.ErrorCleared -> copy(error = null)
        }
    }
}

private inline fun <T> Result<T>.unwrap(onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    fold(onSuccess, onFailure = { error ->
        if (error is CancellationException) throw error
        onFailure(error)
    })
}
