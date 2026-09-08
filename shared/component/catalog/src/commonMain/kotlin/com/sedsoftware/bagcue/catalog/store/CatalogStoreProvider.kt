package com.sedsoftware.bagcue.catalog.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.sedsoftware.bagcue.catalog.domain.CatalogManager
import com.sedsoftware.bagcue.domain.catalog.CatalogCreateOutcome
import com.sedsoftware.bagcue.domain.catalog.DuplicateDecision
import com.sedsoftware.bagcue.domain.catalog.InvalidCatalogNameException
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class CatalogStoreProvider(
    private val storeFactory: StoreFactory,
    private val manager: CatalogManager,
) {
    fun provide(): CatalogStore = object : CatalogStore,
        com.arkivanov.mvikotlin.core.store.Store<CatalogStore.Intent, CatalogStore.State, Nothing> by storeFactory.create(
            name = "CatalogStore",
            initialState = CatalogStore.State(),
            bootstrapper = SimpleBootstrapper(Action.Initialize),
            executorFactory = { ExecutorImpl(manager) },
            reducer = ReducerImpl,
            autoInit = false,
        ) {}

    private sealed interface Action {
        data object Initialize : Action
    }

    private sealed interface Msg {
        data class ItemsLoaded(val items: List<PackingItem>) : Msg
        data class EditorOpened(val editor: CatalogStore.Editor) : Msg
        data class EditorNameChanged(val value: String) : Msg
        data class EditorLocationChanged(val value: String) : Msg
        data class SavingChanged(val value: Boolean) : Msg
        data object BlankName : Msg
        data class DuplicateFound(val item: PackingItem) : Msg
        data object Saved : Msg
        data class DeleteLoaded(val summary: com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary) : Msg
        data class DeletingChanged(val value: Boolean) : Msg
        data object DeleteDismissed : Msg
        data object Deleted : Msg
        data object EditorClosed : Msg
        data class Failed(val error: CatalogStore.Error) : Msg
        data object ErrorCleared : Msg
    }

    private class ExecutorImpl(
        private val manager: CatalogManager,
    ) : CoroutineExecutor<CatalogStore.Intent, Action, CatalogStore.State, Msg, Nothing>() {
        private var observationJob: Job? = null

        override fun executeAction(action: Action) {
            when (action) {
                Action.Initialize -> initialize()
            }
        }

        private fun initialize() {
            scope.launch {
                manager.initialize().unwrap(
                    onSuccess = {
                        observationJob = scope.launch {
                            try {
                                manager.observeItems().collectLatest { dispatch(Msg.ItemsLoaded(it)) }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Throwable) {
                                dispatch(Msg.Failed(CatalogStore.Error.LoadFailed))
                            }
                        }
                    },
                    onFailure = { dispatch(Msg.Failed(CatalogStore.Error.LoadFailed)) },
                )
            }
        }

        override fun executeIntent(intent: CatalogStore.Intent) {
            when (intent) {
                CatalogStore.Intent.StartCreate -> dispatch(
                    Msg.EditorOpened(
                        CatalogStore.Editor(
                            itemId = null,
                            seedNameKey = null,
                            nameInput = "",
                            usualLocation = "",
                        ),
                    ),
                )

                is CatalogStore.Intent.StartEdit -> openEditor(intent.itemId)
                is CatalogStore.Intent.ChangeName -> dispatch(Msg.EditorNameChanged(intent.value))
                is CatalogStore.Intent.ChangeUsualLocation -> dispatch(Msg.EditorLocationChanged(intent.value))
                CatalogStore.Intent.Save -> save(duplicateDecision = null)
                CatalogStore.Intent.CreateAnother -> save(DuplicateDecision.CreateAnother)
                CatalogStore.Intent.UseExistingMatch -> dispatch(Msg.Saved)
                is CatalogStore.Intent.RequestDelete -> loadDelete(intent.itemId)
                CatalogStore.Intent.ConfirmDelete -> delete()
                CatalogStore.Intent.DismissDelete -> dispatch(Msg.DeleteDismissed)
                CatalogStore.Intent.CloseEditor -> dispatch(Msg.EditorClosed)
                CatalogStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)
            }
        }

        private fun openEditor(itemId: PackingItemId) {
            val item = state().items.firstOrNull { it.id == itemId }
            if (item == null) {
                dispatch(Msg.Failed(CatalogStore.Error.ItemNoLongerExists))
                return
            }
            dispatch(
                Msg.EditorOpened(
                    CatalogStore.Editor(
                        itemId = item.id,
                        seedNameKey = item.seedNameKey?.value,
                        nameInput = item.userNameOverride,
                        usualLocation = item.usualLocation.orEmpty(),
                    ),
                ),
            )
        }

        private fun save(duplicateDecision: DuplicateDecision?) {
            val editor = state().editor ?: return
            val name = editor.nameInput
            if (name != null && name.isBlank()) {
                dispatch(Msg.BlankName)
                return
            }
            if (editor.itemId == null && name == null) {
                dispatch(Msg.BlankName)
                return
            }

            scope.launch {
                dispatch(Msg.SavingChanged(true))
                val result = if (editor.itemId == null) {
                    manager.create(
                        name = requireNotNull(name),
                        usualLocation = editor.usualLocation,
                        decision = duplicateDecision,
                    ).map { it }
                } else {
                    manager.update(
                        itemId = editor.itemId,
                        userNameOverride = name,
                        usualLocation = editor.usualLocation,
                    ).map { CatalogCreateOutcome.Created(it) }
                }
                result.unwrap(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is CatalogCreateOutcome.NeedsDecision -> dispatch(Msg.DuplicateFound(outcome.existing))
                            else -> dispatch(Msg.Saved)
                        }
                    },
                    onFailure = { error ->
                        if (error is InvalidCatalogNameException) dispatch(Msg.BlankName)
                        else dispatch(Msg.Failed(CatalogStore.Error.SaveFailed))
                    },
                )
            }
        }

        private fun loadDelete(itemId: PackingItemId) {
            scope.launch {
                manager.readDeleteDependencies(itemId).unwrap(
                    onSuccess = { dispatch(Msg.DeleteLoaded(it)) },
                    onFailure = { dispatch(Msg.Failed(CatalogStore.Error.DependencyLoadFailed)) },
                )
            }
        }

        private fun delete() {
            val itemId = state().deleteConfirmation?.summary?.itemId ?: return
            scope.launch {
                dispatch(Msg.DeletingChanged(true))
                manager.delete(itemId).unwrap(
                    onSuccess = { dispatch(Msg.Deleted) },
                    onFailure = { dispatch(Msg.Failed(CatalogStore.Error.DeleteFailed)) },
                )
            }
        }
    }

    private object ReducerImpl : Reducer<CatalogStore.State, Msg> {
        override fun CatalogStore.State.reduce(msg: Msg): CatalogStore.State = when (msg) {
            is Msg.ItemsLoaded -> copy(items = msg.items, isLoading = false, error = null)
            is Msg.EditorOpened -> copy(editor = msg.editor, duplicateMatch = null, error = null)
            is Msg.EditorNameChanged -> copy(editor = editor?.copy(nameInput = msg.value, blankName = false), duplicateMatch = null)
            is Msg.EditorLocationChanged -> copy(editor = editor?.copy(usualLocation = msg.value), error = null)
            is Msg.SavingChanged -> copy(editor = editor?.copy(isSaving = msg.value), error = null)
            Msg.BlankName -> copy(editor = editor?.copy(isSaving = false, blankName = true))
            is Msg.DuplicateFound -> copy(editor = editor?.copy(isSaving = false), duplicateMatch = msg.item)
            Msg.Saved -> copy(editor = null, duplicateMatch = null, error = null)
            is Msg.DeleteLoaded -> copy(deleteConfirmation = CatalogStore.DeleteConfirmation(msg.summary), error = null)
            is Msg.DeletingChanged -> copy(deleteConfirmation = deleteConfirmation?.copy(isDeleting = msg.value), error = null)
            Msg.DeleteDismissed -> copy(deleteConfirmation = null)
            Msg.Deleted -> copy(deleteConfirmation = null, editor = null, error = null)
            Msg.EditorClosed -> copy(editor = null, duplicateMatch = null)
            is Msg.Failed -> copy(
                isLoading = if (msg.error == CatalogStore.Error.LoadFailed) false else isLoading,
                editor = editor?.copy(isSaving = false),
                deleteConfirmation = deleteConfirmation?.copy(isDeleting = false),
                error = msg.error,
            )
            Msg.ErrorCleared -> copy(error = null)
        }
    }
}

private inline fun <T> Result<T>.unwrap(
    onSuccess: (T) -> Unit,
    onFailure: (Throwable) -> Unit,
) {
    fold(
        onSuccess = onSuccess,
        onFailure = { error ->
            if (error is CancellationException) throw error
            onFailure(error)
        },
    )
}
