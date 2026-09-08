package com.sedsoftware.bagcue.domain.session

import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.normalizeCatalogName
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import kotlinx.datetime.LocalDate

data class SessionMergeInput(
    val item: PackingItem,
    val quantity: PositionQuantity,
    val bagLabel: TemplateBagLabel,
    val sourceHintOverride: String?,
    val sortOrder: Long,
)

fun validateSessionDate(date: LocalDate, today: LocalDate): LocalDate = date.also {
    if (it < today) throw PastPackingSessionDateException()
}

fun sessionHistory(
    sessions: List<PackingSession>,
    localDate: LocalDate? = null,
): SessionHistory {
    val filtered = sessions.filter { localDate == null || it.localDate == localDate }
    return SessionHistory(
        planned = filtered.filter { it.status == PackingSessionStatus.Active }
            .sortedWith(compareBy<PackingSession> { it.localDate }.thenBy { it.createdAtMillis }.thenBy { it.id.value }),
        completed = filtered.filter { it.status == PackingSessionStatus.Completed }
            .sortedWith(
                compareByDescending<PackingSession> { it.localDate }
                    .thenByDescending { it.completedAtMillis }
                    .thenBy { it.id.value },
            ),
    )
}

fun reopenCompletedSession(session: PackingSession): PackingSession {
    if (session.status != PackingSessionStatus.Completed) {
        throw CompletedPackingSessionRequiredException(session.id)
    }
    return session.copy(
        status = PackingSessionStatus.Active,
        completionMode = null,
        completedAtMillis = null,
        revision = session.revision + 1,
    )
}

fun repeatSessionSelection(session: PackingSession): RepeatSessionSelection {
    if (session.status != PackingSessionStatus.Completed) {
        throw CompletedPackingSessionRequiredException(session.id)
    }
    return RepeatSessionSelection(
        sourceSessionId = session.id,
        selectedTemplateIds = session.selectedTemplates.sortedBy { it.sortOrder }.map { it.id },
    )
}

fun requireNonEmptySession(items: List<SessionPackingItem>): List<SessionPackingItem> = items.also {
    if (it.isEmpty()) throw EmptyPackingSessionException()
}

fun mergeSessionItems(
    inputs: List<SessionMergeInput>,
    idGenerator: SessionPackingItemIdGenerator,
): List<SessionPackingItem> = inputs
    .groupBy { it.item.id }
    .values
    .mapIndexed { index, sources ->
        val first = sources.first()
        SessionPackingItem(
            id = idGenerator.nextId(),
            catalogItemId = first.item.id,
            name = SnapshotText(first.item.seedNameKey, first.item.userNameOverride),
            quantity = PositionQuantity(sources.maxOf { it.quantity.value }),
            bagAssignment = mergeBagCandidates(sources.map { it.bagLabel }),
            sourceHint = sources.firstNotNullOfOrNull { it.sourceHintOverride ?: it.item.usualLocation },
            state = SessionItemState.NotPacked,
            skipped = false,
            sortOrder = index.toLong(),
        )
    }

fun mergeBagCandidates(labels: List<TemplateBagLabel>): SessionBagAssignment {
    val distinct = labels
        .filterNot { it == TemplateBagLabel.None }
        .distinctBy(::bagIdentity)
    return when (distinct.size) {
        0 -> SessionBagAssignment.NoBag
        1 -> SessionBagAssignment.Assigned(distinct.single())
        else -> SessionBagAssignment.Unresolved(distinct)
    }
}

fun reapplySessionTemplates(
    current: PackingSession,
    selectedTemplates: List<SessionTemplateSnapshot>,
    recomposedItems: List<SessionPackingItem>,
): SessionMutation {
    check(current.status == PackingSessionStatus.Active)
    requireNonEmptySession(recomposedItems)
    val previousByCatalogId = current.items.associateBy { it.catalogItemId }
    val updated = current.copy(
        revision = current.revision + 1,
        selectedTemplates = selectedTemplates,
        items = recomposedItems.mapIndexed { index, fresh ->
            previousByCatalogId[fresh.catalogItemId]?.let { previous ->
                fresh.copy(
                    id = previous.id,
                    bagAssignment = previous.bagAssignment,
                    state = previous.state,
                    skipped = false,
                    sortOrder = index.toLong(),
                )
            } ?: fresh.copy(sortOrder = index.toLong())
        },
    )
    return SessionMutation(updated, SessionUndoSnapshot(current, updated.revision))
}

fun removeSessionItem(current: PackingSession, itemId: SessionPackingItemId): SessionMutation {
    check(current.status == PackingSessionStatus.Active)
    if (current.items.none { it.id == itemId }) throw SessionItemNotFoundException(itemId)
    val updated = current.copy(
        revision = current.revision + 1,
        items = current.items.filterNot { it.id == itemId }.mapIndexed { index, item -> item.copy(sortOrder = index.toLong()) },
    )
    return SessionMutation(updated, SessionUndoSnapshot(current, updated.revision))
}

fun setSessionItemPacked(
    current: PackingSession,
    itemId: SessionPackingItemId,
    packed: Boolean,
): PackingSession = current.updateItem(itemId) {
    it.copy(state = if (packed) SessionItemState.Packed else SessionItemState.NotPacked, skipped = false)
}

fun editSessionItem(
    current: PackingSession,
    itemId: SessionPackingItemId,
    bagAssignment: SessionBagAssignment,
    sourceHint: String?,
): PackingSession = current.updateItem(itemId) {
    it.copy(bagAssignment = bagAssignment, sourceHint = sourceHint?.takeUnless(String::isBlank))
}

fun completeAllPacked(current: PackingSession, completedAtMillis: Long): PackingSession {
    requireCompletable(current)
    if (current.items.any { it.state != SessionItemState.Packed }) throw IncompletePackingSessionException()
    return current.copy(
        status = PackingSessionStatus.Completed,
        completionMode = SessionCompletionMode.AllPacked,
        completedAtMillis = completedAtMillis,
        revision = current.revision + 1,
    )
}

fun completeWithSkipped(
    current: PackingSession,
    confirmedRemainingCount: Int,
    completedAtMillis: Long,
): PackingSession {
    requireCompletable(current)
    val remaining = current.items.count { it.state == SessionItemState.NotPacked }
    if (remaining == 0 || remaining != confirmedRemainingCount) throw SkippedCountMismatchException()
    return current.copy(
        status = PackingSessionStatus.Completed,
        completionMode = SessionCompletionMode.WithSkipped,
        completedAtMillis = completedAtMillis,
        revision = current.revision + 1,
        items = current.items.map { item -> item.copy(skipped = item.state == SessionItemState.NotPacked) },
    )
}

private fun requireCompletable(session: PackingSession) {
    check(session.status == PackingSessionStatus.Active)
    requireNonEmptySession(session.items)
    if (session.items.any { it.bagAssignment is SessionBagAssignment.Unresolved }) {
        throw UnresolvedBagConflictException()
    }
}

private fun PackingSession.updateItem(
    itemId: SessionPackingItemId,
    update: (SessionPackingItem) -> SessionPackingItem,
): PackingSession {
    if (items.none { it.id == itemId }) throw SessionItemNotFoundException(itemId)
    return copy(
        revision = revision + 1,
        items = items.map { if (it.id == itemId) update(it) else it },
    )
}

private fun bagIdentity(label: TemplateBagLabel): String = label.seedNameKey?.let { "seed:${it.value}" }
    ?: "user:${normalizeCatalogName(requireNotNull(label.userText))}"
