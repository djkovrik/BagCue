package com.sedsoftware.bagcue.templates.integration

import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.templates.TemplateComponent
import com.sedsoftware.bagcue.templates.store.TemplateStore
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateModelMapperTest {
    @Test
    fun selectorUsesLocalizedSearchNameAndExcludesExistingPosition() {
        val laptop = item("laptop", ResourceKey("starter_item_laptop"), null)
        val goggles = item("goggles", ResourceKey("starter_item_goggles"), "Locker")
        val state = TemplateStore.State(
            catalogItems = listOf(laptop, goggles),
            catalogSearchNames = mapOf(laptop.id to "Ноутбук", goggles.id to "Очки для плавания"),
            editor = editor(listOf(position("position-laptop", laptop.id))),
            itemSelector = TemplateStore.ItemSelector("очки"),
            isLoading = false,
        )

        val selector = state.toComponentModel().itemSelector!!

        assertEquals(listOf(goggles.id), selector.items.map(TemplateComponent.CatalogItem::id))
        assertEquals(false, selector.isEmptyResult)
    }

    @Test
    fun positionUsesCatalogLocationWhenNoSourceOverrideExists() {
        val item = item("goggles", ResourceKey("starter_item_goggles"), "Bathroom")
        val state = TemplateStore.State(
            catalogItems = listOf(item),
            catalogSearchNames = mapOf(item.id to "Goggles"),
            editor = editor(listOf(position("position-goggles", item.id))),
            isLoading = false,
        )

        val position = state.toComponentModel().editor!!.positions.single()

        assertEquals("Bathroom", position.source)
        assertEquals(TemplateComponent.UserText.Resource("starter_item_goggles"), position.itemName)
    }
}

private fun item(id: String, key: ResourceKey, location: String?) = PackingItem(
    id = PackingItemId(id),
    seedNameKey = key,
    userNameOverride = null,
    usualLocation = location,
    createdOrder = 0,
)

private fun position(id: String, itemId: PackingItemId) = TemplatePosition(
    id = TemplatePositionId(id),
    itemId = itemId,
    quantity = PositionQuantity(1),
    bagLabel = TemplateBagLabel.None,
    sourceHintOverride = null,
    sortOrder = 0,
)

private fun editor(positions: List<TemplatePosition>) = TemplateStore.Editor(
    templateId = KitTemplateId("template"),
    seedNameKey = null,
    nameInput = "Template",
    sortOrder = 0,
    positions = positions,
    isNew = false,
)
