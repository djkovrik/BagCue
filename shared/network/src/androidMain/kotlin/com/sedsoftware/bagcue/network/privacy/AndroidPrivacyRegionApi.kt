package com.sedsoftware.bagcue.network.privacy

import io.ktor.client.engine.okhttp.OkHttp

fun AndroidPrivacyRegionApi(appVersion: String, nowEpochMillis: () -> Long): KtorPrivacyRegionApi =
    KtorPrivacyRegionApi(privacyRegionHttpClient(OkHttp.create()), appVersion, nowEpochMillis)
