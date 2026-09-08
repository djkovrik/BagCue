package com.sedsoftware.bagcue.data.session

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sedsoftware.bagcue.data.catalog.SqlDelightCatalogRepository
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.data.template.SqlDelightKitTemplateRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.CompletedPackingSessionRequiredException
import com.sedsoftware.bagcue.domain.session.OneOffSessionItemCommand
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SaveSessionItemToTemplatesCommand
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionTemplateSnapshot
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.session.completeAllPacked
import com.sedsoftware.bagcue.domain.session.completeWithSkipped
import com.sedsoftware.bagcue.domain.session.editSessionItem
import com.sedsoftware.bagcue.domain.session.mergeSessionItems
import com.sedsoftware.bagcue.domain.session.reapplySessionTemplates
import com.sedsoftware.bagcue.domain.session.setSessionItemPacked
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import java.nio.file.Files
import java.util.TimeZone
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class PackingSessionPersistenceTest {
    @Test
    fun exactlyOneSessionPerLocalDateReturnsTheExistingSnapshot() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val first = fixtureSession("session_first", LocalDate(2026, 9, 8))
            assertIs<CreateSessionResult.Created>(sessions.createSession(first).getOrThrow())
            val second = fixtureSession("session_second", first.localDate)
            val occupied = assertIs<CreateSessionResult.DateOccupied>(sessions.createSession(second).getOrThrow())
            assertEquals(first, occupied.existing)
            assertNull(sessions.readSession(second.id).getOrThrow())
        }
    }

    @Test
    fun exactSnapshotRestartsWithTemplatesBagsHintsConflictsAndPackedState() {
        val path = Files.createTempFile("bagcue-session", ".db")
        val expected = fixtureSession("session_restart", LocalDate(2026, 9, 9), conflict = true)
        try {
            open(path.absolutePathString(), true).use { first ->
                runBlocking {
                    seed(first.catalog, first.templates)
                    first.sessions.createSession(expected).getOrThrow()
                }
            }
            open(path.absolutePathString(), false).use { second ->
                runBlocking { assertEquals(expected, second.sessions.readSession(expected.id).getOrThrow()) }
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun reapplyRemoveUndoAndOneOffFailureAreAtomic() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val original = fixtureSession("session_mutation", LocalDate(2026, 9, 10))
            sessions.createSession(original).getOrThrow()
            val fresh = original.items.map { item -> item.copy(quantity = PositionQuantity(5), state = SessionItemState.NotPacked) }
            val reapplied = reapplySessionTemplates(original, original.selectedTemplates, fresh)
            assertEquals(reapplied.session, sessions.replaceActiveSnapshot(reapplied.session).getOrThrow())
            assertEquals(SessionItemState.Packed, sessions.readSession(original.id).getOrThrow()!!.items.first().state)

            val removed = sessions.removeItem(original.id, original.items.last().id).getOrThrow()
            assertEquals(1, removed.session.items.size)
            val restored = sessions.restoreUndo(removed.undo).getOrThrow()
            assertEquals(original.items.map { it.id }, restored.items.map { it.id })

            val beforeFailure = sessions.readSession(original.id).getOrThrow()
            val failed = sessions.addOneOffItem(
                OneOffSessionItemCommand(
                    sessionId = original.id,
                    catalogItemId = PackingItemId("item_laptop"),
                    sessionItemId = SessionPackingItemId("one_off_duplicate"),
                    userName = "Duplicate ID",
                    usualLocation = null,
                ),
            )
            assertTrue(failed.isFailure)
            assertEquals(beforeFailure, sessions.readSession(original.id).getOrThrow())

            val added = sessions.addOneOffItem(
                OneOffSessionItemCommand(
                    sessionId = original.id,
                    catalogItemId = PackingItemId("item_camera"),
                    sessionItemId = SessionPackingItemId("one_off_camera"),
                    userName = " Camera ",
                    usualLocation = " Shelf ",
                ),
            ).getOrThrow()
            assertNotNull(catalog.findItem(PackingItemId("item_camera")).getOrThrow())
            assertEquals(SessionItemState.NotPacked, added.items.last().state)
        }
    }

    @Test
    fun packEditAndBothCompletionModesRoundTrip() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val initial = fixtureSession("session_complete", LocalDate(2026, 9, 11))
            sessions.createSession(initial).getOrThrow()

            val packedRule = setSessionItemPacked(initial, initial.items.first().id, true)
            val packed = sessions.updateItem(initial.id, packedRule.items.first()).getOrThrow()
            val editedRule = editSessionItem(
                packed,
                packed.items.last().id,
                SessionBagAssignment.Assigned(TemplateBagLabel.user(" Day bag ")),
                " Hall table ",
            )
            val edited = sessions.updateItem(initial.id, editedRule.items.last()).getOrThrow()
            assertEquals(" Hall table ", edited.items.last().sourceHint)

            val skipped = completeWithSkipped(edited, 1, 200).let { sessions.completeSession(it).getOrThrow() }
            assertEquals(SessionCompletionMode.WithSkipped, skipped.completionMode)
            assertTrue(skipped.items.last().skipped)

            val allInitial = fixtureSession("session_all", LocalDate(2026, 9, 12)).copy(
                items = fixtureSession("unused", LocalDate(2026, 9, 12)).items.map { it.copy(state = SessionItemState.Packed) },
            )
            sessions.createSession(allInitial).getOrThrow()
            val all = completeAllPacked(allInitial, 300).let { sessions.completeSession(it).getOrThrow() }
            assertEquals(SessionCompletionMode.AllPacked, all.completionMode)
        }
    }

    @Test
    fun savingSessionItemToSelectedTemplatesDoesNotCoupleOrRewriteSession() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val session = fixtureSession("session_template_save", LocalDate(2026, 9, 13))
            sessions.createSession(session).getOrThrow()
            val before = sessions.readSession(session.id).getOrThrow()
            val saved = sessions.saveItemToTemplates(
                SaveSessionItemToTemplatesCommand(
                    sessionId = session.id,
                    sessionItemId = session.items.first().id,
                    templatePositionIds = mapOf(
                        KitTemplateId("template_pool") to TemplatePositionId("position_pool_saved_laptop"),
                    ),
                ),
            ).getOrThrow()
            assertEquals(before, sessions.readSession(session.id).getOrThrow())
            assertTrue(saved.single().positions.any { it.itemId == session.items.first().catalogItemId })
        }
    }

    @Test
    fun historyIsChronologicalFilterableAndSurvivesRestartWithTheSameLocalDate() {
        val path = Files.createTempFile("bagcue-history", ".db")
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            open(path.absolutePathString(), true).use { first ->
                runBlocking {
                    seed(first.catalog, first.templates)
                    val later = fixtureSession("planned_later", LocalDate(2027, 1, 2))
                    val earlier = fixtureSession("planned_earlier", LocalDate(2026, 12, 31))
                    val completedSource = fixtureSession("completed", LocalDate(2027, 1, 1))
                    first.sessions.createSession(later).getOrThrow()
                    first.sessions.createSession(earlier).getOrThrow()
                    first.sessions.createSession(completedSource).getOrThrow()
                    first.sessions.completeSession(completeWithSkipped(completedSource, 1, 500)).getOrThrow()
                }
            }

            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            open(path.absolutePathString(), false).use { second ->
                runBlocking {
                    val history = second.sessions.readHistory().getOrThrow()
                    assertEquals(listOf("planned_earlier", "planned_later"), history.planned.map { it.id.value })
                    assertEquals(listOf("completed"), history.completed.map { it.id.value })
                    assertEquals(history, second.sessions.observeHistory().first())
                    val filtered = second.sessions.readHistory(LocalDate(2027, 1, 1)).getOrThrow()
                    assertTrue(filtered.planned.isEmpty())
                    assertEquals(LocalDate(2027, 1, 1), filtered.completed.single().localDate)
                }
            }
        } finally {
            TimeZone.setDefault(originalTimeZone)
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun completedSessionCanPrepareRepeatThenReopenWithoutLosingRecordedState() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val active = fixtureSession("history_reopen", LocalDate(2026, 9, 14))
            sessions.createSession(active).getOrThrow()
            val completed = sessions.completeSession(completeWithSkipped(active, 1, 600)).getOrThrow()

            val repeat = sessions.prepareRepeat(completed.id).getOrThrow()
            assertEquals(completed.selectedTemplates.map { it.id }, repeat.selectedTemplateIds)

            val reopened = sessions.reopenSession(completed.id).getOrThrow()
            assertEquals(completed.id, reopened.id)
            assertEquals(completed.localDate, reopened.localDate)
            assertEquals(completed.items, reopened.items)
            assertEquals(PackingSessionStatus.Active, reopened.status)
            assertIs<CompletedPackingSessionRequiredException>(
                sessions.prepareRepeat(reopened.id).exceptionOrNull(),
            )
        }
    }

    @Test
    fun deleteAndUndoRestoreTheWholeSessionWithoutChangingCatalogOrTemplates() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val session = fixtureSession("history_delete", LocalDate(2026, 9, 15), conflict = true)
            sessions.createSession(session).getOrThrow()
            val catalogBefore = catalog.readItems().getOrThrow()
            val templatesBefore = templates.readTemplates().getOrThrow()

            val deletion = sessions.deleteSession(session.id).getOrThrow()
            assertNull(sessions.readSession(session.id).getOrThrow())
            assertNull(sessions.findSessionByDate(session.localDate).getOrThrow())
            assertEquals(catalogBefore, catalog.readItems().getOrThrow())
            assertEquals(templatesBefore, templates.readTemplates().getOrThrow())

            val restored = assertIs<CreateSessionResult.Created>(
                sessions.restoreDeletedSession(deletion.undo).getOrThrow(),
            ).session
            assertEquals(session, restored)
        }
    }

    @Test
    fun failedWholeSessionUndoRollsBackTheParentAndEverySnapshotRow() = withRepositories { database, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val session = fixtureSession("history_rollback", LocalDate(2026, 9, 16))
            sessions.createSession(session).getOrThrow()
            val undo = sessions.deleteSession(session.id).getOrThrow().undo
            catalog.deleteItemAtomically(PackingItemId("item_laptop")).getOrThrow()

            assertTrue(sessions.restoreDeletedSession(undo).isFailure)
            assertNull(sessions.readSession(session.id).getOrThrow())
            assertNull(sessions.findSessionByDate(session.localDate).getOrThrow())
            assertTrue(database.sessionQueries.selectTemplateSnapshotsBySession(session.id.value).executeAsList().isEmpty())
            assertTrue(database.sessionQueries.selectSessionItemsBySession(session.id.value).executeAsList().isEmpty())
        }
    }

    @Test
    fun forcedItemCascadeFailureLeavesTheCompleteSessionAggregateUnchanged() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        val catalog = catalog(database)
        val templates = templates(database)
        val sessions = sessions(database)
        try {
            runBlocking {
                seed(catalog, templates)
                val session = fixtureSession("session_delete_rollback", LocalDate(2026, 9, 17), conflict = true)
                sessions.createSession(session).getOrThrow()
                val before = sessions.readSession(session.id).getOrThrow()
                assertNotNull(before)
                val snapshotsBefore = database.sessionQueries.selectTemplateSnapshotsBySession(session.id.value).executeAsList()
                val itemsBefore = database.sessionQueries.selectSessionItemsBySession(session.id.value).executeAsList()
                val candidatesBefore = database.sessionQueries.selectBagCandidatesByItem(session.items.first().id.value).executeAsList()
                driver.execute(
                    identifier = null,
                    sql = """
                        CREATE TRIGGER fail_session_item_cascade
                        BEFORE DELETE ON session_packing_item
                        WHEN OLD.session_id = 'session_delete_rollback'
                        BEGIN SELECT RAISE(ABORT, 'forced child cascade failure'); END
                    """.trimIndent(),
                    parameters = 0,
                )

                val result = sessions.deleteSession(session.id)

                assertTrue(result.isFailure)
                assertEquals(before, sessions.readSession(session.id).getOrThrow())
                assertEquals(snapshotsBefore, database.sessionQueries.selectTemplateSnapshotsBySession(session.id.value).executeAsList())
                assertEquals(itemsBefore, database.sessionQueries.selectSessionItemsBySession(session.id.value).executeAsList())
                assertEquals(candidatesBefore, database.sessionQueries.selectBagCandidatesByItem(session.items.first().id.value).executeAsList())
            }
        } finally {
            driver.close()
        }
    }

    @Test
    fun sessionSnapshotsKeepStarterKeysAndUserAuthoredTextVerbatim() = withRepositories { _, catalog, templates, sessions ->
        runBlocking {
            seed(catalog, templates)
            val base = fixtureSession("localized_snapshot", LocalDate(2026, 9, 18))
            val authored = base.copy(
                selectedTemplates = base.selectedTemplates.map {
                    it.copy(name = SnapshotText(ResourceKey("starter_template_office"), "  Мой Office  "))
                },
                items = base.items.mapIndexed { index, item ->
                    if (index == 0) item.copy(
                        name = SnapshotText(ResourceKey("starter_item_laptop"), "  Laptop №2  "),
                        bagAssignment = SessionBagAssignment.Assigned(TemplateBagLabel.user("  Work bag  ")),
                        sourceHint = "  Upper shelf  ",
                    ) else item
                },
            )

            sessions.createSession(authored).getOrThrow()
            val restored = sessions.readSession(authored.id).getOrThrow()!!

            assertEquals(ResourceKey("starter_template_office"), restored.selectedTemplates.single().name.seedNameKey)
            assertEquals("  Мой Office  ", restored.selectedTemplates.single().name.userText)
            assertEquals(ResourceKey("starter_item_laptop"), restored.items.first().name.seedNameKey)
            assertEquals("  Laptop №2  ", restored.items.first().name.userText)
            assertEquals("  Work bag  ", (restored.items.first().bagAssignment as SessionBagAssignment.Assigned).label.userText)
            assertEquals("  Upper shelf  ", restored.items.first().sourceHint)
        }
    }

    private suspend fun seed(catalog: SqlDelightCatalogRepository, templates: SqlDelightKitTemplateRepository) {
        catalog.installStarterItems().getOrThrow()
        templates.installStarterTemplates().getOrThrow()
    }

    private fun fixtureSession(id: String, date: LocalDate, conflict: Boolean = false): PackingSession = PackingSession(
        id = PackingSessionId(id),
        localDate = date,
        status = PackingSessionStatus.Active,
        completionMode = null,
        createdAtMillis = 100,
        completedAtMillis = null,
        revision = 0,
        selectedTemplates = listOf(
            SessionTemplateSnapshot(
                KitTemplateId("template_office"),
                SnapshotText(ResourceKey("starter_template_office"), null),
                0,
            ),
        ),
        items = listOf(
            SessionPackingItem(
                id = SessionPackingItemId("${id}_laptop"),
                catalogItemId = PackingItemId("item_laptop"),
                name = SnapshotText(ResourceKey("starter_item_laptop"), null),
                quantity = PositionQuantity(2),
                bagAssignment = if (conflict) SessionBagAssignment.Unresolved(
                    listOf(
                        TemplateBagLabel.seed(ResourceKey("starter_bag_backpack")),
                        TemplateBagLabel.seed(ResourceKey("starter_bag_sports")),
                    ),
                ) else SessionBagAssignment.Assigned(TemplateBagLabel.seed(ResourceKey("starter_bag_backpack"))),
                sourceHint = " Desk ",
                state = SessionItemState.Packed,
                skipped = false,
                sortOrder = 0,
            ),
            SessionPackingItem(
                id = SessionPackingItemId("${id}_keys"),
                catalogItemId = PackingItemId("item_keys"),
                name = SnapshotText(ResourceKey("starter_item_keys"), null),
                quantity = PositionQuantity(1),
                bagAssignment = SessionBagAssignment.NoBag,
                sourceHint = null,
                state = SessionItemState.NotPacked,
                skipped = false,
                sortOrder = 1,
            ),
        ),
    )

    private fun withRepositories(
        block: (BagCueDatabase, SqlDelightCatalogRepository, SqlDelightKitTemplateRepository, SqlDelightPackingSessionRepository) -> Unit,
    ) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        try {
            block(database, catalog(database), templates(database), sessions(database))
        } finally {
            driver.close()
        }
    }

    private fun open(path: String, createSchema: Boolean): OpenRepositories {
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
        if (createSchema) BagCueDatabase.Schema.create(driver)
        val database = BagCueDatabase(driver)
        return OpenRepositories(driver, database, catalog(database), templates(database), sessions(database))
    }

    private fun catalog(database: BagCueDatabase) = SqlDelightCatalogRepository(database, Dispatchers.Unconfined) { 1000 }
    private fun templates(database: BagCueDatabase) = SqlDelightKitTemplateRepository(database, Dispatchers.Unconfined) { 1000 }
    private fun sessions(database: BagCueDatabase) = SqlDelightPackingSessionRepository(database, Dispatchers.Unconfined) { 1000 }

    private data class OpenRepositories(
        val driver: JdbcSqliteDriver,
        val database: BagCueDatabase,
        val catalog: SqlDelightCatalogRepository,
        val templates: SqlDelightKitTemplateRepository,
        val sessions: SqlDelightPackingSessionRepository,
    ) : AutoCloseable {
        override fun close() = driver.close()
    }
}
