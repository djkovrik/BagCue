package com.sedsoftware.bagcue.data.template

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.data.db.Kit_template
import com.sedsoftware.bagcue.data.db.Template_position
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateNotFoundException
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.StarterKitTemplate
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightKitTemplateRepository(
    database: BagCueDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val currentTimeMillis: () -> Long,
) : KitTemplateRepository {
    private val queries = database.templateQueries

    override fun observeTemplates(): Flow<List<KitTemplate>> =
        queries.selectAllTemplates().asFlow().mapToList(ioDispatcher).map { rows -> rows.map(::toDomain) }

    override suspend fun readTemplates(): Result<List<KitTemplate>> = onDatabase {
        queries.selectAllTemplates().executeAsList().map(::toDomain)
    }

    override suspend fun findTemplate(id: KitTemplateId): Result<KitTemplate?> = onDatabase {
        queries.selectTemplateById(id.value).executeAsOneOrNull()?.let(::toDomain)
    }

    override suspend fun installStarterTemplates(templates: List<StarterKitTemplate>): Result<Unit> = onDatabase {
        queries.transaction {
            templates.forEach { definition ->
                val template = definition.template
                val seedNameKey = requireNotNull(template.seedNameKey) {
                    "A starter template requires a seed resource key"
                }
                if (queries.countTemplateSeedOverrides(template.id.value).executeAsOne() != 0L) {
                    return@forEach
                }
                queries.updateStarterTemplateIfAllowed(
                    seed_resource_key = seedNameKey.value,
                    sort_order = template.sortOrder,
                    id = template.id.value,
                    seed_id = template.id.value,
                )
                queries.insertStarterTemplateIfAllowed(
                    id = template.id.value,
                    seed_resource_key = seedNameKey.value,
                    sort_order = template.sortOrder,
                    seed_id = template.id.value,
                )
                val stored = queries.selectTemplateById(template.id.value).executeAsOneOrNull()
                if (stored?.seed_resource_key != null && stored.user_name_override == null) {
                    replacePositions(template)
                }
            }
        }
    }

    override suspend fun createTemplate(template: KitTemplate): Result<KitTemplate> = onDatabase {
        require(template.seedNameKey == null) { "Starter templates must be installed through reconciliation" }
        queries.transaction {
            insertTemplateRow(template)
            insertPositions(template)
        }
        queries.selectTemplateById(template.id.value).executeAsOne().let(::toDomain)
    }

    override suspend fun updateTemplate(template: KitTemplate): Result<KitTemplate> = onDatabase {
        val previousRow = queries.selectTemplateById(template.id.value).executeAsOneOrNull()
            ?: throw KitTemplateNotFoundException(template.id)
        val previous = toDomain(previousRow)
        if (previous == template) return@onDatabase previous
        val previousSeedNameKey = previous.seedNameKey
        require(previousSeedNameKey != null || template.userNameOverride != null) {
            "A user-created template must keep a user-authored name"
        }
        queries.transaction {
            queries.updateKitTemplate(
                seed_resource_key = previousSeedNameKey?.value,
                user_name_override = template.userNameOverride,
                sort_order = template.sortOrder,
                id = template.id.value,
            )
            replacePositions(template)
            if (previousSeedNameKey != null) {
                queries.upsertTemplateSeedOverride(template.id.value, "modified", currentTimeMillis())
            }
        }
        queries.selectTemplateById(template.id.value).executeAsOne().let(::toDomain)
    }

    override suspend fun deleteTemplate(id: KitTemplateId): Result<Unit> = onDatabase {
        val previous = queries.selectTemplateById(id.value).executeAsOneOrNull()
            ?: throw KitTemplateNotFoundException(id)
        queries.transaction {
            if (previous.seed_resource_key != null) {
                queries.upsertTemplateSeedOverride(id.value, "deleted", currentTimeMillis())
            }
            queries.deleteKitTemplate(id.value)
        }
    }

    private fun insertTemplateRow(template: KitTemplate) {
        queries.insertKitTemplate(
            id = template.id.value,
            seed_resource_key = template.seedNameKey?.value,
            user_name_override = template.userNameOverride,
            sort_order = template.sortOrder,
        )
    }

    private fun replacePositions(template: KitTemplate) {
        queries.deletePositionsByTemplate(template.id.value)
        insertPositions(template)
    }

    private fun insertPositions(template: KitTemplate) {
        template.positions.forEach { position ->
            queries.insertKitTemplatePosition(
                id = position.id.value,
                template_id = template.id.value,
                item_id = position.itemId.value,
                quantity = position.quantity.value.toLong(),
                seed_bag_resource_key = position.bagLabel.seedNameKey?.value,
                user_bag_label = position.bagLabel.userText,
                source_hint_override = position.sourceHintOverride,
                sort_order = position.sortOrder,
            )
        }
    }

    private fun toDomain(row: Kit_template): KitTemplate = KitTemplate(
        id = KitTemplateId(row.id),
        seedNameKey = row.seed_resource_key?.let(::ResourceKey),
        userNameOverride = row.user_name_override,
        sortOrder = row.sort_order,
        positions = queries.selectPositionsByTemplate(row.id).executeAsList().map(::toDomain),
    )

    private fun toDomain(row: Template_position): TemplatePosition = TemplatePosition(
        id = TemplatePositionId(row.id),
        itemId = PackingItemId(row.item_id),
        quantity = PositionQuantity(row.quantity.toInt()),
        bagLabel = TemplateBagLabel(
            seedNameKey = row.seed_bag_resource_key?.let(::ResourceKey),
            userText = row.user_bag_label,
        ),
        sourceHintOverride = row.source_hint_override,
        sortOrder = row.sort_order,
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
