package com.sedsoftware.bagcue.data.catalog

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sedsoftware.bagcue.data.db.BagCueDatabase

class AndroidCatalogDatabaseDriverFactory(
    private val context: Context,
    private val databaseName: String = "bagcue.db",
) : CatalogDatabaseDriverFactory {
    override fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = BagCueDatabase.Schema,
        context = context,
        name = databaseName,
    )
}
