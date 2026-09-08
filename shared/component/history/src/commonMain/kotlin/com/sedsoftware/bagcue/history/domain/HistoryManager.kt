package com.sedsoftware.bagcue.history.domain

import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import com.sedsoftware.bagcue.domain.apa.AnalyticsEventName
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.DeletedSessionUndo
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.RepeatSessionSelection
import com.sedsoftware.bagcue.domain.session.SessionDeletion
import com.sedsoftware.bagcue.domain.session.SessionHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

internal class HistoryManager(
    private val repository: SessionHistoryRepository,
    private val analyticsController: AnalyticsController,
) {
    fun observeHistory(date: LocalDate?): Flow<SessionHistory> = repository.observeHistory(date)

    suspend fun readHistory(date: LocalDate?): Result<SessionHistory> = captureResult {
        repository.readHistory(date).getOrThrow()
    }

    suspend fun reopen(id: PackingSessionId): Result<PackingSession> = captureResult {
        repository.reopenSession(id).getOrThrow().also {
            recordSafely(AnalyticsEventName.PackingSessionReopened)
        }
    }

    suspend fun prepareRepeat(id: PackingSessionId): Result<RepeatSessionSelection> = captureResult {
        repository.prepareRepeat(id).getOrThrow()
    }

    suspend fun delete(id: PackingSessionId): Result<SessionDeletion> = captureResult {
        repository.deleteSession(id).getOrThrow()
    }

    suspend fun undo(value: DeletedSessionUndo): Result<CreateSessionResult> = captureResult {
        repository.restoreDeletedSession(value).getOrThrow()
    }

    private suspend fun recordSafely(name: AnalyticsEventName) {
        analyticsController.record(AnalyticsEvent(name)).exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
        }
    }
}

private suspend inline fun <T> captureResult(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (expectedFailure: Throwable) {
    Result.failure(expectedFailure)
}
