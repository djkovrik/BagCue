package com.sedsoftware.bagcue.data.catalog

import app.cash.sqldelight.db.SqlDriver
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.data.template.SqlDelightKitTemplateRepository
import com.sedsoftware.bagcue.data.session.SqlDelightPackingSessionRepository
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import kotlinx.coroutines.CoroutineDispatcher

fun interface CatalogDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

data class CatalogDataModuleDependencies(
    val driverFactory: CatalogDatabaseDriverFactory,
    val ioDispatcher: CoroutineDispatcher,
    val currentTimeMillis: () -> Long,
)

interface CatalogDataModule {
    val catalogRepository: CatalogRepository
    val kitTemplateRepository: KitTemplateRepository
    val packingSessionRepository: SessionHistoryRepository
}

fun CatalogDataModule(dependencies: CatalogDataModuleDependencies): CatalogDataModule =
    DefaultCatalogDataModule(dependencies)

private class DefaultCatalogDataModule(
    private val dependencies: CatalogDataModuleDependencies,
) : CatalogDataModule {
    private val database: BagCueDatabase by lazy {
        BagCueDatabase(dependencies.driverFactory.createDriver())
    }

    override val catalogRepository: CatalogRepository by lazy {
        SqlDelightCatalogRepository(
            database = database,
            ioDispatcher = dependencies.ioDispatcher,
            currentTimeMillis = dependencies.currentTimeMillis,
        )
    }

    override val kitTemplateRepository: KitTemplateRepository by lazy {
        SqlDelightKitTemplateRepository(
            database = database,
            ioDispatcher = dependencies.ioDispatcher,
            currentTimeMillis = dependencies.currentTimeMillis,
        )
    }

    override val packingSessionRepository: SessionHistoryRepository by lazy {
        SqlDelightPackingSessionRepository(
            database = database,
            ioDispatcher = dependencies.ioDispatcher,
            currentTimeMillis = dependencies.currentTimeMillis,
        )
    }
}
