package com.sedsoftware.bagcue.catalog.integration

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.domain.catalog.PackingItemId

class CatalogComponentPreview(
    initialModel: CatalogComponent.Model = CatalogComponent.Model(
        items = emptyList(),
        isLoading = false,
        editor = null,
        duplicateMatch = null,
        deleteConfirmation = null,
        error = null,
    ),
) : CatalogComponent {
    private val mutableModel = MutableValue(initialModel)
    override val model: Value<CatalogComponent.Model> = mutableModel

    override fun startCreate() = Unit
    override fun startEdit(itemId: PackingItemId) = Unit
    override fun changeName(value: String) = Unit
    override fun changeUsualLocation(value: String) = Unit
    override fun save() = Unit
    override fun createAnother() = Unit
    override fun useExistingMatch() = Unit
    override fun requestDelete(itemId: PackingItemId) = Unit
    override fun confirmDelete() = Unit
    override fun dismissDelete() = Unit
    override fun closeEditor() = Unit
    override fun clearError() = Unit
}
