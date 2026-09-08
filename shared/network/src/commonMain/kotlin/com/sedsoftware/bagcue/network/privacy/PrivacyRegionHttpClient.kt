package com.sedsoftware.bagcue.network.privacy

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val PRIVACY_REQUEST_TIMEOUT_MILLIS = 5_000L

fun privacyRegionHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    followRedirects = false
    install(HttpTimeout) {
        connectTimeoutMillis = PRIVACY_REQUEST_TIMEOUT_MILLIS
        requestTimeoutMillis = PRIVACY_REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = PRIVACY_REQUEST_TIMEOUT_MILLIS
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = false })
    }
}
