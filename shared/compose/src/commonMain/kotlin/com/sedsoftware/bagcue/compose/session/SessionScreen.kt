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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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
import bagcue.shared.compose.generated.resources.session_date
import bagcue.shared.compose.generated.resources.session_date_past
import bagcue.shared.compose.generated.resources.session_done
import bagcue.shared.compose.generated.resources.session_edit_item
import bagcue.shared.compose.generated.resources.session_edit_item_named
import bagcue.shared.compose.generated.resources.session_edit_title
import bagcue.shared.compose.generated.resources.session_empty
import bagcue.shared.compose.generated.resources.session_item_actions
import bagcue.shared.compose.generated.resources.session_item_missing
import bagcue.shared.compose.generated.resources.session_load_failed
import bagcue.shared.compose.generated.resources.session_location_label
import bagcue.shared.compose.generated.resources.session_missing
import bagcue.shared.compose.generated.resources.session_move_from
import bagcue.shared.compose.generated.resources.session_name_label
import bagcue.shared.compose.generated.resources.session_name_required
import bagcue.shared.compose.generated.resources.session_no_bag
import bagcue.shared.compose.generated.resources.session_no_plan
import bagcue.shared.compose.generated.resources.session_occupied_body
import bagcue.shared.compose.generated.resources.session_occupied_title
import bagcue.shared.compose.generated.resources.session_oneoff_title
import bagcue.shared.compose.generated.resources.session_open
import bagcue.shared.compose.generated.resources.session_open_existing
import bagcue.shared.compose.generated.resources.session_plan
import bagcue.shared.compose.generated.resources.session_progress
import bagcue.shared.compose.generated.resources.session_remove_failed
import bagcue.shared.compose.generated.resources.session_remove_today
import bagcue.shared.compose.generated.resources.session_remove_today_named
import bagcue.shared.compose.generated.resources.session_reopen
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
import bagcue.shared.compose.generated.resources.session_skipped_body
import bagcue.shared.compose.generated.resources.session_skipped_title
import bagcue.shared.compose.generated.resources.session_source_label
import bagcue.shared.compose.generated.resources.session_templates
import bagcue.shared.compose.generated.resources.template_positions_count
import bagcue.shared.compose.generated.resources.session_today
import bagcue.shared.compose.generated.resources.session_today_title
import bagcue.shared.compose.generated.resources.session_tomorrow
import bagcue.shared.compose.generated.resources.session_undo
import bagcue.shared.compose.generated.resources.session_undo_failed
import bagcue.shared.compose.generated.resources.settings_privacy_allow
import bagcue.shared.compose.generated.resources.settings_privacy_policy
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.assets.BagCueBrandMark
import com.sedsoftware.bagcue.compose.assets.BagCueIcon
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.session.SessionComponent
import kotlinx.datetime.LocalDate
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
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BagCueBrandMark(Modifier.height(48.dp))
                    Text(
                        screen.date.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        screen.session?.let { session ->
                            stringResource(Res.string.session_progress, session.packedCount, session.totalCount)
                        } ?: stringResource(Res.string.session_no_plan),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            screen.session?.let { session ->
                Button({ component.openSession(session.id) },
                     Modifier.fillMaxWidth()) { BagCueIcon(BagCueAssets.Check,
                         null);
                     Text(stringResource(Res.string.session_open)) }
            } ?: run {
                Button({ component.startCreate(screen.date) },
                     Modifier.fillMaxWidth()) { BagCueIcon(BagCueAssets.Add,
                         null);
                     Text(stringResource(Res.string.session_plan)) }
            }
            OutlinedButton(component::openTemplates, Modifier.fillMaxWidth()) { Text(stringResource(Res.string.session_templates)) }
            OutlinedButton(component::openCatalog, Modifier.fillMaxWidth()) { Text(stringResource(Res.string.session_catalog)) }
        }
    }
}

@Composable
private fun CreateScreen(screen: SessionComponent.Screen.Create, component: SessionComponent) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.session_create_title)) },
                 navigationIcon = { IconButton(component::backToToday) {
                     BagCueIcon(BagCueAssets.Back, stringResource(Res.string.common_back))
                 } }) },
        bottomBar = {
            Button(component::createSession, enabled = !screen.isSaving, modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp)) {
                Text(stringResource(Res.string.session_create))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
             contentPadding = PaddingValues(16.dp),
             verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(Res.string.session_date, screen.date.toString()), style = MaterialTheme.typography.titleLarge) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ component.selectDate(screen.date) },
                         Modifier.fillMaxWidth()) { BagCueIcon(BagCueAssets.Calendar,
                             null);
                         Text(stringResource(Res.string.session_today)) }
                    OutlinedButton({ component.selectDate(LocalDate.fromEpochDays(screen.date.toEpochDays() + 1)) },
                         Modifier.fillMaxWidth()) { Text(stringResource(Res.string.session_tomorrow)) }
                }
            }
            items(screen.templates, key = { it.id.value }) { template ->
                Row(
                    Modifier.fillMaxWidth().toggleable(template.selected,
                         role = Role.Checkbox,
                         onValueChange = { component.toggleTemplate(template.id) }).sizeIn(minHeight = 56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(template.selected, null)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(template.name.resolve(), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.template_positions_count,
                                 template.positionCount),
                             style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
            }
            item { Text(stringResource(Res.string.session_selected_count,
                         screen.mergedItemCount),
                     style = MaterialTheme.typography.titleMedium) }
            screen.validationError?.let { item { Text(stringResource(validationResource(it)), color = MaterialTheme.colorScheme.error) } }
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
                if (resolvedTemplateNames.isNotEmpty()) Text(resolvedTemplateNames.joinToString(),
                     style = MaterialTheme.typography.bodySmall)
                if (screen.undoAvailable) {
                    OutlinedButton(
                        component::undoLastChange,
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) { Text(stringResource(Res.string.session_undo)) }
                }
            }
            screen.groups.forEach { group ->
                item { Row(verticalAlignment = Alignment.CenterVertically) { BagCueIcon(BagCueAssets.Bag,
                             null);
                         Text(groupTitle(group.bag),
                             style = MaterialTheme.typography.titleMedium,
                             modifier = Modifier.padding(top = 18.dp,
                                 bottom = 4.dp).semantics { heading() }) } }
                items(group.items, key = { it.id.value }) { item -> ChecklistRow(item, component) }
            }
        }
    }
    screen.itemEditor?.let { ItemEditorDialog(it, component) }
    screen.oneOffEditor?.let { OneOffDialog(it, component) }
    screen.saveToTemplates?.let { SaveToTemplatesDialog(it, component) }
    screen.skippedConfirmationCount?.let { count -> SkippedDialog(count, component) }
}

@Composable
private fun ChecklistRow(item: SessionComponent.ChecklistItem, component: SessionComponent) {
    val name = item.name.resolve()
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = item.isPacked,
                    role = Role.Checkbox,
                    onValueChange = { component.togglePacked(item.id) },
                )
                .sizeIn(minHeight = 56.dp)
                .semantics(mergeDescendants = true) {
                    testTag = "session-item-${item.id.value}"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(item.isPacked, null)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.common_quantity_compact, item.quantity), style = MaterialTheme.typography.bodySmall)
                item.sourceHint?.let { Text(stringResource(Res.string.session_move_from, it), style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({ component.startEditItem(item.id) }) {
                    BagCueIcon(BagCueAssets.Edit, stringResource(Res.string.session_edit_item_named, name))
                }
                TextButton({ component.startSaveItemToTemplates(item.id) }) { Text(stringResource(Res.string.session_save_templates)) }
                IconButton({ component.removeItem(item.id) }) {
                    BagCueIcon(BagCueAssets.Delete, stringResource(Res.string.session_remove_today_named, name))
                }
            }
        }
        HorizontalDivider()
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
                { component.openSession(screen.sessionId) },
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
    SessionComponent.ErrorKey.SessionNoLongerExists -> Res.string.session_missing
    SessionComponent.ErrorKey.ItemNoLongerExists -> Res.string.session_item_missing
    SessionComponent.ErrorKey.RevisionConflict -> Res.string.session_revision_conflict
}
