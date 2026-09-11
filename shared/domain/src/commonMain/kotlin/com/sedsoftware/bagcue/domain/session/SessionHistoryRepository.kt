package com.sedsoftware.bagcue.domain.session

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SessionHistoryRepository : PackingSessionRepository {
    fun observeHistory(localDate: LocalDate? = null): Flow<SessionHistory>

    suspend fun readHistory(localDate: LocalDate? = null): Result<SessionHistory>

    suspend fun prepareRepeat(id: PackingSessionId): Result<RepeatSessionSelection>

    suspend fun deleteSession(id: PackingSessionId): Result<SessionDeletion>

    suspend fun restoreDeletedSession(undo: DeletedSessionUndo): Result<CreateSessionResult>
}
