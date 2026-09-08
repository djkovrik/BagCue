package com.sedsoftware.bagcue.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.catalog_add
import bagcue.shared.compose.generated.resources.catalog_delete
import bagcue.shared.compose.generated.resources.catalog_edit
import bagcue.shared.compose.generated.resources.catalog_empty_body
import bagcue.shared.compose.generated.resources.catalog_empty_title
import bagcue.shared.compose.generated.resources.catalog_title
import bagcue.shared.compose.generated.resources.catalog_usual_location
import bagcue.shared.compose.generated.resources.delete_body
import bagcue.shared.compose.generated.resources.delete_confirm
import bagcue.shared.compose.generated.resources.delete_failed
import bagcue.shared.compose.generated.resources.delete_title
import bagcue.shared.compose.generated.resources.duplicate_body
import bagcue.shared.compose.generated.resources.duplicate_create_another
import bagcue.shared.compose.generated.resources.duplicate_title
import bagcue.shared.compose.generated.resources.duplicate_use_existing
import bagcue.shared.compose.generated.resources.item_back
import bagcue.shared.compose.generated.resources.item_cancel
import bagcue.shared.compose.generated.resources.item_edit_title
import bagcue.shared.compose.generated.resources.item_load_failed
import bagcue.shared.compose.generated.resources.item_location_label
import bagcue.shared.compose.generated.resources.item_location_support
import bagcue.shared.compose.generated.resources.item_name_label
import bagcue.shared.compose.generated.resources.item_name_required
import bagcue.shared.compose.generated.resources.item_new_title
import bagcue.shared.compose.generated.resources.item_retry
import bagcue.shared.compose.generated.resources.item_save
import bagcue.shared.compose.generated.resources.item_save_failed
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.catalog.integration.CatalogComponentPreview
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.assets.BagCueIcon
import com.sedsoftware.bagcue.compose.theme.AppTheme
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import org.jetbrains.compose.resources.stringResource

private const val LARGE_FONT_SCALE = 1.5f

@Composable
fun CatalogScreen(
    component: CatalogComponent,
    onBack: (() -> Unit)? = null,
) {
    val model by component.model.subscribeAsState()
    val editor = model.editor
    Box(Modifier.fillMaxSize()) {
        if (editor == null) CatalogList(model, component, onBack)
        else CatalogEditor(editor, component)
        DuplicateDialog(model.duplicateMatch, component)
        DeleteDialog(model.deleteConfirmation, component)
        model.error?.let { ErrorSnackbar(it, component::clearError) }
    }
}

@Composable
private fun CatalogList(model: CatalogComponent.Model, component: CatalogComponent, onBack: (() -> Unit)?) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.catalog_title)) },
                navigationIcon = {
                    if (onBack != null) TextButton(onClick = onBack) { BagCueIcon(BagCueAssets.Back,
                             null);
                         Text(stringResource(Res.string.item_back)) }
                },
            )
        },
        bottomBar = {
            if (largeFont) {
                Button(
                    onClick = component::startCreate,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).sizeIn(minHeight = 48.dp),
                ) { BagCueIcon(BagCueAssets.Add, null); Text(stringResource(Res.string.catalog_add)) }
            }
        },
        floatingActionButton = {
            if (!largeFont) {
                ExtendedFloatingActionButton(
                    onClick = component::startCreate,
                ) { BagCueIcon(BagCueAssets.Add, null); Text(stringResource(Res.string.catalog_add)) }
            }
        },
    ) { padding ->
        when {
            model.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            model.items.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.catalog_empty_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.catalog_empty_body), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = if (largeFont) 24.dp else 96.dp),
            ) {
                items(model.items, key = { it.id.value }) { item ->
                    CatalogRow(item, component::startEdit, component::requestDelete)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(
    item: CatalogComponent.Item,
    onEdit: (PackingItemId) -> Unit,
    onDelete: (PackingItemId) -> Unit,
) {
    val name = item.name.resolve()
    if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CatalogItemDescription(item, name)
            TextButton(
                onClick = { onEdit(item.id) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { BagCueIcon(BagCueAssets.Edit, null); Text(stringResource(Res.string.catalog_edit, name)) }
            TextButton(
                onClick = { onDelete(item.id) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { BagCueIcon(BagCueAssets.Delete, null); Text(stringResource(Res.string.catalog_delete, name)) }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                CatalogItemDescription(item, name)
            }
            TextButton(onClick = { onEdit(item.id) }) { BagCueIcon(BagCueAssets.Edit,
                     null);
                 Text(stringResource(Res.string.catalog_edit,
                         name)) }
            TextButton(onClick = { onDelete(item.id) }) { BagCueIcon(BagCueAssets.Delete,
                     null);
                 Text(stringResource(Res.string.catalog_delete,
                         name)) }
        }
    }
}

@Composable
private fun CatalogItemDescription(item: CatalogComponent.Item, name: String) {
    Text(name, style = MaterialTheme.typography.titleMedium)
    item.usualLocation?.let {
        Text(
            stringResource(Res.string.catalog_usual_location, it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CatalogEditor(editor: CatalogComponent.Editor, component: CatalogComponent) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (editor.itemId == null) Res.string.item_new_title else Res.string.item_edit_title,
                        ),
                    )
                },
                navigationIcon = {
                    TextButton(onClick = component::closeEditor) { BagCueIcon(BagCueAssets.Back,
                             null);
                         Text(stringResource(Res.string.item_back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editor.name.resolve(),
                    onValueChange = component::changeName,
                    label = { Text(stringResource(Res.string.item_name_label)) },
                    isError = editor.validationError == CatalogComponent.ValidationError.BlankName,
                    supportingText = {
                        if (editor.validationError == CatalogComponent.ValidationError.BlankName) {
                            Text(stringResource(Res.string.item_name_required))
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editor.usualLocation,
                    onValueChange = component::changeUsualLocation,
                    label = { Text(stringResource(Res.string.item_location_label)) },
                    supportingText = { Text(stringResource(Res.string.item_location_support)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { component.save() }),
                )
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = !editor.isSaving,
                    onClick = component::save,
                ) {
                    if (editor.isSaving) CircularProgressIndicator(Modifier.size(24.dp))
                    else Text(stringResource(Res.string.item_save))
                }
            }
        }
    }
}

@Composable
private fun DuplicateDialog(match: CatalogComponent.Item?, component: CatalogComponent) {
    if (match == null) return
    val name = match.name.resolve()
    AlertDialog(
        onDismissRequest = component::closeEditor,
        title = { Text(stringResource(Res.string.duplicate_title)) },
        text = { Text(stringResource(Res.string.duplicate_body, name)) },
        confirmButton = {
            TextButton(component::useExistingMatch) { Text(stringResource(Res.string.duplicate_use_existing)) }
        },
        dismissButton = {
            TextButton(component::createAnother) { Text(stringResource(Res.string.duplicate_create_another)) }
        },
    )
}

@Composable
private fun DeleteDialog(value: CatalogComponent.DeleteConfirmation?, component: CatalogComponent) {
    if (value == null) return
    AlertDialog(
        onDismissRequest = component::dismissDelete,
        title = { Text(stringResource(Res.string.delete_title)) },
        text = {
            Text(
                stringResource(
                    Res.string.delete_body,
                    value.item.name.resolve(),
                    value.templateDependencies.size,
                    value.unfinishedSessionDependencies.size,
                ),
            )
        },
        confirmButton = {
            TextButton(component::confirmDelete, enabled = !value.isDeleting) {
                Text(stringResource(Res.string.delete_confirm))
            }
        },
        dismissButton = {
            TextButton(component::dismissDelete, enabled = !value.isDeleting) {
                Text(stringResource(Res.string.item_cancel))
            }
        },
    )
}

@Composable
private fun BoxScope.ErrorSnackbar(error: CatalogComponent.ErrorKey, onDismiss: () -> Unit) {
    val resource = when (error) {
        CatalogComponent.ErrorKey.SaveFailed -> Res.string.item_save_failed
        CatalogComponent.ErrorKey.DeleteFailed -> Res.string.delete_failed
        else -> Res.string.item_load_failed
    }
    Snackbar(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        action = { TextButton(onDismiss) { Text(stringResource(Res.string.item_retry)) } },
    ) { Text(stringResource(resource)) }
}

@Preview
@Composable
private fun CatalogPreview() = AppTheme(onThemeChanged = {}) {
    CatalogScreen(CatalogComponentPreview())
}
