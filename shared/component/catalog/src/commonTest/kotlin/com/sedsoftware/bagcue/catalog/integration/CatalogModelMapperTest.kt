package com.sedsoftware.bagcue.catalog.integration

import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.catalog.store.CatalogStore
import com.sedsoftware.bagcue.domain.catalog.CatalogDependency
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CatalogModelMapperTest {
    @Test
    fun mapsLocaleNeutralStarterAndDependencyKeys() {
        val item = starterItem()
        val state = CatalogStore.State(
            items = listOf(item),
            isLoading = false,
            editor = CatalogStore.Editor(
                itemId = item.id,
                seedNameKey = "starter_item_laptop",
                nameInput = null,
                usualLocation = "Desk",
            ),
            deleteConfirmation = CatalogStore.DeleteConfirmation(
                summary = CatalogDependencySummary(
                    itemId = item.id,
                    templates = listOf(
                        CatalogDependency("template-work", ResourceKey("starter_template_work"), null),
                    ),
                    unfinishedSessions = emptyList(),
                ),
            ),
        )

        val model = state.toComponentModel()

        assertEquals("starter_item_laptop", assertIs<CatalogComponent.UserText.Resource>(model.items.single().name).key)
        assertEquals("starter_item_laptop", assertIs<CatalogComponent.EditableName.Resource>(model.editor?.name).key)
        assertEquals(
            "starter_template_work",
            assertIs<CatalogComponent.UserText.Resource>(
                model.deleteConfirmation?.templateDependencies?.single()?.name,
            ).key,
        )
    }

    @Test
    fun mapsUserTextAndRecoverableErrorWithoutResolvingResources() {
        val item = PackingItem(
            id = PackingItemId("custom"),
            seedNameKey = null,
            userNameOverride = "Очень длинное пользовательское название",
            usualLocation = "Верхний ящик",
            createdOrder = 1,
        )
        val model = CatalogStore.State(
            items = listOf(item),
            isLoading = false,
            error = CatalogStore.Error.SaveFailed,
        ).toComponentModel()

        assertEquals(
            item.userNameOverride,
            assertIs<CatalogComponent.UserText.Authored>(model.items.single().name).value,
        )
        assertEquals(CatalogComponent.ErrorKey.SaveFailed, model.error)
    }

    private fun starterItem() = PackingItem(
        id = PackingItemId("seed-laptop"),
        seedNameKey = ResourceKey("starter_item_laptop"),
        userNameOverride = null,
        usualLocation = null,
        createdOrder = 0,
    )
}
