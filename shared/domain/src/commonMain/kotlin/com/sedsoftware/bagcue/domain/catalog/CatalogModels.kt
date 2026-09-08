package com.sedsoftware.bagcue.domain.catalog

import kotlin.jvm.JvmInline

@JvmInline
value class PackingItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "Packing item ID must not be blank" }
    }
}

fun interface PackingItemIdGenerator {
    fun nextId(): PackingItemId
}

@JvmInline
value class ResourceKey(val value: String) {
    init {
        require(value.matches(RESOURCE_KEY_PATTERN)) { "Invalid resource key: $value" }
    }

    private companion object {
        val RESOURCE_KEY_PATTERN = Regex("[a-z][a-z0-9_]*")
    }
}

data class PackingItem(
    val id: PackingItemId,
    val seedNameKey: ResourceKey?,
    val userNameOverride: String?,
    val usualLocation: String?,
    val createdOrder: Long,
) {
    init {
        require(seedNameKey != null || !userNameOverride.isNullOrBlank()) {
            "An item needs either a seed resource key or a user-authored name"
        }
        require(userNameOverride == null || userNameOverride.isNotBlank()) {
            "A user-authored name must not be blank"
        }
        require(usualLocation == null || usualLocation.isNotBlank()) {
            "An empty usual location must be stored as null"
        }
    }

    val isStarter: Boolean get() = seedNameKey != null
}

data class StarterPackingItem(
    val id: PackingItemId,
    val nameKey: ResourceKey,
    val sortOrder: Long,
)

data class VisiblePackingItem(
    val item: PackingItem,
    val visibleName: String,
)

data class CatalogDependency(
    val id: String,
    val seedNameKey: ResourceKey?,
    val userName: String?,
) {
    init {
        require(id.isNotBlank())
        require(seedNameKey != null || !userName.isNullOrBlank())
    }
}

data class CatalogDependencySummary(
    val itemId: PackingItemId,
    val templates: List<CatalogDependency>,
    val unfinishedSessions: List<CatalogDependency>,
)

data class CreatePackingItem(
    val id: PackingItemId,
    val name: String,
    val usualLocation: String?,
)

data class UpdatePackingItem(
    val id: PackingItemId,
    val userNameOverride: String?,
    val usualLocation: String?,
)

sealed interface DuplicateDecision {
    data object ReuseExisting : DuplicateDecision
    data object CreateAnother : DuplicateDecision
}

sealed interface CatalogCreateOutcome {
    data class Reuse(val existing: PackingItem) : CatalogCreateOutcome
    data class Created(val item: PackingItem) : CatalogCreateOutcome
    data class NeedsDecision(val existing: PackingItem) : CatalogCreateOutcome
}

class InvalidCatalogNameException : IllegalArgumentException("Packing item name must not be blank")

class CatalogItemNotFoundException(id: PackingItemId) :
    NoSuchElementException("Packing item ${id.value} does not exist")

fun validatedCatalogName(raw: String): String = raw.also {
    if (it.isBlank()) throw InvalidCatalogNameException()
}

fun normalizeCatalogName(raw: String): String {
    validatedCatalogName(raw)
    return buildString(raw.length) {
        var pendingSpace = false
        raw.forEach { character ->
            if (character.isWhitespace()) {
                if (isNotEmpty()) pendingSpace = true
            } else {
                if (pendingSpace) append(' ')
                append(character)
                pendingSpace = false
            }
        }
    }.lowercase()
        // Kotlin common has locale-independent Unicode lower-casing. These are the
        // multi-code-point/common equivalences required in catalog comparisons.
        .replace("ß", "ss")
        .replace('ς', 'σ')
}

fun findNormalizedNameMatch(
    proposedName: String,
    items: Iterable<VisiblePackingItem>,
): PackingItem? {
    val normalized = normalizeCatalogName(proposedName)
    return items.firstOrNull { normalizeCatalogName(it.visibleName) == normalized }?.item
}

fun optionalUserText(raw: String?): String? = raw?.takeUnless(String::isBlank)

val DefaultStarterPackingItems: List<StarterPackingItem> = listOf(
    "item_laptop" to "starter_item_laptop",
    "item_charger" to "starter_item_charger",
    "item_headphones" to "starter_item_headphones",
    "item_badge" to "starter_item_badge",
    "item_keys" to "starter_item_keys",
    "item_water_bottle" to "starter_item_water_bottle",
    "item_lunch_snack" to "starter_item_lunch_snack",
    "item_swimsuit" to "starter_item_swimsuit",
    "item_swim_cap" to "starter_item_swim_cap",
    "item_goggles" to "starter_item_goggles",
    "item_towel" to "starter_item_towel",
    "item_shower_gel" to "starter_item_shower_gel",
    "item_flip_flops" to "starter_item_flip_flops",
    "item_wet_bag" to "starter_item_wet_bag",
).mapIndexed { index, (id, key) ->
    StarterPackingItem(PackingItemId(id), ResourceKey(key), index.toLong())
}
