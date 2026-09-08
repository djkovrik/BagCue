package com.sedsoftware.bagcue.data.catalog

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.DefaultStarterPackingItems
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CatalogPersistenceTest {
    @Test
    fun generatedSchemaSupportsCreateReadUpdateDeleteAndLocalizationKeyRoundTrip() = withRepository { _, repository, _ ->
        runBlocking {
            repository.installStarterItems().getOrThrow()
            val starter = repository.findItem(PackingItemId("item_laptop")).getOrThrow()
            assertEquals(ResourceKey("starter_item_laptop"), starter?.seedNameKey)
            assertNull(starter?.userNameOverride)

            val created = repository.createItem(
                CreatePackingItem(PackingItemId("user_usb_cable"), "  USB   cable ", " Drawer "),
            ).getOrThrow()
            assertEquals("  USB   cable ", created.userNameOverride)
            assertEquals(" Drawer ", created.usualLocation)

            val updated = repository.updateItem(
                UpdatePackingItem(created.id, "Travel cable", ""),
            ).getOrThrow()
            assertEquals(created.id, updated.id)
            assertEquals("Travel cable", updated.userNameOverride)
            assertNull(updated.usualLocation)

            repository.deleteItemAtomically(created.id).getOrThrow()
            assertNull(repository.findItem(created.id).getOrThrow())
            assertEquals(DefaultStarterPackingItems.size, repository.readItems().getOrThrow().size)
        }
    }

    @Test
    fun starterRenameAndDeleteOverridesSurviveARealDriverRestart() {
        val path = Files.createTempFile("bagcue-catalog", ".db")
        try {
            openFileRepository(path.absolutePathString(), createSchema = true).use { first ->
                runBlocking {
                    first.repository.installStarterItems().getOrThrow()
                    first.repository.updateItem(
                        UpdatePackingItem(PackingItemId("item_laptop"), "Work computer", null),
                    ).getOrThrow()
                    first.repository.deleteItemAtomically(PackingItemId("item_charger")).getOrThrow()
                }
            }

            openFileRepository(path.absolutePathString(), createSchema = false).use { second ->
                runBlocking {
                    second.repository.installStarterItems().getOrThrow()
                    assertEquals(
                        "Work computer",
                        second.repository.findItem(PackingItemId("item_laptop")).getOrThrow()?.userNameOverride,
                    )
                    assertNull(second.repository.findItem(PackingItemId("item_charger")).getOrThrow())
                    assertEquals("modified", second.database.catalogQueries.selectSeedOverride("item_laptop").executeAsOne().state)
                    assertEquals("deleted", second.database.catalogQueries.selectSeedOverride("item_charger").executeAsOne().state)
                }
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun untouchedStarterDefinitionRefreshesButFirstCustomizationFreezesItsIdentity() = withRepository { _, repository, _ ->
        val id = PackingItemId("item_seed_probe")
        runBlocking {
            repository.installStarterItems(listOf(StarterPackingItem(id, ResourceKey("starter_probe_v1"), 100))).getOrThrow()
            repository.installStarterItems(listOf(StarterPackingItem(id, ResourceKey("starter_probe_v2"), 100))).getOrThrow()
            assertEquals(ResourceKey("starter_probe_v2"), repository.findItem(id).getOrThrow()?.seedNameKey)

            repository.updateItem(UpdatePackingItem(id, null, " shelf ")).getOrThrow()
            repository.installStarterItems(listOf(StarterPackingItem(id, ResourceKey("starter_probe_v3"), 100))).getOrThrow()
            val customized = repository.findItem(id).getOrThrow()
            assertEquals(ResourceKey("starter_probe_v2"), customized?.seedNameKey)
            assertNull(customized?.userNameOverride)
            assertEquals(" shelf ", customized?.usualLocation)
        }
    }

    @Test
    fun dependencyDeleteIsAtomicAndCompletedHistoryRetainsItsSnapshot() = withRepository { database, repository, _ ->
        val q = database.catalogQueries
        runBlocking {
            repository.createItem(CreatePackingItem(PackingItemId("item_camera"), "Camera", null)).getOrThrow()
            q.insertTemplate("template_trip", null, "Trip", 1)
            q.insertTemplatePosition("position_camera", "template_trip", "item_camera", 1, null, null, 0)
            q.insertSession("session_active", "2026-09-09", "active", null, "Tomorrow")
            q.insertSessionItem("active_camera", "session_active", "item_camera", null, "Camera", 1)
            q.insertSession("session_done", "2026-09-08", "completed", null, "Today")
            q.insertSessionItem("done_camera", "session_done", "item_camera", null, "Camera", 1)

            val summary = repository.readDeleteDependencies(PackingItemId("item_camera")).getOrThrow()
            assertEquals(listOf("template_trip"), summary.templates.map { it.id })
            assertEquals(listOf("session_active"), summary.unfinishedSessions.map { it.id })

            repository.deleteItemAtomically(PackingItemId("item_camera")).getOrThrow()
            assertEquals(0L, q.countTemplatePositionsForItem("item_camera").executeAsOne())
            assertEquals(0L, q.countActiveSessionItemsForItem("item_camera").executeAsOne())
            assertEquals(0L, q.countCompletedSessionItemsForItem("item_camera").executeAsOne())
            val completedReference = q
                .selectAllSessionItems()
                .executeAsList()
                .single { it.id == "done_camera" }
            assertNull(completedReference.catalog_item_id)
            assertEquals("Camera", completedReference.display_user_name)
        }
    }

    @Test
    fun dependencyDeleteFailureRollsBackEveryEarlierMutation() = withRepository { database, repository, driver ->
        val q = database.catalogQueries
        runBlocking {
            repository.createItem(CreatePackingItem(PackingItemId("item_camera"), "Camera", null)).getOrThrow()
            q.insertTemplate("template_trip", null, "Trip", 1)
            q.insertTemplatePosition("position_camera", "template_trip", "item_camera", 1, null, null, 0)
            q.insertSession("session_active", "2026-09-09", "active", null, "Tomorrow")
            q.insertSessionItem("active_camera", "session_active", "item_camera", null, "Camera", 1)
            driver.execute(
                identifier = null,
                sql = "CREATE TRIGGER fail_catalog_delete BEFORE DELETE ON packing_item BEGIN SELECT RAISE(ABORT, 'forced failure'); END",
                parameters = 0,
            )

            assertTrue(repository.deleteItemAtomically(PackingItemId("item_camera")).isFailure)
            assertNotNull(repository.findItem(PackingItemId("item_camera")).getOrThrow())
            assertEquals(1L, q.countTemplatePositionsForItem("item_camera").executeAsOne())
            assertEquals(1L, q.countActiveSessionItemsForItem("item_camera").executeAsOne())
        }
    }

    private fun withRepository(block: (BagCueDatabase, SqlDelightCatalogRepository, JdbcSqliteDriver) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        try {
            block(database, repository(database), driver)
        } finally {
            driver.close()
        }
    }

    private fun openFileRepository(path: String, createSchema: Boolean): OpenRepository {
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
        if (createSchema) BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        return OpenRepository(driver, database, repository(database))
    }

    private fun repository(database: BagCueDatabase) = SqlDelightCatalogRepository(
        database = database,
        ioDispatcher = Dispatchers.Unconfined,
        currentTimeMillis = { 1_700_000_000_000L },
    )

    private data class OpenRepository(
        val driver: JdbcSqliteDriver,
        val database: BagCueDatabase,
        val repository: SqlDelightCatalogRepository,
    ) : AutoCloseable {
        override fun close() = driver.close()
    }
}
