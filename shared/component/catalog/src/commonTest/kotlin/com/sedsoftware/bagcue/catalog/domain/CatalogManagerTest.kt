package com.sedsoftware.bagcue.catalog.domain

import com.sedsoftware.bagcue.domain.catalog.CatalogCreateOutcome
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.DuplicateDecision
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class CatalogManagerTest {
    @Test
    fun normalizedMatchRequiresExplicitCreateAnother() = runTest {
        val existing = PackingItem(
            id = PackingItemId("existing"),
            seedNameKey = null,
            userNameOverride = "Swim Goggles",
            usualLocation = null,
            createdOrder = 1,
        )
        val repository = FakeCatalogRepository(listOf(existing))
        val manager = manager(repository)

        val suggestion = manager.create("  swim   goggles ", null).getOrThrow()
        assertEquals(existing, assertIs<CatalogCreateOutcome.NeedsDecision>(suggestion).existing)
        assertEquals(listOf(existing), repository.items.value)

        val created = manager.create(
            name = "  swim   goggles ",
            usualLocation = "Locker",
            decision = DuplicateDecision.CreateAnother,
        ).getOrThrow()
        assertEquals(PackingItemId("generated"), assertIs<CatalogCreateOutcome.Created>(created).item.id)
        assertEquals(2, repository.items.value.size)
    }

    @Test
    fun locationOnlyUpdatePreservesSeedIdentity() = runTest {
        val seed = PackingItem(
            id = PackingItemId("seed"),
            seedNameKey = ResourceKey("starter_item_laptop"),
            userNameOverride = null,
            usualLocation = null,
            createdOrder = 0,
        )
        val repository = FakeCatalogRepository(listOf(seed))

        val updated = manager(repository).update(seed.id, userNameOverride = null, usualLocation = "Desk").getOrThrow()

        assertEquals(seed.seedNameKey, updated.seedNameKey)
        assertNull(updated.userNameOverride)
        assertEquals("Desk", updated.usualLocation)
    }

    @Test
    fun cancellationIsNotConvertedToFailure() = runTest {
        val repository = FakeCatalogRepository(emptyList(), cancelOnRead = true)
        assertFailsWith<CancellationException> { manager(repository).create("Keys", null) }
    }

    @Test
    fun updatePreservesVerbatimAuthoredTextAndOriginalFailureCause() = runTest {
        val starter = PackingItem(
            id = PackingItemId("starter"),
            seedNameKey = ResourceKey("starter_item_laptop"),
            userNameOverride = null,
            usualLocation = null,
            createdOrder = 0,
        )
        val failure = IllegalStateException("disk full")
        val failing = FakeCatalogRepository(listOf(starter), updateFailure = failure)

        val failed = manager(failing).update(starter.id, "  My laptop 🎒  ", "  Upper shelf  ")

        assertSame(failure, failed.exceptionOrNull())
        assertEquals(starter, failing.items.value.single())

        failing.updateFailure = null
        val saved = manager(failing).update(starter.id, "  My laptop 🎒  ", "  Upper shelf  ").getOrThrow()
        assertEquals(ResourceKey("starter_item_laptop"), saved.seedNameKey)
        assertEquals("  My laptop 🎒  ", saved.userNameOverride)
        assertEquals("  Upper shelf  ", saved.usualLocation)
    }

    @Test
    fun updateCancellationFromRepositoryResultIsRethrown() = runTest {
        val item = PackingItem(PackingItemId("item"), null, "Item", null, 0)
        val cancellation = CancellationException("cancel update")
        val repository = FakeCatalogRepository(listOf(item), updateFailure = cancellation)

        assertSame(
            cancellation,
            assertFailsWith<CancellationException> { manager(repository).update(item.id, "Changed", null) },
        )
        assertEquals(item, repository.items.value.single())
    }

    private fun manager(repository: CatalogRepository) = CatalogManager(
        repository = repository,
        itemIdGenerator = PackingItemIdGenerator { PackingItemId("generated") },
        resolveResourceKey = { key -> key.value },
    )
}

private class FakeCatalogRepository(
    initialItems: List<PackingItem>,
    private val cancelOnRead: Boolean = false,
    var updateFailure: Throwable? = null,
) : CatalogRepository {
    val items = MutableStateFlow(initialItems)

    override fun observeItems(): Flow<List<PackingItem>> = items

    override suspend fun readItems(): Result<List<PackingItem>> {
        if (cancelOnRead) throw CancellationException("cancel")
        return Result.success(items.value)
    }

    override suspend fun findItem(id: PackingItemId): Result<PackingItem?> =
        Result.success(items.value.firstOrNull { it.id == id })

    override suspend fun installStarterItems(items: List<StarterPackingItem>): Result<Unit> = Result.success(Unit)

    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> {
        val item = PackingItem(
            id = command.id,
            seedNameKey = null,
            userNameOverride = command.name,
            usualLocation = command.usualLocation,
            createdOrder = items.value.size.toLong(),
        )
        items.value += item
        return Result.success(item)
    }

    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> {
        updateFailure?.let { return Result.failure(it) }
        val original = items.value.first { it.id == command.id }
        val updated = original.copy(
            userNameOverride = command.userNameOverride,
            usualLocation = command.usualLocation,
        )
        items.value = items.value.map { if (it.id == command.id) updated else it }
        return Result.success(updated)
    }

    override suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary> =
        Result.success(CatalogDependencySummary(id, emptyList(), emptyList()))

    override suspend fun deleteItemAtomically(id: PackingItemId): Result<Unit> {
        items.value = items.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}
