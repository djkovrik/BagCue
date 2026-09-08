package com.sedsoftware.bagcue.data.reminder

import com.russhwolf.settings.Settings
import com.sedsoftware.bagcue.domain.reminder.ReminderPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher

data class ReminderDataModuleDependencies(
    val ioDispatcher: CoroutineDispatcher,
    val settings: Settings = Settings(),
)

interface ReminderDataModule {
    val reminderPreferencesRepository: ReminderPreferencesRepository
}

fun ReminderDataModule(dependencies: ReminderDataModuleDependencies): ReminderDataModule =
    DefaultReminderDataModule(dependencies)

private class DefaultReminderDataModule(
    private val dependencies: ReminderDataModuleDependencies,
) : ReminderDataModule {
    override val reminderPreferencesRepository: ReminderPreferencesRepository by lazy {
        SettingsReminderPreferencesRepository(dependencies.settings, dependencies.ioDispatcher)
    }
}
