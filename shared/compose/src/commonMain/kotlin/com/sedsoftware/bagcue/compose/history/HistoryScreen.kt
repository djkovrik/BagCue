package com.sedsoftware.bagcue.compose.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.common_cancel
import bagcue.shared.compose.generated.resources.common_retry
import bagcue.shared.compose.generated.resources.history_action_failed
import bagcue.shared.compose.generated.resources.history_clear_filter
import bagcue.shared.compose.generated.resources.history_completed
import bagcue.shared.compose.generated.resources.history_delete
import bagcue.shared.compose.generated.resources.history_delete_body
import bagcue.shared.compose.generated.resources.history_delete_title
import bagcue.shared.compose.generated.resources.history_deleted
import bagcue.shared.compose.generated.resources.history_empty
import bagcue.shared.compose.generated.resources.history_filtered_empty
import bagcue.shared.compose.generated.resources.history_hide_calendar
import bagcue.shared.compose.generated.resources.history_more
import bagcue.shared.compose.generated.resources.history_open
import bagcue.shared.compose.generated.resources.history_planned
import bagcue.shared.compose.generated.resources.history_progress
import bagcue.shared.compose.generated.resources.history_read_failed
import bagcue.shared.compose.generated.resources.history_reopen
import bagcue.shared.compose.generated.resources.history_repeat
import bagcue.shared.compose.generated.resources.history_show_calendar
import bagcue.shared.compose.generated.resources.history_title
import bagcue.shared.compose.generated.resources.history_undo
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.assets.BagCueIcon
import com.sedsoftware.bagcue.history.HistoryComponent
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val DAY_MILLIS = 86_400_000L
private const val LARGE_FONT_SCALE = 1.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(component: HistoryComponent) {
    val model by component.model.subscribeAsState()
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val snackbar = remember { SnackbarHostState() }
    val deleted = stringResource(Res.string.history_deleted)
    val undo = stringResource(Res.string.history_undo)
    LaunchedEffect(model.undoAvailable) {
        if (model.undoAvailable && snackbar.showSnackbar(deleted, undo).name == "ActionPerformed") {
            component.undoDelete()
        }
    }

    model.deleteConfirmation?.let { row ->
        AlertDialog(
            onDismissRequest = component::dismissDelete,
            title = { Text(stringResource(Res.string.history_delete_title)) },
            text = { Text(stringResource(Res.string.history_delete_body, row.date.toString())) },
            confirmButton = {
                TextButton(onClick = component::confirmDelete) {
                    BagCueIcon(BagCueAssets.Delete, null)
                    Text(stringResource(Res.string.history_delete))
                }
            },
            dismissButton = { TextButton(onClick = component::dismissDelete) { Text(stringResource(Res.string.common_cancel)) } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.history_title)) },
                actions = {
                    calendarAction(model.isCalendarExpanded, largeFont, component::toggleCalendar)
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (model.isCalendarExpanded) HistoryCalendar(model.selectedDate, component::selectDate)
            if (model.selectedDate != null) {
                TextButton(onClick = component::clearDateFilter) { Text(stringResource(Res.string.history_clear_filter)) }
            }
            when {
                model.error != null -> ErrorState(model.error!!, component::refresh)
                model.isFilteredEmpty -> EmptyState(stringResource(Res.string.history_filtered_empty))
                model.isEmpty -> EmptyState(stringResource(Res.string.history_empty))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (model.planned.isNotEmpty()) {
                        item { SectionTitle(stringResource(Res.string.history_planned)) }
                        items(model.planned, key = { it.id.value }) { row -> HistoryRow(row, component) }
                    }
                    if (model.completed.isNotEmpty()) {
                        item { SectionTitle(stringResource(Res.string.history_completed)) }
                        items(model.completed, key = { it.id.value }) { row -> HistoryRow(row, component) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCalendar(selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected?.toEpochDays()?.toLong()?.times(DAY_MILLIS),
    )
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { onSelect(LocalDate.fromEpochDays((it / DAY_MILLIS).toInt())) }
    }
    DatePicker(state = state, modifier = Modifier.fillMaxWidth().semantics { testTag = "history_calendar" })
}

@Composable
private fun HistoryRow(row: HistoryComponent.SessionRow, component: HistoryComponent) {
    val names = row.templateNames.map { it.resolve() }.joinToString()
    Surface(
        onClick = { component.openSession(row.id) },
        modifier = Modifier.fillMaxWidth().semantics { testTag = "history_${row.id.value}" },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).sizeIn(minHeight = 96.dp)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                historyIcon(row.isCompleted)
                historyDescription(row, names, Modifier.weight(1f))
            }
            historyActions(row, component)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun historyIcon(completed: Boolean) {
    Surface(
        color = if (completed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (completed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            BagCueIcon(BagCueAssets.Calendar, null, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun historyDescription(
    row: HistoryComponent.SessionRow,
    templateNames: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(row.date.toString(), style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                stringResource(Res.string.history_progress, row.packedCount, row.totalCount),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (templateNames.isNotBlank()) {
            Text(
                templateNames,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        LinearProgressIndicator(
            progress = { if (row.totalCount == 0) 0f else row.packedCount.toFloat() / row.totalCount },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun historyActions(row: HistoryComponent.SessionRow, component: HistoryComponent) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            BagCueIcon(BagCueAssets.More, stringResource(Res.string.history_more, row.date.toString()))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.history_open)) },
                onClick = {
                    expanded = false
                    component.openSession(row.id)
                },
            )
            if (row.isCompleted) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.history_reopen)) },
                    onClick = {
                        expanded = false
                        component.reopenSession(row.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.history_repeat)) },
                    onClick = {
                        expanded = false
                        component.repeatSession(row.id)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.history_delete)) },
                onClick = {
                    expanded = false
                    component.requestDelete(row.id)
                },
                leadingIcon = { BagCueIcon(BagCueAssets.Delete, null) },
            )
        }
    }
}

@Composable
private fun calendarAction(expanded: Boolean, compact: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (expanded) Res.string.history_hide_calendar else Res.string.history_show_calendar)
    if (compact) {
        IconButton(onClick = onClick) { BagCueIcon(BagCueAssets.Calendar, label) }
    } else {
        TextButton(onClick = onClick) {
            BagCueIcon(BagCueAssets.Calendar, null)
            Text(label)
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(error: HistoryComponent.ErrorKey, retry: () -> Unit) {
    val text = if (error == HistoryComponent.ErrorKey.LoadFailed) {
        Res.string.history_read_failed
    } else {
        Res.string.history_action_failed
    }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(text))
        Spacer(Modifier.height(12.dp))
        Button(onClick = retry) { Text(stringResource(Res.string.common_retry)) }
    }
}
