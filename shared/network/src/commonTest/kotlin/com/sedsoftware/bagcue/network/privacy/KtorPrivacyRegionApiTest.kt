package com.sedsoftware.bagcue.network.privacy

import com.sedsoftware.bagcue.domain.apa.PRIVACY_REGION_ENDPOINT
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtorPrivacyRegionApiTest {
    @Test
    fun validMinimalResponseUsesExactHttpsContractAndHeader() = runTest {
        val engine = MockEngine { request ->
            assertEquals(PRIVACY_REGION_ENDPOINT, request.url.toString())
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("application/json", request.headers[HttpHeaders.Accept])
            assertEquals("bagcue/1.0.0", request.headers["X-Privacy-Client"])
            respond(
                content = validJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val response = api(engine).fetch().getOrThrow()

        assertTrue(response.consentRequired)
        assertEquals("policy-v1", response.policyVersion)
    }

    @Test
    fun malformedExpiredUnknownSchemaAndOversizedResponsesFailClosed() = runTest {
        val bodies = listOf(
            "not-json",
            validJson.replace("2026-09-10T00:00:00Z", "2026-09-08T00:00:00Z"),
            validJson.replace("2026-09-10T00:00:00Z", "2026-09-12T00:00:00Z"),
            validJson.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
            validJson.replace("\"policy-v1\"", "\" \""),
            validJson.dropLast(1) + ",\"country\":\"DE\"}",
            validJson + " ".repeat(4096),
        )
        bodies.forEach { body ->
            val engine = MockEngine {
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            assertTrue(api(engine).fetch().isFailure)
        }
    }

    @Test
    fun missingWrongOrParameterizedContentTypeFailsClosed() = runTest {
        listOf(null, "text/json", "application/json; charset=utf-8").forEach { contentType ->
            val engine = MockEngine {
                respond(
                    validJson,
                    HttpStatusCode.OK,
                    contentType?.let { headersOf(HttpHeaders.ContentType, it) } ?: headersOf(),
                )
            }
            assertIs<InvalidPrivacyRegionResponseException>(api(engine).fetch().exceptionOrNull())
        }
    }

    @Test
    fun declaredAndUndeclaredPayloadsOverFourKibFailClosed() = runTest {
        val declared = MockEngine {
            respond(
                validJson,
                HttpStatusCode.OK,
                headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ContentLength to listOf("4097"),
                ),
            )
        }
        val streamed = MockEngine {
            respond(
                validJson + " ".repeat(4096),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertIs<InvalidPrivacyRegionResponseException>(api(declared).fetch().exceptionOrNull())
        assertIs<InvalidPrivacyRegionResponseException>(api(streamed).fetch().exceptionOrNull())
    }

    @Test
    fun responseAtExactFreshnessAndPayloadBoundsIsAccepted() = runTest {
        val exactExpiry = validJson.replace("2026-09-10T00:00:00Z", "2026-09-11T00:00:00Z")
        val exactBody = exactExpiry + " ".repeat(4096 - exactExpiry.encodeToByteArray().size)
        val engine = MockEngine {
            respond(exactBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        assertNotNull(api(engine).fetch().getOrThrow())
    }

    @Test
    fun redirectRateLimitAndOutageAreTypedFailuresWithoutFallbackEligibility() = runTest {
        listOf(HttpStatusCode.Found, HttpStatusCode.TooManyRequests, HttpStatusCode.ServiceUnavailable).forEach { status ->
            val engine = MockEngine { respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json")) }
            assertIs<PrivacyRegionHttpException>(api(engine).fetch().exceptionOrNull())
        }
    }

    @Test
    fun transportFailureIsReturnedWithoutFallbackEligibility() = runTest {
        val engine = MockEngine { throw IllegalStateException("offline") }

        val failure = api(engine).fetch().exceptionOrNull()

        assertIs<IllegalStateException>(failure)
        assertEquals("offline", failure.message)
    }

    private fun api(engine: MockEngine) = KtorPrivacyRegionApi(
        client = privacyRegionHttpClient(engine),
        appVersion = "1.0.0",
        nowEpochMillis = { 1_788_825_600_000 },
    )

    private val validJson =
        """{"schemaVersion":1,"consentRequired":true,"policyVersion":"policy-v1","expiresAt":"2026-09-10T00:00:00Z"}"""
}
