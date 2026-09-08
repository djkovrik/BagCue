package com.sedsoftware.bagcue.compose.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.common_cancel
import bagcue.shared.compose.generated.resources.common_close
import bagcue.shared.compose.generated.resources.common_delete
import bagcue.shared.compose.generated.resources.template_add_position
import bagcue.shared.compose.generated.resources.template_bag_label
import bagcue.shared.compose.generated.resources.template_bags_summary
import bagcue.shared.compose.generated.resources.template_delete
import bagcue.shared.compose.generated.resources.template_delete_body
import bagcue.shared.compose.generated.resources.template_delete_failed
import bagcue.shared.compose.generated.resources.template_delete_title
import bagcue.shared.compose.generated.resources.template_discard
import bagcue.shared.compose.generated.resources.template_discard_body
import bagcue.shared.compose.generated.resources.template_discard_title
import bagcue.shared.compose.generated.resources.template_duplicate
import bagcue.shared.compose.generated.resources.template_duplicate_failed
import bagcue.shared.compose.generated.resources.template_duplicate_feedback
import bagcue.shared.compose.generated.resources.template_edit_position
import bagcue.shared.compose.generated.resources.template_edit_title
import bagcue.shared.compose.generated.resources.template_empty_draft
import bagcue.shared.compose.generated.resources.template_item_missing
import bagcue.shared.compose.generated.resources.template_keep_editing
import bagcue.shared.compose.generated.resources.template_load_failed
import bagcue.shared.compose.generated.resources.template_missing
import bagcue.shared.compose.generated.resources.template_name_label
import bagcue.shared.compose.generated.resources.template_name_required
import bagcue.shared.compose.generated.resources.template_new_title
import bagcue.shared.compose.generated.resources.template_no_bag
import bagcue.shared.compose.generated.resources.template_no_items
import bagcue.shared.compose.generated.resources.template_open
import bagcue.shared.compose.generated.resources.template_position_save
import bagcue.shared.compose.generated.resources.template_position_summary
import bagcue.shared.compose.generated.resources.template_position_title
import bagcue.shared.compose.generated.resources.template_positions_count
import bagcue.shared.compose.generated.resources.template_quantity_error
import bagcue.shared.compose.generated.resources.template_quantity_label
import bagcue.shared.compose.generated.resources.template_remove_position
import bagcue.shared.compose.generated.resources.template_save
import bagcue.shared.compose.generated.resources.template_save_failed
import bagcue.shared.compose.generated.resources.template_search_items
import bagcue.shared.compose.generated.resources.template_select_item_title
import bagcue.shared.compose.generated.resources.template_source_hint
import bagcue.shared.compose.generated.resources.template_source_label
import bagcue.shared.compose.generated.resources.template_source_support
import bagcue.shared.compose.generated.resources.templates_catalog
import bagcue.shared.compose.generated.resources.templates_create
import bagcue.shared.compose.generated.resources.templates_empty_body
import bagcue.shared.compose.generated.resources.templates_empty_title
import bagcue.shared.compose.generated.resources.templates_title
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.assets.BagCueIcon
import com.sedsoftware.bagcue.templates.TemplateComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val LARGE_FONT_SCALE = 1.5f

@Composable
fun TemplateScreen(component: TemplateComponent) {
    val model by component.model.subscribeAsState()
    val editor = model.editor
    Box(Modifier.fillMaxSize()) {
        if (editor == null) TemplateList(model, component) else TemplateEditor(editor, component)
        model.itemSelector?.let { ItemSelector(it, component) }
        model.deleteConfirmation?.let { DeleteTemplateDialog(it, component) }
        if (model.showDiscardConfirmation) DiscardDialog(component)
        model.duplicateFeedback?.let {
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text(it.resolve()) }
        }
        model.error?.let {
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(errorResource(it).let { key -> stringResource(key) }, Modifier.weight(1f))
                    TextButton(component::clearError) { Text(stringResource(Res.string.common_close)) }
                }
            }
        }
    }
}

@Composable
private fun TemplateList(model: TemplateComponent.Model, component: TemplateComponent) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.templates_title)) },
                actions = { TextButton(component::openCatalog) { Text(stringResource(Res.string.templates_catalog)) } },
            )
        },
        bottomBar = {
            if (largeFont) {
                Button(
                    onClick = component::startCreate,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).sizeIn(minHeight = 48.dp),
                ) {
                    BagCueIcon(BagCueAssets.Add, null)
                    Text(stringResource(Res.string.templates_create))
                }
            }
        },
        floatingActionButton = {
            if (!largeFont) {
                ExtendedFloatingActionButton(onClick = component::startCreate) {
                    BagCueIcon(BagCueAssets.Add, null)
                    Text(stringResource(Res.string.templates_create))
                }
            }
        },
    ) { padding ->
        when {
            model.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            model.templates.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.templates_empty_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.templates_empty_body), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = if (largeFont) 24.dp else 96.dp),
            ) {
                items(model.templates, key = { it.id.value }) { template ->
                    TemplateRow(template, component)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(template: TemplateComponent.TemplateSummary, component: TemplateComponent) {
    val name = template.name.resolve()
    val bagNames = mutableListOf<String>()
    for (bag in template.bagSummary) bagNames += bag.resolve()
    Column(
        Modifier.fillMaxWidth().clickable { component.openTemplate(template.id) }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.template_positions_count, template.positionCount))
        if (template.bagSummary.isNotEmpty()) {
            Text(stringResource(Res.string.template_bags_summary, bagNames.joinToString()))
        }
        TemplateActions(template, component, name)
    }
}

@Composable
private fun TemplateActions(template: TemplateComponent.TemplateSummary, component: TemplateComponent, name: String) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    if (largeFont) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = { component.openTemplate(template.id) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { Text(stringResource(Res.string.template_open, name)) }
            TextButton(
                onClick = { component.duplicateTemplate(template.id) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { BagCueIcon(BagCueAssets.Duplicate, null); Text(stringResource(Res.string.template_duplicate, name)) }
            TextButton(
                onClick = { component.requestDeleteTemplate(template.id) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { BagCueIcon(BagCueAssets.Delete, null); Text(stringResource(Res.string.template_delete, name)) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { component.openTemplate(template.id) }) { Text(stringResource(Res.string.template_open, name)) }
            TextButton(onClick = { component.duplicateTemplate(template.id) }) { BagCueIcon(BagCueAssets.Duplicate,
                     null);
                 Text(stringResource(Res.string.template_duplicate,
                         name)) }
            TextButton(onClick = { component.requestDeleteTemplate(template.id) }) { BagCueIcon(BagCueAssets.Delete,
                     null);
                 Text(stringResource(Res.string.template_delete,
                         name)) }
        }
    }
}

@Composable
private fun TemplateEditor(editor: TemplateComponent.Editor, component: TemplateComponent) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = if (editor.templateId == null) {
                        Res.string.template_new_title
                    } else {
                        Res.string.template_edit_title
                    }
                    Text(stringResource(title))
                },
                navigationIcon = { TextButton(component::requestCloseEditor) { BagCueIcon(BagCueAssets.Back,
                             null);
                         Text(stringResource(Res.string.common_cancel)) } },
            )
        },
        bottomBar = {
            Button(
                onClick = component::saveTemplate,
                enabled = !editor.isSaving,
                modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp),
            ) { Text(stringResource(Res.string.template_save)) }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = editor.name.resolve(),
                    onValueChange = component::changeTemplateName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.template_name_label)) },
                    isError = editor.validationError == TemplateComponent.ValidationError.BlankName,
                    supportingText = if (editor.validationError == TemplateComponent.ValidationError.BlankName) {
                        { Text(stringResource(Res.string.template_name_required)) }
                    } else null,
                    singleLine = false,
                )
            }
            if (editor.positions.isEmpty()) item { Text(stringResource(Res.string.template_empty_draft)) }
            items(editor.positions, key = { it.id.value }) { position -> PositionRow(position, component) }
            item {
                OutlinedButton(component::startAddPosition, Modifier.fillMaxWidth()) {
                    BagCueIcon(BagCueAssets.Add, null)
                    Text(stringResource(Res.string.template_add_position))
                }
            }
        }
    }
    editor.positionEditor?.let { PositionEditorDialog(it, component) }
}

@Composable
private fun PositionRow(position: TemplateComponent.Position, component: TemplateComponent) {
    val name = position.itemName.resolve()
    val bag = position.bag?.resolve() ?: stringResource(Res.string.template_no_bag)
    Column(Modifier.fillMaxWidth().clickable { component.editPosition(position.id) }.padding(vertical = 8.dp)) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.template_position_summary, position.quantity, bag))
        position.source?.let { Text(stringResource(Res.string.template_source_hint, it)) }
        if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { component.editPosition(position.id) },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                ) { BagCueIcon(BagCueAssets.Bag, null); Text(stringResource(Res.string.template_edit_position, name)) }
                TextButton(
                    onClick = { component.removePosition(position.id) },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                ) { Text(stringResource(Res.string.template_remove_position, name)) }
            }
        } else {
            Row {
                TextButton(onClick = { component.editPosition(position.id) }) { BagCueIcon(BagCueAssets.Bag,
                         null);
                     Text(stringResource(Res.string.template_edit_position,
                             name)) }
                TextButton(onClick = { component.removePosition(position.id) }) { Text(stringResource(Res.string.template_remove_position,
                             name)) }
            }
        }
    }
}

@Composable
private fun ItemSelector(selector: TemplateComponent.ItemSelector, component: TemplateComponent) {
    AlertDialog(
        onDismissRequest = component::closeItemSelector,
        title = { Text(stringResource(Res.string.template_select_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selector.query,
                    onValueChange = component::changeItemSearch,
                    label = { Text(stringResource(Res.string.template_search_items)) },
                    leadingIcon = { BagCueIcon(BagCueAssets.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (selector.isEmptyResult) Text(stringResource(Res.string.template_no_items))
                else LazyColumn(Modifier.fillMaxWidth()) {
                    items(selector.items, key = { it.id.value }) { item ->
                        TextButton(onClick = { component.selectItem(item.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(item.name.resolve(), Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(component::closeItemSelector) { Text(stringResource(Res.string.common_close)) } },
    )
}

@Composable
private fun PositionEditorDialog(editor: TemplateComponent.PositionEditor, component: TemplateComponent) {
    AlertDialog(
        onDismissRequest = component::closePositionEditor,
        title = { Text(stringResource(Res.string.template_position_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(editor.itemName.resolve(), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = editor.quantityInput,
                    onValueChange = component::changePositionQuantity,
                    label = { Text(stringResource(Res.string.template_quantity_label)) },
                    isError = editor.validationError == TemplateComponent.ValidationError.QuantityOutOfRange,
                    supportingText = if (editor.validationError == TemplateComponent.ValidationError.QuantityOutOfRange) {
                        { Text(stringResource(Res.string.template_quantity_error)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.bag?.resolve().orEmpty(),
                    onValueChange = component::changePositionBag,
                    label = { Text(stringResource(Res.string.template_bag_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.sourceInput,
                    onValueChange = component::changePositionSource,
                    label = { Text(stringResource(Res.string.template_source_label)) },
                    supportingText = { Text(stringResource(Res.string.template_source_support)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { component.savePosition() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(component::savePosition) { Text(stringResource(Res.string.template_position_save)) } },
        dismissButton = { TextButton(component::closePositionEditor) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
private fun DeleteTemplateDialog(confirmation: TemplateComponent.DeleteConfirmation, component: TemplateComponent) {
    val name = confirmation.template.name.resolve()
    AlertDialog(
        onDismissRequest = component::dismissDeleteTemplate,
        title = { Text(stringResource(Res.string.template_delete_title)) },
        text = { Text(stringResource(Res.string.template_delete_body, name)) },
        confirmButton = {
            Button(component::confirmDeleteTemplate, enabled = !confirmation.isDeleting) {
                Text(stringResource(Res.string.common_delete))
            }
        },
        dismissButton = { TextButton(component::dismissDeleteTemplate) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
private fun DiscardDialog(component: TemplateComponent) {
    AlertDialog(
        onDismissRequest = component::dismissDiscardChanges,
        title = { Text(stringResource(Res.string.template_discard_title)) },
        text = { Text(stringResource(Res.string.template_discard_body)) },
        confirmButton = { Button(component::confirmDiscardChanges) { Text(stringResource(Res.string.template_discard)) } },
        dismissButton = { TextButton(component::dismissDiscardChanges) { Text(stringResource(Res.string.template_keep_editing)) } },
    )
}

private fun errorResource(error: TemplateComponent.ErrorKey): StringResource = when (error) {
    TemplateComponent.ErrorKey.LoadFailed -> Res.string.template_load_failed
    TemplateComponent.ErrorKey.SaveFailed -> Res.string.template_save_failed
    TemplateComponent.ErrorKey.DuplicateFailed -> Res.string.template_duplicate_failed
    TemplateComponent.ErrorKey.DeleteFailed -> Res.string.template_delete_failed
    TemplateComponent.ErrorKey.TemplateNoLongerExists -> Res.string.template_missing
    TemplateComponent.ErrorKey.ItemNoLongerExists -> Res.string.template_item_missing
}
