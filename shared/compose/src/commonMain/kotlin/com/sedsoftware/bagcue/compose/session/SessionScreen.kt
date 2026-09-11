package com.sedsoftware.bagcue.compose.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.common_back
import bagcue.shared.compose.generated.resources.common_cancel
import bagcue.shared.compose.generated.resources.common_close
import bagcue.shared.compose.generated.resources.common_quantity_compact
import bagcue.shared.compose.generated.resources.common_name_quantity
import bagcue.shared.compose.generated.resources.common_retry
import bagcue.shared.compose.generated.resources.session_add_item
import bagcue.shared.compose.generated.resources.session_ad_consent
import bagcue.shared.compose.generated.resources.session_ad_consent_body
import bagcue.shared.compose.generated.resources.session_ad_decline
import bagcue.shared.compose.generated.resources.session_ad_label
import bagcue.shared.compose.generated.resources.session_ad_loading
import bagcue.shared.compose.generated.resources.session_bag_label
import bagcue.shared.compose.generated.resources.session_catalog
import bagcue.shared.compose.generated.resources.session_complete
import bagcue.shared.compose.generated.resources.session_complete_failed
import bagcue.shared.compose.generated.resources.session_complete_skipped
import bagcue.shared.compose.generated.resources.session_confirm
import bagcue.shared.compose.generated.resources.session_choose_bag
import bagcue.shared.compose.generated.resources.session_create
import bagcue.shared.compose.generated.resources.session_create_failed
import bagcue.shared.compose.generated.resources.session_create_title
import bagcue.shared.compose.generated.resources.session_continue_description
import bagcue.shared.compose.generated.resources.session_date
import bagcue.shared.compose.generated.resources.session_date_section
import bagcue.shared.compose.generated.resources.session_date_past
import bagcue.shared.compose.generated.resources.session_done
import bagcue.shared.compose.generated.resources.session_edit_item
import bagcue.shared.compose.generated.resources.session_edit_title
import bagcue.shared.compose.generated.resources.session_empty
import bagcue.shared.compose.generated.resources.session_empty_body
import bagcue.shared.compose.generated.resources.session_empty_title
import bagcue.shared.compose.generated.resources.session_item_actions
import bagcue.shared.compose.generated.resources.session_item_missing
import bagcue.shared.compose.generated.resources.session_load_failed
import bagcue.shared.compose.generated.resources.session_location_label
import bagcue.shared.compose.generated.resources.session_missing
import bagcue.shared.compose.generated.resources.session_move_from
import bagcue.shared.compose.generated.resources.session_name_label
import bagcue.shared.compose.generated.resources.session_name_required
import bagcue.shared.compose.generated.resources.session_no_bag
import bagcue.shared.compose.generated.resources.session_occupied_body
import bagcue.shared.compose.generated.resources.session_occupied_title
import bagcue.shared.compose.generated.resources.session_oneoff_title
import bagcue.shared.compose.generated.resources.session_open
import bagcue.shared.compose.generated.resources.session_open_existing
import bagcue.shared.compose.generated.resources.session_plan
import bagcue.shared.compose.generated.resources.session_progress
import bagcue.shared.compose.generated.resources.session_remove_failed
import bagcue.shared.compose.generated.resources.session_remove_today
import bagcue.shared.compose.generated.resources.session_reopen
import bagcue.shared.compose.generated.resources.session_reopen_failed
import bagcue.shared.compose.generated.resources.session_replace
import bagcue.shared.compose.generated.resources.session_replace_failed
import bagcue.shared.compose.generated.resources.session_result_date
import bagcue.shared.compose.generated.resources.session_result_skipped
import bagcue.shared.compose.generated.resources.session_result_success
import bagcue.shared.compose.generated.resources.session_revision_conflict
import bagcue.shared.compose.generated.resources.session_save
import bagcue.shared.compose.generated.resources.session_save_failed
import bagcue.shared.compose.generated.resources.session_save_templates
import bagcue.shared.compose.generated.resources.session_save_to_required
import bagcue.shared.compose.generated.resources.session_save_to_title
import bagcue.shared.compose.generated.resources.session_select_template
import bagcue.shared.compose.generated.resources.session_selected_count
import bagcue.shared.compose.generated.resources.session_summary_section
import bagcue.shared.compose.generated.resources.session_skipped_body
import bagcue.shared.compose.generated.resources.session_skipped_title
import bagcue.shared.compose.generated.resources.session_source_label
import bagcue.shared.compose.generated.resources.session_templates
import bagcue.shared.compose.generated.resources.session_templates_section
import bagcue.shared.compose.generated.resources.session_starter_templates
import bagcue.shared.compose.generated.resources.template_positions_count
import bagcue.shared.compose.generated.resources.session_today
import bagcue.shared.compose.generated.resources.session_today_title
import bagcue.shared.compose.generated.resources.session_tomorrow
import bagcue.shared.compose.generated.resources.session_undo
import bagcue.shared.compose.generated.resources.session_undo_failed
import bagcue.shared.compose.generated.resources.session_up_next
import bagcue.shared.compose.generated.resources.session_use_template
import bagcue.shared.compose.generated.resources.settings_privacy_allow
import bagcue.shared.compose.generated.resources.settings_privacy_policy
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.assets.BagCueBrandMark
import com.sedsoftware.bagcue.compose.assets.BagCueIcon
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.session.SessionComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val LARGE_FONT_SCALE = 1.5f

@Composable
fun SessionScreen(
    component: SessionComponent,
    inlineResultAd: @Composable () -> Unit = {},
) {
    val model by component.model.subscribeAsState()
    Box(Modifier.fillMaxSize()) {
        when (val screen = model.screen) {
            is SessionComponent.Screen.Today -> TodayScreen(screen, component)
            is SessionComponent.Screen.Create -> CreateScreen(screen, component)
            is SessionComponent.Screen.Active -> ActiveScreen(screen, component)
            is SessionComponent.Screen.Result -> ResultScreen(screen, component, inlineResultAd)
        }
        if (model.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        model.error?.let { error ->
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(errorResource(error)), Modifier.weight(1f))
                    TextButton(component::clearError) { Text(stringResource(Res.string.common_close)) }
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(screen: SessionComponent.Screen.Today, component: SessionComponent) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.session_today_title)) }) }) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                screen.session?.let { session ->
                    todaySessionHero(session, component)
                } ?: emptyTodayHero(screen.date, component)

                if (screen.session == null && screen.isFirstRun && screen.starterTemplates.isNotEmpty()) {
                    starterTemplates(screen.date, screen.starterTemplates, component)
                }

                screen.nextSession?.let { next ->
                    nextSessionCard(next, component)
                }

                todayShortcuts(component)
            }
        }
    }
}

@Composable
private fun todaySessionHero(session: SessionComponent.SessionSummary, component: SessionComponent) {
    val templateNames = mutableListOf<String>()
    for (name in session.templateNames) templateNames += name.resolve()
    val remaining = (session.totalCount - session.packedCount).coerceAtLeast(0)
    val progress = if (session.totalCount == 0) 0f else {
        (session.packedCount.toFloat() / session.totalCount).coerceIn(0f, 1f)
    }
    val actionDescription = stringResource(
        Res.string.session_continue_description,
        session.date.toString(),
        remaining,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BagCueBrandMark(Modifier.height(32.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        session.date.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (templateNames.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BagCueIcon(BagCueAssets.Templates, null, Modifier.size(18.dp))
                            Text(
                                templateNames.joinToString(),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    stringResource(Res.string.session_progress, session.packedCount, session.totalCount),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraLarge),
            )
            Button(
                onClick = { component.openSession(session.id) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                    .semantics { contentDescription = actionDescription },
            ) {
                BagCueIcon(BagCueAssets.Check, null)
                Text(stringResource(Res.string.session_open))
            }
        }
    }
}

@Composable
private fun emptyTodayHero(date: kotlinx.datetime.LocalDate, component: SessionComponent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BagCueBrandMark(Modifier.height(36.dp))
                Text(date.toString(), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(Res.string.session_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(Res.string.session_empty_body), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { component.startCreate(date) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) {
                BagCueIcon(BagCueAssets.Add, null)
                Text(stringResource(Res.string.session_plan))
            }
        }
    }
}

@Composable
private fun starterTemplates(
    date: kotlinx.datetime.LocalDate,
    templates: List<SessionComponent.TemplateChoice>,
    component: SessionComponent,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.session_starter_templates),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        templates.forEach { template ->
            val name = template.name.resolve()
            val actionDescription = stringResource(Res.string.session_use_template, name)
            Surface(
                onClick = { component.startRepeat(date, listOf(template.id)) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = actionDescription },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    Modifier.fillMaxWidth().sizeIn(minHeight = 64.dp).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    todayShortcutIcon(BagCueAssets.Templates)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text(
                            stringResource(Res.string.template_positions_count, template.positionCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun nextSessionCard(session: SessionComponent.SessionSummary, component: SessionComponent) {
    val templateNames = mutableListOf<String>()
    for (name in session.templateNames) templateNames += name.resolve()
    Surface(
        onClick = { component.openSession(session.id) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth().sizeIn(minHeight = 76.dp).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            todayShortcutIcon(BagCueAssets.Calendar)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(Res.string.session_up_next),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(session.date.toString(), style = MaterialTheme.typography.titleMedium)
                if (templateNames.isNotEmpty()) {
                    Text(
                        templateNames.joinToString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                stringResource(Res.string.session_progress, session.packedCount, session.totalCount),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun todayShortcuts(component: SessionComponent) {
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    if (largeFont) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            todayShortcut(BagCueAssets.Templates, Res.string.session_templates, component::openTemplates)
            todayShortcut(BagCueAssets.Bag, Res.string.session_catalog, component::openCatalog)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            todayShortcut(
                BagCueAssets.Templates,
                Res.string.session_templates,
                component::openTemplates,
                Modifier.weight(1f),
            )
            todayShortcut(BagCueAssets.Bag, Res.string.session_catalog, component::openCatalog, Modifier.weight(1f))
        }
    }
}

@Composable
private fun todayShortcut(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth().sizeIn(minHeight = 64.dp).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            todayShortcutIcon(icon)
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun todayShortcutIcon(icon: org.jetbrains.compose.resources.DrawableResource) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            BagCueIcon(icon, null, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CreateScreen(screen: SessionComponent.Screen.Create, component: SessionComponent) {
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.session_create_title)) },
                navigationIcon = {
                    IconButton(component::backToToday) {
                        BagCueIcon(BagCueAssets.Back, stringResource(Res.string.common_back))
                    }
                },
            )
        },
        bottomBar = {
            Box(
                Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    component::createSession,
                    enabled = !screen.isSaving,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).sizeIn(minHeight = 52.dp),
                ) {
                    Text(stringResource(Res.string.session_create))
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                Modifier.fillMaxWidth().widthIn(max = 720.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { createDateSection(screen, component, largeFont) }
                item {
                    Text(
                        stringResource(Res.string.session_templates_section),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                items(screen.templates, key = { it.id.value }) { template ->
                    createTemplateChoice(template, component)
                }
                item { createChecklistSummary(screen) }
            }
        }
    }
    if (screen.occupiedSession != null) AlertDialog(
        onDismissRequest = component::backToToday,
        title = { Text(stringResource(Res.string.session_occupied_title)) },
        text = { Text(stringResource(Res.string.session_occupied_body)) },
        confirmButton = { Button(component::openOccupiedSession) { Text(stringResource(Res.string.session_open_existing)) } },
        dismissButton = { TextButton(component::replaceOccupiedSession) { Text(stringResource(Res.string.session_replace)) } },
    )
}

@Composable
private fun createDateSection(
    screen: SessionComponent.Screen.Create,
    component: SessionComponent,
    largeFont: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(Res.string.session_date_section),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    todayShortcutIcon(BagCueAssets.Calendar)
                    Text(
                        stringResource(Res.string.session_date, screen.date.toString()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (largeFont) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        createDateChip(screen.date == screen.today, Res.string.session_today) {
                            component.selectDate(screen.today)
                        }
                        createDateChip(screen.date == screen.tomorrow, Res.string.session_tomorrow) {
                            component.selectDate(screen.tomorrow)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        createDateChip(
                            selected = screen.date == screen.today,
                            label = Res.string.session_today,
                            modifier = Modifier.weight(1f),
                        ) { component.selectDate(screen.today) }
                        createDateChip(
                            selected = screen.date == screen.tomorrow,
                            label = Res.string.session_tomorrow,
                            modifier = Modifier.weight(1f),
                        ) { component.selectDate(screen.tomorrow) }
                    }
                }
            }
        }
    }
}

@Composable
private fun createDateChip(
    selected: Boolean,
    label: StringResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(label)) },
        leadingIcon = if (selected) {
            { BagCueIcon(BagCueAssets.Check, null, Modifier.size(18.dp)) }
        } else {
            null
        },
        modifier = modifier.sizeIn(minHeight = 48.dp),
    )
}

@Composable
private fun createTemplateChoice(
    template: SessionComponent.TemplateChoice,
    component: SessionComponent,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        color = if (template.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (template.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = shape,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .toggleable(
                    value = template.selected,
                    role = Role.Checkbox,
                    onValueChange = { component.toggleTemplate(template.id) },
                )
                .sizeIn(minHeight = 72.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            todayShortcutIcon(BagCueAssets.Templates)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(template.name.resolve(), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(Res.string.template_positions_count, template.positionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(template.selected, null)
        }
    }
}

@Composable
private fun createChecklistSummary(screen: SessionComponent.Screen.Create) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(Res.string.session_summary_section),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            color = if (screen.validationError == null) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            contentColor = if (screen.validationError == null) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BagCueIcon(BagCueAssets.Bag, null, Modifier.size(24.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(Res.string.session_selected_count, screen.mergedItemCount),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    screen.validationError?.let {
                        Text(stringResource(validationResource(it)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveScreen(screen: SessionComponent.Screen.Active, component: SessionComponent) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val remaining = screen.totalCount - screen.packedCount
    val resolvedTemplateNames = mutableListOf<String>()
    for (name in screen.templateNames) resolvedTemplateNames += name.resolve()
    Scaffold(
        topBar = { TopAppBar(title = { Text(screen.date.toString()) },
                 navigationIcon = { IconButton(component::backToToday) {
                     BagCueIcon(BagCueAssets.Back, stringResource(Res.string.common_back))
                 } }) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().imePadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (largeFont) {
                    Button(component::startAddOneOff, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
                        BagCueIcon(BagCueAssets.Add, null)
                        Text(stringResource(Res.string.session_add_item))
                    }
                }
                Button(component::completeAllPacked,
                     enabled = screen.canCompleteAll && !screen.isSaving,
                     modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.session_complete)) }
                if (screen.canCompleteWithSkipped) OutlinedButton(component::requestCompleteWithSkipped,
                     Modifier.fillMaxWidth()) { Text(stringResource(Res.string.session_complete_skipped,
                             remaining)) }
            }
        },
        floatingActionButton = {
            if (!largeFont) {
                ExtendedFloatingActionButton(
                    onClick = component::startAddOneOff,
                    icon = { BagCueIcon(BagCueAssets.Add, null) },
                    text = { Text(stringResource(Res.string.session_add_item)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = if (largeFont) 24.dp else 88.dp,
            ),
        ) {
            item {
                Text(stringResource(Res.string.session_progress,
                         screen.packedCount,
                         screen.totalCount),
                     style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { if (screen.totalCount == 0) 0f else screen.packedCount.toFloat() / screen.totalCount },
                     modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                if (resolvedTemplateNames.isNotEmpty()) sessionTemplateSummary(resolvedTemplateNames)
                if (screen.undoAvailable) {
                    OutlinedButton(
                        component::undoLastChange,
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) { Text(stringResource(Res.string.session_undo)) }
                }
            }
            screen.groups.forEach { group ->
                item { bagGroupHeader(group.bag) }
                items(group.items, key = { it.id.value }) { item ->
                    ChecklistRow(item, component, Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
    screen.itemEditor?.let { ItemEditorDialog(it, component) }
    screen.oneOffEditor?.let { OneOffDialog(it, component) }
    screen.saveToTemplates?.let { SaveToTemplatesDialog(it, component) }
    screen.skippedConfirmationCount?.let { count -> SkippedDialog(count, component) }
}

@Composable
private fun sessionTemplateSummary(templateNames: List<String>) {
    Text(
        stringResource(Res.string.session_templates),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BagCueIcon(BagCueAssets.Templates, null, Modifier.size(18.dp))
            Text(templateNames.joinToString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun bagGroupHeader(bag: SessionComponent.Bag) {
    Row(
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                BagCueIcon(BagCueAssets.Bag, null, Modifier.size(22.dp))
            }
        }
        Column {
            Text(
                stringResource(Res.string.session_bag_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                groupTitle(bag),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

@Composable
private fun ChecklistRow(
    item: SessionComponent.ChecklistItem,
    component: SessionComponent,
    modifier: Modifier = Modifier,
) {
    val name = item.name.resolve()
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val itemShape = MaterialTheme.shapes.medium
    Column(modifier.fillMaxWidth()) {
        Surface(
            color = if (item.isPacked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (item.isPacked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            shape = itemShape,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .toggleable(
                            value = item.isPacked,
                            role = Role.Checkbox,
                            onValueChange = { component.togglePacked(item.id) },
                        )
                        .sizeIn(minHeight = 64.dp)
                        .semantics(mergeDescendants = true) {
                            testTag = "session-item-${item.id.value}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(item.isPacked, null)
                    Column(Modifier.weight(1f).padding(start = 8.dp, end = 4.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Text(
                                    stringResource(Res.string.common_quantity_compact, item.quantity),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            item.sourceHint?.let {
                                Text(
                                    stringResource(Res.string.session_move_from, it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (!largeFont) {
                    checklistItemActions(item, name, component)
                }
            }
        }
        if (largeFont) {
            largeFontChecklistItemActions(item, component)
        }
    }
}

@Composable
private fun checklistItemActions(
    item: SessionComponent.ChecklistItem,
    name: String,
    component: SessionComponent,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) {
            BagCueIcon(BagCueAssets.More, stringResource(Res.string.session_item_actions, name))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.session_edit_item)) },
                onClick = {
                    expanded = false
                    component.startEditItem(item.id)
                },
                leadingIcon = { BagCueIcon(BagCueAssets.Edit, null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.session_save_templates)) },
                onClick = {
                    expanded = false
                    component.startSaveItemToTemplates(item.id)
                },
                leadingIcon = { BagCueIcon(BagCueAssets.Templates, null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.session_remove_today)) },
                onClick = {
                    expanded = false
                    component.removeItem(item.id)
                },
                leadingIcon = { BagCueIcon(BagCueAssets.Delete, null) },
            )
        }
    }
}

@Composable
private fun largeFontChecklistItemActions(
    item: SessionComponent.ChecklistItem,
    component: SessionComponent,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            { component.startEditItem(item.id) },
            Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) { BagCueIcon(BagCueAssets.Edit, null); Text(stringResource(Res.string.session_edit_item)) }
        TextButton(
            { component.startSaveItemToTemplates(item.id) },
            Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) { Text(stringResource(Res.string.session_save_templates)) }
        TextButton(
            { component.removeItem(item.id) },
            Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) { BagCueIcon(BagCueAssets.Delete, null); Text(stringResource(Res.string.session_remove_today)) }
    }
}

@Composable
private fun ItemEditorDialog(editor: SessionComponent.ItemEditor, component: SessionComponent) {
    AlertDialog(
        onDismissRequest = component::closeItemEdit,
        title = { Text(stringResource(Res.string.session_edit_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(editor.name.resolve(), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.session_bag_label))
            if (editor.bagCandidates.isNotEmpty()) editor.bagCandidates.forEach { candidate ->
                OutlinedButton({ component.chooseBag(candidate) }, Modifier.fillMaxWidth()) { Text(candidate.resolve()) }
            } else OutlinedTextField(editor.bag?.resolve().orEmpty(),
                 { component.chooseBag(it.takeUnless(String::isBlank)?.let(SessionComponent.EditableText::Input)) },
                 label = { Text(stringResource(Res.string.session_bag_label)) },
                 modifier = Modifier.fillMaxWidth())
            OutlinedTextField(editor.source,
                 component::changeSource,
                 label = { Text(stringResource(Res.string.session_source_label)) },
                 modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { Button(component::saveItemEdit, enabled = !editor.isSaving) { Text(stringResource(Res.string.session_save)) } },
        dismissButton = { TextButton(component::closeItemEdit) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
private fun OneOffDialog(editor: SessionComponent.OneOffEditor, component: SessionComponent) {
    AlertDialog(
        onDismissRequest = component::closeOneOffEditor,
        title = { Text(stringResource(Res.string.session_oneoff_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(editor.name,
                 component::changeOneOffName,
                 label = { Text(stringResource(Res.string.session_name_label)) },
                 isError = editor.validationError == SessionComponent.ValidationError.BlankName,
                 modifier = Modifier.fillMaxWidth())
            OutlinedTextField(editor.usualLocation,
                 component::changeOneOffLocation,
                 label = { Text(stringResource(Res.string.session_location_label)) },
                 modifier = Modifier.fillMaxWidth())
            if (editor.validationError == SessionComponent.ValidationError.BlankName) Text(stringResource(Res.string.session_name_required),
                 color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(component::addOneOffItem,
                 enabled = !editor.isSaving) { Text(stringResource(Res.string.session_add_item)) } },
        dismissButton = { TextButton(component::closeOneOffEditor) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
private fun SaveToTemplatesDialog(model: SessionComponent.SaveToTemplates, component: SessionComponent) {
    AlertDialog(
        onDismissRequest = component::closeSaveToTemplates,
        title = { Text(stringResource(Res.string.session_save_to_title)) },
        text = { LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(model.templates, key = { it.id.value }) { template ->
                Row(Modifier.fillMaxWidth().toggleable(template.selected,
                         role = Role.Checkbox,
                         onValueChange = { component.toggleSaveTarget(template.id) }).sizeIn(minHeight = 56.dp),
                     verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(template.selected, null); Text(template.name.resolve(), Modifier.padding(start = 8.dp))
                }
            }
            if (model.validationError == SessionComponent.ValidationError.SelectTemplate) {
                item {
                    Text(
                        stringResource(Res.string.session_save_to_required),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } },
        confirmButton = { Button(component::saveItemToTemplates,
                 enabled = !model.isSaving) { Text(stringResource(Res.string.session_save)) } },
        dismissButton = { TextButton(component::closeSaveToTemplates) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
private fun SkippedDialog(count: Int, component: SessionComponent) = AlertDialog(
    onDismissRequest = component::dismissCompleteWithSkipped,
    title = { Text(stringResource(Res.string.session_skipped_title)) },
    text = { Text(stringResource(Res.string.session_skipped_body, count)) },
    confirmButton = { Button(component::confirmCompleteWithSkipped) { Text(stringResource(Res.string.session_confirm)) } },
    dismissButton = { TextButton(component::dismissCompleteWithSkipped) { Text(stringResource(Res.string.common_cancel)) } },
)

@Composable
@Suppress("LongMethod")
private fun ResultScreen(
    screen: SessionComponent.Screen.Result,
    component: SessionComponent,
    inlineResultAd: @Composable () -> Unit,
) {
    Scaffold { padding -> Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BagCueBrandMark(Modifier.height(48.dp))
                Text(
                    stringResource(if (screen.allPacked) Res.string.session_result_success else Res.string.session_result_skipped),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(Res.string.session_result_date, screen.date.toString()))
            }
        }
        if (!screen.allPacked) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(vertical = 12.dp)) {
            items(screen.skippedItems) {
                Text(stringResource(Res.string.common_name_quantity, it.name.resolve(), it.quantity), Modifier.padding(8.dp))
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(component::backToToday, Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
                Text(stringResource(Res.string.session_done))
            }
            OutlinedButton(
                { component.reopenSession(screen.sessionId) },
                Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { Text(stringResource(Res.string.session_reopen)) }
        }
        when (screen.advertising.status) {
            SessionComponent.ResultAdvertisingStatus.Hidden,
            SessionComponent.ResultAdvertisingStatus.ConsentRequired -> Unit
            SessionComponent.ResultAdvertisingStatus.Loading -> Text(
                stringResource(Res.string.session_ad_loading),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            SessionComponent.ResultAdvertisingStatus.Ready -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.session_ad_label), style = MaterialTheme.typography.labelSmall)
                inlineResultAd()
            }
        }
    } }

    if (screen.advertising.status == SessionComponent.ResultAdvertisingStatus.ConsentRequired) {
        AlertDialog(
            onDismissRequest = { component.chooseResultAdvertisingConsent(AdvertisingConsentChoice.Declined) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BagCueIcon(BagCueAssets.Privacy, null)
                    Text(stringResource(Res.string.session_ad_consent), Modifier.padding(start = 8.dp))
                }
            },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.session_ad_consent_body))
                    if (screen.advertising.privacyPolicyUrl != null) {
                        TextButton(
                            component::openAdvertisingPrivacyPolicy,
                            Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                        ) {
                            Text(stringResource(Res.string.settings_privacy_policy))
                        }
                    }
                }
            },
            confirmButton = {
                Button({ component.chooseResultAdvertisingConsent(AdvertisingConsentChoice.Allowed) }) {
                    Text(stringResource(Res.string.settings_privacy_allow))
                }
            },
            dismissButton = {
                TextButton({ component.chooseResultAdvertisingConsent(AdvertisingConsentChoice.Declined) }) {
                    Text(stringResource(Res.string.session_ad_decline))
                }
            },
        )
    }
}

@Composable
private fun groupTitle(bag: SessionComponent.Bag): String = when (bag) {
    SessionComponent.Bag.Unresolved -> stringResource(Res.string.session_choose_bag)
    SessionComponent.Bag.None -> stringResource(Res.string.session_no_bag)
    is SessionComponent.Bag.Named -> bag.name.resolve()
}

private fun validationResource(error: SessionComponent.ValidationError): StringResource = when (error) {
    SessionComponent.ValidationError.EmptySession -> Res.string.session_empty
    SessionComponent.ValidationError.BlankName -> Res.string.session_name_required
    SessionComponent.ValidationError.SelectTemplate -> Res.string.session_select_template
    SessionComponent.ValidationError.DateInPast -> Res.string.session_date_past
    SessionComponent.ValidationError.BagConflict -> Res.string.session_choose_bag
}

private fun errorResource(error: SessionComponent.ErrorKey): StringResource = when (error) {
    SessionComponent.ErrorKey.LoadFailed -> Res.string.session_load_failed
    SessionComponent.ErrorKey.CreateFailed -> Res.string.session_create_failed
    SessionComponent.ErrorKey.ReplaceFailed -> Res.string.session_replace_failed
    SessionComponent.ErrorKey.SaveFailed -> Res.string.session_save_failed
    SessionComponent.ErrorKey.RemoveFailed -> Res.string.session_remove_failed
    SessionComponent.ErrorKey.UndoFailed -> Res.string.session_undo_failed
    SessionComponent.ErrorKey.CompleteFailed -> Res.string.session_complete_failed
    SessionComponent.ErrorKey.ReopenFailed -> Res.string.session_reopen_failed
    SessionComponent.ErrorKey.SessionNoLongerExists -> Res.string.session_missing
    SessionComponent.ErrorKey.ItemNoLongerExists -> Res.string.session_item_missing
    SessionComponent.ErrorKey.RevisionConflict -> Res.string.session_revision_conflict
}
