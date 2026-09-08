package com.sedsoftware.bagcue.data.template

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sedsoftware.bagcue.data.catalog.SqlDelightCatalogRepository
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.template.DefaultStarterKitTemplates
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.userKitTemplate
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class KitTemplatePersistenceTest {
    @Test
    fun independentEmptyCrudDuplicateAndDeleteUseStableAggregateIds() = withRepositories { _, catalog, templates ->
        runBlocking {
            catalog.installStarterItems().getOrThrow()
            val empty = userKitTemplate(KitTemplateId("template_trip"), "Trip", 10)
            assertEquals(empty, templates.createTemplate(empty).getOrThrow())

            val position = position(
                id = "position_trip_laptop",
                itemId = "item_laptop",
                quantity = 2,
                bag = TemplateBagLabel.user(" cabin bag "),
                source = " desk ",
            )
            val updated = empty.copy(userNameOverride = "Work trip", positions = listOf(position))
            assertEquals(updated, templates.updateTemplate(updated).getOrThrow())

            val duplicate = updated.copy(
                id = KitTemplateId("template_trip_copy"),
                userNameOverride = "Work trip copy",
                sortOrder = 11,
                positions = updated.positions.map { it.copy(id = TemplatePositionId("position_trip_copy_laptop")) },
            )
            assertEquals(duplicate, templates.createTemplate(duplicate).getOrThrow())
            assertEquals(listOf("template_trip", "template_trip_copy"), templates.readTemplates().getOrThrow().map { it.id.value })

            templates.deleteTemplate(updated.id).getOrThrow()
            assertNull(templates.findTemplate(updated.id).getOrThrow())
            assertEquals(duplicate, templates.findTemplate(duplicate.id).getOrThrow())
        }
    }

    @Test
    fun updateAndDeleteNeverMutateAnExistingSessionSnapshot() = withRepositories { database, catalog, templates ->
        val catalogQueries = database.catalogQueries
        runBlocking {
            catalog.installStarterItems().getOrThrow()
            templates.installStarterTemplates().getOrThrow()
            catalogQueries.insertSession("session_snapshot", "2026-09-09", "active", "starter_template_office", null)
            catalogQueries.insertSessionItem(
                "snapshot_laptop", "session_snapshot", "item_laptop", "starter_item_laptop", null, 1,
            )
            val sessionBefore = catalogQueries.selectSessionById("session_snapshot").executeAsOne()
            val itemsBefore = catalogQueries.selectAllSessionItems().executeAsList()

            val office = templates.findTemplate(KitTemplateId("template_office")).getOrThrow()!!
            templates.updateTemplate(
                office.copy(
                    userNameOverride = "My office",
                    positions = office.positions.mapIndexed { index, item ->
                        if (index == 0) item.copy(quantity = PositionQuantity(3)) else item
                    },
                ),
            ).getOrThrow()
            templates.deleteTemplate(KitTemplateId("template_office")).getOrThrow()

            assertEquals(sessionBefore, catalogQueries.selectSessionById("session_snapshot").executeAsOne())
            assertEquals(itemsBefore, catalogQueries.selectAllSessionItems().executeAsList())
        }
    }

    @Test
    fun officeModificationAndPoolDeletionSurviveRestartAndReconciliation() {
        val path = Files.createTempFile("bagcue-templates", ".db")
        try {
            open(path.absolutePathString(), createSchema = true).use { first ->
                runBlocking {
                    first.catalog.installStarterItems().getOrThrow()
                    first.templates.installStarterTemplates().getOrThrow()
                    val office = first.templates.findTemplate(KitTemplateId("template_office")).getOrThrow()!!
                    first.templates.updateTemplate(
                        office.copy(
                            userNameOverride = "  My Office  ",
                            positions = office.positions.mapIndexed { index, position ->
                                if (index == 0) position.copy(
                                    quantity = PositionQuantity(4),
                                    bagLabel = TemplateBagLabel.user(" Personal bag "),
                                    sourceHintOverride = " Home desk ",
                                ) else position
                            },
                        ),
                    ).getOrThrow()
                    first.templates.deleteTemplate(KitTemplateId("template_pool")).getOrThrow()
                }
            }

            open(path.absolutePathString(), createSchema = false).use { second ->
                runBlocking {
                    second.catalog.installStarterItems().getOrThrow()
                    second.templates.installStarterTemplates().getOrThrow()
                    val office = second.templates.findTemplate(KitTemplateId("template_office")).getOrThrow()
                    assertNotNull(office)
                    assertEquals(ResourceKey("starter_template_office"), office.seedNameKey)
                    assertEquals("  My Office  ", office.userNameOverride)
                    assertEquals(4, office.positions.first().quantity.value)
                    assertEquals(" Personal bag ", office.positions.first().bagLabel.userText)
                    assertEquals(" Home desk ", office.positions.first().sourceHintOverride)
                    assertNull(second.templates.findTemplate(KitTemplateId("template_pool")).getOrThrow())
                    assertEquals(
                        "modified",
                        second.database.templateQueries.selectTemplateSeedOverride("template_office").executeAsOne().state,
                    )
                    assertEquals(
                        "deleted",
                        second.database.templateQueries.selectTemplateSeedOverride("template_pool").executeAsOne().state,
                    )
                }
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun invalidChildReferenceRollsBackNewTemplateParent() = withRepositories { _, catalog, templates ->
        runBlocking {
            catalog.installStarterItems().getOrThrow()
            val invalid = userKitTemplate(
                id = KitTemplateId("template_invalid"),
                name = "Invalid",
                sortOrder = 20,
                positions = listOf(position("invalid_child", "missing_item", 1)),
            )
            assertTrue(templates.createTemplate(invalid).isFailure)
            assertNull(templates.findTemplate(invalid.id).getOrThrow())
        }
    }

    @Test
    fun forcedChildCascadeFailureRollsBackTemplateParentPositionsAndSeedOverride() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        val catalog = catalog(database)
        val templates = templates(database)
        try {
            runBlocking {
                catalog.installStarterItems().getOrThrow()
                templates.installStarterTemplates().getOrThrow()
                val id = KitTemplateId("template_office")
                val before = templates.findTemplate(id).getOrThrow()
                assertNotNull(before)
                assertNull(database.templateQueries.selectTemplateSeedOverride(id.value).executeAsOneOrNull())
                driver.execute(
                    identifier = null,
                    sql = """
                        CREATE TRIGGER fail_template_position_cascade
                        BEFORE DELETE ON template_position
                        WHEN OLD.template_id = 'template_office'
                        BEGIN SELECT RAISE(ABORT, 'forced child cascade failure'); END
                    """.trimIndent(),
                    parameters = 0,
                )

                val result = templates.deleteTemplate(id)

                assertTrue(result.isFailure)
                assertEquals(before, templates.findTemplate(id).getOrThrow())
                assertEquals(before.positions.size, database.templateQueries.selectPositionsByTemplate(id.value).executeAsList().size)
                assertNull(database.templateQueries.selectTemplateSeedOverride(id.value).executeAsOneOrNull())
            }
        } finally {
            driver.close()
        }
    }

    @Test
    fun starterKeysAndUserAuthoredTemplateTextRoundTripWithoutResolutionOrNormalization() = withRepositories { _, catalog, templates ->
        runBlocking {
            catalog.installStarterItems().getOrThrow()
            templates.installStarterTemplates().getOrThrow()
            val office = templates.findTemplate(KitTemplateId("template_office")).getOrThrow()!!
            val authored = office.copy(
                userNameOverride = "  My офис 🎒  ",
                positions = office.positions.mapIndexed { index, position ->
                    if (index == 0) position.copy(
                        bagLabel = TemplateBagLabel.user("  Bag №1  "),
                        sourceHintOverride = "  Left shelf  ",
                    ) else position
                },
            )

            templates.updateTemplate(authored).getOrThrow()
            templates.installStarterTemplates().getOrThrow()
            val restored = templates.findTemplate(authored.id).getOrThrow()!!

            assertEquals(ResourceKey("starter_template_office"), restored.seedNameKey)
            assertEquals("  My офис 🎒  ", restored.userNameOverride)
            assertEquals("  Bag №1  ", restored.positions.first().bagLabel.userText)
            assertEquals("  Left shelf  ", restored.positions.first().sourceHintOverride)
            assertNull(restored.positions.first().bagLabel.seedNameKey)
        }
    }

    private fun position(
        id: String,
        itemId: String,
        quantity: Int,
        bag: TemplateBagLabel = TemplateBagLabel.None,
        source: String? = null,
    ) = TemplatePosition(
        id = TemplatePositionId(id),
        itemId = PackingItemId(itemId),
        quantity = PositionQuantity(quantity),
        bagLabel = bag,
        sourceHintOverride = source,
        sortOrder = 0,
    )

    private fun withRepositories(
        block: (BagCueDatabase, SqlDelightCatalogRepository, SqlDelightKitTemplateRepository) -> Unit,
    ) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        try {
            block(database, catalog(database), templates(database))
        } finally {
            driver.close()
        }
    }

    private fun open(path: String, createSchema: Boolean): OpenRepositories {
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
        if (createSchema) BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        return OpenRepositories(driver, database, catalog(database), templates(database))
    }

    private fun catalog(database: BagCueDatabase) = SqlDelightCatalogRepository(
        database = database,
        ioDispatcher = Dispatchers.Unconfined,
        currentTimeMillis = { 1_700_000_000_000L },
    )

    private fun templates(database: BagCueDatabase) = SqlDelightKitTemplateRepository(
        database = database,
        ioDispatcher = Dispatchers.Unconfined,
        currentTimeMillis = { 1_700_000_000_000L },
    )

    private data class OpenRepositories(
        val driver: JdbcSqliteDriver,
        val database: BagCueDatabase,
        val catalog: SqlDelightCatalogRepository,
        val templates: SqlDelightKitTemplateRepository,
    ) : AutoCloseable {
        override fun close() = driver.close()
    }
}
