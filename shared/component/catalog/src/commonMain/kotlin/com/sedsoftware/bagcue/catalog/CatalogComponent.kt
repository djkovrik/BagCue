package com.sedsoftware.bagcue.catalog

import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.catalog.PackingItemId

interface CatalogComponent {
    val model: Value<Model>

    fun startCreate()
    fun startEdit(itemId: PackingItemId)
    fun changeName(value: String)
    fun changeUsualLocation(value: String)
    fun save()
    fun createAnother()
    fun useExistingMatch()
    fun requestDelete(itemId: PackingItemId)
    fun confirmDelete()
    fun dismissDelete()
    fun closeEditor()
    fun clearError()

    data class Model(
        val items: List<Item>,
        val isLoading: Boolean,
        val editor: Editor?,
        val duplicateMatch: Item?,
        val deleteConfirmation: DeleteConfirmation?,
        val error: ErrorKey?,
    )

    data class Item(
        val id: PackingItemId,
        val name: UserText,
        val usualLocation: String?,
        val isStarter: Boolean,
    )

    data class Editor(
        val itemId: PackingItemId?,
        val name: EditableName,
        val usualLocation: String,
        val isSaving: Boolean,
        val validationError: ValidationError?,
    )

    data class DeleteConfirmation(
        val item: Item,
        val templateDependencies: List<Dependency>,
        val unfinishedSessionDependencies: List<Dependency>,
        val isDeleting: Boolean,
    )

    data class Dependency(
        val id: String,
        val name: UserText,
    )

    sealed interface UserText {
        data class Resource(val key: String) : UserText
        data class Authored(val value: String) : UserText
    }

    sealed interface EditableName {
        data class Resource(val key: String) : EditableName
        data class Input(val value: String) : EditableName
    }

    enum class ValidationError {
        BlankName,
    }

    enum class ErrorKey {
        LoadFailed,
        SaveFailed,
        DependencyLoadFailed,
        DeleteFailed,
        ItemNoLongerExists,
    }
}
