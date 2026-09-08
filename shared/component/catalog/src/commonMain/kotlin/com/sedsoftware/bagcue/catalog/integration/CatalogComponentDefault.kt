package com.sedsoftware.bagcue.catalog.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.catalog.domain.CatalogManager
import com.sedsoftware.bagcue.catalog.store.CatalogStore
import com.sedsoftware.bagcue.catalog.store.CatalogStoreProvider
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.ResourceKey

class CatalogComponentDefault(
    componentContext: ComponentContext,
    repository: CatalogRepository,
    storeFactory: StoreFactory,
    itemIdGenerator: PackingItemIdGenerator,
    resolveResourceKey: suspend (ResourceKey) -> String,
) : CatalogComponent, ComponentContext by componentContext {
    private val store = instanceKeeper.getOrCreate(key = STORE_KEY) {
        StoreHolder(
            CatalogStoreProvider(
                storeFactory = storeFactory,
                manager = CatalogManager(
                    repository = repository,
                    itemIdGenerator = itemIdGenerator,
                    resolveResourceKey = resolveResourceKey,
                ),
            ).provide().also(CatalogStore::init),
        )
    }.store

    override val model: Value<CatalogComponent.Model> = store.asValue().map(CatalogStore.State::toComponentModel)

    override fun startCreate() = store.accept(CatalogStore.Intent.StartCreate)
    override fun startEdit(itemId: PackingItemId) = store.accept(CatalogStore.Intent.StartEdit(itemId))
    override fun changeName(value: String) = store.accept(CatalogStore.Intent.ChangeName(value))
    override fun changeUsualLocation(value: String) = store.accept(CatalogStore.Intent.ChangeUsualLocation(value))
    override fun save() = store.accept(CatalogStore.Intent.Save)
    override fun createAnother() = store.accept(CatalogStore.Intent.CreateAnother)
    override fun useExistingMatch() = store.accept(CatalogStore.Intent.UseExistingMatch)
    override fun requestDelete(itemId: PackingItemId) = store.accept(CatalogStore.Intent.RequestDelete(itemId))
    override fun confirmDelete() = store.accept(CatalogStore.Intent.ConfirmDelete)
    override fun dismissDelete() = store.accept(CatalogStore.Intent.DismissDelete)
    override fun closeEditor() = store.accept(CatalogStore.Intent.CloseEditor)
    override fun clearError() = store.accept(CatalogStore.Intent.ClearError)

    private class StoreHolder(val store: CatalogStore) : InstanceKeeper.Instance {
        override fun onDestroy() = store.dispose()
    }

    private companion object {
        const val STORE_KEY = "CatalogStore"
    }
}
