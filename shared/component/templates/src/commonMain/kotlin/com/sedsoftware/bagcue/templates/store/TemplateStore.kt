package com.sedsoftware.bagcue.templates.store

import com.arkivanov.mvikotlin.core.store.Store
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId

internal interface TemplateStore : Store<TemplateStore.Intent, TemplateStore.State, Nothing> {
    sealed interface Intent {
        data object StartCreate : Intent
        data class OpenTemplate(val templateId: KitTemplateId) : Intent
        data class ChangeTemplateName(val value: String) : Intent
        data object SaveTemplate : Intent
        data class DuplicateTemplate(val templateId: KitTemplateId) : Intent
        data class RequestDeleteTemplate(val templateId: KitTemplateId) : Intent
        data object ConfirmDeleteTemplate : Intent
        data object DismissDeleteTemplate : Intent
        data object StartAddPosition : Intent
        data class ChangeItemSearch(val value: String) : Intent
        data class SelectItem(val itemId: PackingItemId) : Intent
        data object CloseItemSelector : Intent
        data class EditPosition(val positionId: TemplatePositionId) : Intent
        data class ChangePositionQuantity(val value: String) : Intent
        data class ChangePositionBag(val value: String) : Intent
        data class ChangePositionSource(val value: String) : Intent
        data object SavePosition : Intent
        data class RemovePosition(val positionId: TemplatePositionId) : Intent
        data object ClosePositionEditor : Intent
        data object RequestCloseEditor : Intent
        data object ConfirmDiscardChanges : Intent
        data object DismissDiscardChanges : Intent
        data object ClearError : Intent
    }

    data class State(
        val templates: List<KitTemplate> = emptyList(),
        val catalogItems: List<PackingItem> = emptyList(),
        val catalogSearchNames: Map<PackingItemId, String> = emptyMap(),
        val isLoading: Boolean = true,
        val editor: Editor? = null,
        val itemSelector: ItemSelector? = null,
        val deleteTemplateId: KitTemplateId? = null,
        val isDeleting: Boolean = false,
        val showDiscardConfirmation: Boolean = false,
        val duplicateFeedback: KitTemplate? = null,
        val error: Error? = null,
    )

    data class Editor(
        val templateId: KitTemplateId,
        val seedNameKey: String?,
        val nameInput: String?,
        val sortOrder: Long,
        val positions: List<TemplatePosition>,
        val positionEditor: PositionEditor? = null,
        val isNew: Boolean,
        val isSaving: Boolean = false,
        val isDirty: Boolean = false,
        val blankName: Boolean = false,
    )

    data class PositionEditor(
        val positionId: TemplatePositionId,
        val quantityInput: String,
        val bagLabel: TemplateBagLabel,
        val sourceInput: String,
        val quantityInvalid: Boolean = false,
    )

    data class ItemSelector(val query: String = "")

    enum class Error {
        LoadFailed,
        SaveFailed,
        DuplicateFailed,
        DeleteFailed,
        TemplateNoLongerExists,
        ItemNoLongerExists,
    }
}
