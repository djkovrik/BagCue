package com.sedsoftware.bagcue.templates

import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.TemplatePositionId

interface TemplateComponent : TemplateLifecycleActions, TemplatePositionActions, TemplateCatalogActions {
    val model: Value<Model>

    data class Model(
        val templates: List<TemplateSummary>,
        val isLoading: Boolean,
        val editor: Editor?,
        val itemSelector: ItemSelector?,
        val deleteConfirmation: DeleteConfirmation?,
        val showDiscardConfirmation: Boolean,
        val duplicateFeedback: UserText?,
        val error: ErrorKey?,
    )

    data class TemplateSummary(
        val id: KitTemplateId,
        val name: UserText,
        val positionCount: Int,
        val bagSummary: List<UserText>,
        val isStarter: Boolean,
    )

    data class Editor(
        val templateId: KitTemplateId?,
        val name: EditableText,
        val positions: List<Position>,
        val positionEditor: PositionEditor?,
        val isSaving: Boolean,
        val hasUnsavedChanges: Boolean,
        val validationError: ValidationError?,
    )

    data class Position(
        val id: TemplatePositionId,
        val itemId: PackingItemId,
        val itemName: UserText,
        val quantity: Int,
        val bag: UserText?,
        val source: String?,
    )

    data class PositionEditor(
        val positionId: TemplatePositionId,
        val itemName: UserText,
        val quantityInput: String,
        val bag: EditableText?,
        val sourceInput: String,
        val inheritedUsualLocation: String?,
        val validationError: ValidationError?,
    )

    data class ItemSelector(
        val query: String,
        val items: List<CatalogItem>,
        val isEmptyResult: Boolean,
    )

    data class CatalogItem(
        val id: PackingItemId,
        val name: UserText,
        val usualLocation: String?,
    )

    data class DeleteConfirmation(
        val template: TemplateSummary,
        val isDeleting: Boolean,
    )

    sealed interface UserText {
        data class Resource(val key: String) : UserText
        data class Authored(val value: String) : UserText
    }

    sealed interface EditableText {
        data class Resource(val key: String) : EditableText
        data class Input(val value: String) : EditableText
    }

    enum class ValidationError {
        BlankName,
        QuantityOutOfRange,
    }

    enum class ErrorKey {
        LoadFailed,
        SaveFailed,
        DuplicateFailed,
        DeleteFailed,
        TemplateNoLongerExists,
        ItemNoLongerExists,
    }
}

interface TemplateLifecycleActions {
    fun startCreate()
    fun openTemplate(templateId: KitTemplateId)
    fun changeTemplateName(value: String)
    fun saveTemplate()
    fun duplicateTemplate(templateId: KitTemplateId)
    fun requestDeleteTemplate(templateId: KitTemplateId)
    fun confirmDeleteTemplate()
    fun dismissDeleteTemplate()
    fun requestCloseEditor()
    fun confirmDiscardChanges()
    fun dismissDiscardChanges()
    fun clearError()
}

interface TemplatePositionActions {
    fun startAddPosition()
    fun editPosition(positionId: TemplatePositionId)
    fun changePositionQuantity(value: String)
    fun changePositionBag(value: String)
    fun changePositionSource(value: String)
    fun savePosition()
    fun removePosition(positionId: TemplatePositionId)
    fun closePositionEditor()
}

interface TemplateCatalogActions {
    fun changeItemSearch(value: String)
    fun selectItem(itemId: PackingItemId)
    fun closeItemSelector()
    fun openCatalog()
}
