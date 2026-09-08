package com.sedsoftware.bagcue.platform.apa

import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsEvent
import kotlinx.coroutines.CancellationException

interface AnalyticsEventSink {
    suspend fun setEnabled(enabled: Boolean)
    suspend fun reset()
    suspend fun appInstanceId(): String?
    suspend fun sendAllowlistedEvent(name: String)
}

class SafeAnalyticsController(
    private val sink: AnalyticsEventSink,
    initiallyEnabled: Boolean = false,
) : AnalyticsController {
    private var enabled = initiallyEnabled

    override suspend fun setCollectionEnabled(enabled: Boolean): Result<Unit> = capture {
        sink.setEnabled(enabled)
        this.enabled = enabled
    }

    override suspend fun resetAnalyticsData(): Result<Unit> = capture {
        enabled = false
        sink.setEnabled(false)
        sink.reset()
    }

    override suspend fun readAppInstanceId(): Result<String?> = capture { sink.appInstanceId() }

    override suspend fun record(event: AnalyticsEvent): Result<Unit> = capture {
        if (enabled) sink.sendAllowlistedEvent(event.name.wireName)
    }

    private suspend inline fun <T> capture(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
    }
}
