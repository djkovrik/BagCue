package com.sedsoftware.bagcue.data.catalog

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.data.db.Packing_item
import com.sedsoftware.bagcue.domain.catalog.CatalogDependency
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogItemNotFoundException
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import com.sedsoftware.bagcue.domain.catalog.optionalUserText
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightCatalogRepository(
    database: BagCueDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val currentTimeMillis: () -> Long,
) : CatalogRepository {
    private val queries = database.catalogQueries

    override fun observeItems(): Flow<List<PackingItem>> =
        queries.selectAllItems().asFlow().mapToList(ioDispatcher).map { rows -> rows.map(::toDomain) }

    override suspend fun readItems(): Result<List<PackingItem>> = onDatabase {
        queries.selectAllItems().executeAsList().map(::toDomain)
    }

    override suspend fun findItem(id: PackingItemId): Result<PackingItem?> = onDatabase {
        queries.selectItemById(id.value).executeAsOneOrNull()?.let(::toDomain)
    }

    override suspend fun installStarterItems(items: List<StarterPackingItem>): Result<Unit> = onDatabase {
        queries.transaction {
            val now = currentTimeMillis()
            items.forEach { starter ->
                queries.updateStarterIfAllowed(
                    seed_resource_key = starter.nameKey.value,
                    created_order = starter.sortOrder,
                    updated_at = now,
                    id = starter.id.value,
                    seed_id = starter.id.value,
                )
                queries.insertStarterIfAllowed(
                    id = starter.id.value,
                    seed_resource_key = starter.nameKey.value,
                    created_order = starter.sortOrder,
                    updated_at = now,
                    seed_id = starter.id.value,
                )
            }
        }
    }

    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> = onDatabase {
        val name = validatedCatalogName(command.name)
        val location = optionalUserText(command.usualLocation)
        val now = currentTimeMillis()
        queries.insertItem(
            id = command.id.value,
            seed_resource_key = null,
            user_name_override = name,
            usual_location = location,
            created_order = now,
            updated_at = now,
        )
        queries.selectItemById(command.id.value).executeAsOne().let(::toDomain)
    }

    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> = onDatabase {
        val previous = queries.selectItemById(command.id.value).executeAsOneOrNull()
            ?: throw CatalogItemNotFoundException(command.id)
        val nameOverride = command.userNameOverride?.let(::validatedCatalogName)
        require(previous.seed_resource_key != null || nameOverride != null) {
            "A user-created item must keep a user-authored name"
        }
        val now = currentTimeMillis()
        queries.transaction {
            queries.updateItem(
                user_name_override = nameOverride,
                usual_location = optionalUserText(command.usualLocation),
                updated_at = now,
                id = command.id.value,
            )
            if (previous.seed_resource_key != null &&
                (nameOverride != previous.user_name_override || command.usualLocation != previous.usual_location)
            ) {
                queries.upsertSeedOverride(command.id.value, "modified", now)
            }
        }
        queries.selectItemById(command.id.value).executeAsOne().let(::toDomain)
    }

    override suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary> = onDatabase {
        if (queries.selectItemById(id.value).executeAsOneOrNull() == null) {
            throw CatalogItemNotFoundException(id)
        }
        CatalogDependencySummary(
            itemId = id,
            templates = queries.selectTemplateDependencies(id.value) { templateId, seedKey, userName ->
                CatalogDependency(templateId, seedKey?.let(::ResourceKey), userName)
            }.executeAsList(),
            unfinishedSessions = queries.selectActiveSessionDependencies(id.value) { sessionId, seedKey, userName ->
                CatalogDependency(sessionId, seedKey?.let(::ResourceKey), userName)
            }.executeAsList(),
        )
    }

    override suspend fun deleteItemAtomically(id: PackingItemId): Result<Unit> = onDatabase {
        val item = queries.selectItemById(id.value).executeAsOneOrNull()
            ?: throw CatalogItemNotFoundException(id)
        val now = currentTimeMillis()
        queries.transaction {
            if (item.seed_resource_key != null) {
                queries.upsertSeedOverride(id.value, "deleted", now)
            }
            queries.deleteTemplatePositionsForItem(id.value)
            queries.deleteActiveSessionItemsForItem(id.value)
            queries.deletePackingItem(id.value)
        }
    }

    private fun toDomain(row: Packing_item): PackingItem = PackingItem(
        id = PackingItemId(row.id),
        seedNameKey = row.seed_resource_key?.let(::ResourceKey),
        userNameOverride = row.user_name_override,
        usualLocation = row.usual_location,
        createdOrder = row.created_order,
    )

    private suspend fun <T> onDatabase(block: () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
        }
    }
}
