package com.sedsoftware.bagcue.session.integration

import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.session.store.SessionStore
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionModelMapperTest {
    @Test
    fun createScreenKeepsTodayIndependentFromSelectedFutureDate() {
        val today = LocalDate(2026, 9, 8)
        val selectedDate = LocalDate(2026, 9, 15)

        val screen = assertIs<SessionComponent.Screen.Create>(
            SessionStore.State(
                route = SessionStore.Route.Create,
                today = today,
                createDraft = SessionStore.CreateDraft(selectedDate),
                isLoading = false,
            ).toComponentModel().screen,
        )

        assertEquals(today, screen.today)
        assertEquals(LocalDate(2026, 9, 9), screen.tomorrow)
        assertEquals(selectedDate, screen.date)
    }

    @Test
    fun activeChecklistGroupsConflictsFirstAndHidesPackedMoveHint() {
        val session = session(listOf(
            item("packed", SessionBagAssignment.NoBag, SessionItemState.Packed, "Desk"),
            item("conflict", SessionBagAssignment.Unresolved(listOf(TemplateBagLabel.user("A"), TemplateBagLabel.user("B")))),
        ))

        val screen = assertIs<SessionComponent.Screen.Active>(
            SessionStore.State(route = SessionStore.Route.Active, today = session.localDate, session = session, isLoading = false)
                .toComponentModel().screen,
        )

        assertIs<SessionComponent.Bag.Unresolved>(screen.groups.first().bag)
        assertNull(screen.groups.last().items.single().sourceHint)
        assertFalse(screen.canCompleteAll)
        assertFalse(screen.canCompleteWithSkipped)
    }

    @Test
    fun completedSkippedSnapshotMapsToHonestResult() {
        val completed = session(listOf(
            item("packed", state = SessionItemState.Packed),
            item("skipped", skipped = true),
        )).copy(
            status = PackingSessionStatus.Completed,
            completionMode = SessionCompletionMode.WithSkipped,
            completedAtMillis = 2,
        )

        val result = assertIs<SessionComponent.Screen.Result>(
            SessionStore.State(route = SessionStore.Route.Result, today = completed.localDate, session = completed, isLoading = false)
                .toComponentModel().screen,
        )

        assertFalse(result.allPacked)
        assertEquals(listOf(SessionComponent.UserText.Authored("skipped")), result.skippedItems.map { it.name })
    }

    @Test
    fun onlyCompletedResultMapsReadyInlineAdvertisingSlot() {
        val completed = session(emptyList()).copy(
            status = PackingSessionStatus.Completed,
            completionMode = SessionCompletionMode.AllPacked,
            completedAtMillis = 2,
        )
        val result = assertIs<SessionComponent.Screen.Result>(
            SessionStore.State(
                route = SessionStore.Route.Result,
                today = completed.localDate,
                session = completed,
                resultAdvertisingStatus = SessionStore.ResultAdvertisingStatus.Ready,
                privacyPolicyUrl = "https://example.invalid/policy",
                isLoading = false,
            ).toComponentModel().screen,
        )
        assertEquals(SessionComponent.ResultAdvertisingStatus.Ready, result.advertising.status)
        assertEquals("https://example.invalid/policy", result.advertising.privacyPolicyUrl)
    }

    private fun session(items: List<SessionPackingItem>) = PackingSession(
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

    private fun item(
        id: String,
        bag: SessionBagAssignment = SessionBagAssignment.NoBag,
        state: SessionItemState = SessionItemState.NotPacked,
        source: String? = null,
        skipped: Boolean = false,
    ) = SessionPackingItem(
        id = SessionPackingItemId(id),
        catalogItemId = PackingItemId("catalog-$id"),
        name = SnapshotText(null, id),
        quantity = PositionQuantity(1),
        bagAssignment = bag,
        sourceHint = source,
        state = state,
        skipped = skipped,
        sortOrder = 0,
    )
}
