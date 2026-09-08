package com.sedsoftware.bagcue

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bagcue.apa.AndroidApaPlatform
import com.sedsoftware.bagcue.compose.App
import com.sedsoftware.bagcue.compose.catalog.resolveCatalogResource
import com.sedsoftware.bagcue.compose.templates.createDuplicateTemplateName
import com.sedsoftware.bagcue.data.apa.ApaDataModule
import com.sedsoftware.bagcue.data.apa.ApaDataModuleDependencies
import com.sedsoftware.bagcue.data.catalog.AndroidCatalogDatabaseDriverFactory
import com.sedsoftware.bagcue.data.catalog.CatalogDataModule
import com.sedsoftware.bagcue.data.catalog.CatalogDataModuleDependencies
import com.sedsoftware.bagcue.data.reminder.ReminderDataModule
import com.sedsoftware.bagcue.data.reminder.ReminderDataModuleDependencies
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.reminder.NotificationPermissionState
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateIdGenerator
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.platform.reminder.AndroidReminderNotificationScheduler
import com.sedsoftware.bagcue.network.privacy.AndroidPrivacyRegionApi
import com.sedsoftware.bagcue.root.integration.RootComponentFactory
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Clock

class AppActivity : ComponentActivity() {
    private var notificationPermissionContinuation: CancellableContinuation<NotificationPermissionState>? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionContinuation?.let { continuation ->
            notificationPermissionContinuation = null
            if (continuation.isActive) {
                continuation.resume(
                    if (granted) NotificationPermissionState.Granted else NotificationPermissionState.Denied,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dataModule = CatalogDataModule(
            CatalogDataModuleDependencies(
                driverFactory = AndroidCatalogDatabaseDriverFactory(applicationContext),
                ioDispatcher = Dispatchers.IO,
                currentTimeMillis = System::currentTimeMillis,
            ),
        )
        val reminderDataModule = ReminderDataModule(
            ReminderDataModuleDependencies(ioDispatcher = Dispatchers.IO),
        )
        val apaDataModule = ApaDataModule(
            ApaDataModuleDependencies(ioDispatcher = Dispatchers.IO),
        )
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val productionAdUnitId = BuildConfig.YANDEX_AD_UNIT_ID.takeIf(String::isNotBlank)
        val apaPlatform = AndroidApaPlatform(applicationContext, productionAdUnitId)
        val privacyRegionApi = AndroidPrivacyRegionApi(versionName, System::currentTimeMillis)
        val reminderScheduler = AndroidReminderNotificationScheduler(
            context = applicationContext,
            permissionRequester = ::requestNotificationPermission,
        )
        val rootComponent = RootComponentFactory(
            repository = dataModule.catalogRepository,
            storeFactory = DefaultStoreFactory(),
            itemIdGenerator = PackingItemIdGenerator {
                PackingItemId(UUID.randomUUID().toString())
            },
            templateRepository = dataModule.kitTemplateRepository,
            templateIdGenerator = KitTemplateIdGenerator {
                KitTemplateId(UUID.randomUUID().toString())
            },
            templatePositionIdGenerator = TemplatePositionIdGenerator {
                TemplatePositionId(UUID.randomUUID().toString())
            },
            sessionRepository = dataModule.packingSessionRepository,
            sessionIdGenerator = PackingSessionIdGenerator {
                PackingSessionId(UUID.randomUUID().toString())
            },
            sessionItemIdGenerator = SessionPackingItemIdGenerator {
                SessionPackingItemId(UUID.randomUUID().toString())
            },
            reminderPreferencesRepository = reminderDataModule.reminderPreferencesRepository,
            reminderNotificationScheduler = reminderScheduler,
            analyticsPreferenceRepository = apaDataModule.analyticsPreferenceRepository,
            analyticsController = apaPlatform.analyticsController,
            advertisingPrivacyRepository = apaDataModule.advertisingPrivacyRepository,
            privacyRegionApi = privacyRegionApi,
            inlineAdController = apaPlatform.inlineAdController,
            today = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
            now = Clock.System::now,
            timeZone = TimeZone::currentSystemDefault,
            currentTimeMillis = System::currentTimeMillis,
            resolveResourceKey = ::resolveCatalogResource,
            createDuplicateTemplateName = ::createDuplicateTemplateName,
            privacyPolicyUrl = null,
            versionName = versionName,
            openExternalUrl = {},
        ).create(defaultComponentContext())
        setContent {
            App(
                rootComponent = rootComponent,
                onThemeChanged = { ThemeChanged(it) },
                inlineResultAd = {
                    apaPlatform.inlineAdGateway.bannerAdView?.let { banner ->
                        AndroidView(
                            factory = {
                                (banner.parent as? ViewGroup)?.removeView(banner)
                                banner
                            },
                        )
                    }
                },
            )
        }
    }

    private suspend fun requestNotificationPermission(): NotificationPermissionState =
        suspendCancellableCoroutine { continuation ->
            check(notificationPermissionContinuation == null) { "A notification permission request is already active" }
            notificationPermissionContinuation = continuation
            continuation.invokeOnCancellation {
                if (notificationPermissionContinuation === continuation) notificationPermissionContinuation = null
            }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}
