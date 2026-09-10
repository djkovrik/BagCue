import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.compose.App
import com.sedsoftware.bagcue.compose.catalog.resolveCatalogResource
import com.sedsoftware.bagcue.compose.templates.createDuplicateTemplateName
import com.sedsoftware.bagcue.data.catalog.CatalogDataModule
import com.sedsoftware.bagcue.data.catalog.CatalogDataModuleDependencies
import com.sedsoftware.bagcue.data.catalog.IosCatalogDatabaseDriverFactory
import com.sedsoftware.bagcue.data.reminder.ReminderDataModule
import com.sedsoftware.bagcue.data.reminder.ReminderDataModuleDependencies
import com.sedsoftware.bagcue.data.apa.ApaDataModule
import com.sedsoftware.bagcue.data.apa.ApaDataModuleDependencies
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.root.integration.RootComponentFactory
import com.sedsoftware.bagcue.network.privacy.IosPrivacyRegionApi
import com.sedsoftware.bagcue.platform.apa.IosAnalyticsController
import com.sedsoftware.bagcue.platform.apa.IosInlineAdController
import com.sedsoftware.bagcue.platform.reminder.IosReminderNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import platform.Foundation.NSUUID
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.setStatusBarStyle
import kotlin.time.Clock

private const val PRIVACY_POLICY_URL = "https://sedsoftware.com/apps/bagcue/policy.html"

fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry().apply { resume() }
    val dataModule = CatalogDataModule(
        CatalogDataModuleDependencies(
            driverFactory = IosCatalogDatabaseDriverFactory(),
            ioDispatcher = Dispatchers.Default,
            currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
        ),
    )
    val reminderDataModule = ReminderDataModule(
        ReminderDataModuleDependencies(ioDispatcher = Dispatchers.Default),
    )
    val apaDataModule = ApaDataModule(
        ApaDataModuleDependencies(ioDispatcher = Dispatchers.Default),
    )
    val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    val nowEpochMillis = { Clock.System.now().toEpochMilliseconds() }
    val reminderScheduler = IosReminderNotificationScheduler(
        currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
    )
    val rootComponent = RootComponentFactory(
        repository = dataModule.catalogRepository,
        storeFactory = DefaultStoreFactory(),
        itemIdGenerator = PackingItemIdGenerator {
            PackingItemId(NSUUID().UUIDString())
        },
        templateRepository = dataModule.kitTemplateRepository,
        templateIdGenerator = KitTemplateIdGenerator {
            KitTemplateId(NSUUID().UUIDString())
        },
        templatePositionIdGenerator = TemplatePositionIdGenerator {
            TemplatePositionId(NSUUID().UUIDString())
        },
        sessionRepository = dataModule.packingSessionRepository,
        sessionIdGenerator = PackingSessionIdGenerator {
            PackingSessionId(NSUUID().UUIDString())
        },
        sessionItemIdGenerator = SessionPackingItemIdGenerator {
            SessionPackingItemId(NSUUID().UUIDString())
        },
        reminderPreferencesRepository = reminderDataModule.reminderPreferencesRepository,
        reminderNotificationScheduler = reminderScheduler,
        analyticsPreferenceRepository = apaDataModule.analyticsPreferenceRepository,
        analyticsController = IosAnalyticsController(),
        advertisingPrivacyRepository = apaDataModule.advertisingPrivacyRepository,
        privacyRegionApi = IosPrivacyRegionApi(versionName, nowEpochMillis),
        inlineAdController = IosInlineAdController(),
        today = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
        now = Clock.System::now,
        timeZone = TimeZone::currentSystemDefault,
        currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
        resolveResourceKey = ::resolveCatalogResource,
        createDuplicateTemplateName = ::createDuplicateTemplateName,
        privacyPolicyUrl = PRIVACY_POLICY_URL,
        versionName = versionName,
        openExternalUrl = { url ->
            NSURL.URLWithString(url)?.let { policyUrl ->
                UIApplication.sharedApplication.openURL(
                    policyUrl,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
            }
        },
    ).create(DefaultComponentContext(lifecycle = lifecycle))

    return ComposeUIViewController {
        App(rootComponent = rootComponent, onThemeChanged = { ThemeChanged(it) })
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        )
    }
}
