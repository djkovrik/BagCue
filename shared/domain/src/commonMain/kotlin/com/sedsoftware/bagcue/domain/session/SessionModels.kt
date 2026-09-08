package com.sedsoftware.bagcue.domain.session

import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class PackingSessionId(val value: String) {
    init { require(value.isNotBlank()) { "Session ID must not be blank" } }
}

@JvmInline
value class SessionPackingItemId(val value: String) {
    init { require(value.isNotBlank()) { "Session item ID must not be blank" } }
}

fun interface PackingSessionIdGenerator {
    fun nextId(): PackingSessionId
}

fun interface SessionPackingItemIdGenerator {
    fun nextId(): SessionPackingItemId
}

enum class PackingSessionStatus { Active, Completed }

enum class SessionItemState { NotPacked, Packed }

enum class SessionCompletionMode { AllPacked, WithSkipped }

data class SnapshotText(
    val seedNameKey: ResourceKey?,
    val userText: String?,
) {
    init {
        require(seedNameKey != null || !userText.isNullOrBlank()) { "Snapshot text must retain an identity" }
        require(userText == null || userText.isNotBlank()) { "Snapshot user text must not be blank" }
    }
}

data class SessionTemplateSnapshot(
    val id: KitTemplateId,
    val name: SnapshotText,
    val sortOrder: Long,
)

sealed interface SessionBagAssignment {
    data object NoBag : SessionBagAssignment
    data class Assigned(val label: TemplateBagLabel) : SessionBagAssignment {
        init { require(label != TemplateBagLabel.None) }
    }
    data class Unresolved(val candidates: List<TemplateBagLabel>) : SessionBagAssignment {
        init { require(candidates.size >= 2) }
    }
}

data class SessionPackingItem(
    val id: SessionPackingItemId,
    val catalogItemId: PackingItemId?,
    val name: SnapshotText,
    val quantity: PositionQuantity,
    val bagAssignment: SessionBagAssignment,
    val sourceHint: String?,
    val state: SessionItemState,
    val skipped: Boolean,
    val sortOrder: Long,
) {
    init {
        require(sourceHint == null || sourceHint.isNotBlank()) { "An empty source hint must be null" }
        require(!skipped || state == SessionItemState.NotPacked) { "Packed items cannot be skipped" }
        require(sortOrder >= 0)
    }
}

data class PackingSession(
    val id: PackingSessionId,
    val localDate: LocalDate,
    val status: PackingSessionStatus,
    val completionMode: SessionCompletionMode?,
    val createdAtMillis: Long,
    val completedAtMillis: Long?,
    val revision: Long,
    val selectedTemplates: List<SessionTemplateSnapshot>,
    val items: List<SessionPackingItem>,
) {
    init {
        require(revision >= 0)
        require(selectedTemplates.map { it.id }.distinct().size == selectedTemplates.size)
        require(items.map { it.id }.distinct().size == items.size)
        require(items.mapNotNull { it.catalogItemId }.distinct().size == items.mapNotNull { it.catalogItemId }.size)
        require(status != PackingSessionStatus.Active || (completionMode == null && completedAtMillis == null))
        require(status != PackingSessionStatus.Completed || (completionMode != null && completedAtMillis != null))
    }
}

data class SessionUndoSnapshot(
    val before: PackingSession,
    val expectedCurrentRevision: Long,
)

data class SessionMutation(
    val session: PackingSession,
    val undo: SessionUndoSnapshot,
)

data class SessionHistory(
    val planned: List<PackingSession>,
    val completed: List<PackingSession>,
)

data class RepeatSessionSelection(
    val sourceSessionId: PackingSessionId,
    val selectedTemplateIds: List<KitTemplateId>,
) {
    init {
        require(selectedTemplateIds.distinct().size == selectedTemplateIds.size)
    }
}

data class DeletedSessionUndo(
    val session: PackingSession,
)

data class SessionDeletion(
    val deleted: PackingSession,
    val undo: DeletedSessionUndo,
)

sealed interface CreateSessionResult {
    data class Created(val session: PackingSession) : CreateSessionResult
    data class DateOccupied(val existing: PackingSession) : CreateSessionResult
}

data class OneOffSessionItemCommand(
    val sessionId: PackingSessionId,
    val catalogItemId: PackingItemId,
    val sessionItemId: SessionPackingItemId,
    val userName: String,
    val usualLocation: String?,
    val quantity: PositionQuantity = PositionQuantity(1),
    val bagAssignment: SessionBagAssignment = SessionBagAssignment.NoBag,
)

data class SaveSessionItemToTemplatesCommand(
    val sessionId: PackingSessionId,
    val sessionItemId: SessionPackingItemId,
    val templatePositionIds: Map<KitTemplateId, com.sedsoftware.bagcue.domain.template.TemplatePositionId>,
)

class PackingSessionNotFoundException(id: PackingSessionId) :
    NoSuchElementException("Session ${id.value} does not exist")

class CompletedPackingSessionRequiredException(id: PackingSessionId) :
    IllegalStateException("Session ${id.value} must be completed")

class SessionItemNotFoundException(id: SessionPackingItemId) :
    NoSuchElementException("Session item ${id.value} does not exist")

class EmptyPackingSessionException : IllegalArgumentException("Packing session must contain at least one item")

class PastPackingSessionDateException : IllegalArgumentException("Packing session date must be today or later")

class SessionRevisionConflictException : IllegalStateException("Packing session changed before undo could be applied")

class UnresolvedBagConflictException : IllegalStateException("Every bag conflict must be resolved before completion")

class IncompletePackingSessionException : IllegalStateException("Not every item is packed")

class SkippedCountMismatchException : IllegalArgumentException("Confirmed skipped count does not match")
