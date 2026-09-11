package com.sedsoftware.bagcue.session.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.sedsoftware.bagcue.domain.apa.AdEligibility
import com.sedsoftware.bagcue.domain.apa.InlineAdState
import com.sedsoftware.bagcue.domain.session.CreateSessionResult
import com.sedsoftware.bagcue.domain.session.EmptyPackingSessionException
import com.sedsoftware.bagcue.domain.session.PackingSession
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionNotFoundException
import com.sedsoftware.bagcue.domain.session.PackingSessionStatus
import com.sedsoftware.bagcue.domain.session.PastPackingSessionDateException
import com.sedsoftware.bagcue.domain.session.SessionBagAssignment
import com.sedsoftware.bagcue.domain.session.SessionItemNotFoundException
import com.sedsoftware.bagcue.domain.session.SessionItemState
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionRevisionConflictException
import com.sedsoftware.bagcue.domain.session.SessionUndoSnapshot
import com.sedsoftware.bagcue.domain.session.UnresolvedBagConflictException
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.session.domain.SessionManager
import com.sedsoftware.bagcue.session.domain.SessionReferenceData
import com.sedsoftware.bagcue.session.domain.TodayOverview
import com.sedsoftware.bagcue.session.domain.ResultAdvertisingDecision
import com.sedsoftware.bagcue.session.domain.ResultAdvertisingManager
import com.sedsoftware.bagcue.session.domain.mergedItemCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

internal class SessionStoreProvider(
    private val storeFactory: StoreFactory,
    private val manager: SessionManager,
    private val advertisingManager: ResultAdvertisingManager,
    private val privacyPolicyUrl: String?,
) {
    fun provide(): SessionStore = object : SessionStore,
        com.arkivanov.mvikotlin.core.store.Store<SessionStore.Intent, SessionStore.State, Nothing> by storeFactory.create(
            name = "SessionStore",
            initialState = SessionStore.State(today = manager.currentDate, privacyPolicyUrl = privacyPolicyUrl),
            bootstrapper = SimpleBootstrapper(Action.Initialize),
            executorFactory = { ExecutorImpl(manager, advertisingManager) },
            reducer = ReducerImpl,
            autoInit = false,
        ) {}

    private sealed interface Action { data object Initialize : Action }

    private sealed interface Msg {
        data class ReferenceLoaded(val data: SessionReferenceData) : Msg
        data class TodayLoaded(val date: LocalDate, val overview: TodayOverview) : Msg
        data class CreateOpened(val draft: SessionStore.CreateDraft) : Msg
        data class CreateDateChanged(val date: LocalDate, val occupied: PackingSession?) : Msg
        data class TemplateSelectionChanged(val ids: Set<KitTemplateId>) : Msg
        data class CreateValidation(val error: SessionStore.ValidationError?) : Msg
        data class Saving(val value: Boolean) : Msg
        data class Occupied(val session: PackingSession) : Msg
        data class SessionLoaded(val session: PackingSession) : Msg
        data class ItemEditorChanged(val editor: SessionStore.ItemEditor?) : Msg
        data class OneOffEditorChanged(val editor: SessionStore.OneOffEditor?) : Msg
        data class SaveToTemplatesChanged(val value: SessionStore.SaveToTemplates?) : Msg
        data class UndoChanged(val snapshot: SessionUndoSnapshot?) : Msg
        data class SkippedConfirmation(val count: Int?) : Msg
        data object BackToToday : Msg
        data class Failed(val error: SessionStore.Error) : Msg
        data object ErrorCleared : Msg
        data class ResultAdvertisingChanged(val status: SessionStore.ResultAdvertisingStatus) : Msg
        data class AdRequested(val sessionId: PackingSessionId) : Msg
    }

    private class ExecutorImpl(
        private val manager: SessionManager,
        private val advertisingManager: ResultAdvertisingManager,
    ) :
        CoroutineExecutor<SessionStore.Intent, Action, SessionStore.State, Msg, Nothing>() {
        private var sessionJob: Job? = null
        private var adEvaluationJob: Job? = null
        private var adEvaluationSessionId: PackingSessionId? = null

        override fun executeAction(action: Action) {
            when (action) { Action.Initialize -> initialize() }
        }

        override fun executeIntent(intent: SessionStore.Intent) {
            when (intent) {
                SessionStore.Intent.RefreshToday -> loadToday()
                is SessionStore.Intent.StartCreate -> openCreate(intent.date)
                is SessionStore.Intent.StartRepeat -> openRepeat(intent.date, intent.templateIds)
                is SessionStore.Intent.SelectDate -> selectDate(intent.date)
                is SessionStore.Intent.ToggleTemplate -> toggleTemplate(intent.id)
                SessionStore.Intent.CreateSession -> createSession()
                SessionStore.Intent.OpenOccupiedSession -> state().createDraft?.occupiedSession?.let { openSession(it.id) }
                SessionStore.Intent.ReplaceOccupiedSession -> replaceOccupied()
                is SessionStore.Intent.OpenSession -> openSession(intent.id)
                SessionStore.Intent.BackToToday -> { sessionJob?.cancel(); hideAdvertising(); dispatch(Msg.BackToToday); loadToday() }
                is SessionStore.Intent.TogglePacked -> togglePacked(intent.id)
                is SessionStore.Intent.StartEditItem -> startEdit(intent.id)
                is SessionStore.Intent.ChooseBag -> chooseBag(intent.label)
                is SessionStore.Intent.ChangeSource -> changeSource(intent.value)
                SessionStore.Intent.SaveItemEdit -> saveEdit()
                SessionStore.Intent.CloseItemEdit -> dispatch(Msg.ItemEditorChanged(null))
                SessionStore.Intent.StartAddOneOff -> dispatch(Msg.OneOffEditorChanged(SessionStore.OneOffEditor()))
                is SessionStore.Intent.ChangeOneOffName -> updateOneOff(name = intent.value)
                is SessionStore.Intent.ChangeOneOffLocation -> updateOneOff(location = intent.value)
                SessionStore.Intent.AddOneOffItem -> addOneOff()
                SessionStore.Intent.CloseOneOffEditor -> dispatch(Msg.OneOffEditorChanged(null))
                else -> executePackingIntent(intent)
            }
        }

        private fun executePackingIntent(intent: SessionStore.Intent) {
            when (intent) {
                is SessionStore.Intent.RemoveItem -> removeItem(intent.id)
                SessionStore.Intent.UndoLastChange -> undo()
                SessionStore.Intent.CompleteAllPacked -> completeAll()
                is SessionStore.Intent.ReopenSession -> reopenSession(intent.id)
                SessionStore.Intent.RequestCompleteWithSkipped -> requestSkipped()
                SessionStore.Intent.ConfirmCompleteWithSkipped -> completeSkipped()
                SessionStore.Intent.DismissCompleteWithSkipped -> dispatch(Msg.SkippedConfirmation(null))
                is SessionStore.Intent.StartSaveItemToTemplates -> dispatch(
                    Msg.SaveToTemplatesChanged(SessionStore.SaveToTemplates(intent.id)),
                )
                is SessionStore.Intent.ToggleSaveTarget -> toggleSaveTarget(intent.id)
                SessionStore.Intent.SaveItemToTemplates -> saveToTemplates()
                SessionStore.Intent.CloseSaveToTemplates -> dispatch(Msg.SaveToTemplatesChanged(null))
                SessionStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)
                SessionStore.Intent.RefreshResultAdvertising -> state().session?.let { evaluateAdvertising(it) }
                is SessionStore.Intent.ChooseResultAdvertisingConsent -> chooseAdvertisingConsent(intent.choice)
                SessionStore.Intent.HideResultAdvertising -> hideAdvertising()
                else -> Unit
            }
        }

        private fun initialize() {
            scope.launch {
                try {
                    manager.observeReferenceData().collectLatest { dispatch(Msg.ReferenceLoaded(it)) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    dispatch(Msg.Failed(SessionStore.Error.LoadFailed))
                }
            }
            loadToday()
        }

        private fun loadToday() {
            scope.launch {
                manager.loadToday().unwrap(
                    onSuccess = { dispatch(Msg.TodayLoaded(manager.currentDate, it)) },
                    onFailure = { dispatch(Msg.Failed(SessionStore.Error.LoadFailed)) },
                )
            }
        }

        private fun openCreate(date: LocalDate) {
            dispatch(Msg.CreateOpened(SessionStore.CreateDraft(date)))
            selectDate(date)
        }

        private fun openRepeat(date: LocalDate, templateIds: Set<KitTemplateId>) {
            dispatch(Msg.CreateOpened(SessionStore.CreateDraft(date, selectedTemplateIds = templateIds)))
            selectDate(date)
            scope.launch { manager.recordHistoryRepeat() }
        }

        private fun selectDate(date: LocalDate) {
            scope.launch {
                manager.findByDate(date).unwrap(
                    onSuccess = { dispatch(Msg.CreateDateChanged(date, it)) },
                    onFailure = { error ->
                        if (error is PastPackingSessionDateException) {
                            dispatch(Msg.CreateValidation(SessionStore.ValidationError.DateInPast))
                        }
                        else dispatch(Msg.Failed(SessionStore.Error.LoadFailed))
                    },
                )
            }
        }

        private fun toggleTemplate(id: KitTemplateId) {
            val selected = state().createDraft?.selectedTemplateIds ?: return
            dispatch(Msg.TemplateSelectionChanged(if (id in selected) selected - id else selected + id))
        }

        private fun createSession() {
            val draft = state().createDraft ?: return
            val data = state().referenceData ?: return
            if (mergedItemCount(draft.selectedTemplateIds, data) == 0) {
                dispatch(Msg.CreateValidation(SessionStore.ValidationError.EmptySession))
                return
            }
            scope.launch {
                dispatch(Msg.Saving(true))
                manager.create(draft.date, draft.selectedTemplateIds).unwrap(
                    onSuccess = { result ->
                        when (result) {
                            is CreateSessionResult.Created -> openPersisted(result.session)
                            is CreateSessionResult.DateOccupied -> dispatch(Msg.Occupied(result.existing))
                        }
                    },
                    onFailure = { error ->
                        when (error) {
                            is EmptyPackingSessionException -> dispatch(Msg.CreateValidation(SessionStore.ValidationError.EmptySession))
                            is PastPackingSessionDateException -> dispatch(Msg.CreateValidation(SessionStore.ValidationError.DateInPast))
                            else -> dispatch(Msg.Failed(SessionStore.Error.CreateFailed))
                        }
                    },
                )
            }
        }

        private fun replaceOccupied() {
            val draft = state().createDraft ?: return
            val existing = draft.occupiedSession ?: return
            scope.launch {
                dispatch(Msg.Saving(true))
                manager.replace(existing, draft.selectedTemplateIds).unwrap(
                    onSuccess = { mutation -> dispatch(Msg.UndoChanged(mutation.undo)); openPersisted(mutation.session) },
                    onFailure = { error ->
                        if (error is EmptyPackingSessionException) dispatch(Msg.CreateValidation(SessionStore.ValidationError.EmptySession))
                        else dispatch(Msg.Failed(SessionStore.Error.ReplaceFailed))
                    },
                )
            }
        }

        private fun openSession(id: PackingSessionId) {
            sessionJob?.cancel()
            sessionJob = scope.launch {
                try {
                    manager.observeSession(id).collectLatest { session ->
                        if (session == null) dispatch(Msg.Failed(SessionStore.Error.SessionNoLongerExists))
                        else acceptSession(session)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    dispatch(Msg.Failed(SessionStore.Error.LoadFailed))
                }
            }
        }

        private fun openPersisted(session: PackingSession) {
            acceptSession(session)
            openSession(session.id)
        }

        private fun togglePacked(id: SessionPackingItemId) {
            val session = state().session ?: return
            val item = session.items.firstOrNull { it.id == id } ?: return
            save(SessionStore.Error.SaveFailed) {
                manager.setPacked(session, id, item.state != SessionItemState.Packed)
            }
        }

        private fun startEdit(id: SessionPackingItemId) {
            val item = state().session?.items?.firstOrNull { it.id == id }
            if (item == null) dispatch(Msg.Failed(SessionStore.Error.ItemNoLongerExists))
            else dispatch(
                Msg.ItemEditorChanged(
                    SessionStore.ItemEditor(
                        itemId = id,
                        bagAssignment = item.bagAssignment,
                        bagCandidates = (item.bagAssignment as? SessionBagAssignment.Unresolved)?.candidates.orEmpty(),
                        source = item.sourceHint.orEmpty(),
                    ),
                ),
            )
        }

        private fun chooseBag(label: TemplateBagLabel?) {
            val editor = state().itemEditor ?: return
            dispatch(Msg.ItemEditorChanged(editor.copy(
                bagAssignment = label?.let(SessionBagAssignment::Assigned) ?: SessionBagAssignment.NoBag,
            )))
        }

        private fun changeSource(value: String) {
            state().itemEditor?.let { dispatch(Msg.ItemEditorChanged(it.copy(source = value))) }
        }

        private fun saveEdit() {
            val session = state().session ?: return
            val editor = state().itemEditor ?: return
            save(SessionStore.Error.SaveFailed, onSuccess = { dispatch(Msg.ItemEditorChanged(null)) }) {
                manager.editItem(session, editor.itemId, editor.bagAssignment, editor.source)
            }
        }

        private fun updateOneOff(name: String? = null, location: String? = null) {
            state().oneOffEditor?.let { editor ->
                dispatch(Msg.OneOffEditorChanged(editor.copy(
                    name = name ?: editor.name,
                    location = location ?: editor.location,
                    blankName = false,
                )))
            }
        }

        private fun addOneOff() {
            val session = state().session ?: return
            val editor = state().oneOffEditor ?: return
            if (editor.name.isBlank()) {
                dispatch(Msg.OneOffEditorChanged(editor.copy(blankName = true)))
                return
            }
            save(SessionStore.Error.SaveFailed, onSuccess = { dispatch(Msg.OneOffEditorChanged(null)) }) {
                manager.addOneOff(session.id, editor.name, editor.location)
            }
        }

        private fun removeItem(id: SessionPackingItemId) {
            val session = state().session ?: return
            scope.launch {
                dispatch(Msg.Saving(true))
                manager.remove(session.id, id).unwrap(
                    onSuccess = { mutation -> dispatch(Msg.UndoChanged(mutation.undo)); acceptSession(mutation.session) },
                    onFailure = { error ->
                        if (error is SessionItemNotFoundException) dispatch(Msg.Failed(SessionStore.Error.ItemNoLongerExists))
                        else dispatch(Msg.Failed(SessionStore.Error.RemoveFailed))
                    },
                )
            }
        }

        private fun undo() {
            val undo = state().undo ?: return
            scope.launch {
                dispatch(Msg.Saving(true))
                manager.undo(undo).unwrap(
                    onSuccess = { dispatch(Msg.UndoChanged(null)); acceptSession(it) },
                    onFailure = { error ->
                        if (error is SessionRevisionConflictException) dispatch(Msg.Failed(SessionStore.Error.RevisionConflict))
                        else dispatch(Msg.Failed(SessionStore.Error.UndoFailed))
                    },
                )
            }
        }

        private fun completeAll() {
            val session = state().session ?: return
            save(SessionStore.Error.CompleteFailed) { manager.completeAll(session) }
        }

        private fun reopenSession(id: PackingSessionId) {
            if (state().isSaving) return
            dispatch(Msg.Saving(true))
            hideAdvertising()
            scope.launch {
                manager.reopen(id).unwrap(
                    onSuccess = ::acceptSession,
                    onFailure = { error ->
                        dispatch(
                            Msg.Failed(
                                if (error is PackingSessionNotFoundException) {
                                    SessionStore.Error.SessionNoLongerExists
                                } else {
                                    SessionStore.Error.ReopenFailed
                                },
                            ),
                        )
                    },
                )
            }
        }

        private fun requestSkipped() {
            val remaining = state().session?.items?.count { it.state == SessionItemState.NotPacked } ?: return
            if (remaining > 0) dispatch(Msg.SkippedConfirmation(remaining))
        }

        private fun completeSkipped() {
            val session = state().session ?: return
            val count = state().skippedConfirmationCount ?: return
            save(SessionStore.Error.CompleteFailed) { manager.completeSkipped(session, count) }
        }

        private fun toggleSaveTarget(id: KitTemplateId) {
            val dialog = state().saveToTemplates ?: return
            val selected = dialog.selectedTemplateIds
            dispatch(Msg.SaveToTemplatesChanged(dialog.copy(
                selectedTemplateIds = if (id in selected) selected - id else selected + id,
                selectTemplate = false,
            )))
        }

        private fun saveToTemplates() {
            val session = state().session ?: return
            val dialog = state().saveToTemplates ?: return
            if (dialog.selectedTemplateIds.isEmpty()) {
                dispatch(Msg.SaveToTemplatesChanged(dialog.copy(selectTemplate = true)))
                return
            }
            scope.launch {
                dispatch(Msg.Saving(true))
                manager.saveItemToTemplates(session.id, dialog.itemId, dialog.selectedTemplateIds).unwrap(
                    onSuccess = { dispatch(Msg.SaveToTemplatesChanged(null)); dispatch(Msg.Saving(false)) },
                    onFailure = { dispatch(Msg.Failed(SessionStore.Error.SaveFailed)) },
                )
            }
        }

        private fun save(
            error: SessionStore.Error,
            onSuccess: () -> Unit = {},
            operation: suspend () -> Result<PackingSession>,
        ) {
            scope.launch {
                dispatch(Msg.Saving(true))
                operation().unwrap(
                    onSuccess = { acceptSession(it); onSuccess() },
                    onFailure = { failure ->
                        if (failure is UnresolvedBagConflictException) {
                            dispatch(Msg.CreateValidation(SessionStore.ValidationError.BagConflict))
                        }
                        else dispatch(Msg.Failed(error))
                    },
                )
            }
        }

        private fun acceptSession(session: PackingSession) {
            dispatch(Msg.SessionLoaded(session))
            if (session.status == com.sedsoftware.bagcue.domain.session.PackingSessionStatus.Completed) evaluateAdvertising(session)
        }

        private fun evaluateAdvertising(session: PackingSession) {
            if (session.id in state().adRequestedSessionIds) return
            if (adEvaluationSessionId == session.id && adEvaluationJob?.isActive == true) return
            adEvaluationJob?.cancel()
            adEvaluationSessionId = session.id
            adEvaluationJob = scope.launch {
                advertisingManager.evaluate(session, alreadyRequested = session.id in state().adRequestedSessionIds).unwrap(
                    onSuccess = { decision ->
                        when (decision) {
                            ResultAdvertisingDecision.Suppressed -> dispatch(
                                Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.Hidden),
                            )
                            ResultAdvertisingDecision.ConsentRequired -> dispatch(
                                Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.ConsentRequired),
                            )
                            is ResultAdvertisingDecision.Eligible -> requestAdvertising(decision.eligibility)
                        }
                    },
                    onFailure = { dispatch(Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.Hidden)) },
                )
            }
        }

        private fun requestAdvertising(eligibility: AdEligibility.Eligible) {
            dispatch(Msg.AdRequested(eligibility.sessionId))
            dispatch(Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.Loading))
            scope.launch {
                advertisingManager.request(eligibility).unwrap(
                    onSuccess = { state ->
                        val status = if (state == InlineAdState.Ready) {
                            SessionStore.ResultAdvertisingStatus.Ready
                        } else {
                            SessionStore.ResultAdvertisingStatus.Hidden
                        }
                        dispatch(Msg.ResultAdvertisingChanged(status))
                    },
                    onFailure = { dispatch(Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.Hidden)) },
                )
            }
        }

        private fun chooseAdvertisingConsent(choice: com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice) {
            val session = state().session ?: return
            scope.launch {
                advertisingManager.chooseConsent(choice).unwrap(
                    onSuccess = { adEvaluationSessionId = null; evaluateAdvertising(session) },
                    onFailure = { dispatch(Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.Hidden)) },
                )
            }
        }

        private fun hideAdvertising() {
            adEvaluationJob?.cancel()
            adEvaluationSessionId = null
            dispatch(Msg.ResultAdvertisingChanged(SessionStore.ResultAdvertisingStatus.Hidden))
            scope.launch { advertisingManager.dispose() }
        }
    }

    private object ReducerImpl : Reducer<SessionStore.State, Msg> {
        override fun SessionStore.State.reduce(msg: Msg): SessionStore.State = when (msg) {
            is Msg.ReferenceLoaded -> copy(referenceData = msg.data, isLoading = false, error = null)
            is Msg.TodayLoaded -> copy(
                today = msg.date,
                todaySession = msg.overview.session,
                nextSession = msg.overview.nextSession,
                isFirstRun = msg.overview.isFirstRun,
                isLoading = false,
                error = null,
            )
            is Msg.CreateOpened -> copy(route = SessionStore.Route.Create, createDraft = msg.draft, error = null)
            is Msg.CreateDateChanged -> copy(
                createDraft = createDraft?.copy(date = msg.date, occupiedSession = msg.occupied, validationError = null),
                error = null,
            )
            is Msg.TemplateSelectionChanged -> copy(
                createDraft = createDraft?.copy(selectedTemplateIds = msg.ids, validationError = null),
                error = null,
            )
            is Msg.CreateValidation -> copy(createDraft = createDraft?.copy(validationError = msg.error), isSaving = false)
            is Msg.Saving -> copy(isSaving = msg.value, error = null)
            is Msg.Occupied -> copy(createDraft = createDraft?.copy(occupiedSession = msg.session), isSaving = false)
            is Msg.SessionLoaded -> copy(
                route = if (msg.session.status == PackingSessionStatus.Completed) {
                    SessionStore.Route.Result
                } else {
                    SessionStore.Route.Active
                },
                session = msg.session,
                createDraft = null,
                isLoading = false,
                isSaving = false,
                skippedConfirmationCount = null,
                error = null,
                resultAdvertisingStatus = if (
                    msg.session.id == session?.id &&
                        msg.session.status == PackingSessionStatus.Completed
                ) resultAdvertisingStatus else SessionStore.ResultAdvertisingStatus.Hidden,
            )
            is Msg.ItemEditorChanged -> copy(itemEditor = msg.editor, error = null)
            is Msg.OneOffEditorChanged -> copy(oneOffEditor = msg.editor, error = null)
            is Msg.SaveToTemplatesChanged -> copy(saveToTemplates = msg.value, error = null)
            is Msg.UndoChanged -> copy(undo = msg.snapshot)
            is Msg.SkippedConfirmation -> copy(skippedConfirmationCount = msg.count)
            Msg.BackToToday -> copy(
                route = SessionStore.Route.Today,
                session = null,
                createDraft = null,
                itemEditor = null,
                oneOffEditor = null,
                saveToTemplates = null,
                undo = null,
                error = null,
                resultAdvertisingStatus = SessionStore.ResultAdvertisingStatus.Hidden,
            )
            is Msg.Failed -> copy(isLoading = false, isSaving = false, error = msg.error)
            Msg.ErrorCleared -> copy(error = null)
            is Msg.ResultAdvertisingChanged -> copy(resultAdvertisingStatus = msg.status)
            is Msg.AdRequested -> copy(adRequestedSessionIds = adRequestedSessionIds + msg.sessionId)
        }
    }
}

private inline fun <T> Result<T>.unwrap(onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    fold(onSuccess, onFailure = { error ->
        if (error is CancellationException) throw error
        onFailure(error)
    })
}
