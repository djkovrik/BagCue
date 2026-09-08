package com.sedsoftware.bagcue.data.apa

import com.russhwolf.settings.Settings
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreference
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreferenceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class SettingsAnalyticsPreferenceRepository(
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : AnalyticsPreferenceRepository {
    private val state = MutableStateFlow(AnalyticsPreference(settings.getBoolean(KEY, false)))

    override fun observe(): Flow<AnalyticsPreference> = state

    override suspend fun read(): Result<AnalyticsPreference> = capture {
        AnalyticsPreference(settings.getBoolean(KEY, false)).also { state.value = it }
    }

    override suspend fun update(preference: AnalyticsPreference): Result<AnalyticsPreference> = capture {
        settings.putBoolean(KEY, preference.enabled)
        preference.also { state.value = it }
    }

    private suspend fun <T> capture(block: () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
        }
    }

    private companion object { const val KEY = "analytics.collection.enabled" }
}
