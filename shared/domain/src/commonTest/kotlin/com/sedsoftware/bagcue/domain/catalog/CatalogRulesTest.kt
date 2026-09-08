package com.sedsoftware.bagcue.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CatalogRulesTest {
    @Test
    fun normalizationTrimsCollapsesWhitespaceAndCaseFolds() {
        assertEquals("my travel charger", normalizeCatalogName("  MY\tTravel\nCharger  "))
        assertEquals("strasse", normalizeCatalogName("Straße"))
        assertEquals("i", normalizeCatalogName("I"))
        assertEquals("οσ", normalizeCatalogName("ΟΣ"))
    }

    @Test
    fun blankNameIsRejectedWithTypedCause() {
        assertFailsWith<InvalidCatalogNameException> { normalizeCatalogName(" \n\t ") }
    }

    @Test
    fun duplicateMatchReturnsOriginalStableIdentity() {
        val item = PackingItem(
            id = PackingItemId("item_laptop"),
            seedNameKey = ResourceKey("starter_item_laptop"),
            userNameOverride = null,
            usualLocation = null,
            createdOrder = 0,
        )

        assertSame(item, findNormalizedNameMatch(" LAPTOP ", listOf(VisiblePackingItem(item, "Laptop"))))
    }

    @Test
    fun starterDatasetUsesOnlyStableIdsAndResourceKeys() {
        assertEquals(14, DefaultStarterPackingItems.size)
        assertEquals(DefaultStarterPackingItems.size, DefaultStarterPackingItems.map { it.id }.toSet().size)
        assertEquals(DefaultStarterPackingItems.size, DefaultStarterPackingItems.map { it.nameKey }.toSet().size)
    }

    @Test
    fun validationDoesNotRewriteUserAuthoredText() {
        assertEquals("  My   cable ", validatedCatalogName("  My   cable "))
        assertEquals(" drawer ", optionalUserText(" drawer "))
    }
}
