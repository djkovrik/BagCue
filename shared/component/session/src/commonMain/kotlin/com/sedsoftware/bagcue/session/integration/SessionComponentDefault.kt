package com.sedsoftware.bagcue.session.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bagcue.domain.catalog.CatalogRepository
import com.sedsoftware.bagcue.domain.catalog.PackingItemIdGenerator
import com.sedsoftware.bagcue.domain.catalog.ResourceKey
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.apa.AdvertisingPrivacyRepository
import com.sedsoftware.bagcue.domain.apa.AnalyticsController
import com.sedsoftware.bagcue.domain.apa.InlineAdController
import com.sedsoftware.bagcue.domain.apa.PrivacyRegionApi
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.session.PackingSessionIdGenerator
import com.sedsoftware.bagcue.domain.session.PackingSessionRepository
import com.sedsoftware.bagcue.domain.session.SessionHistoryRepository
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.session.SessionPackingItemIdGenerator
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.domain.template.KitTemplateRepository
import com.sedsoftware.bagcue.domain.template.TemplateBagLabel
import com.sedsoftware.bagcue.domain.template.TemplatePositionIdGenerator
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.session.domain.SessionManager
import com.sedsoftware.bagcue.session.domain.ResultAdvertisingManager
import com.sedsoftware.bagcue.session.store.SessionStore
import com.sedsoftware.bagcue.session.store.SessionStoreProvider
import kotlinx.datetime.LocalDate

class SessionComponentDefault(
    componentContext: ComponentContext,
    sessionRepository: PackingSessionRepository,
    templateRepository: KitTemplateRepository,
    catalogRepository: CatalogRepository,
    storeFactory: StoreFactory,
    sessionIdGenerator: PackingSessionIdGenerator,
    sessionItemIdGenerator: SessionPackingItemIdGenerator,
    catalogItemIdGenerator: PackingItemIdGenerator,
    templatePositionIdGenerator: TemplatePositionIdGenerator,
    today: () -> LocalDate,
    currentTimeMillis: () -> Long,
    advertisingPrivacyRepository: AdvertisingPrivacyRepository,
    privacyRegionApi: PrivacyRegionApi,
    inlineAdController: InlineAdController,
    analyticsController: AnalyticsController,
    private val privacyPolicyUrl: String?,
    resolveResourceKey: suspend (ResourceKey) -> String,
    private val onOpenTemplates: () -> Unit,
    private val onOpenCatalog: () -> Unit,
    private val onOpenPrivacyPolicy: (String) -> Unit,
) : SessionComponent, ComponentContext by componentContext {
    private val store = instanceKeeper.getOrCreate(key = STORE_KEY) {
        StoreHolder(
            SessionStoreProvider(
                storeFactory = storeFactory,
                manager = SessionManager(
                    sessionRepository = sessionRepository,
                    templateRepository = templateRepository,
                    catalogRepository = catalogRepository,
                    sessionIdGenerator = sessionIdGenerator,
                    sessionItemIdGenerator = sessionItemIdGenerator,
                    catalogItemIdGenerator = catalogItemIdGenerator,
                    templatePositionIdGenerator = templatePositionIdGenerator,
                    today = today,
                    currentTimeMillis = currentTimeMillis,
                    resolveResourceKey = resolveResourceKey,
                    analyticsController = analyticsController,
                    historyRepository = sessionRepository as? SessionHistoryRepository,
                ),
                advertisingManager = ResultAdvertisingManager(
                    privacyRepository = advertisingPrivacyRepository,
                    privacyApi = privacyRegionApi,
                    controller = inlineAdController,
                    currentTimeMillis = currentTimeMillis,
                ),
                privacyPolicyUrl = privacyPolicyUrl,
            ).provide().also(SessionStore::init),
        )
    }.store

    override val model: Value<SessionComponent.Model> = store.asValue().map(SessionStore.State::toComponentModel)

    override fun refreshToday() = store.accept(SessionStore.Intent.RefreshToday)
    override fun startCreate(date: LocalDate) = store.accept(SessionStore.Intent.StartCreate(date))
    override fun startRepeat(date: LocalDate, templateIds: List<KitTemplateId>) =
        store.accept(SessionStore.Intent.StartRepeat(date, templateIds.toSet()))
    override fun selectDate(date: LocalDate) = store.accept(SessionStore.Intent.SelectDate(date))
    override fun toggleTemplate(templateId: KitTemplateId) = store.accept(SessionStore.Intent.ToggleTemplate(templateId))
    override fun createSession() = store.accept(SessionStore.Intent.CreateSession)
    override fun openOccupiedSession() = store.accept(SessionStore.Intent.OpenOccupiedSession)
    override fun replaceOccupiedSession() = store.accept(SessionStore.Intent.ReplaceOccupiedSession)
    override fun openSession(sessionId: PackingSessionId) = store.accept(SessionStore.Intent.OpenSession(sessionId))
    override fun backToToday() = store.accept(SessionStore.Intent.BackToToday)
    override fun togglePacked(itemId: SessionPackingItemId) = store.accept(SessionStore.Intent.TogglePacked(itemId))
    override fun startEditItem(itemId: SessionPackingItemId) = store.accept(SessionStore.Intent.StartEditItem(itemId))
    override fun chooseBag(value: SessionComponent.EditableText?) = store.accept(SessionStore.Intent.ChooseBag(value.toBagLabel()))
    override fun changeSource(value: String) = store.accept(SessionStore.Intent.ChangeSource(value))
    override fun saveItemEdit() = store.accept(SessionStore.Intent.SaveItemEdit)
    override fun closeItemEdit() = store.accept(SessionStore.Intent.CloseItemEdit)
    override fun startAddOneOff() = store.accept(SessionStore.Intent.StartAddOneOff)
    override fun changeOneOffName(value: String) = store.accept(SessionStore.Intent.ChangeOneOffName(value))
    override fun changeOneOffLocation(value: String) = store.accept(SessionStore.Intent.ChangeOneOffLocation(value))
    override fun addOneOffItem() = store.accept(SessionStore.Intent.AddOneOffItem)
    override fun closeOneOffEditor() = store.accept(SessionStore.Intent.CloseOneOffEditor)
    override fun removeItem(itemId: SessionPackingItemId) = store.accept(SessionStore.Intent.RemoveItem(itemId))
    override fun undoLastChange() = store.accept(SessionStore.Intent.UndoLastChange)
    override fun completeAllPacked() = store.accept(SessionStore.Intent.CompleteAllPacked)
    override fun reopenSession(sessionId: PackingSessionId) = store.accept(SessionStore.Intent.ReopenSession(sessionId))
    override fun requestCompleteWithSkipped() = store.accept(SessionStore.Intent.RequestCompleteWithSkipped)
    override fun confirmCompleteWithSkipped() = store.accept(SessionStore.Intent.ConfirmCompleteWithSkipped)
    override fun dismissCompleteWithSkipped() = store.accept(SessionStore.Intent.DismissCompleteWithSkipped)
    override fun startSaveItemToTemplates(itemId: SessionPackingItemId) = store.accept(SessionStore.Intent.StartSaveItemToTemplates(itemId))
    override fun toggleSaveTarget(templateId: KitTemplateId) = store.accept(SessionStore.Intent.ToggleSaveTarget(templateId))
    override fun saveItemToTemplates() = store.accept(SessionStore.Intent.SaveItemToTemplates)
    override fun closeSaveToTemplates() = store.accept(SessionStore.Intent.CloseSaveToTemplates)
    override fun openTemplates() = onOpenTemplates()
    override fun openCatalog() = onOpenCatalog()
    override fun refreshResultAdvertising() = store.accept(SessionStore.Intent.RefreshResultAdvertising)
    override fun chooseResultAdvertisingConsent(choice: AdvertisingConsentChoice) =
        store.accept(SessionStore.Intent.ChooseResultAdvertisingConsent(choice))
    override fun openAdvertisingPrivacyPolicy() = privacyPolicyUrl?.let(onOpenPrivacyPolicy) ?: Unit
    override fun dismissResultAdvertising() = store.accept(SessionStore.Intent.HideResultAdvertising)
    override fun clearError() = store.accept(SessionStore.Intent.ClearError)

    private fun SessionComponent.EditableText?.toBagLabel(): TemplateBagLabel? = when (this) {
        null -> null
        is SessionComponent.EditableText.Input -> TemplateBagLabel.user(value).takeUnless { it == TemplateBagLabel.None }
        is SessionComponent.EditableText.Resource -> TemplateBagLabel.seed(ResourceKey(key))
    }

    private class StoreHolder(val store: SessionStore) : InstanceKeeper.Instance {
        override fun onDestroy() = store.dispose()
    }

    private companion object { const val STORE_KEY = "SessionStore" }
}
