package com.sedsoftware.bagcue.domain.session

import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionRulesTest {
    @Test
    fun mergeUsesStableItemIdentityMaximumQuantityAndTypedBagConflict() {
        var nextId = 0
        val merged = mergeSessionItems(
            inputs = listOf(
                input("item_laptop", 1, TemplateBagLabel.seed(ResourceKey("starter_bag_backpack"))),
                input("item_laptop", 4, TemplateBagLabel.seed(ResourceKey("starter_bag_sports"))),
                input("item_keys", 2, TemplateBagLabel.None),
            ),
            idGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("session_item_${nextId++}") },
        )

        assertEquals(2, merged.size)
        assertEquals(4, merged.first().quantity.value)
        assertIs<SessionBagAssignment.Unresolved>(merged.first().bagAssignment)
        assertIs<SessionBagAssignment.NoBag>(merged.last().bagAssignment)
    }

    @Test
    fun reapplyPreservesMatchingProgressBagAndSupportsWholeUndoSnapshot() {
        val current = activeSession(
            items = listOf(item("old", "item_laptop", SessionItemState.Packed, SessionBagAssignment.Assigned(TemplateBagLabel.user("Cabin")))),
        )
        val fresh = listOf(
            item("fresh", "item_laptop", quantity = 8),
            item("new", "item_keys"),
        )

        val mutation = reapplySessionTemplates(
            current,
            listOf(SessionTemplateSnapshot(KitTemplateId("template_pool"), SnapshotText(ResourceKey("starter_template_pool"), null), 0)),
            fresh,
        )

        assertEquals(current, mutation.undo.before)
        assertEquals(SessionPackingItemId("old"), mutation.session.items.first().id)
        assertEquals(SessionItemState.Packed, mutation.session.items.first().state)
        assertEquals(current.items.first().bagAssignment, mutation.session.items.first().bagAssignment)
        assertEquals(8, mutation.session.items.first().quantity.value)
        assertEquals(SessionItemState.NotPacked, mutation.session.items.last().state)
    }

    @Test
    fun completionRejectsEmptyConflictsAndWrongSkippedConfirmation() {
        assertFailsWith<EmptyPackingSessionException> { requireNonEmptySession(emptyList()) }
        val conflict = activeSession(items = listOf(item("one", "item_laptop", bag = SessionBagAssignment.Unresolved(
            listOf(TemplateBagLabel.user("A"), TemplateBagLabel.user("B")),
        ))))
        assertFailsWith<UnresolvedBagConflictException> { completeAllPacked(conflict, 2) }
        val partial = activeSession(items = listOf(item("one", "item_laptop"), item("two", "item_keys", SessionItemState.Packed)))
        assertFailsWith<SkippedCountMismatchException> { completeWithSkipped(partial, 2, 2) }
    }

    @Test
    fun fullAndSkippedCompletionProduceExplicitDurableOutcomes() {
        val packed = activeSession(items = listOf(item("one", "item_laptop", SessionItemState.Packed)))
        assertEquals(SessionCompletionMode.AllPacked, completeAllPacked(packed, 2).completionMode)

        val partial = activeSession(items = listOf(item("one", "item_laptop"), item("two", "item_keys", SessionItemState.Packed)))
        val completed = completeWithSkipped(partial, 1, 2)
        assertEquals(SessionCompletionMode.WithSkipped, completed.completionMode)
        assertTrue(completed.items.first().skipped)
    }

    @Test
    fun localDateValidationUsesTheSuppliedLocalDateDirectly() {
        val today = LocalDate(2026, 9, 8)
        assertEquals(today, validateSessionDate(today, today))
        assertFailsWith<PastPackingSessionDateException> { validateSessionDate(LocalDate(2026, 9, 7), today) }
    }

    @Test
    fun historyPartitionsChronologicallyAndFiltersByExactLocalDate() {
        val earlyPlanned = activeSession(emptyList()).copy(id = PackingSessionId("planned_early"), localDate = LocalDate(2026, 9, 9))
        val latePlanned = activeSession(emptyList()).copy(id = PackingSessionId("planned_late"), localDate = LocalDate(2026, 9, 12))
        val olderCompleted = completedSession("completed_older", LocalDate(2026, 9, 7), 20)
        val newerCompleted = completedSession("completed_newer", LocalDate(2026, 9, 10), 30)

        val all = sessionHistory(listOf(latePlanned, olderCompleted, earlyPlanned, newerCompleted))
        assertEquals(listOf(earlyPlanned.id, latePlanned.id), all.planned.map { it.id })
        assertEquals(listOf(newerCompleted.id, olderCompleted.id), all.completed.map { it.id })

        val filtered = sessionHistory(listOf(latePlanned, olderCompleted, earlyPlanned, newerCompleted), LocalDate(2026, 9, 10))
        assertTrue(filtered.planned.isEmpty())
        assertEquals(listOf(newerCompleted.id), filtered.completed.map { it.id })
    }

    @Test
    fun reopenPreservesTheSameDatedSnapshotAndEveryRecordedItemState() {
        val completed = completedSession("completed", LocalDate(2026, 9, 8), 20)
        val reopened = reopenCompletedSession(completed)

        assertEquals(completed.id, reopened.id)
        assertEquals(completed.localDate, reopened.localDate)
        assertEquals(completed.selectedTemplates, reopened.selectedTemplates)
        assertEquals(completed.items, reopened.items)
        assertEquals(PackingSessionStatus.Active, reopened.status)
        assertNull(reopened.completedAtMillis)
        assertNull(reopened.completionMode)
    }

    @Test
    fun repeatSelectionRetainsOnlyTheExactStableTemplateCombination() {
        val selected = listOf(
            SessionTemplateSnapshot(KitTemplateId("template_pool"), SnapshotText(ResourceKey("starter_template_pool"), null), 1),
            SessionTemplateSnapshot(KitTemplateId("template_office"), SnapshotText(ResourceKey("starter_template_office"), null), 0),
        )
        val completed = completedSession("completed", LocalDate(2026, 9, 8), 20).copy(selectedTemplates = selected)

        val repeat = repeatSessionSelection(completed)

        assertEquals(completed.id, repeat.sourceSessionId)
        assertEquals(listOf(KitTemplateId("template_office"), KitTemplateId("template_pool")), repeat.selectedTemplateIds)
        assertFailsWith<CompletedPackingSessionRequiredException> { repeatSessionSelection(activeSession(emptyList())) }
    }

    private fun input(id: String, quantity: Int, bag: TemplateBagLabel) = SessionMergeInput(
        item = PackingItem(PackingItemId(id), ResourceKey("starter_$id"), null, null, 0),
        quantity = PositionQuantity(quantity),
        bagLabel = bag,
        sourceHintOverride = null,
        sortOrder = 0,
    )

    private fun activeSession(items: List<SessionPackingItem>) = PackingSession(
        id = PackingSessionId("session"),
        localDate = LocalDate(2026, 9, 8),
        status = PackingSessionStatus.Active,
        completionMode = null,
        createdAtMillis = 1,
        completedAtMillis = null,
        revision = 0,
        selectedTemplates = emptyList(),
        items = items,
    )

    private fun completedSession(id: String, date: LocalDate, completedAt: Long): PackingSession = PackingSession(
        id = PackingSessionId(id),
        localDate = date,
        status = PackingSessionStatus.Completed,
        completionMode = SessionCompletionMode.WithSkipped,
        createdAtMillis = 1,
        completedAtMillis = completedAt,
        revision = 1,
        selectedTemplates = emptyList(),
        items = listOf(
            item("${id}_packed", "item_laptop", SessionItemState.Packed),
            item("${id}_skipped", "item_keys").copy(skipped = true, sortOrder = 1),
        ),
    )

    private fun item(
        id: String,
        catalogId: String,
        state: SessionItemState = SessionItemState.NotPacked,
        bag: SessionBagAssignment = SessionBagAssignment.NoBag,
        quantity: Int = 1,
    ) = SessionPackingItem(
        id = SessionPackingItemId(id),
        catalogItemId = PackingItemId(catalogId),
        name = SnapshotText(ResourceKey("starter_$catalogId"), null),
        quantity = PositionQuantity(quantity),
        bagAssignment = bag,
        sourceHint = null,
        state = state,
        skipped = false,
        sortOrder = 0,
    )
}
