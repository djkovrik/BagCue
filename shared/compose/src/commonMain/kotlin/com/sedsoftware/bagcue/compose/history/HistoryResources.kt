package com.sedsoftware.bagcue.compose.history

import androidx.compose.runtime.Composable
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.starter_template_office
import bagcue.shared.compose.generated.resources.starter_template_pool
import com.sedsoftware.bagcue.history.HistoryComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val historyResources: Map<String, StringResource> = mapOf(
    "starter_template_office" to Res.string.starter_template_office,
    "starter_template_pool" to Res.string.starter_template_pool,
)

@Composable
internal fun HistoryComponent.UserText.resolve(): String = when (this) {
    is HistoryComponent.UserText.Authored -> value
    is HistoryComponent.UserText.Resource -> stringResource(historyResources.getValue(key))
}
