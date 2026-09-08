package com.sedsoftware.bagcue.root.integration

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.domain.catalog.CatalogDependencySummary
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.CreatePackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItem
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.StarterPackingItem
import com.sedsoftware.bagcue.domain.catalog.UpdatePackingItem
import com.sedsoftware.bagcue.domain.apa.*
import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.reminder.ReminderNotificationScheduler
import com.sedsoftware.bagcue.domain.reminder.ReminderOccurrence
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferences
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferencesRepository
import com.sedsoftware.bagcue.domain.reminder.ReminderRescheduleReason
import com.sedsoftware.bagcue.domain.reminder.ReminderScheduleResult
import com.sedsoftware.bagcue.domain.reminder.SessionReminderPreferences
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.OneOffSessionItemCommand
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.SaveSessionItemToTemplatesCommand
import com.sedsoftware.bagcue.domain.session.SessionMutation
import com.sedsoftware.bagcue.domain.session.SessionHistory
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.StarterKitTemplate
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.root.RootComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RootComponentNavigationTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun primaryDestinationsExposeStableSelectionWithoutDuplicateConfigs() = runTest(dispatcher) {
        EmptyInlineAdController.disposeCount = 0
        val root = createRoot()

        assertIs<RootComponent.Child.Session>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Today, root.selectedPrimaryDestination.value)

        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Templates)
        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Templates)
        advanceUntilIdle()
        assertIs<RootComponent.Child.Templates>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Templates, root.selectedPrimaryDestination.value)
        assertEquals(2, root.stack.value.items.size)
        assertEquals(1, EmptyInlineAdController.disposeCount)

        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Sessions)
        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Sessions)
        assertIs<RootComponent.Child.History>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Sessions, root.selectedPrimaryDestination.value)
        assertEquals(3, root.stack.value.items.size)

        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Settings)
        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Settings)
        assertIs<RootComponent.Child.Settings>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Settings, root.selectedPrimaryDestination.value)
        assertEquals(4, root.stack.value.items.size)

        root.selectPrimaryDestination(RootComponent.PrimaryDestination.Today)
        assertIs<RootComponent.Child.Session>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Today, root.selectedPrimaryDestination.value)
        assertEquals(4, root.stack.value.items.size)
    }

    @Test
    fun catalogRemainsNestedUnderTemplatesAndBackReturnsToTemplates() = runTest(dispatcher) {
        val backDispatcher = BackDispatcher()
        val root = createRoot(
            DefaultComponentContext(
                lifecycle = LifecycleRegistry(),
                backHandler = backDispatcher,
            ),
        )

        root.showCatalog()
        root.showCatalog()

        assertIs<RootComponent.Child.Catalog>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Templates, root.selectedPrimaryDestination.value)
        assertEquals(3, root.stack.value.items.size)
        assertTrue(backDispatcher.back())
        assertIs<RootComponent.Child.Templates>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Templates, root.selectedPrimaryDestination.value)
    }

    @Test
    fun backReturnsToImmediatelyPreviousPrimaryDestination() = runTest(dispatcher) {
        val backDispatcher = BackDispatcher()
        val root = createRoot(
            DefaultComponentContext(
                lifecycle = LifecycleRegistry(),
                backHandler = backDispatcher,
            ),
        )

        root.showHistory()
        root.showSettings()

        assertEquals(RootComponent.PrimaryDestination.Settings, root.selectedPrimaryDestination.value)
        assertTrue(backDispatcher.back())
        assertIs<RootComponent.Child.History>(root.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Sessions, root.selectedPrimaryDestination.value)
    }

    @Test
    fun selectedPrimaryDestinationRestoresWithSerializedStack() = runTest(dispatcher) {
        val stateKeeper = StateKeeperDispatcher()
        val root = createRoot(
            DefaultComponentContext(
                lifecycle = LifecycleRegistry(),
                stateKeeper = stateKeeper,
            ),
        )
        root.showTemplates()
        root.showCatalog()

        val restored = createRoot(
            DefaultComponentContext(
                lifecycle = LifecycleRegistry(),
                stateKeeper = StateKeeperDispatcher(stateKeeper.save()),
            ),
        )

        assertIs<RootComponent.Child.Catalog>(restored.stack.value.active.instance)
        assertEquals(RootComponent.PrimaryDestination.Templates, restored.selectedPrimaryDestination.value)
        assertEquals(3, restored.stack.value.items.size)
    }

    private fun createRoot(
        componentContext: ComponentContext = DefaultComponentContext(LifecycleRegistry()),
    ) = RootComponentDefault(
            componentContext = componentContext,
            repository = EmptyCatalogRepository,
            storeFactory = DefaultStoreFactory(),
            itemIdGenerator = PackingItemIdGenerator { PackingItemId("item") },
            templateRepository = EmptyTemplateRepository,
            templateIdGenerator = KitTemplateIdGenerator { KitTemplateId("template") },
            templatePositionIdGenerator = TemplatePositionIdGenerator { TemplatePositionId("position") },
            sessionRepository = EmptySessionRepository,
            sessionIdGenerator = PackingSessionIdGenerator { PackingSessionId("session") },
            sessionItemIdGenerator = SessionPackingItemIdGenerator { SessionPackingItemId("session-item") },
            reminderPreferencesRepository = EmptyReminderPreferencesRepository,
            reminderNotificationScheduler = EmptyReminderNotificationScheduler,
            analyticsPreferenceRepository = EmptyAnalyticsPreferenceRepository,
            analyticsController = EmptyAnalyticsController,
            advertisingPrivacyRepository = EmptyAdvertisingPrivacyRepository,
            privacyRegionApi = EmptyPrivacyRegionApi,
            inlineAdController = EmptyInlineAdController,
            today = { LocalDate(2026, 9, 8) },
            now = { Instant.fromEpochMilliseconds(1L) },
            timeZone = { TimeZone.UTC },
            currentTimeMillis = { 1L },
            resolveResourceKey = { it.value },
            createDuplicateTemplateName = { "$it copy" },
            privacyPolicyUrl = null,
            versionName = "test",
            openExternalUrl = {},
        )
}

private object EmptyReminderPreferencesRepository : ReminderPreferencesRepository {
    private val global = MutableStateFlow(ReminderPreferences())
    override fun observeGlobalPreferences(): Flow<ReminderPreferences> = global
    override fun observeSessionPreferences(sessionId: PackingSessionId): Flow<SessionReminderPreferences?> = MutableStateFlow(null)
    override suspend fun readGlobalPreferences() = Result.success(global.value)
    override suspend fun updateGlobalPreferences(preferences: ReminderPreferences) = Result.success(preferences)
    override suspend fun readSessionPreferences(sessionId: PackingSessionId) = Result.success<SessionReminderPreferences?>(null)
    override suspend fun copyGlobalPreferencesToSession(sessionId: PackingSessionId) = Result.success(SessionReminderPreferences(sessionId, global.value.evening, global.value.morning))
    override suspend fun updateSessionPreferences(preferences: SessionReminderPreferences) = Result.success(preferences)
}

private object EmptyReminderNotificationScheduler : ReminderNotificationScheduler {
    override suspend fun permissionState() = Result.success(NotificationPermissionState.Granted)
    override suspend fun requestPermission() = Result.success(NotificationPermissionState.Granted)
    override suspend fun consumePendingRescheduleReasons() = Result.success(emptySet<ReminderRescheduleReason>())
    override suspend fun synchronize(occurrences: List<ReminderOccurrence>, reason: ReminderRescheduleReason) =
        Result.success<ReminderScheduleResult>(ReminderScheduleResult.Synchronized(occurrences.map { it.id }.toSet()))
}

private object EmptyAnalyticsPreferenceRepository : AnalyticsPreferenceRepository {
    override fun observe(): Flow<AnalyticsPreference> = MutableStateFlow(AnalyticsPreference())
    override suspend fun read() = Result.success(AnalyticsPreference())
    override suspend fun update(preference: AnalyticsPreference) = Result.success(preference)
}

private object EmptyAnalyticsController : AnalyticsController {
    override suspend fun setCollectionEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun resetAnalyticsData() = Result.success(Unit)
    override suspend fun readAppInstanceId() = Result.success<String?>(null)
    override suspend fun record(event: AnalyticsEvent) = Result.success(Unit)
}

private object EmptyAdvertisingPrivacyRepository : AdvertisingPrivacyRepository {
    override fun observe(): Flow<AdvertisingPrivacyState?> = MutableStateFlow(null)
    override suspend fun readFresh(nowEpochMillis: Long) = Result.success<AdvertisingPrivacyState?>(null)
    override suspend fun storeResponse(response: PrivacyRegionResponse, nowEpochMillis: Long) = Result.success(AdvertisingPrivacyState(response, null, null))
    override suspend fun storeChoice(choice: AdvertisingConsentChoice, nowEpochMillis: Long): Result<AdvertisingPrivacyState> = error("Not used")
    override suspend fun clear() = Result.success(Unit)
}

private object EmptyPrivacyRegionApi : PrivacyRegionApi {
    override suspend fun fetch() = Result.failure<PrivacyRegionResponse>(IllegalStateException("offline"))
}

private object EmptyInlineAdController : InlineAdController {
    override val platform = AdPlatform.Ios
    override val hasProductionAdUnit = false
    var disposeCount = 0
    override suspend fun initialize(userConsent: Boolean) = Result.success(Unit)
    override suspend fun requestInlineAd() = Result.success<InlineAdState>(InlineAdState.Suppressed)
    override suspend fun dispose() = Result.success(Unit).also { disposeCount += 1 }
}

private object EmptyCatalogRepository : CatalogRepository {
    override fun observeItems(): Flow<List<PackingItem>> = MutableStateFlow(emptyList())
    override suspend fun readItems() = Result.success(emptyList<PackingItem>())
    override suspend fun findItem(id: PackingItemId) = Result.success<PackingItem?>(null)
    override suspend fun installStarterItems(items: List<StarterPackingItem>) = Result.success(Unit)
    override suspend fun createItem(command: CreatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun updateItem(command: UpdatePackingItem): Result<PackingItem> = error("Not used")
    override suspend fun readDeleteDependencies(id: PackingItemId) = Result.success(CatalogDependencySummary(id, emptyList(), emptyList()))
    override suspend fun deleteItemAtomically(id: PackingItemId) = Result.success(Unit)
}

private object EmptyTemplateRepository : KitTemplateRepository {
    override fun observeTemplates(): Flow<List<KitTemplate>> = MutableStateFlow(emptyList())
    override suspend fun readTemplates() = Result.success(emptyList<KitTemplate>())
    override suspend fun findTemplate(id: KitTemplateId) = Result.success<KitTemplate?>(null)
    override suspend fun installStarterTemplates(templates: List<StarterKitTemplate>) = Result.success(Unit)
    override suspend fun createTemplate(template: KitTemplate) = Result.success(template)
    override suspend fun updateTemplate(template: KitTemplate) = Result.success(template)
    override suspend fun deleteTemplate(id: KitTemplateId) = Result.success(Unit)
}

private object EmptySessionRepository : SessionHistoryRepository {
    override fun observeSession(id: PackingSessionId): Flow<PackingSession?> = MutableStateFlow(null)
    override fun observeHistory(localDate: LocalDate?): Flow<SessionHistory> = MutableStateFlow(SessionHistory(emptyList(), emptyList()))
    override suspend fun readSession(id: PackingSessionId) = Result.success<PackingSession?>(null)
    override suspend fun findSessionByDate(date: LocalDate) = Result.success<PackingSession?>(null)
    override suspend fun readHistory(localDate: LocalDate?) = Result.success(SessionHistory(emptyList(), emptyList()))
    override suspend fun createSession(session: PackingSession) = Result.success<CreateSessionResult>(CreateSessionResult.Created(session))
    override suspend fun replaceActiveSnapshot(session: PackingSession) = Result.success(session)
    override suspend fun restoreUndo(snapshot: SessionUndoSnapshot) = Result.success(snapshot.before)
    override suspend fun addOneOffItem(command: OneOffSessionItemCommand): Result<PackingSession> = error("Not used")
    override suspend fun removeItem(sessionId: PackingSessionId, itemId: SessionPackingItemId): Result<SessionMutation> = error("Not used")
    override suspend fun updateItem(sessionId: PackingSessionId, item: SessionPackingItem): Result<PackingSession> = error("Not used")
    override suspend fun completeSession(session: PackingSession) = Result.success(session)
    override suspend fun reopenSession(id: PackingSessionId): Result<PackingSession> = error("Not used")
    override suspend fun prepareRepeat(id: PackingSessionId): Result<com.sedsoftware.bagcue.domain.session.RepeatSessionSelection> = error("Not used")
    override suspend fun deleteSession(id: PackingSessionId): Result<com.sedsoftware.bagcue.domain.session.SessionDeletion> = error("Not used")
    override suspend fun restoreDeletedSession(undo: com.sedsoftware.bagcue.domain.session.DeletedSessionUndo): Result<CreateSessionResult> = error("Not used")
    override suspend fun saveItemToTemplates(command: SaveSessionItemToTemplatesCommand) = Result.success(emptyList<KitTemplate>())
}
