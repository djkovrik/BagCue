package com.sedsoftware.bagcue.session.store

import com.arkivanov.mvikotlin.core.store.Store
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.session.domain.SessionReferenceData
import kotlinx.datetime.LocalDate

internal interface SessionStore : Store<SessionStore.Intent, SessionStore.State, Nothing> {
    sealed interface Intent {
        data object RefreshToday : Intent
        data class StartCreate(val date: LocalDate) : Intent
        data class StartRepeat(val date: LocalDate, val templateIds: Set<KitTemplateId>) : Intent
        data class SelectDate(val date: LocalDate) : Intent
        data class ToggleTemplate(val id: KitTemplateId) : Intent
        data object CreateSession : Intent
        data object OpenOccupiedSession : Intent
        data object ReplaceOccupiedSession : Intent
        data class OpenSession(val id: PackingSessionId) : Intent
        data object BackToToday : Intent
        data class TogglePacked(val id: SessionPackingItemId) : Intent
        data class StartEditItem(val id: SessionPackingItemId) : Intent
        data class ChooseBag(val label: TemplateBagLabel?) : Intent
        data class ChangeSource(val value: String) : Intent
        data object SaveItemEdit : Intent
        data object CloseItemEdit : Intent
        data object StartAddOneOff : Intent
        data class ChangeOneOffName(val value: String) : Intent
        data class ChangeOneOffLocation(val value: String) : Intent
        data object AddOneOffItem : Intent
        data object CloseOneOffEditor : Intent
        data class RemoveItem(val id: SessionPackingItemId) : Intent
        data object UndoLastChange : Intent
        data object CompleteAllPacked : Intent
        data class ReopenSession(val id: PackingSessionId) : Intent
        data object RequestCompleteWithSkipped : Intent
        data object ConfirmCompleteWithSkipped : Intent
        data object DismissCompleteWithSkipped : Intent
        data class StartSaveItemToTemplates(val id: SessionPackingItemId) : Intent
        data class ToggleSaveTarget(val id: KitTemplateId) : Intent
        data object SaveItemToTemplates : Intent
        data object CloseSaveToTemplates : Intent
        data object ClearError : Intent
        data object RefreshResultAdvertising : Intent
        data class ChooseResultAdvertisingConsent(val choice: AdvertisingConsentChoice) : Intent
        data object HideResultAdvertising : Intent
    }

    data class State(
        val route: Route = Route.Today,
        val referenceData: SessionReferenceData? = null,
        val today: LocalDate? = null,
        val todaySession: PackingSession? = null,
        val nextSession: PackingSession? = null,
        val isFirstRun: Boolean = false,
        val createDraft: CreateDraft? = null,
        val session: PackingSession? = null,
        val itemEditor: ItemEditor? = null,
        val oneOffEditor: OneOffEditor? = null,
        val saveToTemplates: SaveToTemplates? = null,
        val undo: SessionUndoSnapshot? = null,
        val skippedConfirmationCount: Int? = null,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val error: Error? = null,
        val resultAdvertisingStatus: ResultAdvertisingStatus = ResultAdvertisingStatus.Hidden,
        val adRequestedSessionIds: Set<PackingSessionId> = emptySet(),
        val privacyPolicyUrl: String? = null,
    )

    enum class Route { Today, Create, Active, Result }
    enum class ResultAdvertisingStatus { Hidden, ConsentRequired, Loading, Ready }

    data class CreateDraft(
        val date: LocalDate,
        val selectedTemplateIds: Set<KitTemplateId> = emptySet(),
        val occupiedSession: PackingSession? = null,
        val validationError: ValidationError? = null,
    )

    data class ItemEditor(
        val itemId: SessionPackingItemId,
        val bagAssignment: SessionBagAssignment,
        val bagCandidates: List<TemplateBagLabel>,
        val source: String,
    )

    data class OneOffEditor(
        val name: String = "",
        val location: String = "",
        val blankName: Boolean = false,
    )

    data class SaveToTemplates(
        val itemId: SessionPackingItemId,
        val selectedTemplateIds: Set<KitTemplateId> = emptySet(),
        val selectTemplate: Boolean = false,
    )

    enum class ValidationError { EmptySession, DateInPast, BagConflict }

    enum class Error {
        LoadFailed, CreateFailed, ReplaceFailed, SaveFailed, RemoveFailed, UndoFailed, CompleteFailed, ReopenFailed,
        SessionNoLongerExists, ItemNoLongerExists, RevisionConflict,
    }
}
