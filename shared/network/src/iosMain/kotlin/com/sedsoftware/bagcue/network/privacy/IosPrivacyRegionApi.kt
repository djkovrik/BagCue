package com.sedsoftware.bagcue.network.privacy

import io.ktor.client.engine.darwin.Darwin

fun IosPrivacyRegionApi(appVersion: String, nowEpochMillis: () -> Long): KtorPrivacyRegionApi =
    KtorPrivacyRegionApi(privacyRegionHttpClient(Darwin.create()), appVersion, nowEpochMillis)
