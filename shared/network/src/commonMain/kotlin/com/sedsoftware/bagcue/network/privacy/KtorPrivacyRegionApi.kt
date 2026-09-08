package com.sedsoftware.bagcue.network.privacy

import com.sedsoftware.bagcue.domain.apa.PRIVACY_REGION_ENDPOINT
import com.sedsoftware.bagcue.domain.apa.PRIVACY_REGION_MAX_AGE_MILLIS
import com.sedsoftware.bagcue.domain.apa.PRIVACY_REGION_SCHEMA_VERSION
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionApi
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant
import kotlinx.io.readByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PrivacyRegionHttpException(val statusCode: Int) : IllegalStateException("Privacy region HTTP status $statusCode")
class InvalidPrivacyRegionResponseException(message: String) : IllegalArgumentException(message)

class KtorPrivacyRegionApi(
    private val client: HttpClient,
    private val appVersion: String,
    private val nowEpochMillis: () -> Long,
) : PrivacyRegionApi, AutoCloseable {
    override suspend fun fetch(): Result<PrivacyRegionResponse> = try {
        val resolved = client.prepareGet(PRIVACY_REGION_ENDPOINT) {
            accept(ContentType.Application.Json)
            if (appVersion.isNotBlank()) header("X-Privacy-Client", "bagcue/$appVersion")
        }.execute { response ->
            if (response.status != HttpStatusCode.OK) throw PrivacyRegionHttpException(response.status.value)
            val responseContentType = response.contentType()
            if (
                responseContentType?.withoutParameters() != ContentType.Application.Json ||
                responseContentType.parameters.isNotEmpty()
            ) {
                throw InvalidPrivacyRegionResponseException("Privacy region response must be application/json")
            }
            val declaredLengthHeader = response.headers["Content-Length"]
            val declaredLength = declaredLengthHeader?.toLongOrNull()
            if (declaredLengthHeader != null && (declaredLength == null || declaredLength < 0 || declaredLength > MAX_RESPONSE_BYTES)) {
                throw InvalidPrivacyRegionResponseException("Privacy region response has invalid length")
            }
            val bytes = response.bodyAsChannel().readRemaining(MAX_RESPONSE_BYTES.toLong() + 1).readByteArray()
            if (bytes.size > MAX_RESPONSE_BYTES) {
                throw InvalidPrivacyRegionResponseException("Privacy region response is too large")
            }
            val body = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
                .getOrElse { throw InvalidPrivacyRegionResponseException("Privacy region response must be UTF-8") }
            val dto = runCatching { Json.decodeFromString<PrivacyRegionDto>(body) }
                .getOrElse { throw InvalidPrivacyRegionResponseException("Invalid privacy region JSON") }
            val expiresAt = runCatching { Instant.parse(dto.expiresAt).toEpochMilliseconds() }
                .getOrElse { throw InvalidPrivacyRegionResponseException("Invalid expiresAt") }
            val now = nowEpochMillis()
            if (dto.schemaVersion != PRIVACY_REGION_SCHEMA_VERSION || dto.policyVersion.isBlank()) {
                throw InvalidPrivacyRegionResponseException("Unsupported privacy region response")
            }
            if (expiresAt <= now || expiresAt > now + PRIVACY_REGION_MAX_AGE_MILLIS) {
                throw InvalidPrivacyRegionResponseException("Privacy region response is not fresh")
            }
            PrivacyRegionResponse(dto.schemaVersion, dto.consentRequired, dto.policyVersion, expiresAt)
        }
        Result.success(resolved)
    } catch (cancellation: CancellationException) {
        throw cancellation
        } catch (expectedFailure: Throwable) {
            Result.failure(expectedFailure)
    }

    override fun close() = client.close()

    private companion object { const val MAX_RESPONSE_BYTES = 4 * 1024 }
}

@Serializable
private data class PrivacyRegionDto(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("consentRequired") val consentRequired: Boolean,
    @SerialName("policyVersion") val policyVersion: String,
    @SerialName("expiresAt") val expiresAt: String,
)
