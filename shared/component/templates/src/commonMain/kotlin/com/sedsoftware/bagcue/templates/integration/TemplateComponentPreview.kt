package com.sedsoftware.bagcue.templates.integration

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.templates.TemplateComponent

class TemplateComponentPreview(
    initialModel: TemplateComponent.Model = TemplateComponent.Model(
        templates = emptyList(),
        isLoading = false,
        editor = null,
        itemSelector = null,
        deleteConfirmation = null,
        showDiscardConfirmation = false,
        duplicateFeedback = null,
        error = null,
    ),
) : TemplateComponent {
    private val mutableModel = MutableValue(initialModel)
    override val model: Value<TemplateComponent.Model> = mutableModel

    override fun startCreate() = Unit
    override fun openTemplate(templateId: KitTemplateId) = Unit
    override fun changeTemplateName(value: String) = Unit
    override fun saveTemplate() = Unit
    override fun duplicateTemplate(templateId: KitTemplateId) = Unit
    override fun requestDeleteTemplate(templateId: KitTemplateId) = Unit
    override fun confirmDeleteTemplate() = Unit
    override fun dismissDeleteTemplate() = Unit
    override fun startAddPosition() = Unit
    override fun changeItemSearch(value: String) = Unit
    override fun selectItem(itemId: PackingItemId) = Unit
    override fun closeItemSelector() = Unit
    override fun editPosition(positionId: TemplatePositionId) = Unit
    override fun changePositionQuantity(value: String) = Unit
    override fun changePositionBag(value: String) = Unit
    override fun changePositionSource(value: String) = Unit
    override fun savePosition() = Unit
    override fun removePosition(positionId: TemplatePositionId) = Unit
    override fun closePositionEditor() = Unit
    override fun requestCloseEditor() = Unit
    override fun confirmDiscardChanges() = Unit
    override fun dismissDiscardChanges() = Unit
    override fun openCatalog() = Unit
    override fun clearError() = Unit
}
