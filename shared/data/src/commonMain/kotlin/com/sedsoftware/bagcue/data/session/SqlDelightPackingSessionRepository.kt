package com.sedsoftware.bagcue.data.session

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sedsoftware.bagcue.data.db.BagCueDatabase
import com.sedsoftware.bagcue.data.db.Kit_template
import com.sedsoftware.bagcue.data.db.Packing_session
import com.sedsoftware.bagcue.data.db.Session_packing_item
import com.sedsoftware.bagcue.data.db.Session_template_snapshot
import com.sedsoftware.bagcue.data.db.Template_position
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.catalog.optionalUserText
import com.sedsoftware.bagcue.domain.catalog.validatedCatalogName
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.DeletedSessionUndo
import com.sedsoftware.bagcue.domain.session.EmptyPackingSessionException
import com.sedsoftware.bagcue.domain.session.OneOffSessionItemCommand
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionNotFoundException
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SaveSessionItemToTemplatesCommand
import com.sedsoftware.bagcue.domain.session.SessionDeletion
import com.sedsoftware.bagcue.domain.session.SessionHistory
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.domain.session.SessionItemNotFoundException
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionMutation
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionRevisionConflictException
import com.sedsoftware.bagcue.domain.session.SessionTemplateSnapshot
import com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.session.RepeatSessionSelection
import com.sedsoftware.bagcue.domain.session.removeSessionItem
import com.sedsoftware.bagcue.domain.session.reopenCompletedSession
import com.sedsoftware.bagcue.domain.session.repeatSessionSelection
import com.sedsoftware.bagcue.domain.session.sessionHistory
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateNotFoundException
import com.sedsoftware.bagcue.domain.template.PositionQuantity
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePosition
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class SqlDelightPackingSessionRepository(
    database: BagCueDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val currentTimeMillis: () -> Long,
) : SessionHistoryRepository {
    private val catalogQueries = database.catalogQueries
    private val sessionQueries = database.sessionQueries
    private val templateQueries = database.templateQueries

    override fun observeSession(id: PackingSessionId): Flow<PackingSession?> =
        sessionQueries.selectSessionByStableId(id.value).asFlow().mapToOneOrNull(ioDispatcher).map { row ->
            row?.let(::toDomain)
        }

    override fun observeHistory(localDate: LocalDate?): Flow<SessionHistory> {
        val query = localDate?.let { sessionQueries.selectSessionsForHistoryByLocalDate(it.toString()) }
            ?: sessionQueries.selectAllSessionsForHistory()
        return query.asFlow().mapToList(ioDispatcher).map { rows ->
            sessionHistory(rows.map(::toDomain), localDate)
        }
    }

    override suspend fun readSession(id: PackingSessionId): Result<PackingSession?> = onDatabase {
        sessionQueries.selectSessionByStableId(id.value).executeAsOneOrNull()?.let(::toDomain)
    }

    override suspend fun findSessionByDate(date: LocalDate): Result<PackingSession?> = onDatabase {
        sessionQueries.selectSessionByLocalDate(date.toString()).executeAsOneOrNull()?.let(::toDomain)
    }

    override suspend fun readHistory(localDate: LocalDate?): Result<SessionHistory> = onDatabase {
        val rows = localDate?.let { sessionQueries.selectSessionsForHistoryByLocalDate(it.toString()).executeAsList() }
            ?: sessionQueries.selectAllSessionsForHistory().executeAsList()
        sessionHistory(rows.map(::toDomain), localDate)
    }

    override suspend fun createSession(session: PackingSession): Result<CreateSessionResult> = onDatabase {
        require(session.status == PackingSessionStatus.Active)
        if (session.items.isEmpty()) throw EmptyPackingSessionException()
        sessionQueries.transactionWithResult {
            val occupied = sessionQueries.selectSessionByLocalDate(session.localDate.toString()).executeAsOneOrNull()
            if (occupied != null) {
                CreateSessionResult.DateOccupied(toDomain(occupied))
            } else {
                insertAggregate(session)
                CreateSessionResult.Created(toDomain(sessionQueries.selectSessionByStableId(session.id.value).executeAsOne()))
            }
        }
    }

    override suspend fun replaceActiveSnapshot(session: PackingSession): Result<PackingSession> = onDatabase {
        require(session.status == PackingSessionStatus.Active)
        if (session.items.isEmpty()) throw EmptyPackingSessionException()
        replaceAggregate(session, expectedRevision = session.revision - 1)
    }

    override suspend fun restoreUndo(snapshot: SessionUndoSnapshot): Result<PackingSession> = onDatabase {
        val current = requireSession(snapshot.before.id)
        if (current.revision != snapshot.expectedCurrentRevision) throw SessionRevisionConflictException()
        replaceAggregate(snapshot.before.copy(revision = current.revision + 1), expectedRevision = current.revision)
    }

    override suspend fun addOneOffItem(command: OneOffSessionItemCommand): Result<PackingSession> = onDatabase {
        val current = requireSession(command.sessionId)
        require(current.status == PackingSessionStatus.Active)
        val name = validatedCatalogName(command.userName)
        val location = optionalUserText(command.usualLocation)
        sessionQueries.transactionWithResult {
            val now = currentTimeMillis()
            catalogQueries.insertItem(
                id = command.catalogItemId.value,
                seed_resource_key = null,
                user_name_override = name,
                usual_location = location,
                created_order = now,
                updated_at = now,
            )
            val updated = current.copy(
                revision = current.revision + 1,
                items = current.items + SessionPackingItem(
                    id = command.sessionItemId,
                    catalogItemId = command.catalogItemId,
                    name = SnapshotText(null, name),
                    quantity = command.quantity,
                    bagAssignment = command.bagAssignment,
                    sourceHint = location,
                    state = SessionItemState.NotPacked,
                    skipped = false,
                    sortOrder = (current.items.maxOfOrNull { it.sortOrder } ?: -1L) + 1L,
                ),
            )
            replaceAggregateInsideTransaction(updated, current.revision)
            updated
        }
    }

    override suspend fun removeItem(
        sessionId: PackingSessionId,
        itemId: SessionPackingItemId,
    ): Result<SessionMutation> = onDatabase {
        val mutation = removeSessionItem(requireSession(sessionId), itemId)
        replaceAggregate(mutation.session, mutation.undo.before.revision)
        mutation
    }

    override suspend fun updateItem(
        sessionId: PackingSessionId,
        item: SessionPackingItem,
    ): Result<PackingSession> = onDatabase {
        val current = requireSession(sessionId)
        require(current.status == PackingSessionStatus.Active)
        if (current.items.none { it.id == item.id }) throw SessionItemNotFoundException(item.id)
        val updated = current.copy(
            revision = current.revision + 1,
            items = current.items.map { existing -> if (existing.id == item.id) item else existing },
        )
        replaceAggregate(updated, current.revision)
    }

    override suspend fun completeSession(session: PackingSession): Result<PackingSession> = onDatabase {
        require(session.status == PackingSessionStatus.Completed)
        if (session.items.isEmpty()) throw EmptyPackingSessionException()
        replaceAggregate(session, expectedRevision = session.revision - 1)
    }

    override suspend fun reopenSession(id: PackingSessionId): Result<PackingSession> = onDatabase {
        val current = requireSession(id)
        replaceAggregate(reopenCompletedSession(current), expectedRevision = current.revision)
    }

    override suspend fun prepareRepeat(id: PackingSessionId): Result<RepeatSessionSelection> = onDatabase {
        repeatSessionSelection(requireSession(id))
    }

    override suspend fun deleteSession(id: PackingSessionId): Result<SessionDeletion> = onDatabase {
        sessionQueries.transactionWithResult {
            val deleted = requireSession(id)
            sessionQueries.deletePackingSessionByStableId(id.value)
            SessionDeletion(deleted, DeletedSessionUndo(deleted))
        }
    }

    override suspend fun restoreDeletedSession(undo: DeletedSessionUndo): Result<CreateSessionResult> = onDatabase {
        sessionQueries.transactionWithResult {
            val occupied = sessionQueries.selectSessionByLocalDate(undo.session.localDate.toString()).executeAsOneOrNull()
            if (occupied != null) {
                CreateSessionResult.DateOccupied(toDomain(occupied))
            } else {
                insertAggregate(undo.session)
                CreateSessionResult.Created(
                    toDomain(sessionQueries.selectSessionByStableId(undo.session.id.value).executeAsOne()),
                )
            }
        }
    }

    override suspend fun saveItemToTemplates(
        command: SaveSessionItemToTemplatesCommand,
    ): Result<List<KitTemplate>> = onDatabase {
        require(command.templatePositionIds.isNotEmpty())
        val session = requireSession(command.sessionId)
        val item = session.items.firstOrNull { it.id == command.sessionItemId }
            ?: throw SessionItemNotFoundException(command.sessionItemId)
        val catalogItemId = item.catalogItemId ?: error("A detached history item cannot be saved to templates")
        val bag = item.bagAssignment
        require(bag !is SessionBagAssignment.Unresolved) { "Resolve the bag before saving to templates" }
        sessionQueries.transactionWithResult {
            command.templatePositionIds.forEach { (templateId, positionId) ->
                val template = templateQueries.selectTemplateById(templateId.value).executeAsOneOrNull()
                    ?: throw KitTemplateNotFoundException(templateId)
                val assigned = bag as? SessionBagAssignment.Assigned
                val existing = sessionQueries.selectTemplatePositionForItem(templateId.value, catalogItemId.value)
                    .executeAsOneOrNull()
                if (existing == null) {
                    templateQueries.insertKitTemplatePosition(
                        id = positionId.value,
                        template_id = templateId.value,
                        item_id = catalogItemId.value,
                        quantity = item.quantity.value.toLong(),
                        seed_bag_resource_key = assigned?.label?.seedNameKey?.value,
                        user_bag_label = assigned?.label?.userText,
                        source_hint_override = item.sourceHint,
                        sort_order = templateQueries.selectPositionsByTemplate(templateId.value).executeAsList().size.toLong(),
                    )
                } else {
                    sessionQueries.updateTemplatePositionFromSession(
                        quantity = item.quantity.value.toLong(),
                        seed_bag_resource_key = assigned?.label?.seedNameKey?.value,
                        user_bag_label = assigned?.label?.userText,
                        source_hint_override = item.sourceHint,
                        template_id = templateId.value,
                        item_id = catalogItemId.value,
                    )
                }
                if (template.seed_resource_key != null) {
                    templateQueries.upsertTemplateSeedOverride(templateId.value, "modified", currentTimeMillis())
                }
            }
            command.templatePositionIds.keys.map { templateId ->
                toDomain(requireNotNull(templateQueries.selectTemplateById(templateId.value).executeAsOneOrNull()))
            }
        }
    }

    private fun replaceAggregate(session: PackingSession, expectedRevision: Long): PackingSession =
        sessionQueries.transactionWithResult {
            replaceAggregateInsideTransaction(session, expectedRevision)
            toDomain(sessionQueries.selectSessionByStableId(session.id.value).executeAsOne())
        }

    private fun replaceAggregateInsideTransaction(session: PackingSession, expectedRevision: Long) {
        val stored = sessionQueries.selectSessionByStableId(session.id.value).executeAsOneOrNull()
            ?: throw PackingSessionNotFoundException(session.id)
        if (stored.revision != expectedRevision) throw SessionRevisionConflictException()
        sessionQueries.updatePackingSession(
            local_date = session.localDate.toString(),
            status = session.status.toStorage(),
            completion_mode = session.completionMode?.toStorage(),
            completed_at = session.completedAtMillis,
            revision = session.revision,
            id = session.id.value,
        )
        sessionQueries.deleteTemplateSnapshotsBySession(session.id.value)
        sessionQueries.deleteSessionItemsBySession(session.id.value)
        insertChildren(session)
    }

    private fun insertAggregate(session: PackingSession) {
        sessionQueries.insertPackingSession(
            id = session.id.value,
            local_date = session.localDate.toString(),
            status = session.status.toStorage(),
            completion_mode = session.completionMode?.toStorage(),
            created_at = session.createdAtMillis,
            completed_at = session.completedAtMillis,
            revision = session.revision,
        )
        insertChildren(session)
    }

    private fun insertChildren(session: PackingSession) {
        session.selectedTemplates.forEach { selected ->
            sessionQueries.insertSessionTemplateSnapshot(
                session_id = session.id.value,
                template_id = selected.id.value,
                display_seed_resource_key = selected.name.seedNameKey?.value,
                display_user_name = selected.name.userText,
                sort_order = selected.sortOrder,
            )
        }
        session.items.forEach { item ->
            val assigned = item.bagAssignment as? SessionBagAssignment.Assigned
            sessionQueries.insertPackingSessionItem(
                id = item.id.value,
                session_id = session.id.value,
                catalog_item_id = item.catalogItemId?.value,
                display_seed_resource_key = item.name.seedNameKey?.value,
                display_user_name = item.name.userText,
                quantity = item.quantity.value.toLong(),
                bag_kind = item.bagAssignment.toStorage(),
                bag_seed_resource_key = assigned?.label?.seedNameKey?.value,
                bag_user_text = assigned?.label?.userText,
                source_hint = item.sourceHint,
                item_state = item.state.toStorage(),
                skipped = if (item.skipped) 1L else 0L,
                sort_order = item.sortOrder,
            )
            (item.bagAssignment as? SessionBagAssignment.Unresolved)?.candidates?.forEachIndexed { index, label ->
                sessionQueries.insertSessionBagCandidate(
                    session_item_id = item.id.value,
                    seed_resource_key = label.seedNameKey?.value,
                    user_text = label.userText,
                    sort_order = index.toLong(),
                )
            }
        }
    }

    private fun requireSession(id: PackingSessionId): PackingSession =
        sessionQueries.selectSessionByStableId(id.value).executeAsOneOrNull()?.let(::toDomain)
            ?: throw PackingSessionNotFoundException(id)

    private fun toDomain(row: Packing_session): PackingSession = PackingSession(
        id = PackingSessionId(row.id),
        localDate = LocalDate.parse(row.local_date),
        status = row.status.toSessionStatus(),
        completionMode = row.completion_mode?.toCompletionMode(),
        createdAtMillis = row.created_at,
        completedAtMillis = row.completed_at,
        revision = row.revision,
        selectedTemplates = sessionQueries.selectTemplateSnapshotsBySession(row.id).executeAsList().map(::toDomain),
        items = sessionQueries.selectSessionItemsBySession(row.id).executeAsList().map(::toDomain),
    )

    private fun toDomain(row: Session_template_snapshot): SessionTemplateSnapshot = SessionTemplateSnapshot(
        id = KitTemplateId(row.template_id),
        name = SnapshotText(row.display_seed_resource_key?.let(::ResourceKey), row.display_user_name),
        sortOrder = row.sort_order,
    )

    private fun toDomain(row: Session_packing_item): SessionPackingItem = SessionPackingItem(
        id = SessionPackingItemId(row.id),
        catalogItemId = row.catalog_item_id?.let(::PackingItemId),
        name = SnapshotText(row.display_seed_resource_key?.let(::ResourceKey), row.display_user_name),
        quantity = PositionQuantity(row.quantity.toInt()),
        bagAssignment = when (row.bag_kind) {
            "none" -> SessionBagAssignment.NoBag
            "assigned" -> SessionBagAssignment.Assigned(
                TemplateBagLabel(row.bag_seed_resource_key?.let(::ResourceKey), row.bag_user_text),
            )
            "unresolved" -> SessionBagAssignment.Unresolved(
                sessionQueries.selectBagCandidatesByItem(row.id).executeAsList().map { candidate ->
                    TemplateBagLabel(candidate.seed_resource_key?.let(::ResourceKey), candidate.user_text)
                },
            )
            else -> error("Unknown bag kind ${row.bag_kind}")
        },
        sourceHint = row.source_hint,
        state = row.item_state.toItemState(),
        skipped = row.skipped != 0L,
        sortOrder = row.sort_order,
    )

    private fun toDomain(row: Kit_template): KitTemplate = KitTemplate(
        id = KitTemplateId(row.id),
        seedNameKey = row.seed_resource_key?.let(::ResourceKey),
        userNameOverride = row.user_name_override,
        sortOrder = row.sort_order,
        positions = templateQueries.selectPositionsByTemplate(row.id).executeAsList().map(::toDomain),
    )

    private fun toDomain(row: Template_position): TemplatePosition = TemplatePosition(
        id = TemplatePositionId(row.id),
        itemId = PackingItemId(row.item_id),
        quantity = PositionQuantity(row.quantity.toInt()),
        bagLabel = TemplateBagLabel(row.seed_bag_resource_key?.let(::ResourceKey), row.user_bag_label),
        sourceHintOverride = row.source_hint_override,
        sortOrder = row.sort_order,
    )

    private suspend fun <T> onDatabase(block: () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
        }
    }
}

private fun PackingSessionStatus.toStorage() = if (this == PackingSessionStatus.Active) "active" else "completed"
private fun String.toSessionStatus() = if (this == "active") PackingSessionStatus.Active else PackingSessionStatus.Completed
private fun SessionCompletionMode.toStorage() = if (this == SessionCompletionMode.AllPacked) "all-packed" else "with-skipped"
private fun String.toCompletionMode() = if (this == "all-packed") SessionCompletionMode.AllPacked else SessionCompletionMode.WithSkipped
private fun SessionItemState.toStorage() = if (this == SessionItemState.NotPacked) "not-packed" else "packed"
private fun String.toItemState() = if (this == "not-packed") SessionItemState.NotPacked else SessionItemState.Packed
private fun SessionBagAssignment.toStorage() = when (this) {
    SessionBagAssignment.NoBag -> "none"
    is SessionBagAssignment.Assigned -> "assigned"
    is SessionBagAssignment.Unresolved -> "unresolved"
}
