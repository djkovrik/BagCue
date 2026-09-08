package com.sedsoftware.bagcue.compose.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(component: HistoryComponent) {
    val model by component.model.subscribeAsState()
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
                    TextButton(onClick = component::toggleCalendar) {
                        BagCueIcon(BagCueAssets.Calendar, null)
                        val label = if (model.isCalendarExpanded) {
                            Res.string.history_hide_calendar
                        } else {
                            Res.string.history_show_calendar
                        }
                        Text(stringResource(label))
                    }
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
                else -> LazyColumn(Modifier.fillMaxSize()) {
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
    Column(
        Modifier.fillMaxWidth().clickable(role = Role.Button) { component.openSession(row.id) }
            .padding(horizontal = 20.dp, vertical = 14.dp).semantics { testTag = "history_${row.id.value}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.date.toString(), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.history_progress, row.packedCount, row.totalCount), style = MaterialTheme.typography.labelMedium)
        }
        if (names.isNotBlank()) Text(names, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        LinearProgressIndicator(
            progress = { if (row.totalCount == 0) 0f else row.packedCount.toFloat() / row.totalCount },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { component.openSession(row.id) }) { Text(stringResource(Res.string.history_open)) }
            if (row.isCompleted) {
                TextButton(onClick = { component.reopenSession(row.id) }) { Text(stringResource(Res.string.history_reopen)) }
                TextButton(onClick = { component.repeatSession(row.id) }) { Text(stringResource(Res.string.history_repeat)) }
            }
            TextButton(onClick = { component.requestDelete(row.id) }) { BagCueIcon(BagCueAssets.More,
                     null);
                 Text(stringResource(Res.string.history_delete)) }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))
}

@Composable
private fun EmptyState(text: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text(text, style = MaterialTheme.typography.headlineSmall)
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
