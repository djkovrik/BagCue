package com.sedsoftware.bagcue.domain.template

import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.optionalUserText
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import kotlin.jvm.JvmInline

@JvmInline
value class KitTemplateId(val value: String) {
    init {
        require(value.isNotBlank()) { "Template ID must not be blank" }
    }
}

@JvmInline
value class TemplatePositionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Template position ID must not be blank" }
    }
}

@JvmInline
value class PositionQuantity(val value: Int) {
    init {
        require(value in MIN_VALUE..MAX_VALUE) { "Position quantity must be between 1 and 99" }
    }

    companion object {
        const val MIN_VALUE: Int = 1
        const val MAX_VALUE: Int = 99
    }
}

fun interface KitTemplateIdGenerator {
    fun nextId(): KitTemplateId
}

fun interface TemplatePositionIdGenerator {
    fun nextId(): TemplatePositionId
}

data class TemplateBagLabel(
    val seedNameKey: ResourceKey?,
    val userText: String?,
) {
    init {
        require(seedNameKey == null || userText == null) { "A bag label cannot be both seeded and user-authored" }
        require(userText == null || userText.isNotBlank()) { "An empty bag label must be stored as null" }
    }

    companion object {
        val None: TemplateBagLabel = TemplateBagLabel(null, null)

        fun seed(key: ResourceKey): TemplateBagLabel = TemplateBagLabel(key, null)

        fun user(text: String?): TemplateBagLabel = TemplateBagLabel(null, optionalUserText(text))
    }
}

data class TemplatePosition(
    val id: TemplatePositionId,
    val itemId: PackingItemId,
    val quantity: PositionQuantity,
    val bagLabel: TemplateBagLabel,
    val sourceHintOverride: String?,
    val sortOrder: Long,
) {
    init {
        require(sourceHintOverride == null || sourceHintOverride.isNotBlank()) {
            "An empty source hint must be stored as null"
        }
        require(sortOrder >= 0) { "Position sort order must not be negative" }
    }
}

data class KitTemplate(
    val id: KitTemplateId,
    val seedNameKey: ResourceKey?,
    val userNameOverride: String?,
    val sortOrder: Long,
    val positions: List<TemplatePosition>,
) {
    init {
        require(seedNameKey != null || !userNameOverride.isNullOrBlank()) {
            "A template needs either a seed resource key or a user-authored name"
        }
        require(userNameOverride == null || userNameOverride.isNotBlank()) {
            "A user-authored template name must not be blank"
        }
        require(sortOrder >= 0) { "Template sort order must not be negative" }
        require(positions.map { it.id }.distinct().size == positions.size) {
            "Template position IDs must be unique"
        }
        require(positions.map { it.itemId }.distinct().size == positions.size) {
            "A PackingItem may occur only once in a template"
        }
        require(positions.map { it.sortOrder }.distinct().size == positions.size) {
            "Template position sort orders must be unique"
        }
    }

    val isStarter: Boolean get() = seedNameKey != null
}

data class StarterKitTemplate(
    val template: KitTemplate,
)

class KitTemplateNotFoundException(id: KitTemplateId) :
    NoSuchElementException("Template ${id.value} does not exist")

fun userKitTemplate(
    id: KitTemplateId,
    name: String,
    sortOrder: Long,
    positions: List<TemplatePosition> = emptyList(),
): KitTemplate = KitTemplate(
    id = id,
    seedNameKey = null,
    userNameOverride = validatedCatalogName(name),
    sortOrder = sortOrder,
    positions = positions,
)

val DefaultStarterKitTemplates: List<StarterKitTemplate> = listOf(
    StarterKitTemplate(
        template = KitTemplate(
            id = KitTemplateId("template_office"),
            seedNameKey = ResourceKey("starter_template_office"),
            userNameOverride = null,
            sortOrder = 0,
            positions = listOf(
                "laptop", "charger", "headphones", "badge", "keys", "water_bottle", "lunch_snack",
            ).mapIndexed { index, suffix ->
                TemplatePosition(
                    id = TemplatePositionId("position_office_$suffix"),
                    itemId = PackingItemId("item_$suffix"),
                    quantity = PositionQuantity(1),
                    bagLabel = TemplateBagLabel.seed(ResourceKey("starter_bag_backpack")),
                    sourceHintOverride = null,
                    sortOrder = index.toLong(),
                )
            },
        ),
    ),
    StarterKitTemplate(
        template = KitTemplate(
            id = KitTemplateId("template_pool"),
            seedNameKey = ResourceKey("starter_template_pool"),
            userNameOverride = null,
            sortOrder = 1,
            positions = listOf(
                "swimsuit", "swim_cap", "goggles", "towel", "shower_gel", "flip_flops", "wet_bag",
            ).mapIndexed { index, suffix ->
                TemplatePosition(
                    id = TemplatePositionId("position_pool_$suffix"),
                    itemId = PackingItemId("item_$suffix"),
                    quantity = PositionQuantity(1),
                    bagLabel = TemplateBagLabel.seed(ResourceKey("starter_bag_sports")),
                    sourceHintOverride = null,
                    sortOrder = index.toLong(),
                )
            },
        ),
    ),
)
