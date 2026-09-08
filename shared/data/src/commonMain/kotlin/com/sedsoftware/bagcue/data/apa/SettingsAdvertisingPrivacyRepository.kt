package com.sedsoftware.bagcue.data.apa

import com.russhwolf.settings.Settings
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyState
import com.sedsoftware.bagcue.domain.apa.PRIVACY_REGION_MAX_AGE_MILLIS
import com.sedsoftware.bagcue.domain.apa.PRIVACY_REGION_SCHEMA_VERSION
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class SettingsAdvertisingPrivacyRepository(
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : AdvertisingPrivacyRepository {
    private val state = MutableStateFlow(settings.getStringOrNull(KEY)?.let(::decode))

    override fun observe(): Flow<AdvertisingPrivacyState?> = state

    override suspend fun readFresh(nowEpochMillis: Long): Result<AdvertisingPrivacyState?> = capture {
        val stored = settings.getStringOrNull(KEY)?.let(::decode)
        val fresh = stored?.takeIf { it.response.expiresAtEpochMillis > nowEpochMillis }
        if (fresh == null) settings.remove(KEY)
        fresh.also { state.value = it }
    }

    override suspend fun storeResponse(
        response: PrivacyRegionResponse,
        nowEpochMillis: Long,
    ): Result<AdvertisingPrivacyState> = capture {
        require(response.schemaVersion == PRIVACY_REGION_SCHEMA_VERSION)
        require(response.policyVersion.isNotBlank() && '|' !in response.policyVersion)
        require(response.expiresAtEpochMillis > nowEpochMillis)
        val clamped = response.copy(
            expiresAtEpochMillis = minOf(response.expiresAtEpochMillis, nowEpochMillis + PRIVACY_REGION_MAX_AGE_MILLIS),
        )
        val old = settings.getStringOrNull(KEY)?.let(::decode)
        val keepChoice = clamped.consentRequired && old?.response?.policyVersion == clamped.policyVersion
        save(
            AdvertisingPrivacyState(
                response = clamped,
                choice = old?.choice.takeIf { keepChoice },
                choicePolicyVersion = old?.choicePolicyVersion.takeIf { keepChoice },
            ),
        )
    }

    override suspend fun storeChoice(
        choice: AdvertisingConsentChoice,
        nowEpochMillis: Long,
    ): Result<AdvertisingPrivacyState> = capture {
        val current = settings.getStringOrNull(KEY)?.let(::decode)
            ?.takeIf { it.response.expiresAtEpochMillis > nowEpochMillis }
            ?: error("A fresh privacy-region response is required")
        require(current.response.consentRequired) { "Consent choice is not applicable" }
        save(current.copy(choice = choice, choicePolicyVersion = current.response.policyVersion))
    }

    override suspend fun clear(): Result<Unit> = capture {
        settings.remove(KEY)
        state.value = null
    }

    private fun save(value: AdvertisingPrivacyState): AdvertisingPrivacyState {
        settings.putString(KEY, encode(value))
        state.value = value
        return value
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

    private companion object {
        const val KEY = "advertising.privacy.state"
        const val VERSION = "v1"

        fun encode(state: AdvertisingPrivacyState): String = listOf(
            VERSION,
            state.response.schemaVersion.toString(),
            if (state.response.consentRequired) "1" else "0",
            state.response.policyVersion,
            state.response.expiresAtEpochMillis.toString(),
            state.choice?.name ?: "-",
            state.choicePolicyVersion ?: "-",
        ).joinToString("|")

        fun decode(value: String): AdvertisingPrivacyState? = runCatching {
            val parts = value.split('|')
            require(parts.size == ENCODED_PART_COUNT && parts[0] == VERSION)
            require(parts[2] == "0" || parts[2] == "1")
            val response = PrivacyRegionResponse(parts[1].toInt(), parts[2] == "1", parts[3], parts[4].toLong())
            require(response.schemaVersion == PRIVACY_REGION_SCHEMA_VERSION && response.policyVersion.isNotBlank())
            val choice = parts[5].takeUnless { it == "-" }?.let(AdvertisingConsentChoice::valueOf)
            val choicePolicy = parts[6].takeUnless { it == "-" }
            require((choice == null) == (choicePolicy == null))
            AdvertisingPrivacyState(response, choice, choicePolicy)
        }.getOrNull()

        const val ENCODED_PART_COUNT = 7
    }
}
