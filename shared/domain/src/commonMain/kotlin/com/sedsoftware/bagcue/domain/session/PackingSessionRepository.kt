package com.sedsoftware.bagcue.domain.session

import com.sedsoftware.bagcue.domain.template.KitTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface PackingSessionRepository {
    fun observeSession(id: PackingSessionId): Flow<PackingSession?>

    suspend fun readSession(id: PackingSessionId): Result<PackingSession?>

    suspend fun findSessionByDate(date: LocalDate): Result<PackingSession?>

    suspend fun createSession(session: PackingSession): Result<CreateSessionResult>

    suspend fun replaceActiveSnapshot(session: PackingSession): Result<PackingSession>

    suspend fun restoreUndo(snapshot: SessionUndoSnapshot): Result<PackingSession>

    suspend fun addOneOffItem(command: OneOffSessionItemCommand): Result<PackingSession>

    suspend fun removeItem(sessionId: PackingSessionId, itemId: SessionPackingItemId): Result<SessionMutation>

    suspend fun updateItem(sessionId: PackingSessionId, item: SessionPackingItem): Result<PackingSession>

    suspend fun completeSession(session: PackingSession): Result<PackingSession>

    suspend fun saveItemToTemplates(command: SaveSessionItemToTemplatesCommand): Result<List<KitTemplate>>
}
