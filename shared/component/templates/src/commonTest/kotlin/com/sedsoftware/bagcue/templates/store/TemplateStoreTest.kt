package com.sedsoftware.bagcue.templates.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.StarterKitTemplate
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.templates.domain.TemplateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateStoreTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun failedSaveRetainsVerbatimTemplateDraftAndRetryCommitsIt() = runTest(dispatcher) {
        val repository = FailingTemplateRepository()
        val manager = TemplateManager(
            templateRepository = repository,
            catalogRepository = EmptyCatalogRepository,
            templateIdGenerator = KitTemplateIdGenerator { KitTemplateId("created") },
            positionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("position") },
            resolveResourceKey = { it.value },
            createDuplicateName = { "$it copy" },
            analyticsController = NoOpAnalyticsController,
        )
        val store = TemplateStoreProvider(DefaultStoreFactory(), manager).provide()
        try {
            store.init()
            advanceUntilIdle()
            store.accept(TemplateStore.Intent.StartCreate)
            store.accept(TemplateStore.Intent.ChangeTemplateName("  Мой work kit 🎒  "))
            store.accept(TemplateStore.Intent.SaveTemplate)
            advanceUntilIdle()

            assertEquals(TemplateStore.Error.SaveFailed, store.state.error)
            assertEquals("  Мой work kit 🎒  ", store.state.editor?.nameInput)
            assertEquals(false, store.state.editor?.isSaving)
            assertEquals(emptyList(), repository.templates.value)

            repository.createFailure = null
            store.accept(TemplateStore.Intent.SaveTemplate)
            advanceUntilIdle()

            assertNull(store.state.editor)
            assertEquals("  Мой work kit 🎒  ", repository.templates.value.single().userNameOverride)
        } finally {
            store.dispose()
        }
    }
}

private class FailingTemplateRepository : KitTemplateRepository {
    val templates = MutableStateFlow<List<KitTemplate>>(emptyList())
    var createFailure: Throwable? = IllegalStateException("forced save failure")

    override fun observeTemplates(): Flow<List<KitTemplate>> = templates
    override suspend fun readTemplates(): Result<List<KitTemplate>> = Result.success(templates.value)
    override suspend fun findTemplate(id: KitTemplateId): Result<KitTemplate?> = Result.success(templates.value.firstOrNull { it.id == id })
    override suspend fun installStarterTemplates(templates: List<StarterKitTemplate>): Result<Unit> = Result.success(Unit)
    override suspend fun createTemplate(template: KitTemplate): Result<KitTemplate> {
        createFailure?.let { return Result.failure(it) }
        templates.value += template
        return Result.success(template)
    }
    override suspend fun updateTemplate(template: KitTemplate): Result<KitTemplate> = error("Not used")
    override suspend fun deleteTemplate(id: KitTemplateId): Result<Unit> = error("Not used")
}

private data object EmptyCatalogRepository : CatalogRepository {
    override fun observeItems(): Flow<List<PackingItem>> = MutableStateFlow(emptyList())
    override suspend fun readItems(): Result<List<PackingItem>> = Result.success(emptyList())
    override suspend fun findItem(id: PackingItemId): Result<PackingItem?> = Result.success(null)
    override suspend fun installStarterItems(items: List<StarterPackingItem>): Result<Unit> = Result.success(Unit)
    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary> = error("Not used")
    override suspend fun deleteItemAtomically(id: PackingItemId): Result<Unit> = error("Not used")
}

private data object NoOpAnalyticsController : AnalyticsController {
    override suspend fun setCollectionEnabled(enabled: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun resetAnalyticsData(): Result<Unit> = Result.success(Unit)
    override suspend fun readAppInstanceId(): Result<String?> = Result.success(null)
    override suspend fun record(event: AnalyticsEvent): Result<Unit> = Result.success(Unit)
}
