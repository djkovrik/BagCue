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
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import com.sedsoftware.bagcue.domain.catalog.VisiblePackingItem
import com.sedsoftware.bagcue.domain.catalog.findNormalizedNameMatch
import com.sedsoftware.bagcue.domain.catalog.optionalUserText
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

internal class CatalogManager(
    private val repository: CatalogRepository,
    private val itemIdGenerator: PackingItemIdGenerator,
    private val resolveResourceKey: suspend (ResourceKey) -> String,
) {
    fun observeItems(): Flow<List<PackingItem>> = repository.observeItems()

    suspend fun initialize(): Result<Unit> = captureResult {
        repository.installStarterItems().getOrThrow()
    }

    suspend fun create(
        name: String,
        usualLocation: String?,
        decision: DuplicateDecision? = null,
    ): Result<CatalogCreateOutcome> = captureResult {
        val validatedName = validatedCatalogName(name)
        val existing = findNormalizedNameMatch(
            proposedName = validatedName,
            items = repository.readItems().getOrThrow().map { item ->
                VisiblePackingItem(
                    item = item,
                    visibleName = item.userNameOverride
                        ?: resolveResourceKey(requireNotNull(item.seedNameKey)),
                )
            },
        )

        when {
            existing == null -> CatalogCreateOutcome.Created(
                repository.createItem(
                    CreatePackingItem(
                        id = itemIdGenerator.nextId(),
                        name = validatedName,
                        usualLocation = optionalUserText(usualLocation),
                    ),
                ).getOrThrow(),
            )

            decision == null -> CatalogCreateOutcome.NeedsDecision(existing)
            decision == DuplicateDecision.ReuseExisting -> CatalogCreateOutcome.Reuse(existing)
            else -> CatalogCreateOutcome.Created(
                repository.createItem(
                    CreatePackingItem(
                        id = itemIdGenerator.nextId(),
                        name = validatedName,
                        usualLocation = optionalUserText(usualLocation),
                    ),
                ).getOrThrow(),
            )
        }
    }

    suspend fun update(
        itemId: PackingItemId,
        userNameOverride: String?,
        usualLocation: String?,
    ): Result<PackingItem> = captureResult {
        repository.updateItem(
            UpdatePackingItem(
                id = itemId,
                userNameOverride = userNameOverride?.let(::validatedCatalogName),
                usualLocation = optionalUserText(usualLocation),
            ),
        ).getOrThrow()
    }

    suspend fun readDeleteDependencies(itemId: PackingItemId): Result<CatalogDependencySummary> = captureResult {
        repository.readDeleteDependencies(itemId).getOrThrow()
    }

    suspend fun delete(itemId: PackingItemId): Result<Unit> = captureResult {
        repository.deleteItemAtomically(itemId).getOrThrow()
    }
}

private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
