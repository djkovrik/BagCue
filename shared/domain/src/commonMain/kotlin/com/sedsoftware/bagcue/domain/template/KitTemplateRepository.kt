package com.sedsoftware.bagcue.domain.template

import kotlinx.coroutines.flow.Flow

interface KitTemplateRepository {
    fun observeTemplates(): Flow<List<KitTemplate>>

    suspend fun readTemplates(): Result<List<KitTemplate>>

    suspend fun findTemplate(id: KitTemplateId): Result<KitTemplate?>

    suspend fun installStarterTemplates(
        templates: List<StarterKitTemplate> = DefaultStarterKitTemplates,
    ): Result<Unit>

    suspend fun createTemplate(template: KitTemplate): Result<KitTemplate>

    suspend fun updateTemplate(template: KitTemplate): Result<KitTemplate>

    suspend fun deleteTemplate(id: KitTemplateId): Result<Unit>
}
