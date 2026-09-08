package com.sedsoftware.bagcue.templates.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.optionalUserText
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.templates.domain.TemplateData
import com.sedsoftware.bagcue.templates.domain.TemplateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class TemplateStoreProvider(
    private val storeFactory: StoreFactory,
    private val manager: TemplateManager,
) {
    fun provide(): TemplateStore = object : TemplateStore,
        com.arkivanov.mvikotlin.core.store.Store<TemplateStore.Intent, TemplateStore.State, Nothing> by storeFactory.create(
            name = "TemplateStore",
            initialState = TemplateStore.State(),
            bootstrapper = SimpleBootstrapper(Action.Initialize),
            executorFactory = { ExecutorImpl(manager) },
            reducer = ReducerImpl,
            autoInit = false,
        ) {}

    private sealed interface Action {
        data object Initialize : Action
    }

    private sealed interface Msg {
        data class DataLoaded(val data: TemplateData) : Msg
        data class EditorOpened(val editor: TemplateStore.Editor) : Msg
        data class TemplateNameChanged(val value: String) : Msg
        data class SavingChanged(val value: Boolean) : Msg
        data object BlankName : Msg
        data object TemplateSaved : Msg
        data class TemplateDuplicated(val template: KitTemplate) : Msg
        data class DeleteRequested(val templateId: com.sedsoftware.bagcue.domain.template.KitTemplateId) : Msg
        data class DeletingChanged(val value: Boolean) : Msg
        data object DeleteDismissed : Msg
        data class TemplateDeleted(val templateId: com.sedsoftware.bagcue.domain.template.KitTemplateId) : Msg
        data class SelectorChanged(val selector: TemplateStore.ItemSelector?) : Msg
        data class PositionAdded(val position: TemplatePosition) : Msg
        data class PositionEditorChanged(val editor: TemplateStore.PositionEditor?) : Msg
        data class PositionApplied(val position: TemplatePosition) : Msg
        data class PositionRemoved(val positionId: TemplatePositionId) : Msg
        data object QuantityInvalid : Msg
        data object CloseEditorRequested : Msg
        data object EditorClosed : Msg
        data object DiscardDismissed : Msg
        data class Failed(val error: TemplateStore.Error) : Msg
        data object ErrorCleared : Msg
    }

    private class ExecutorImpl(
        private val manager: TemplateManager,
    ) : CoroutineExecutor<TemplateStore.Intent, Action, TemplateStore.State, Msg, Nothing>() {
        override fun executeAction(action: Action) {
            when (action) {
                Action.Initialize -> initialize()
            }
        }

        override fun executeIntent(intent: TemplateStore.Intent) {
            when (intent) {
                TemplateStore.Intent.StartCreate -> startCreate()
                is TemplateStore.Intent.OpenTemplate -> openTemplate(intent.templateId)
                is TemplateStore.Intent.ChangeTemplateName -> dispatch(Msg.TemplateNameChanged(intent.value))
                TemplateStore.Intent.SaveTemplate -> saveTemplate()
                is TemplateStore.Intent.DuplicateTemplate -> duplicate(intent.templateId)
                is TemplateStore.Intent.RequestDeleteTemplate -> dispatch(Msg.DeleteRequested(intent.templateId))
                TemplateStore.Intent.ConfirmDeleteTemplate -> deleteTemplate()
                TemplateStore.Intent.DismissDeleteTemplate -> dispatch(Msg.DeleteDismissed)
                TemplateStore.Intent.StartAddPosition -> dispatch(Msg.SelectorChanged(TemplateStore.ItemSelector()))
                is TemplateStore.Intent.ChangeItemSearch -> dispatch(Msg.SelectorChanged(TemplateStore.ItemSelector(intent.value)))
                is TemplateStore.Intent.SelectItem -> selectItem(intent.itemId)
                TemplateStore.Intent.CloseItemSelector -> dispatch(Msg.SelectorChanged(null))
                is TemplateStore.Intent.EditPosition -> editPosition(intent.positionId)
                is TemplateStore.Intent.ChangePositionQuantity -> updatePositionEditor(quantity = intent.value)
                is TemplateStore.Intent.ChangePositionBag -> updatePositionEditor(bag = intent.value)
                is TemplateStore.Intent.ChangePositionSource -> updatePositionEditor(source = intent.value)
                TemplateStore.Intent.SavePosition -> savePosition()
                is TemplateStore.Intent.RemovePosition -> dispatch(Msg.PositionRemoved(intent.positionId))
                TemplateStore.Intent.ClosePositionEditor -> dispatch(Msg.PositionEditorChanged(null))
                TemplateStore.Intent.RequestCloseEditor -> dispatch(Msg.CloseEditorRequested)
                TemplateStore.Intent.ConfirmDiscardChanges -> dispatch(Msg.EditorClosed)
                TemplateStore.Intent.DismissDiscardChanges -> dispatch(Msg.DiscardDismissed)
                TemplateStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)
            }
        }

        private fun initialize() {
            scope.launch {
                manager.initialize().unwrap(
                    onSuccess = {
                        scope.launch {
                            try {
                                manager.observeData().collectLatest { dispatch(Msg.DataLoaded(it)) }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Throwable) {
                                dispatch(Msg.Failed(TemplateStore.Error.LoadFailed))
                            }
                        }
                    },
                    onFailure = { dispatch(Msg.Failed(TemplateStore.Error.LoadFailed)) },
                )
            }
        }

        private fun startCreate() {
            val nextSortOrder = (state().templates.maxOfOrNull(KitTemplate::sortOrder) ?: -1L) + 1L
            dispatch(
                Msg.EditorOpened(
                    TemplateStore.Editor(
                        templateId = manager.nextTemplateId(),
                        seedNameKey = null,
                        nameInput = "",
                        sortOrder = nextSortOrder,
                        positions = emptyList(),
                        isNew = true,
                        isDirty = true,
                    ),
                ),
            )
        }

        private fun openTemplate(templateId: com.sedsoftware.bagcue.domain.template.KitTemplateId) {
            val template = state().templates.firstOrNull { it.id == templateId }
            if (template == null) {
                dispatch(Msg.Failed(TemplateStore.Error.TemplateNoLongerExists))
                return
            }
            dispatch(
                Msg.EditorOpened(
                    TemplateStore.Editor(
                        templateId = template.id,
                        seedNameKey = template.seedNameKey?.value,
                        nameInput = template.userNameOverride,
                        sortOrder = template.sortOrder,
                        positions = template.positions,
                        isNew = false,
                    ),
                ),
            )
        }

        private fun saveTemplate() {
            val editor = state().editor ?: return
            if (editor.nameInput != null && editor.nameInput.isBlank()) {
                dispatch(Msg.BlankName)
                return
            }
            scope.launch {
                dispatch(Msg.SavingChanged(true))
                val result = runCatchingTemplate {
                    KitTemplate(
                        id = editor.templateId,
                        seedNameKey = editor.seedNameKey?.let(::ResourceKey),
                        userNameOverride = editor.nameInput?.let(::validatedCatalogName),
                        sortOrder = editor.sortOrder,
                        positions = editor.positions,
                    )
                }.fold(
                    onSuccess = { manager.save(it) },
                    onFailure = { Result.failure(it) },
                )
                result.unwrap(
                    onSuccess = { dispatch(Msg.TemplateSaved) },
                    onFailure = { dispatch(Msg.Failed(TemplateStore.Error.SaveFailed)) },
                )
            }
        }

        private fun duplicate(templateId: com.sedsoftware.bagcue.domain.template.KitTemplateId) {
            scope.launch {
                manager.duplicate(templateId).unwrap(
                    onSuccess = { dispatch(Msg.TemplateDuplicated(it)) },
                    onFailure = { dispatch(Msg.Failed(TemplateStore.Error.DuplicateFailed)) },
                )
            }
        }

        private fun deleteTemplate() {
            val id = state().deleteTemplateId ?: return
            scope.launch {
                dispatch(Msg.DeletingChanged(true))
                manager.delete(id).unwrap(
                    onSuccess = { dispatch(Msg.TemplateDeleted(id)) },
                    onFailure = { dispatch(Msg.Failed(TemplateStore.Error.DeleteFailed)) },
                )
            }
        }

        private fun selectItem(itemId: PackingItemId) {
            val editor = state().editor ?: return
            if (editor.positions.any { it.itemId == itemId }) {
                dispatch(Msg.SelectorChanged(null))
                return
            }
            val item = state().catalogItems.firstOrNull { it.id == itemId }
            if (item == null) {
                dispatch(Msg.Failed(TemplateStore.Error.ItemNoLongerExists))
                return
            }
            val position = TemplatePosition(
                id = manager.nextPositionId(),
                itemId = item.id,
                quantity = PositionQuantity(1),
                bagLabel = TemplateBagLabel.None,
                sourceHintOverride = null,
                sortOrder = editor.positions.size.toLong(),
            )
            dispatch(Msg.PositionAdded(position))
            dispatch(Msg.SelectorChanged(null))
            editPosition(position.id)
        }

        private fun editPosition(positionId: TemplatePositionId) {
            val position = state().editor?.positions?.firstOrNull { it.id == positionId } ?: return
            dispatch(
                Msg.PositionEditorChanged(
                    TemplateStore.PositionEditor(
                        positionId = position.id,
                        quantityInput = position.quantity.value.toString(),
                        bagLabel = position.bagLabel,
                        sourceInput = position.sourceHintOverride.orEmpty(),
                    ),
                ),
            )
        }

        private fun updatePositionEditor(
            quantity: String? = null,
            bag: String? = null,
            source: String? = null,
        ) {
            val current = state().editor?.positionEditor ?: return
            dispatch(
                Msg.PositionEditorChanged(
                    current.copy(
                        quantityInput = quantity ?: current.quantityInput,
                        bagLabel = bag?.let(TemplateBagLabel::user) ?: current.bagLabel,
                        sourceInput = source ?: current.sourceInput,
                        quantityInvalid = false,
                    ),
                ),
            )
        }

        private fun savePosition() {
            val editor = state().editor
            val positionEditor = editor?.positionEditor
            if (editor == null || positionEditor == null) return
            val quantity = positionEditor.quantityInput.toIntOrNull()
            if (quantity == null || quantity !in PositionQuantity.MIN_VALUE..PositionQuantity.MAX_VALUE) {
                dispatch(Msg.QuantityInvalid)
            } else {
                editor.positions.firstOrNull { it.id == positionEditor.positionId }?.let { original ->
                    dispatch(
                        Msg.PositionApplied(
                            original.copy(
                                quantity = PositionQuantity(quantity),
                                bagLabel = positionEditor.bagLabel,
                                sourceHintOverride = optionalUserText(positionEditor.sourceInput),
                            ),
                        ),
                    )
                }
            }
        }
    }

    private object ReducerImpl : Reducer<TemplateStore.State, Msg> {
        override fun TemplateStore.State.reduce(msg: Msg): TemplateStore.State = when (msg) {
            is Msg.DataLoaded -> {
                val availableIds = msg.data.catalogItems.mapTo(mutableSetOf(), PackingItem::id)
                val currentEditor = editor
                val retainedEditor = currentEditor?.let { draft ->
                    val retainedPositions = draft.positions.filter { it.itemId in availableIds }
                    draft.copy(
                        positions = retainedPositions,
                        positionEditor = draft.positionEditor?.takeIf { positionEditor ->
                            retainedPositions.any { it.id == positionEditor.positionId }
                        },
                        isDirty = draft.isDirty || retainedPositions.size != draft.positions.size,
                    )
                }
                copy(
                    templates = msg.data.templates,
                    catalogItems = msg.data.catalogItems,
                    catalogSearchNames = msg.data.catalogSearchNames,
                    isLoading = false,
                    editor = retainedEditor,
                    error = null,
                )
            }
            is Msg.EditorOpened -> copy(editor = msg.editor, itemSelector = null, duplicateFeedback = null, error = null)
            is Msg.TemplateNameChanged -> copy(
                editor = editor?.copy(nameInput = msg.value, isDirty = true, blankName = false),
                error = null,
            )
            is Msg.SavingChanged -> copy(editor = editor?.copy(isSaving = msg.value), error = null)
            Msg.BlankName -> copy(editor = editor?.copy(isSaving = false, blankName = true))
            Msg.TemplateSaved -> copy(editor = null, itemSelector = null, error = null)
            is Msg.TemplateDuplicated -> copy(duplicateFeedback = msg.template, error = null)
            is Msg.DeleteRequested -> copy(deleteTemplateId = msg.templateId, isDeleting = false, error = null)
            is Msg.DeletingChanged -> copy(isDeleting = msg.value, error = null)
            Msg.DeleteDismissed -> copy(deleteTemplateId = null, isDeleting = false)
            else -> reduceEditorMessage(msg)
        }

        private fun TemplateStore.State.reduceEditorMessage(msg: Msg): TemplateStore.State = when (msg) {
            is Msg.TemplateDeleted -> copy(
                deleteTemplateId = null,
                isDeleting = false,
                editor = editor?.takeUnless { it.templateId == msg.templateId },
                error = null,
            )
            is Msg.SelectorChanged -> copy(itemSelector = msg.selector, error = null)
            is Msg.PositionAdded -> copy(
                editor = editor?.let { draft ->
                    draft.copy(positions = draft.positions + msg.position, isDirty = true)
                },
                error = null,
            )
            is Msg.PositionEditorChanged -> copy(editor = editor?.copy(positionEditor = msg.editor), error = null)
            is Msg.PositionApplied -> copy(
                editor = editor?.let { draft ->
                    draft.copy(
                        positions = draft.positions.map { if (it.id == msg.position.id) msg.position else it },
                        positionEditor = null,
                        isDirty = true,
                    )
                },
                error = null,
            )
            is Msg.PositionRemoved -> copy(
                editor = editor?.let { draft ->
                    draft.copy(
                        positions = draft.positions.filterNot { it.id == msg.positionId }
                            .mapIndexed { index, position -> position.copy(sortOrder = index.toLong()) },
                        positionEditor = null,
                        isDirty = true,
                    )
                },
            )
            Msg.QuantityInvalid -> copy(
                editor = editor?.let { draft ->
                    draft.copy(positionEditor = draft.positionEditor?.copy(quantityInvalid = true))
                },
            )
            Msg.CloseEditorRequested -> if (editor?.isDirty == true) copy(showDiscardConfirmation = true)
            else copy(editor = null, itemSelector = null)
            Msg.EditorClosed -> copy(editor = null, itemSelector = null, showDiscardConfirmation = false)
            Msg.DiscardDismissed -> copy(showDiscardConfirmation = false)
            is Msg.Failed -> copy(
                isLoading = if (msg.error == TemplateStore.Error.LoadFailed) false else isLoading,
                isDeleting = false,
                editor = editor?.copy(isSaving = false),
                error = msg.error,
            )
            Msg.ErrorCleared -> copy(error = null, duplicateFeedback = null)
            else -> this
        }
    }
}

private inline fun <T> Result<T>.unwrap(onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    fold(
        onSuccess = onSuccess,
        onFailure = { error ->
            if (error is CancellationException) throw error
            onFailure(error)
        },
    )
}

private inline fun <T> runCatchingTemplate(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
