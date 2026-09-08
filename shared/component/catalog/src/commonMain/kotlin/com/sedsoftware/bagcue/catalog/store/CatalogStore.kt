package com.sedsoftware.bagcue.catalog.store

import com.arkivanov.mvikotlin.core.store.Store
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId

internal interface CatalogStore : Store<CatalogStore.Intent, CatalogStore.State, Nothing> {
    sealed interface Intent {
        data object StartCreate : Intent
        data class StartEdit(val itemId: PackingItemId) : Intent
        data class ChangeName(val value: String) : Intent
        data class ChangeUsualLocation(val value: String) : Intent
        data object Save : Intent
        data object CreateAnother : Intent
        data object UseExistingMatch : Intent
        data class RequestDelete(val itemId: PackingItemId) : Intent
        data object ConfirmDelete : Intent
        data object DismissDelete : Intent
        data object CloseEditor : Intent
        data object ClearError : Intent
    }

    data class State(
        val items: List<PackingItem> = emptyList(),
        val isLoading: Boolean = true,
        val editor: Editor? = null,
        val duplicateMatch: PackingItem? = null,
        val deleteConfirmation: DeleteConfirmation? = null,
        val error: Error? = null,
    )

    data class Editor(
        val itemId: PackingItemId?,
        val seedNameKey: String?,
        val nameInput: String?,
        val usualLocation: String,
        val isSaving: Boolean = false,
        val blankName: Boolean = false,
    )

    data class DeleteConfirmation(
        val summary: CatalogDependencySummary,
        val isDeleting: Boolean = false,
    )

    enum class Error {
        LoadFailed,
        SaveFailed,
        DependencyLoadFailed,
        DeleteFailed,
        ItemNoLongerExists,
    }
}
