package com.sedsoftware.bagcue.root.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.catalog.integration.CatalogComponentDefault
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
import com.sedsoftware.bagcue.history.integration.HistoryComponentDefault
import com.sedsoftware.bagcue.root.RootComponent
import com.sedsoftware.bagcue.session.integration.SessionComponentDefault
import com.sedsoftware.bagcue.settings.integration.SettingsComponentDefault
import com.sedsoftware.bagcue.templates.integration.TemplateComponentDefault
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

class RootComponentDefault(
    componentContext: ComponentContext,
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
) : RootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Session,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    override val selectedPrimaryDestination: Value<RootComponent.PrimaryDestination> =
        stack.mapValue { childStack -> childStack.active.instance.primaryDestination() }

    override fun selectPrimaryDestination(destination: RootComponent.PrimaryDestination) {
        when (destination) {
            RootComponent.PrimaryDestination.Today -> showSession()
            RootComponent.PrimaryDestination.Sessions -> showHistory()
            RootComponent.PrimaryDestination.Templates -> showTemplates()
            RootComponent.PrimaryDestination.Settings -> showSettings()
        }
    }

    override fun showSession() = navigatePrimary(Config.Session)

    override fun showHistory() = navigatePrimary(Config.History)

    override fun showTemplates() = navigatePrimary(Config.Templates)

    override fun showCatalog() {
        leaveSession()
        navigation.navigate { stack ->
            if (stack.lastOrNull() == Config.Catalog) {
                stack
            } else {
                stack.filterNot { it == Config.Templates || it == Config.Catalog } +
                    listOf(Config.Templates, Config.Catalog)
            }
        }
    }

    override fun showSettings() = navigatePrimary(Config.Settings)

    private fun navigatePrimary(destination: Config) {
        leaveSession()
        navigation.navigate { stack ->
            stack.filterNot { it == Config.Catalog || it::class == destination::class } + destination
        }
    }

    private fun createChild(config: Config, componentContext: ComponentContext): RootComponent.Child = when (config) {
        Config.Session -> RootComponent.Child.Session(
            SessionComponentDefault(
                componentContext = componentContext,
                sessionRepository = sessionRepository,
                templateRepository = templateRepository,
                catalogRepository = repository,
                storeFactory = storeFactory,
                sessionIdGenerator = sessionIdGenerator,
                sessionItemIdGenerator = sessionItemIdGenerator,
                catalogItemIdGenerator = itemIdGenerator,
                templatePositionIdGenerator = templatePositionIdGenerator,
                today = today,
                currentTimeMillis = currentTimeMillis,
                advertisingPrivacyRepository = advertisingPrivacyRepository,
                privacyRegionApi = privacyRegionApi,
                inlineAdController = inlineAdController,
                analyticsController = analyticsController,
                privacyPolicyUrl = privacyPolicyUrl,
                resolveResourceKey = resolveResourceKey,
                onOpenTemplates = ::showTemplates,
                onOpenCatalog = ::showCatalog,
                onOpenPrivacyPolicy = openExternalUrl,
            ),
        )
        Config.History -> RootComponent.Child.History(
            HistoryComponentDefault(
                componentContext = componentContext,
                repository = sessionRepository,
                storeFactory = storeFactory,
                analyticsController = analyticsController,
                onOpenSession = ::openSessionFromHistory,
                onRepeatSession = ::repeatFromHistory,
                onOpenToday = ::showSession,
            ),
        )
        Config.Templates -> RootComponent.Child.Templates(
            TemplateComponentDefault(
                componentContext = componentContext,
                templateRepository = templateRepository,
                catalogRepository = repository,
                storeFactory = storeFactory,
                templateIdGenerator = templateIdGenerator,
                positionIdGenerator = templatePositionIdGenerator,
                resolveResourceKey = resolveResourceKey,
                createDuplicateName = createDuplicateTemplateName,
                analyticsController = analyticsController,
                onOpenCatalog = ::showCatalog,
            ),
        )
        Config.Catalog -> RootComponent.Child.Catalog(
            CatalogComponentDefault(
                componentContext = componentContext,
                repository = repository,
                storeFactory = storeFactory,
                itemIdGenerator = itemIdGenerator,
                resolveResourceKey = resolveResourceKey,
            ),
        )
        Config.Settings -> RootComponent.Child.Settings(
            SettingsComponentDefault(
                componentContext = componentContext,
                preferencesRepository = reminderPreferencesRepository,
                sessionRepository = sessionRepository,
                scheduler = reminderNotificationScheduler,
                analyticsRepository = analyticsPreferenceRepository,
                analyticsController = analyticsController,
                privacyRepository = advertisingPrivacyRepository,
                privacyApi = privacyRegionApi,
                inlineAdController = inlineAdController,
                storeFactory = storeFactory,
                now = now,
                timeZone = timeZone,
                privacyPolicyUrl = privacyPolicyUrl,
                versionName = versionName,
                onOpenPrivacyPolicy = openExternalUrl,
            ),
        )
    }

    private fun openSessionFromHistory(id: com.sedsoftware.bagcue.domain.session.PackingSessionId) {
        showSession()
        (stack.value.active.instance as? RootComponent.Child.Session)?.component?.openSession(id)
    }

    private fun repeatFromHistory(templateIds: List<com.sedsoftware.bagcue.domain.template.KitTemplateId>) {
        showSession()
        (stack.value.active.instance as? RootComponent.Child.Session)?.component?.let { component ->
            component.startRepeat(today(), templateIds)
        }
    }

    private fun leaveSession() {
        (stack.value.active.instance as? RootComponent.Child.Session)?.component?.dismissResultAdvertising()
    }

    private fun RootComponent.Child.primaryDestination(): RootComponent.PrimaryDestination = when (this) {
        is RootComponent.Child.Session -> RootComponent.PrimaryDestination.Today
        is RootComponent.Child.History -> RootComponent.PrimaryDestination.Sessions
        is RootComponent.Child.Templates,
        is RootComponent.Child.Catalog -> RootComponent.PrimaryDestination.Templates
        is RootComponent.Child.Settings -> RootComponent.PrimaryDestination.Settings
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Session : Config

        @Serializable
        data object History : Config

        @Serializable
        data object Templates : Config

        @Serializable
        data object Catalog : Config

        @Serializable
        data object Settings : Config
    }
}

private fun <T : Any, R : Any> Value<T>.mapValue(transform: (T) -> R): Value<R> =
    object : Value<R>() {
        override val value: R
            get() = transform(this@mapValue.value)

        override fun subscribe(observer: (R) -> Unit): Cancellation =
            this@mapValue.subscribe { observer(transform(it)) }
    }
