package com.sedsoftware.bagcue.templates.domain

import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class TemplateData(
    val templates: List<KitTemplate>,
    val catalogItems: List<PackingItem>,
    val catalogSearchNames: Map<PackingItemId, String>,
)

internal class TemplateManager(
    private val templateRepository: KitTemplateRepository,
    private val catalogRepository: CatalogRepository,
    private val templateIdGenerator: KitTemplateIdGenerator,
    private val positionIdGenerator: TemplatePositionIdGenerator,
    private val resolveResourceKey: suspend (ResourceKey) -> String,
    private val createDuplicateName: suspend (String) -> String,
    private val analyticsController: AnalyticsController,
) {
    fun observeData(): Flow<TemplateData> = combine(
        templateRepository.observeTemplates(),
        catalogRepository.observeItems(),
    ) { templates, items ->
        TemplateData(
            templates = templates,
            catalogItems = items,
            catalogSearchNames = items.associate { item ->
                item.id to (item.userNameOverride ?: resolveResourceKey(requireNotNull(item.seedNameKey)))
            },
        )
    }

    suspend fun initialize(): Result<Unit> = captureResult {
        catalogRepository.installStarterItems().getOrThrow()
        templateRepository.installStarterTemplates().getOrThrow()
    }

    suspend fun save(template: KitTemplate): Result<KitTemplate> = captureResult {
        if (template.id.value.isBlank()) error("Template ID is required")
        if (templateRepository.findTemplate(template.id).getOrThrow() == null) {
            templateRepository.createTemplate(template).getOrThrow().also {
                recordSafely(AnalyticsEventName.TemplateCreated)
            }
        } else {
            templateRepository.updateTemplate(template).getOrThrow()
        }
    }

    suspend fun duplicate(templateId: KitTemplateId): Result<KitTemplate> = captureResult {
        val original = templateRepository.findTemplate(templateId).getOrThrow()
            ?: throw com.sedsoftware.bagcue.domain.template.KitTemplateNotFoundException(templateId)
        val visibleName = original.userNameOverride
            ?: resolveResourceKey(requireNotNull(original.seedNameKey))
        val existing = templateRepository.readTemplates().getOrThrow()
        val copy = KitTemplate(
            id = templateIdGenerator.nextId(),
            seedNameKey = null,
            userNameOverride = validatedCatalogName(createDuplicateName(visibleName)),
            sortOrder = (existing.maxOfOrNull(KitTemplate::sortOrder) ?: -1L) + 1L,
            positions = original.positions.mapIndexed { index, position ->
                position.copy(
                    id = positionIdGenerator.nextId(),
                    sortOrder = index.toLong(),
                )
            },
        )
        templateRepository.createTemplate(copy).getOrThrow().also {
            recordSafely(AnalyticsEventName.TemplateCreated)
        }
    }

    suspend fun delete(templateId: KitTemplateId): Result<Unit> = captureResult {
        templateRepository.deleteTemplate(templateId).getOrThrow()
    }

    fun nextTemplateId(): KitTemplateId = templateIdGenerator.nextId()
    fun nextPositionId() = positionIdGenerator.nextId()

    private suspend fun recordSafely(name: AnalyticsEventName) {
        analyticsController.record(AnalyticsEvent(name)).exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
        }
    }
}

private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
