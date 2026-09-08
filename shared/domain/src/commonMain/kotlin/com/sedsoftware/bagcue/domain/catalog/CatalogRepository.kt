package com.sedsoftware.bagcue.domain.catalog

import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeItems(): Flow<List<PackingItem>>

    suspend fun readItems(): Result<List<PackingItem>>

    suspend fun findItem(id: PackingItemId): Result<PackingItem?>

    suspend fun installStarterItems(items: List<StarterPackingItem> = DefaultStarterPackingItems): Result<Unit>

    suspend fun createItem(command: CreatePackingItem): Result<PackingItem>

    suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem>

    suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary>

    suspend fun deleteItemAtomically(id: PackingItemId): Result<Unit>
}
