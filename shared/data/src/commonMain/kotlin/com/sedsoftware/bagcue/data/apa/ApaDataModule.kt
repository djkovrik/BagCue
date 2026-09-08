package com.sedsoftware.bagcue.data.apa

import com.russhwolf.settings.Settings
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreferenceRepository
import kotlinx.coroutines.CoroutineDispatcher

data class ApaDataModuleDependencies(
    val ioDispatcher: CoroutineDispatcher,
    val settings: Settings = Settings(),
)

interface ApaDataModule {
    val analyticsPreferenceRepository: AnalyticsPreferenceRepository
    val advertisingPrivacyRepository: AdvertisingPrivacyRepository
}

fun ApaDataModule(dependencies: ApaDataModuleDependencies): ApaDataModule = object : ApaDataModule {
    override val analyticsPreferenceRepository by lazy {
        SettingsAnalyticsPreferenceRepository(dependencies.settings, dependencies.ioDispatcher)
    }
    override val advertisingPrivacyRepository by lazy {
        SettingsAdvertisingPrivacyRepository(dependencies.settings, dependencies.ioDispatcher)
    }
}
