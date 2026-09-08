package com.sedsoftware.bagcue.root.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.AnalyticsPreferenceRepository
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionApi
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationScheduler
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferencesRepository
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.root.RootComponent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class RootComponentFactory(
    private val repository: CatalogRepository,
    private val storeFactory: StoreFactory,
    private val itemIdGenerator: PackingItemIdGenerator,
    private val templateRepository: KitTemplateRepository,
    private val templateIdGenerator: KitTemplateIdGenerator,
    private val templatePositionIdGenerator: TemplatePositionIdGenerator,
    private val sessionRepository: SessionHistoryRepository,
    private val sessionIdGenerator: PackingSessionIdGenerator,
    private val sessionItemIdGenerator: SessionPackingItemIdGenerator,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val reminderNotificationScheduler: ReminderNotificationScheduler,
    private val analyticsPreferenceRepository: AnalyticsPreferenceRepository,
    private val analyticsController: AnalyticsController,
    private val advertisingPrivacyRepository: AdvertisingPrivacyRepository,
    private val privacyRegionApi: PrivacyRegionApi,
    private val inlineAdController: InlineAdController,
    private val today: () -> LocalDate,
    private val now: () -> Instant,
    private val timeZone: () -> TimeZone,
    private val currentTimeMillis: () -> Long,
    private val resolveResourceKey: suspend (ResourceKey) -> String,
    private val createDuplicateTemplateName: suspend (String) -> String,
    private val privacyPolicyUrl: String?,
    private val versionName: String,
    private val openExternalUrl: (String) -> Unit,
) {
    fun create(componentContext: ComponentContext): RootComponent = RootComponentDefault(
        componentContext = componentContext,
        repository = repository,
        storeFactory = storeFactory,
        itemIdGenerator = itemIdGenerator,
        templateRepository = templateRepository,
        templateIdGenerator = templateIdGenerator,
        templatePositionIdGenerator = templatePositionIdGenerator,
        sessionRepository = sessionRepository,
        sessionIdGenerator = sessionIdGenerator,
        sessionItemIdGenerator = sessionItemIdGenerator,
        reminderPreferencesRepository = reminderPreferencesRepository,
        reminderNotificationScheduler = reminderNotificationScheduler,
        analyticsPreferenceRepository = analyticsPreferenceRepository,
        analyticsController = analyticsController,
        advertisingPrivacyRepository = advertisingPrivacyRepository,
        privacyRegionApi = privacyRegionApi,
        inlineAdController = inlineAdController,
        today = today,
        now = now,
        timeZone = timeZone,
        currentTimeMillis = currentTimeMillis,
        resolveResourceKey = resolveResourceKey,
        createDuplicateTemplateName = createDuplicateTemplateName,
        privacyPolicyUrl = privacyPolicyUrl,
        versionName = versionName,
        openExternalUrl = openExternalUrl,
    )
}
