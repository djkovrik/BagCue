package com.sedsoftware.bagcue.catalog.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.catalog.domain.CatalogManager
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogStoreTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun failedSaveRetainsVerbatimDraftAndRetryCommitsTheSameInput() = runTest(dispatcher) {
        val repository = FailingCatalogRepository()
        val store = CatalogStoreProvider(
            DefaultStoreFactory(),
            CatalogManager(repository, PackingItemIdGenerator { PackingItemId("created") }) { it.value },
        ).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(CatalogStore.Intent.StartCreate)
            store.accept(CatalogStore.Intent.ChangeName("  Passport 🎒  "))
            store.accept(CatalogStore.Intent.ChangeUsualLocation("  Desk drawer  "))
            store.accept(CatalogStore.Intent.Save)
            advanceUntilIdle()

            assertEquals(CatalogStore.Error.SaveFailed, store.state.error)
            assertEquals("  Passport 🎒  ", store.state.editor?.nameInput)
            assertEquals("  Desk drawer  ", store.state.editor?.usualLocation)
            assertEquals(false, store.state.editor?.isSaving)
            assertEquals(emptyList(), repository.items.value)

            repository.createFailure = null
            store.accept(CatalogStore.Intent.Save)
            advanceUntilIdle()

            assertNull(store.state.editor)
            assertEquals("  Passport 🎒  ", repository.items.value.single().userNameOverride)
            assertEquals("  Desk drawer  ", repository.items.value.single().usualLocation)
        } finally {
            store.dispose()
        }
    }
}

private class FailingCatalogRepository : CatalogRepository {
    val items = MutableStateFlow<List<PackingItem>>(emptyList())
    var createFailure: Throwable? = IllegalStateException("forced save failure")

    override fun observeItems(): Flow<List<PackingItem>> = items
    override suspend fun readItems(): Result<List<PackingItem>> = Result.success(items.value)
    override suspend fun findItem(id: PackingItemId): Result<PackingItem?> = Result.success(items.value.firstOrNull { it.id == id })
    override suspend fun installStarterItems(items: List<StarterPackingItem>): Result<Unit> = Result.success(Unit)
    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> {
        createFailure?.let { return Result.failure(it) }
        val item = PackingItem(command.id, null, command.name, command.usualLocation, 0)
        items.value += item
        return Result.success(item)
    }
    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary> = error("Not used")
    override suspend fun deleteItemAtomically(id: PackingItemId): Result<Unit> = error("Not used")
}
