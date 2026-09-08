package com.sedsoftware.bagcue.templates.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.templates.TemplateComponent
import com.sedsoftware.bagcue.templates.domain.TemplateManager
import com.sedsoftware.bagcue.templates.store.TemplateStore
import com.sedsoftware.bagcue.templates.store.TemplateStoreProvider

class TemplateComponentDefault(
    componentContext: ComponentContext,
    templateRepository: KitTemplateRepository,
    catalogRepository: CatalogRepository,
    storeFactory: StoreFactory,
    templateIdGenerator: KitTemplateIdGenerator,
    positionIdGenerator: TemplatePositionIdGenerator,
    resolveResourceKey: suspend (ResourceKey) -> String,
    createDuplicateName: suspend (String) -> String,
    analyticsController: AnalyticsController,
    private val onOpenCatalog: () -> Unit,
) : TemplateComponent, ComponentContext by componentContext {
    private val store = instanceKeeper.getOrCreate(key = STORE_KEY) {
        StoreHolder(
            TemplateStoreProvider(
                storeFactory = storeFactory,
                manager = TemplateManager(
                    templateRepository = templateRepository,
                    catalogRepository = catalogRepository,
                    templateIdGenerator = templateIdGenerator,
                    positionIdGenerator = positionIdGenerator,
                    resolveResourceKey = resolveResourceKey,
                    createDuplicateName = createDuplicateName,
                    analyticsController = analyticsController,
                ),
            ).provide().also(TemplateStore::init),
        )
    }.store

    override val model: Value<TemplateComponent.Model> = store.asValue().map(TemplateStore.State::toComponentModel)

    override fun startCreate() = store.accept(TemplateStore.Intent.StartCreate)
    override fun openTemplate(templateId: KitTemplateId) = store.accept(TemplateStore.Intent.OpenTemplate(templateId))
    override fun changeTemplateName(value: String) = store.accept(TemplateStore.Intent.ChangeTemplateName(value))
    override fun saveTemplate() = store.accept(TemplateStore.Intent.SaveTemplate)
    override fun duplicateTemplate(templateId: KitTemplateId) = store.accept(TemplateStore.Intent.DuplicateTemplate(templateId))
    override fun requestDeleteTemplate(templateId: KitTemplateId) = store.accept(TemplateStore.Intent.RequestDeleteTemplate(templateId))
    override fun confirmDeleteTemplate() = store.accept(TemplateStore.Intent.ConfirmDeleteTemplate)
    override fun dismissDeleteTemplate() = store.accept(TemplateStore.Intent.DismissDeleteTemplate)
    override fun startAddPosition() = store.accept(TemplateStore.Intent.StartAddPosition)
    override fun changeItemSearch(value: String) = store.accept(TemplateStore.Intent.ChangeItemSearch(value))
    override fun selectItem(itemId: PackingItemId) = store.accept(TemplateStore.Intent.SelectItem(itemId))
    override fun closeItemSelector() = store.accept(TemplateStore.Intent.CloseItemSelector)
    override fun editPosition(positionId: TemplatePositionId) = store.accept(TemplateStore.Intent.EditPosition(positionId))
    override fun changePositionQuantity(value: String) = store.accept(TemplateStore.Intent.ChangePositionQuantity(value))
    override fun changePositionBag(value: String) = store.accept(TemplateStore.Intent.ChangePositionBag(value))
    override fun changePositionSource(value: String) = store.accept(TemplateStore.Intent.ChangePositionSource(value))
    override fun savePosition() = store.accept(TemplateStore.Intent.SavePosition)
    override fun removePosition(positionId: TemplatePositionId) = store.accept(TemplateStore.Intent.RemovePosition(positionId))
    override fun closePositionEditor() = store.accept(TemplateStore.Intent.ClosePositionEditor)
    override fun requestCloseEditor() = store.accept(TemplateStore.Intent.RequestCloseEditor)
    override fun confirmDiscardChanges() = store.accept(TemplateStore.Intent.ConfirmDiscardChanges)
    override fun dismissDiscardChanges() = store.accept(TemplateStore.Intent.DismissDiscardChanges)
    override fun openCatalog() = onOpenCatalog()
    override fun clearError() = store.accept(TemplateStore.Intent.ClearError)

    private class StoreHolder(val store: TemplateStore) : InstanceKeeper.Instance {
        override fun onDestroy() = store.dispose()
    }

    private companion object {
        const val STORE_KEY = "TemplateStore"
    }
}
