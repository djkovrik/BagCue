package com.sedsoftware.bagcue.templates.domain

import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.StarterKitTemplate
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TemplateManagerTest {
    @Test
    fun duplicateCreatesIndependentAggregateWithDistinguishableName() = runTest {
        val original = template(
            id = "original",
            name = "Office",
            positions = listOf(position("old-position", "item_laptop")),
        )
        val repository = FakeTemplateRepository(listOf(original))
        val manager = manager(repository)

        val duplicate = manager.duplicate(original.id).getOrThrow()

        assertEquals("Office copy", duplicate.userNameOverride)
        assertNotEquals(original.id, duplicate.id)
        assertNotEquals(original.positions.single().id, duplicate.positions.single().id)
        assertEquals(original.positions.single().itemId, duplicate.positions.single().itemId)
        assertEquals(listOf(original, duplicate), repository.templates.value)
    }

    @Test
    fun deleteOnlyDelegatesTemplateAggregateDeletion() = runTest {
        val original = template("original", "Pool")
        val repository = FakeTemplateRepository(listOf(original))

        manager(repository).delete(original.id).getOrThrow()

        assertEquals(emptyList(), repository.templates.value)
    }

    @Test
    fun cancellationIsNotConvertedToFailure() = runTest {
        val original = template("original", "Office")
        val repository = FakeTemplateRepository(listOf(original), cancelOnFind = true)

        assertFailsWith<CancellationException> { manager(repository).duplicate(original.id) }
    }

    @Test
    fun successfulTemplateCreationsRecordOnlyContentFreeTemplateCreatedEvents() = runTest {
        val repository = FakeTemplateRepository(emptyList())
        val analytics = RecordingTemplateAnalyticsController()
        val manager = manager(repository, analytics)
        val created = template("created", "User-authored private name")

        manager.save(created).getOrThrow()
        manager.duplicate(created.id).getOrThrow()

        assertEquals(
            listOf(
                AnalyticsEvent(AnalyticsEventName.TemplateCreated),
                AnalyticsEvent(AnalyticsEventName.TemplateCreated),
            ),
            analytics.events,
        )
    }

    @Test
    fun updatedTemplateDoesNotRecordTemplateCreatedEvent() = runTest {
        val original = template("existing", "Existing")
        val analytics = RecordingTemplateAnalyticsController()

        manager(FakeTemplateRepository(listOf(original)), analytics)
            .save(original.copy(userNameOverride = "Changed private name"))
            .getOrThrow()

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun failedTemplateCreationDoesNotRecordAnalyticsEvent() = runTest {
        val analytics = RecordingTemplateAnalyticsController()
        val result = manager(
            FakeTemplateRepository(emptyList(), failOnCreate = true),
            analytics,
        ).save(template("failed", "Private"))

        assertTrue(result.isFailure)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun cancelledTemplateCreationDoesNotRecordAnalyticsEvent() = runTest {
        val analytics = RecordingTemplateAnalyticsController()
        val manager = manager(
            FakeTemplateRepository(emptyList(), cancelOnCreate = true),
            analytics,
        )

        assertFailsWith<CancellationException> { manager.save(template("cancelled", "Private")) }
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun failedUpdatePreservesOriginalCauseAndPriorDurableTemplate() = runTest {
        val original = template("existing", "  Original 🎒  ")
        val failure = IllegalStateException("write rejected")
        val repository = FakeTemplateRepository(listOf(original), updateFailure = failure)

        val result = manager(repository).save(original.copy(userNameOverride = "  Changed имя  "))

        assertSame(failure, result.exceptionOrNull())
        assertEquals(original, repository.templates.value.single())
    }

    @Test
    fun updateCancellationFromRepositoryResultIsRethrown() = runTest {
        val original = template("existing", "Original")
        val cancellation = CancellationException("cancel update")
        val repository = FakeTemplateRepository(listOf(original), updateFailure = cancellation)

        assertSame(
            cancellation,
            assertFailsWith<CancellationException> {
                manager(repository).save(original.copy(userNameOverride = "Changed"))
            },
        )
        assertEquals(original, repository.templates.value.single())
    }

    private fun manager(
        repository: KitTemplateRepository,
        analyticsController: AnalyticsController = RecordingTemplateAnalyticsController(),
    ): TemplateManager {
        var positionIndex = 0
        return TemplateManager(
            templateRepository = repository,
            catalogRepository = FakeCatalogRepository(),
            templateIdGenerator = KitTemplateIdGenerator { KitTemplateId("duplicate") },
            positionIdGenerator = TemplatePositionIdGenerator {
                TemplatePositionId("duplicate-position-${positionIndex++}")
            },
            resolveResourceKey = { it.value },
            createDuplicateName = { "$it copy" },
            analyticsController = analyticsController,
        )
    }
}

private fun template(
    id: String,
    name: String,
    positions: List<TemplatePosition> = emptyList(),
) = KitTemplate(
    id = KitTemplateId(id),
    seedNameKey = null,
    userNameOverride = name,
    sortOrder = 0,
    positions = positions,
)

private fun position(id: String, itemId: String) = TemplatePosition(
    id = TemplatePositionId(id),
    itemId = PackingItemId(itemId),
    quantity = PositionQuantity(1),
    bagLabel = TemplateBagLabel.None,
    sourceHintOverride = null,
    sortOrder = 0,
)

private class FakeTemplateRepository(
    initialTemplates: List<KitTemplate>,
    private val cancelOnFind: Boolean = false,
    private val failOnCreate: Boolean = false,
    private val cancelOnCreate: Boolean = false,
    private val updateFailure: Throwable? = null,
) : KitTemplateRepository {
    val templates = MutableStateFlow(initialTemplates)

    override fun observeTemplates(): Flow<List<KitTemplate>> = templates
    override suspend fun readTemplates(): Result<List<KitTemplate>> = Result.success(templates.value)
    override suspend fun findTemplate(id: KitTemplateId): Result<KitTemplate?> {
        if (cancelOnFind) throw CancellationException("cancel")
        return Result.success(templates.value.firstOrNull { it.id == id })
    }
    override suspend fun installStarterTemplates(templates: List<StarterKitTemplate>): Result<Unit> = Result.success(Unit)
    override suspend fun createTemplate(template: KitTemplate): Result<KitTemplate> {
        if (cancelOnCreate) throw CancellationException("cancel create")
        if (failOnCreate) return Result.failure(IllegalStateException("create failed"))
        templates.value += template
        return Result.success(template)
    }
    override suspend fun updateTemplate(template: KitTemplate): Result<KitTemplate> {
        updateFailure?.let { return Result.failure(it) }
        templates.value = templates.value.map { if (it.id == template.id) template else it }
        return Result.success(template)
    }
    override suspend fun deleteTemplate(id: KitTemplateId): Result<Unit> {
        templates.value = templates.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

private class RecordingTemplateAnalyticsController : AnalyticsController {
    val events = mutableListOf<AnalyticsEvent>()

    override suspend fun setCollectionEnabled(enabled: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun resetAnalyticsData(): Result<Unit> = Result.success(Unit)
    override suspend fun readAppInstanceId(): Result<String?> = Result.success(null)
    override suspend fun record(event: AnalyticsEvent): Result<Unit> = Result.success(Unit).also {
        events += event
    }
}

private class FakeCatalogRepository : CatalogRepository {
    override fun observeItems(): Flow<List<PackingItem>> = MutableStateFlow(emptyList())
    override suspend fun readItems(): Result<List<PackingItem>> = Result.success(emptyList())
    override suspend fun findItem(id: PackingItemId): Result<PackingItem?> = Result.success(null)
    override suspend fun installStarterItems(items: List<StarterPackingItem>): Result<Unit> = Result.success(Unit)
    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun readDeleteDependencies(id: PackingItemId): Result<CatalogDependencySummary> = error("Not used")
    override suspend fun deleteItemAtomically(id: PackingItemId): Result<Unit> = error("Not used")
}
