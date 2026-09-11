package com.sedsoftware.bagcue.session.integration

import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionCompletionMode
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionPackingItem
import com.sedsoftware.bagcue.domain.session.SnapshotText
import com.sedsoftware.bagcue.domain.template.KitTemplate
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.session.store.SessionStore

internal fun SessionStore.State.toComponentModel(): SessionComponent.Model = SessionComponent.Model(
    screen = when (route) {
        SessionStore.Route.Today -> SessionComponent.Screen.Today(
            date = requireNotNull(today),
            session = todaySession?.toSummary(),
            nextSession = nextSession?.toSummary(),
            starterTemplates = if (isFirstRun) {
                referenceData?.templates.orEmpty()
                    .filter { it.seedNameKey != null }
                    .map { it.toChoice(selected = false) }
            } else {
                emptyList()
            },
            isFirstRun = isFirstRun,
        )
        SessionStore.Route.Create -> createScreen()
        SessionStore.Route.Active -> activeScreen(requireNotNull(session))
        SessionStore.Route.Result -> resultScreen(requireNotNull(session))
    },
    isLoading = isLoading,
    error = error?.let { SessionComponent.ErrorKey.valueOf(it.name) },
)

private fun SessionStore.State.createScreen(): SessionComponent.Screen.Create {
    val draft = requireNotNull(createDraft)
    val templates = referenceData?.templates.orEmpty()
    return SessionComponent.Screen.Create(
        today = requireNotNull(today),
        date = draft.date,
        templates = templates.map { it.toChoice(it.id in draft.selectedTemplateIds) },
        mergedItemCount = referenceData?.let { data ->
            data.templates.filter { it.id in draft.selectedTemplateIds }.flatMap(KitTemplate::positions)
                .map { it.itemId }.distinct().size
        } ?: 0,
        occupiedSession = draft.occupiedSession?.toSummary(),
        isSaving = isSaving,
        validationError = draft.validationError?.let { SessionComponent.ValidationError.valueOf(it.name) },
    )
}

private fun SessionStore.State.activeScreen(value: PackingSession): SessionComponent.Screen.Active {
    val grouped = value.items.groupBy(SessionPackingItem::bagAssignment)
        .entries.sortedBy { (bag) -> when (bag) {
            is SessionBagAssignment.Unresolved -> 0
            is SessionBagAssignment.Assigned -> 1
            SessionBagAssignment.NoBag -> 2
        } }
        .map { (bag, items) -> SessionComponent.BagGroup(bag.toBag(), items.map(SessionPackingItem::toChecklistItem)) }
    val packed = value.items.count { it.state == SessionItemState.Packed }
    val hasConflict = value.items.any { it.bagAssignment is SessionBagAssignment.Unresolved }
    val currentEditor = itemEditor?.let { editor ->
        value.items.firstOrNull { it.id == editor.itemId }?.let { item ->
            SessionComponent.ItemEditor(
                itemId = item.id,
                name = item.name.toText(),
                quantity = item.quantity.value,
                bag = editor.bagAssignment.editableOrNull(),
                bagCandidates = editor.bagCandidates.map(TemplateBagLabel::toEditableText),
                source = editor.source,
                isSaving = isSaving,
            )
        }
    }
    return SessionComponent.Screen.Active(
        sessionId = value.id,
        date = value.localDate,
        templateNames = value.selectedTemplates.map { it.name.toText() },
        groups = grouped,
        packedCount = packed,
        totalCount = value.items.size,
        canCompleteAll = value.items.isNotEmpty() && packed == value.items.size && !hasConflict,
        canCompleteWithSkipped = value.items.isNotEmpty() && packed < value.items.size && !hasConflict,
        itemEditor = currentEditor,
        oneOffEditor = oneOffEditor?.let {
            SessionComponent.OneOffEditor(
                name = it.name,
                usualLocation = it.location,
                isSaving = isSaving,
                validationError = SessionComponent.ValidationError.BlankName.takeIf { _ -> it.blankName },
            )
        },
        saveToTemplates = saveToTemplates?.let { dialog ->
            SessionComponent.SaveToTemplates(
                itemId = dialog.itemId,
                templates = referenceData?.templates.orEmpty().map { it.toChoice(it.id in dialog.selectedTemplateIds) },
                isSaving = isSaving,
                validationError = SessionComponent.ValidationError.SelectTemplate.takeIf { dialog.selectTemplate },
            )
        },
        undoAvailable = undo != null,
        skippedConfirmationCount = skippedConfirmationCount,
        isSaving = isSaving,
    )
}

private fun SessionStore.State.resultScreen(value: PackingSession) = SessionComponent.Screen.Result(
    sessionId = value.id,
    date = value.localDate,
    allPacked = value.completionMode == SessionCompletionMode.AllPacked,
    skippedItems = value.items.filter(SessionPackingItem::skipped).map { item ->
        SessionComponent.ResultItem(item.name.toText(), item.quantity.value, item.bagAssignment.toBag())
    },
    advertising = SessionComponent.ResultAdvertising(
        status = SessionComponent.ResultAdvertisingStatus.valueOf(resultAdvertisingStatus.name),
        choiceAvailable = resultAdvertisingStatus == SessionStore.ResultAdvertisingStatus.ConsentRequired,
        privacyPolicyUrl = privacyPolicyUrl,
    ),
)

private fun PackingSession.toSummary() = SessionComponent.SessionSummary(
    id = id,
    date = localDate,
    templateNames = selectedTemplates.map { it.name.toText() },
    packedCount = items.count { it.state == SessionItemState.Packed },
    totalCount = items.size,
    isCompleted = status == PackingSessionStatus.Completed,
)

private fun KitTemplate.toChoice(selected: Boolean) = SessionComponent.TemplateChoice(
    id = id,
    name = userNameOverride?.let(SessionComponent.UserText::Authored)
        ?: SessionComponent.UserText.Resource(requireNotNull(seedNameKey).value),
    positionCount = positions.size,
    selected = selected,
)

private fun SessionPackingItem.toChecklistItem() = SessionComponent.ChecklistItem(
    id = id,
    name = name.toText(),
    quantity = quantity.value,
    sourceHint = sourceHint.takeIf { state == SessionItemState.NotPacked },
    isPacked = state == SessionItemState.Packed,
    hasBagConflict = bagAssignment is SessionBagAssignment.Unresolved,
)

private fun SnapshotText.toText(): SessionComponent.UserText = userText
    ?.let(SessionComponent.UserText::Authored)
    ?: SessionComponent.UserText.Resource(requireNotNull(seedNameKey).value)

private fun SessionBagAssignment.toBag(): SessionComponent.Bag = when (this) {
    SessionBagAssignment.NoBag -> SessionComponent.Bag.None
    is SessionBagAssignment.Assigned -> SessionComponent.Bag.Named(label.toText())
    is SessionBagAssignment.Unresolved -> SessionComponent.Bag.Unresolved
}

private fun SessionBagAssignment.editableOrNull(): SessionComponent.EditableText? = when (this) {
    SessionBagAssignment.NoBag, is SessionBagAssignment.Unresolved -> null
    is SessionBagAssignment.Assigned -> label.toEditableText()
}

private fun TemplateBagLabel.toText(): SessionComponent.UserText = userText
    ?.let(SessionComponent.UserText::Authored)
    ?: SessionComponent.UserText.Resource(requireNotNull(seedNameKey).value)

private fun TemplateBagLabel.toEditableText(): SessionComponent.EditableText = userText
    ?.let(SessionComponent.EditableText::Input)
    ?: SessionComponent.EditableText.Resource(requireNotNull(seedNameKey).value)
