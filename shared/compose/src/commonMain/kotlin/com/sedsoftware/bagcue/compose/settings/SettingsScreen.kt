package com.sedsoftware.bagcue.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.common_retry
import bagcue.shared.compose.generated.resources.settings_disable_all
import bagcue.shared.compose.generated.resources.settings_about
import bagcue.shared.compose.generated.resources.settings_analytics
import bagcue.shared.compose.generated.resources.settings_analytics_id_active
import bagcue.shared.compose.generated.resources.settings_analytics_support
import bagcue.shared.compose.generated.resources.settings_evening
import bagcue.shared.compose.generated.resources.settings_evening_support
import bagcue.shared.compose.generated.resources.settings_load_failed
import bagcue.shared.compose.generated.resources.settings_morning
import bagcue.shared.compose.generated.resources.settings_morning_support
import bagcue.shared.compose.generated.resources.settings_open_system
import bagcue.shared.compose.generated.resources.settings_permission_denied
import bagcue.shared.compose.generated.resources.settings_permission_unavailable
import bagcue.shared.compose.generated.resources.settings_reminders
import bagcue.shared.compose.generated.resources.settings_privacy
import bagcue.shared.compose.generated.resources.settings_privacy_allow
import bagcue.shared.compose.generated.resources.settings_privacy_allowed
import bagcue.shared.compose.generated.resources.settings_privacy_decline
import bagcue.shared.compose.generated.resources.settings_privacy_declined
import bagcue.shared.compose.generated.resources.settings_privacy_not_required
import bagcue.shared.compose.generated.resources.settings_privacy_policy
import bagcue.shared.compose.generated.resources.settings_privacy_refresh
import bagcue.shared.compose.generated.resources.settings_privacy_required
import bagcue.shared.compose.generated.resources.settings_privacy_unresolved
import bagcue.shared.compose.generated.resources.settings_save_failed
import bagcue.shared.compose.generated.resources.settings_saved_immediately
import bagcue.shared.compose.generated.resources.settings_title
import bagcue.shared.compose.generated.resources.settings_time_earlier
import bagcue.shared.compose.generated.resources.settings_time_later
import bagcue.shared.compose.generated.resources.settings_version
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bagcue.compose.assets.BagCueAssets
import com.sedsoftware.bagcue.compose.assets.BagCueBrandMark
import com.sedsoftware.bagcue.compose.assets.BagCueIcon
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.settings.SettingsComponent
import org.jetbrains.compose.resources.stringResource

private const val REMINDER_TIME_SHIFT_MINUTES = 15

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val model by component.model.subscribeAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.settings_title)) },
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BagCueBrandMark(Modifier.height(48.dp).align(Alignment.CenterHorizontally))
            Text(stringResource(Res.string.settings_reminders), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.settings_saved_immediately), style = MaterialTheme.typography.bodySmall)
            model.error?.let {
                Column(
                    Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(
                            if (it == SettingsComponent.Error.LoadFailed) {
                                Res.string.settings_load_failed
                            } else {
                                Res.string.settings_save_failed
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(component::refresh) { Text(stringResource(Res.string.common_retry)) }
                }
            }
            ReminderRow(model.evening,
                 stringResource(Res.string.settings_evening),
                 stringResource(Res.string.settings_evening_support),
                 component)
            ReminderRow(model.morning,
                 stringResource(Res.string.settings_morning),
                 stringResource(Res.string.settings_morning_support),
                 component)
            OutlinedButton(component::disableAll, Modifier.fillMaxWidth(), enabled = !model.isSaving) {
                Text(stringResource(Res.string.settings_disable_all))
            }
            when (model.notificationCapability) {
                SettingsComponent.NotificationCapability.Denied -> CapabilityNotice(stringResource(Res.string.settings_permission_denied),
                     component)
                SettingsComponent.NotificationCapability.Unavailable -> CapabilityNotice(
                    stringResource(Res.string.settings_permission_unavailable),
                    component,
                )
                SettingsComponent.NotificationCapability.Unknown -> Button(
                    component::requestNotificationPermission,
                    Modifier.fillMaxWidth(),
                    enabled = !model.isSaving,
                ) { Text(stringResource(Res.string.settings_open_system)) }
                SettingsComponent.NotificationCapability.Available -> Unit
            }
            Text(stringResource(Res.string.settings_analytics), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            AnalyticsRow(model, component)
            Text(stringResource(Res.string.settings_privacy), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            PrivacyControls(model.advertisingPrivacy, component)
            if (model.privacyPolicyUrl != null) {
                TextButton(
                    component::openPrivacyPolicy,
                    Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                ) { Text(stringResource(Res.string.settings_privacy_policy)) }
            }
            Text(stringResource(Res.string.settings_about), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.settings_version, model.about.versionName))
            if (model.isLoading || model.isSaving) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun PrivacyControls(privacy: SettingsComponent.AdvertisingPrivacy, component: SettingsComponent) {
    val message = when (privacy.status) {
        SettingsComponent.AdvertisingPrivacyStatus.Unresolved -> Res.string.settings_privacy_unresolved
        SettingsComponent.AdvertisingPrivacyStatus.NotRequired -> Res.string.settings_privacy_not_required
        SettingsComponent.AdvertisingPrivacyStatus.ConsentRequired -> Res.string.settings_privacy_required
        SettingsComponent.AdvertisingPrivacyStatus.Allowed -> Res.string.settings_privacy_allowed
        SettingsComponent.AdvertisingPrivacyStatus.Declined -> Res.string.settings_privacy_declined
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BagCueIcon(BagCueAssets.Privacy, null)
            Text(stringResource(message), Modifier.weight(1f).padding(start = 8.dp))
        }
        if (privacy.choiceAvailable) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ component.chooseAdvertisingConsent(AdvertisingConsentChoice.Allowed) },
                 Modifier.fillMaxWidth()) { Text(stringResource(Res.string.settings_privacy_allow)) }
            OutlinedButton({ component.chooseAdvertisingConsent(AdvertisingConsentChoice.Declined) },
                 Modifier.fillMaxWidth()) { Text(stringResource(Res.string.settings_privacy_decline)) }
        }
        TextButton(
            component::refreshAdvertisingPrivacy,
            Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) { Text(stringResource(Res.string.settings_privacy_refresh)) }
    }
}

@Composable
private fun AnalyticsRow(model: SettingsComponent.Model, component: SettingsComponent) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val modifier = Modifier
        .fillMaxWidth()
        .sizeIn(minHeight = 56.dp)
        .toggleable(
            value = model.analytics.enabled,
            enabled = !model.isSaving,
            role = Role.Switch,
            onValueChange = component::setAnalyticsEnabled,
        )
    if (largeFont) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsDescription(model)
            Switch(
                model.analytics.enabled,
                null,
                Modifier.align(Alignment.End),
                enabled = !model.isSaving,
            )
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { AnalyticsDescription(model) }
            Switch(model.analytics.enabled, null, enabled = !model.isSaving)
        }
    }
}

@Composable
private fun AnalyticsDescription(model: SettingsComponent.Model) {
    Text(stringResource(Res.string.settings_analytics_support), style = MaterialTheme.typography.bodyMedium)
    if (model.analytics.hasAppInstanceId) {
        Text(stringResource(Res.string.settings_analytics_id_active), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReminderRow(
    reminder: SettingsComponent.Reminder,
    title: String,
    supporting: String,
    component: SettingsComponent,
) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val toggleModifier = Modifier
        .fillMaxWidth()
        .sizeIn(minHeight = 56.dp)
        .toggleable(
            value = reminder.enabled,
            enabled = !component.model.value.isSaving,
            role = Role.Switch,
            onValueChange = { component.setEnabled(reminder.kind, it) },
        )
    Column(
        Modifier.fillMaxWidth().sizeIn(minHeight = 72.dp).semantics { testTag = "reminder_${reminder.kind.name.lowercase()}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (largeFont) {
            Column(toggleModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    BagCueIcon(BagCueAssets.Notifications, null)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(supporting, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Switch(
                    reminder.enabled,
                    null,
                    Modifier.align(Alignment.End),
                    enabled = !component.model.value.isSaving,
                )
            }
        } else {
            Row(toggleModifier, verticalAlignment = Alignment.CenterVertically) {
                BagCueIcon(BagCueAssets.Notifications, null)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(supporting, style = MaterialTheme.typography.bodySmall)
                }
                Switch(reminder.enabled, null, enabled = !component.model.value.isSaving)
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(reminder.time.toString(), style = MaterialTheme.typography.titleMedium)
            TextButton(
                { component.setTime(reminder.kind, reminder.time.shift(-REMINDER_TIME_SHIFT_MINUTES)) },
                Modifier.fillMaxWidth(),
                enabled = reminder.enabled,
                colors = ButtonDefaults.textButtonColors(
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                ),
            ) {
                Text(stringResource(Res.string.settings_time_earlier))
            }
            TextButton(
                { component.setTime(reminder.kind, reminder.time.shift(REMINDER_TIME_SHIFT_MINUTES)) },
                Modifier.fillMaxWidth(),
                enabled = reminder.enabled,
                colors = ButtonDefaults.textButtonColors(
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                ),
            ) {
                Text(stringResource(Res.string.settings_time_later))
            }
        }
    }
}

@Composable
private fun CapabilityNotice(text: String, component: SettingsComponent) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, color = MaterialTheme.colorScheme.error)
        Button(component::requestNotificationPermission, Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.settings_open_system))
        }
    }
}

private fun ReminderLocalTime.shift(minutes: Int): ReminderLocalTime {
    val value = (minuteOfDay + minutes).mod(
        ReminderLocalTime.HOURS_PER_DAY * ReminderLocalTime.MINUTES_PER_HOUR,
    )
    return ReminderLocalTime.of(
        value / ReminderLocalTime.MINUTES_PER_HOUR,
        value % ReminderLocalTime.MINUTES_PER_HOUR,
    )
}
