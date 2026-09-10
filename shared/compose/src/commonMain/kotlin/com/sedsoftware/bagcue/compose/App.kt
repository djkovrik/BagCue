package com.sedsoftware.bagcue.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.history_title
import bagcue.shared.compose.generated.resources.session_today_title
import bagcue.shared.compose.generated.resources.settings_title
import bagcue.shared.compose.generated.resources.templates_title
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.compose.catalog.CatalogScreen
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.history.HistoryScreen
import com.sedsoftware.bagcue.compose.session.SessionScreen
import com.sedsoftware.bagcue.compose.settings.SettingsScreen
import com.sedsoftware.bagcue.compose.templates.TemplateScreen
import com.sedsoftware.bagcue.compose.theme.AppTheme
import com.sedsoftware.bagcue.root.RootComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val EXPANDED_NAVIGATION_BREAKPOINT_DP = 600
internal const val FONT_SCALE_THRESHOLD = 1.5f

internal enum class ProductNavigationLayout { Bar, Rail }

internal fun productNavigationLayout(width: androidx.compose.ui.unit.Dp): ProductNavigationLayout =
    if (width < EXPANDED_NAVIGATION_BREAKPOINT_DP.dp) ProductNavigationLayout.Bar else ProductNavigationLayout.Rail

internal fun reduceNavigationMotion(durationScale: Float): Boolean = durationScale == 0f

internal fun primaryNavigationRows(fontScale: Float): List<List<PrimaryDestinationUi>> =
    if (fontScale >= FONT_SCALE_THRESHOLD) primaryDestinations().chunked(2) else listOf(primaryDestinations())

@Composable
fun App(
    rootComponent: RootComponent,
    onThemeChanged: @Composable (Boolean) -> Unit = {},
    inlineResultAd: @Composable () -> Unit = {},
) = AppTheme(onThemeChanged) {
    val stack by rootComponent.stack.subscribeAsState()
    val selectedDestination by rootComponent.selectedPrimaryDestination.subscribeAsState()
    ProductShell(
        selectedDestination = selectedDestination,
        onDestinationSelected = rootComponent::selectPrimaryDestination,
    ) {
        when (val child = stack.active.instance) {
            is RootComponent.Child.Session -> SessionScreen(
                component = child.component,
                inlineResultAd = inlineResultAd,
            )
            is RootComponent.Child.History -> HistoryScreen(child.component)
            is RootComponent.Child.Settings -> SettingsScreen(child.component)
            is RootComponent.Child.Templates -> TemplateScreen(child.component)
            is RootComponent.Child.Catalog -> CatalogScreen(child.component, onBack = rootComponent::showTemplates)
        }
    }
}

@Composable
internal fun ProductShell(
    selectedDestination: RootComponent.PrimaryDestination,
    onDestinationSelected: (RootComponent.PrimaryDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val navigationLayout = productNavigationLayout(maxWidth)
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (navigationLayout == ProductNavigationLayout.Bar) {
                    PrimaryNavigationBar(selectedDestination, onDestinationSelected)
                }
            },
        ) { contentPadding ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
            ) {
                if (navigationLayout == ProductNavigationLayout.Rail) {
                    PrimaryNavigationRail(selectedDestination, onDestinationSelected)
                }
                Box(Modifier.weight(1f).fillMaxSize()) { content() }
            }
        }
    }
}

@Composable
internal fun PrimaryNavigationBar(
    selectedDestination: RootComponent.PrimaryDestination,
    onDestinationSelected: (RootComponent.PrimaryDestination) -> Unit,
) {
    val navigationRows = primaryNavigationRows(LocalDensity.current.fontScale)
    val useTwoRows = navigationRows.size > 1
    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    val reduceMotion = reduceNavigationMotion(durationScale)
    val dockShape = MaterialTheme.shapes.extraLarge
    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(elevation = 3.dp, shape = dockShape)
            .clip(dockShape)
            .semantics { testTag = "primary-navigation-bar" },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        if (useTwoRows) {
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                navigationRows.forEach { destinations ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
                        destinations.forEach { destination ->
                            PrimaryNavigationBarItem(
                                destination = destination,
                                selectedDestination = selectedDestination,
                                onDestinationSelected = onDestinationSelected,
                                reduceMotion = reduceMotion,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        } else {
            navigationRows.single().forEach { destination ->
                PrimaryNavigationBarItem(
                    destination = destination,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    reduceMotion = reduceMotion,
                )
            }
        }
    }
}

@Composable
private fun RowScope.PrimaryNavigationBarItem(
    destination: PrimaryDestinationUi,
    selectedDestination: RootComponent.PrimaryDestination,
    onDestinationSelected: (RootComponent.PrimaryDestination) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val selected = destination.destination == selectedDestination
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "primary-navigation-icon-scale",
    )
    val iconLift by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "primary-navigation-icon-lift",
    )
    NavigationBarItem(
        selected = selected,
        onClick = { onDestinationSelected(destination.destination) },
        icon = {
            Icon(
                painter = destination.icon(),
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = iconLift.toPx()
                },
            )
        },
        label = {
            androidx.compose.material3.Text(
                text = stringResource(destination.label),
                maxLines = 1,
                softWrap = false,
            )
        },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.semantics { testTag = destination.testTag },
    )
}

@Composable
private fun PrimaryNavigationRail(
    selectedDestination: RootComponent.PrimaryDestination,
    onDestinationSelected: (RootComponent.PrimaryDestination) -> Unit,
) {
    NavigationRail(Modifier.semantics { testTag = "primary-navigation-rail" }) {
        primaryDestinations().forEach { destination ->
            NavigationRailItem(
                selected = destination.destination == selectedDestination,
                onClick = { onDestinationSelected(destination.destination) },
                icon = { Icon(destination.icon(), contentDescription = null) },
                label = { androidx.compose.material3.Text(stringResource(destination.label)) },
                alwaysShowLabel = true,
                modifier = Modifier.semantics { testTag = destination.testTag },
            )
        }
    }
}

internal data class PrimaryDestinationUi(
    val destination: RootComponent.PrimaryDestination,
    val label: StringResource,
    val icon: @Composable () -> Painter,
    val testTag: String,
)

internal fun primaryDestinations(): List<PrimaryDestinationUi> = listOf(
    PrimaryDestinationUi(
        RootComponent.PrimaryDestination.Today,
        Res.string.session_today_title,
        { painterResource(BagCueAssets.Today) },
        "destination-today",
    ),
    PrimaryDestinationUi(
        RootComponent.PrimaryDestination.Sessions,
        Res.string.history_title,
        { painterResource(BagCueAssets.Sessions) },
        "destination-sessions",
    ),
    PrimaryDestinationUi(
        RootComponent.PrimaryDestination.Templates,
        Res.string.templates_title,
        { painterResource(BagCueAssets.Templates) },
        "destination-templates",
    ),
    PrimaryDestinationUi(
        RootComponent.PrimaryDestination.Settings,
        Res.string.settings_title,
        { painterResource(BagCueAssets.Settings) },
        "destination-settings",
    ),
)
