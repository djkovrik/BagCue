package com.sedsoftware.bagcue.domain.apa

import kotlinx.coroutines.flow.Flow

const val PRIVACY_REGION_ENDPOINT = "https://168.222.252.178/v1/privacy/region"
const val PRIVACY_REGION_SCHEMA_VERSION = 1
const val PRIVACY_REGION_MAX_AGE_MILLIS = 72L * 60L * 60L * 1000L

enum class AdvertisingConsentChoice { Allowed, Declined }

data class PrivacyRegionResponse(
    val schemaVersion: Int,
    val consentRequired: Boolean,
    val policyVersion: String,
    val expiresAtEpochMillis: Long,
)

data class AdvertisingPrivacyState(
    val response: PrivacyRegionResponse,
    val choice: AdvertisingConsentChoice?,
    val choicePolicyVersion: String?,
)

sealed interface AdvertisingPrivacyResolution {
    data object Unresolved : AdvertisingPrivacyResolution
    data class ConsentRequired(val policyVersion: String) : AdvertisingPrivacyResolution
    data class Eligible(val userConsent: Boolean) : AdvertisingPrivacyResolution
    data object Declined : AdvertisingPrivacyResolution
}

interface PrivacyRegionApi {
    suspend fun fetch(): Result<PrivacyRegionResponse>
}

interface AdvertisingPrivacyRepository {
    fun observe(): Flow<AdvertisingPrivacyState?>
    suspend fun readFresh(nowEpochMillis: Long): Result<AdvertisingPrivacyState?>
    suspend fun storeResponse(response: PrivacyRegionResponse, nowEpochMillis: Long): Result<AdvertisingPrivacyState>
    suspend fun storeChoice(choice: AdvertisingConsentChoice, nowEpochMillis: Long): Result<AdvertisingPrivacyState>
    suspend fun clear(): Result<Unit>
}

fun resolveAdvertisingPrivacy(
    state: AdvertisingPrivacyState?,
    nowEpochMillis: Long,
): AdvertisingPrivacyResolution = when {
    state == null || state.response.schemaVersion != PRIVACY_REGION_SCHEMA_VERSION -> AdvertisingPrivacyResolution.Unresolved
    state.response.expiresAtEpochMillis <= nowEpochMillis -> AdvertisingPrivacyResolution.Unresolved
    !state.response.consentRequired -> AdvertisingPrivacyResolution.Eligible(userConsent = true)
    state.choicePolicyVersion != state.response.policyVersion ->
        AdvertisingPrivacyResolution.ConsentRequired(state.response.policyVersion)
    state.choice == AdvertisingConsentChoice.Allowed -> AdvertisingPrivacyResolution.Eligible(userConsent = true)
    state.choice == AdvertisingConsentChoice.Declined -> AdvertisingPrivacyResolution.Declined
    else -> AdvertisingPrivacyResolution.ConsentRequired(state.response.policyVersion)
}
