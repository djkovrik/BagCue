package com.sedsoftware.bagcue.catalog.integration

import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.catalog.store.CatalogStore
import com.sedsoftware.bagcue.domain.catalog.CatalogDependency
import com.sedsoftware.bagcue.domain.catalog.PackingItem

internal fun CatalogStore.State.toComponentModel(): CatalogComponent.Model = CatalogComponent.Model(
    items = items.map(PackingItem::toComponentItem),
    isLoading = isLoading,
    editor = editor?.toComponentEditor(),
    duplicateMatch = duplicateMatch?.toComponentItem(),
    deleteConfirmation = deleteConfirmation?.let { confirmation ->
        items.firstOrNull { it.id == confirmation.summary.itemId }?.let { item ->
            CatalogComponent.DeleteConfirmation(
                item = item.toComponentItem(),
                templateDependencies = confirmation.summary.templates.map(CatalogDependency::toComponentDependency),
                unfinishedSessionDependencies = confirmation.summary.unfinishedSessions.map(CatalogDependency::toComponentDependency),
                isDeleting = confirmation.isDeleting,
            )
        }
    },
    error = error?.let { CatalogComponent.ErrorKey.valueOf(it.name) },
)

private fun PackingItem.toComponentItem(): CatalogComponent.Item = CatalogComponent.Item(
    id = id,
    name = userNameOverride?.let(CatalogComponent.UserText::Authored)
        ?: CatalogComponent.UserText.Resource(requireNotNull(seedNameKey).value),
    usualLocation = usualLocation,
    isStarter = isStarter,
)

private fun CatalogStore.Editor.toComponentEditor(): CatalogComponent.Editor = CatalogComponent.Editor(
    itemId = itemId,
    name = nameInput?.let(CatalogComponent.EditableName::Input)
        ?: CatalogComponent.EditableName.Resource(requireNotNull(seedNameKey)),
    usualLocation = usualLocation,
    isSaving = isSaving,
    validationError = CatalogComponent.ValidationError.BlankName.takeIf { blankName },
)

private fun CatalogDependency.toComponentDependency(): CatalogComponent.Dependency = CatalogComponent.Dependency(
    id = id,
    name = userName?.let(CatalogComponent.UserText::Authored)
        ?: CatalogComponent.UserText.Resource(requireNotNull(seedNameKey).value),
)
