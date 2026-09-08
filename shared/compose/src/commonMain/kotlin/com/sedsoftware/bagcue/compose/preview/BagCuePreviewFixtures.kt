package com.sedsoftware.bagcue.compose.preview

import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.catalog.PackingItemId
import com.sedsoftware.bagcue.domain.reminder.ReminderKind
import com.sedsoftware.bagcue.domain.reminder.ReminderLocalTime
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.TemplatePositionId
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.templates.TemplateComponent
import kotlinx.datetime.LocalDate

internal object BagCuePreviewFixtures {
    private const val OFFICE_POSITION_COUNT = 7
    private const val POOL_POSITION_COUNT = 8
    private const val EVENING_HOUR = 20
    private const val MORNING_HOUR = 7
    private const val MORNING_MINUTE = 30
    val today = LocalDate(2026, 9, 8)
    val tomorrow = LocalDate(2026, 9, 9)
    val future = LocalDate(2026, 9, 12)

    private val sessionId = PackingSessionId("preview-session-2026-09-08")
    private val officeId = KitTemplateId("starter-office")
    private val poolId = KitTemplateId("starter-pool")
    private val laptopId = PackingItemId("starter-laptop")
    private val chargerId = PackingItemId("starter-charger")
    private val gogglesId = PackingItemId("starter-goggles")

    fun today(
        summary: SessionComponent.SessionSummary? = null,
        isLoading: Boolean = false,
        error: SessionComponent.ErrorKey? = null,
    ) = SessionComponent.Model(SessionComponent.Screen.Today(today, summary), isLoading, error)

    fun todaySummary(
        date: LocalDate = today,
        packed: Int = 3,
        total: Int = 8,
        completed: Boolean = false,
    ) = SessionComponent.SessionSummary(
        id = sessionId,
        date = date,
        templateNames = listOf(
            SessionComponent.UserText.Resource("starter_template_office"),
            SessionComponent.UserText.Resource("starter_template_pool"),
        ),
        packedCount = packed,
        totalCount = total,
        isCompleted = completed,
    )

    fun create(
        selected: Boolean = false,
        occupied: Boolean = false,
        saving: Boolean = false,
        validation: SessionComponent.ValidationError? = null,
        error: SessionComponent.ErrorKey? = null,
    ) = SessionComponent.Model(
        screen = SessionComponent.Screen.Create(
            date = tomorrow,
            templates = listOf(
                SessionComponent.TemplateChoice(
                    officeId,
                    SessionComponent.UserText.Resource("starter_template_office"),
                    OFFICE_POSITION_COUNT,
                    selected,
                ),
                SessionComponent.TemplateChoice(
                    poolId,
                    SessionComponent.UserText.Resource("starter_template_pool"),
                    POOL_POSITION_COUNT,
                    selected,
                ),
            ),
            mergedItemCount = if (selected) 14 else 0,
            occupiedSession = if (occupied) todaySummary(date = tomorrow) else null,
            isSaving = saving,
            validationError = validation,
        ),
        isLoading = false,
        error = error,
    )

    fun active(
        allPacked: Boolean = false,
        conflict: Boolean = false,
        undo: Boolean = false,
        skippedConfirmation: Int? = null,
        error: SessionComponent.ErrorKey? = null,
    ): SessionComponent.Model {
        val items = listOf(
            checklist(laptopId.value, "starter_item_laptop", packed = true, source = "Desk drawer"),
            checklist(chargerId.value, "starter_item_charger", packed = allPacked, source = "Office shelf"),
            checklist(gogglesId.value, "starter_item_goggles", packed = allPacked, conflict = conflict),
            SessionComponent.ChecklistItem(
                id = SessionPackingItemId("preview-long-user-item"),
                name = SessionComponent.UserText.Authored("Очень длинное пользовательское название вещи для вечерней тренировки"),
                quantity = 2,
                sourceHint = "Верхняя полка шкафа рядом с запасными полотенцами",
                isPacked = allPacked,
                hasBagConflict = false,
            ),
        )
        return SessionComponent.Model(
            screen = SessionComponent.Screen.Active(
                sessionId = sessionId,
                date = today,
                templateNames = listOf(SessionComponent.UserText.Resource("starter_template_office"),
                     SessionComponent.UserText.Resource("starter_template_pool")),
                groups = listOf(
                    SessionComponent.BagGroup(
                        if (conflict) {
                            SessionComponent.Bag.Unresolved
                        } else {
                            SessionComponent.Bag.Named(
                                SessionComponent.UserText.Resource("starter_bag_backpack"),
                            )
                        },
                        items,
                    ),
                ),
                packedCount = items.count { it.isPacked },
                totalCount = items.size,
                canCompleteAll = allPacked && !conflict,
                canCompleteWithSkipped = !allPacked && !conflict,
                itemEditor = null,
                oneOffEditor = null,
                saveToTemplates = null,
                undoAvailable = undo,
                skippedConfirmationCount = skippedConfirmation,
                isSaving = false,
            ),
            isLoading = false,
            error = error,
        )
    }

    fun result(
        allPacked: Boolean = true,
        advertising: SessionComponent.ResultAdvertisingStatus = SessionComponent.ResultAdvertisingStatus.Hidden,
        choiceAvailable: Boolean = false,
        error: SessionComponent.ErrorKey? = null,
    ) = SessionComponent.Model(
        screen = SessionComponent.Screen.Result(
            sessionId = sessionId,
            date = today,
            allPacked = allPacked,
            skippedItems = if (allPacked) emptyList() else listOf(
                SessionComponent.ResultItem(
                    name = SessionComponent.UserText.Authored("Запасная бутылка воды для долгой дороги"),
                    quantity = 1,
                    bag = SessionComponent.Bag.None,
                ),
            ),
            advertising = SessionComponent.ResultAdvertising(
                status = advertising,
                choiceAvailable = choiceAvailable,
                privacyPolicyUrl = "https://example.invalid/privacy",
            ),
        ),
        isLoading = false,
        error = error,
    )

    fun history(
        empty: Boolean = false,
        filteredEmpty: Boolean = false,
        calendar: Boolean = false,
        deleting: Boolean = false,
        undo: Boolean = false,
        error: HistoryComponent.ErrorKey? = null,
    ): HistoryComponent.Model {
        val planned = HistoryComponent.SessionRow(
            PackingSessionId("planned-preview"), future,
            listOf(HistoryComponent.UserText.Resource("starter_template_office")), 2, 7, false, 0,
        )
        val completed = HistoryComponent.SessionRow(
            PackingSessionId("completed-preview"), today,
            listOf(HistoryComponent.UserText.Resource("starter_template_pool")), 8, 8, true, 0,
        )
        return HistoryComponent.Model(
            planned = if (empty) emptyList() else listOf(planned),
            completed = if (empty) emptyList() else listOf(completed),
            selectedDate = if (filteredEmpty) tomorrow else null,
            isCalendarExpanded = calendar,
            isLoading = false,
            isEmpty = empty && !filteredEmpty,
            isFilteredEmpty = filteredEmpty,
            deleteConfirmation = if (deleting) completed else null,
            undoAvailable = undo,
            error = error,
        )
    }

    fun templates(
        empty: Boolean = false,
        long: Boolean = false,
        editor: TemplateComponent.Editor? = null,
        duplicate: Boolean = false,
        deleting: Boolean = false,
        error: TemplateComponent.ErrorKey? = null,
    ): TemplateComponent.Model {
        val rows = if (empty) emptyList() else templateRows(long)
        return TemplateComponent.Model(
            templates = rows,
            isLoading = false,
            editor = editor,
            itemSelector = null,
            deleteConfirmation = if (deleting) TemplateComponent.DeleteConfirmation(rows.first(), false) else null,
            showDiscardConfirmation = false,
            duplicateFeedback = if (duplicate) TemplateComponent.UserText.Authored("Office copy") else null,
            error = error,
        )
    }

    fun templateEditor(
        new: Boolean = false,
        long: Boolean = false,
        error: TemplateComponent.ErrorKey? = null,
        validation: TemplateComponent.ValidationError? = null,
    ) = templates(
        editor = TemplateComponent.Editor(
            templateId = if (new) null else officeId,
            name = if (new) TemplateComponent.EditableText.Input("") else TemplateComponent.EditableText.Input(
                if (long) "Очень длинный пользовательский шаблон для офиса, бассейна и вечерней поездки" else "Office + Pool",
            ),
            positions = if (new) emptyList() else templatePositions(long),
            positionEditor = if (long) TemplateComponent.PositionEditor(
                positionId = TemplatePositionId("position-laptop"),
                itemName = TemplateComponent.UserText.Resource("starter_item_laptop"),
                quantityInput = "100",
                bag = TemplateComponent.EditableText.Input("Большой спортивный рюкзак"),
                sourceInput = "Верхняя полка рабочего шкафа",
                inheritedUsualLocation = "Desk",
                validationError = validation,
            ) else null,
            isSaving = false,
            hasUnsavedChanges = long,
            validationError = validation,
        ),
        error = error,
    )

    fun catalog(
        empty: Boolean = false,
        editor: CatalogComponent.Editor? = null,
        duplicate: Boolean = false,
        deleting: Boolean = false,
        error: CatalogComponent.ErrorKey? = null,
    ): CatalogComponent.Model {
        val items = if (empty) emptyList() else catalogItems()
        return CatalogComponent.Model(
            items = items,
            isLoading = false,
            editor = editor,
            duplicateMatch = if (duplicate) items.first() else null,
            deleteConfirmation = if (deleting) CatalogComponent.DeleteConfirmation(
                item = items.first(),
                templateDependencies = listOf(CatalogComponent.Dependency("office",
                         CatalogComponent.UserText.Resource("starter_template_office"))),
                unfinishedSessionDependencies = listOf(CatalogComponent.Dependency("today",
                         CatalogComponent.UserText.Authored("Today · Office + Pool"))),
                isDeleting = false,
            ) else null,
            error = error,
        )
    }

    fun catalogEditor(
        existing: Boolean = false,
        long: Boolean = false,
        invalid: Boolean = false,
        duplicate: Boolean = false,
        error: CatalogComponent.ErrorKey? = null,
    ) = catalog(
        editor = CatalogComponent.Editor(
            itemId = if (existing) laptopId else null,
            name = CatalogComponent.EditableName.Input(
                when {
                    long -> "Очень длинное название пользовательской вещи для ежедневных поездок"
                    invalid -> ""
                    else -> "Notebook"
                },
            ),
            usualLocation = if (long) "Нижний ящик у входной двери рядом с запасной сумкой" else "Hall shelf",
            isSaving = false,
            validationError = if (invalid) CatalogComponent.ValidationError.BlankName else null,
        ),
        duplicate = duplicate,
        error = error,
    )

    fun settings(
        reminders: Boolean = false,
        capability: SettingsComponent.NotificationCapability = SettingsComponent.NotificationCapability.Available,
        analytics: Boolean = false,
        privacy: SettingsComponent.AdvertisingPrivacyStatus = SettingsComponent.AdvertisingPrivacyStatus.NotRequired,
        choice: AdvertisingConsentChoice? = null,
        choiceAvailable: Boolean = false,
        error: SettingsComponent.Error? = null,
    ) = SettingsComponent.Model(
        scope = SettingsComponent.Scope.Global,
        evening = SettingsComponent.Reminder(ReminderKind.Evening,
             reminders,
             ReminderLocalTime.of(EVENING_HOUR,
                 0),
             SettingsComponent.Source.GlobalDefault),
        morning = SettingsComponent.Reminder(ReminderKind.Morning,
             reminders,
             ReminderLocalTime.of(MORNING_HOUR,
                 MORNING_MINUTE),
             SettingsComponent.Source.GlobalDefault),
        notificationCapability = capability,
        analytics = SettingsComponent.Analytics(analytics, analytics),
        advertisingPrivacy = SettingsComponent.AdvertisingPrivacy(privacy, choice, choiceAvailable),
        privacyPolicyUrl = "https://example.invalid/privacy",
        about = SettingsComponent.About("1.0.0-preview"),
        isLoading = false,
        isSaving = false,
        error = error,
    )

    private fun checklist(
        id: String,
        resource: String,
        packed: Boolean,
        source: String? = null,
        conflict: Boolean = false,
    ) = SessionComponent.ChecklistItem(
        SessionPackingItemId("session-$id"), SessionComponent.UserText.Resource(resource), 1, source, packed, conflict,
    )

    private fun templateRows(long: Boolean) = listOf(
        TemplateComponent.TemplateSummary(
            officeId,
            TemplateComponent.UserText.Resource("starter_template_office"),
            OFFICE_POSITION_COUNT,
            listOf(TemplateComponent.UserText.Resource("starter_bag_backpack")),
            true,
        ),
        TemplateComponent.TemplateSummary(
            poolId,
            if (long) {
                TemplateComponent.UserText.Authored(
                    "Очень длинный пользовательский шаблон для бассейна после рабочего дня",
                )
            } else {
                TemplateComponent.UserText.Resource("starter_template_pool")
            },
            POOL_POSITION_COUNT,
            listOf(TemplateComponent.UserText.Resource("starter_bag_sports")),
            !long,
        ),
    )

    private fun templatePositions(long: Boolean) = listOf(
        TemplateComponent.Position(
            TemplatePositionId("position-laptop"), laptopId,
            TemplateComponent.UserText.Resource("starter_item_laptop"), 1,
            TemplateComponent.UserText.Resource("starter_bag_backpack"), "Desk drawer",
        ),
        TemplateComponent.Position(
            TemplatePositionId("position-user"), PackingItemId("custom-long"),
            TemplateComponent.UserText.Authored(
                if (long) {
                    "Очень длинное пользовательское название вещи для вечерней тренировки"
                } else {
                    "Notebook"
                },
            ),
            2, null, "Hall shelf",
        ),
    )

    private fun catalogItems() = listOf(
        CatalogComponent.Item(laptopId, CatalogComponent.UserText.Resource("starter_item_laptop"), "Desk", true),
        CatalogComponent.Item(chargerId, CatalogComponent.UserText.Resource("starter_item_charger"), "Office shelf", true),
        CatalogComponent.Item(
            PackingItemId("custom-long-item"),
            CatalogComponent.UserText.Authored("Очень длинное пользовательское название вещи для ежедневных поездок"),
            "Нижний ящик у входной двери",
            false,
        ),
    )
}
