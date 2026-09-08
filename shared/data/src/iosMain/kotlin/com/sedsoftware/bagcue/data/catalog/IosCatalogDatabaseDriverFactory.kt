package com.sedsoftware.bagcue.data.catalog

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sedsoftware.bagcue.data.db.BagCueDatabase

class IosCatalogDatabaseDriverFactory(
    private val databaseName: String = "bagcue.db",
) : CatalogDatabaseDriverFactory {
    override fun createDriver(): SqlDriver = NativeSqliteDriver(BagCueDatabase.Schema, databaseName)
}
