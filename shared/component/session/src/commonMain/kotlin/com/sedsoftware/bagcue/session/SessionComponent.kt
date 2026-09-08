package com.sedsoftware.bagcue.session

import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import kotlinx.datetime.LocalDate

interface SessionComponent :
    SessionNavigationActions,
    SessionItemActions,
    SessionCompletionActions,
    SessionAdvertisingActions {
    val model: Value<Model>

    data class Model(
        val screen: Screen,
        val isLoading: Boolean,
        val error: ErrorKey?,
    )

    sealed interface Screen {
        data class Today(
            val date: LocalDate,
            val session: SessionSummary?,
        ) : Screen

        data class Create(
            val date: LocalDate,
            val templates: List<TemplateChoice>,
            val mergedItemCount: Int,
            val occupiedSession: SessionSummary?,
            val isSaving: Boolean,
            val validationError: ValidationError?,
        ) : Screen

        data class Active(
            val sessionId: PackingSessionId,
            val date: LocalDate,
            val templateNames: List<UserText>,
            val groups: List<BagGroup>,
            val packedCount: Int,
            val totalCount: Int,
            val canCompleteAll: Boolean,
            val canCompleteWithSkipped: Boolean,
            val itemEditor: ItemEditor?,
            val oneOffEditor: OneOffEditor?,
            val saveToTemplates: SaveToTemplates?,
            val undoAvailable: Boolean,
            val skippedConfirmationCount: Int?,
            val isSaving: Boolean,
        ) : Screen

        data class Result(
            val sessionId: PackingSessionId,
            val date: LocalDate,
            val allPacked: Boolean,
            val skippedItems: List<ResultItem>,
            val advertising: ResultAdvertising = ResultAdvertising(ResultAdvertisingStatus.Hidden, choiceAvailable = false),
        ) : Screen
    }

    data class SessionSummary(
        val id: PackingSessionId,
        val date: LocalDate,
        val templateNames: List<UserText>,
        val packedCount: Int,
        val totalCount: Int,
        val isCompleted: Boolean,
    )

    data class TemplateChoice(
        val id: KitTemplateId,
        val name: UserText,
        val positionCount: Int,
        val selected: Boolean,
    )

    data class BagGroup(
        val bag: Bag,
        val items: List<ChecklistItem>,
    )

    data class ChecklistItem(
        val id: SessionPackingItemId,
        val name: UserText,
        val quantity: Int,
        val sourceHint: String?,
        val isPacked: Boolean,
        val hasBagConflict: Boolean,
    )

    data class ItemEditor(
        val itemId: SessionPackingItemId,
        val name: UserText,
        val quantity: Int,
        val bag: EditableText?,
        val bagCandidates: List<EditableText>,
        val source: String,
        val isSaving: Boolean,
    )

    data class OneOffEditor(
        val name: String,
        val usualLocation: String,
        val isSaving: Boolean,
        val validationError: ValidationError?,
    )

    data class SaveToTemplates(
        val itemId: SessionPackingItemId,
        val templates: List<TemplateChoice>,
        val isSaving: Boolean,
        val validationError: ValidationError?,
    )

    data class ResultItem(
        val name: UserText,
        val quantity: Int,
        val bag: Bag,
    )

    data class ResultAdvertising(
        val status: ResultAdvertisingStatus,
        val choiceAvailable: Boolean,
        val privacyPolicyUrl: String? = null,
    )

    sealed interface Bag {
        data object Unresolved : Bag
        data object None : Bag
        data class Named(val name: UserText) : Bag
    }

    sealed interface UserText {
        data class Resource(val key: String) : UserText
        data class Authored(val value: String) : UserText
    }

    sealed interface EditableText {
        data class Resource(val key: String) : EditableText
        data class Input(val value: String) : EditableText
    }

    enum class ValidationError {
        EmptySession,
        BlankName,
        SelectTemplate,
        DateInPast,
        BagConflict,
    }

    enum class ResultAdvertisingStatus { Hidden, ConsentRequired, Loading, Ready }

    enum class ErrorKey {
        LoadFailed,
        CreateFailed,
        ReplaceFailed,
        SaveFailed,
        RemoveFailed,
        UndoFailed,
        CompleteFailed,
        SessionNoLongerExists,
        ItemNoLongerExists,
        RevisionConflict,
    }
}

interface SessionNavigationActions {
    fun refreshToday()
    fun startCreate(date: LocalDate)
    fun startRepeat(date: LocalDate, templateIds: List<KitTemplateId>)
    fun selectDate(date: LocalDate)
    fun toggleTemplate(templateId: KitTemplateId)
    fun createSession()
    fun openOccupiedSession()
    fun replaceOccupiedSession()
    fun openSession(sessionId: PackingSessionId)
    fun backToToday()
    fun openTemplates()
    fun openCatalog()
}

interface SessionItemActions {
    fun togglePacked(itemId: SessionPackingItemId)
    fun startEditItem(itemId: SessionPackingItemId)
    fun chooseBag(value: SessionComponent.EditableText?)
    fun changeSource(value: String)
    fun saveItemEdit()
    fun closeItemEdit()
    fun startAddOneOff()
    fun changeOneOffName(value: String)
    fun changeOneOffLocation(value: String)
    fun addOneOffItem()
    fun closeOneOffEditor()
    fun removeItem(itemId: SessionPackingItemId)
    fun undoLastChange()
}

interface SessionCompletionActions {
    fun completeAllPacked()
    fun requestCompleteWithSkipped()
    fun confirmCompleteWithSkipped()
    fun dismissCompleteWithSkipped()
    fun startSaveItemToTemplates(itemId: SessionPackingItemId)
    fun toggleSaveTarget(templateId: KitTemplateId)
    fun saveItemToTemplates()
    fun closeSaveToTemplates()
    fun clearError()
}

interface SessionAdvertisingActions {
    fun refreshResultAdvertising()
    fun chooseResultAdvertisingConsent(choice: AdvertisingConsentChoice)
    fun openAdvertisingPrivacyPolicy()
    fun dismissResultAdvertising()
}
