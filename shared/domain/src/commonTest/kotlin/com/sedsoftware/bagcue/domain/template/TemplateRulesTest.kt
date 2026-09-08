package com.sedsoftware.bagcue.domain.template

import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateRulesTest {
    @Test
    fun quantityAcceptsOnlyInclusiveOneThroughNinetyNine() {
        assertEquals(1, PositionQuantity(1).value)
        assertEquals(99, PositionQuantity(99).value)
        assertFailsWith<IllegalArgumentException> { PositionQuantity(0) }
        assertFailsWith<IllegalArgumentException> { PositionQuantity(100) }
    }

    @Test
    fun templateRejectsDuplicateItemMembershipEvenWithDifferentPositionIds() {
        val first = position("first")
        val second = position("second")
        assertFailsWith<IllegalArgumentException> {
            userKitTemplate(KitTemplateId("template"), "Template", 0, listOf(first, second))
        }
    }

    @Test
    fun starterTemplatesUseStableIdsKeysBagsAndUnitQuantities() {
        assertEquals(listOf("template_office", "template_pool"), DefaultStarterKitTemplates.map { it.template.id.value })
        assertEquals(
            listOf("starter_template_office", "starter_template_pool"),
            DefaultStarterKitTemplates.map { it.template.seedNameKey?.value },
        )
        DefaultStarterKitTemplates.flatMap { it.template.positions }.forEach { position ->
            assertEquals(1, position.quantity.value)
            assertEquals(null, position.bagLabel.userText)
        }
    }

    private fun position(id: String) = TemplatePosition(
        id = TemplatePositionId(id),
        itemId = PackingItemId("item"),
        quantity = PositionQuantity(1),
        bagLabel = TemplateBagLabel.seed(ResourceKey("starter_bag_backpack")),
        sourceHintOverride = null,
        sortOrder = if (id == "first") 0 else 1,
    )
}
