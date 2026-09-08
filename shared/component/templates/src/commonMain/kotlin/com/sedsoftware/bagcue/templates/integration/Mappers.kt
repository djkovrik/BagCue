package com.sedsoftware.bagcue.templates.integration

import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.templates.TemplateComponent
import com.sedsoftware.bagcue.templates.store.TemplateStore

internal fun TemplateStore.State.toComponentModel(): TemplateComponent.Model = TemplateComponent.Model(
    templates = templates.map { it.toSummary() },
    isLoading = isLoading,
    editor = editor?.toComponentEditor(catalogItems),
    itemSelector = itemSelector?.let { selector ->
        val selectedIds = editor?.positions?.mapTo(mutableSetOf(), TemplatePosition::itemId).orEmpty()
        val candidates = catalogItems
            .filterNot { it.id in selectedIds }
            .filter { item ->
                selector.query.isBlank() || catalogSearchNames[item.id]
                    ?.contains(selector.query.trim(), ignoreCase = true) == true
            }
            .map(PackingItem::toCatalogItem)
        TemplateComponent.ItemSelector(
            query = selector.query,
            items = candidates,
            isEmptyResult = candidates.isEmpty(),
        )
    },
    deleteConfirmation = deleteTemplateId?.let { id ->
        templates.firstOrNull { it.id == id }?.let { template ->
            TemplateComponent.DeleteConfirmation(template.toSummary(), isDeleting)
        }
    },
    showDiscardConfirmation = showDiscardConfirmation,
    duplicateFeedback = duplicateFeedback?.nameText(),
    error = error?.let { TemplateComponent.ErrorKey.valueOf(it.name) },
)

private fun KitTemplate.toSummary(): TemplateComponent.TemplateSummary = TemplateComponent.TemplateSummary(
    id = id,
    name = nameText(),
    positionCount = positions.size,
    bagSummary = positions.map(TemplatePosition::bagLabel)
        .filterNot { it == TemplateBagLabel.None }
        .distinct()
        .map(TemplateBagLabel::text),
    isStarter = isStarter,
)

private fun TemplateStore.Editor.toComponentEditor(
    catalogItems: List<PackingItem>,
): TemplateComponent.Editor = TemplateComponent.Editor(
    templateId = templateId.takeUnless { isNew },
    name = nameInput?.let(TemplateComponent.EditableText::Input)
        ?: TemplateComponent.EditableText.Resource(requireNotNull(seedNameKey)),
    positions = positions.map { it.toComponentPosition(catalogItems) },
    positionEditor = positionEditor?.toComponentPositionEditor(catalogItems, positions),
    isSaving = isSaving,
    hasUnsavedChanges = isDirty,
    validationError = TemplateComponent.ValidationError.BlankName.takeIf { blankName },
)

private fun TemplatePosition.toComponentPosition(
    catalogItems: List<PackingItem>,
): TemplateComponent.Position {
    val item = requireNotNull(catalogItems.firstOrNull { it.id == itemId })
    return TemplateComponent.Position(
        id = id,
        itemId = itemId,
        itemName = item.nameText(),
        quantity = quantity.value,
        bag = bagLabel.takeUnless { it == TemplateBagLabel.None }?.text(),
        source = sourceHintOverride ?: item.usualLocation,
    )
}

private fun TemplateStore.PositionEditor.toComponentPositionEditor(
    catalogItems: List<PackingItem>,
    positions: List<TemplatePosition>,
): TemplateComponent.PositionEditor {
    val position = positions.first { it.id == positionId }
    val item = requireNotNull(catalogItems.firstOrNull { it.id == position.itemId })
    return TemplateComponent.PositionEditor(
        positionId = positionId,
        itemName = item.nameText(),
        quantityInput = quantityInput,
        bag = bagLabel.takeUnless { it == TemplateBagLabel.None }?.editableText(),
        sourceInput = sourceInput,
        inheritedUsualLocation = item.usualLocation,
        validationError = TemplateComponent.ValidationError.QuantityOutOfRange.takeIf { quantityInvalid },
    )
}

private fun PackingItem.toCatalogItem(): TemplateComponent.CatalogItem = TemplateComponent.CatalogItem(
    id = id,
    name = nameText(),
    usualLocation = usualLocation,
)

private fun PackingItem.nameText(): TemplateComponent.UserText = userNameOverride
    ?.let(TemplateComponent.UserText::Authored)
    ?: TemplateComponent.UserText.Resource(requireNotNull(seedNameKey).value)

private fun KitTemplate.nameText(): TemplateComponent.UserText = userNameOverride
    ?.let(TemplateComponent.UserText::Authored)
    ?: TemplateComponent.UserText.Resource(requireNotNull(seedNameKey).value)

private fun TemplateBagLabel.text(): TemplateComponent.UserText = userText
    ?.let(TemplateComponent.UserText::Authored)
    ?: TemplateComponent.UserText.Resource(requireNotNull(seedNameKey).value)

private fun TemplateBagLabel.editableText(): TemplateComponent.EditableText = userText
    ?.let(TemplateComponent.EditableText::Input)
    ?: TemplateComponent.EditableText.Resource(requireNotNull(seedNameKey).value)
