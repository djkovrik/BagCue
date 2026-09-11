package com.sedsoftware.bagcue.session.integration

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.domain.session.PackingSessionId
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.domain.session.SessionPackingItemId
import com.sedsoftware.bagcue.domain.template.KitTemplateId
import com.sedsoftware.bagcue.session.SessionComponent
import kotlinx.datetime.LocalDate

class SessionComponentPreview(initialModel: SessionComponent.Model) : SessionComponent {
    private val mutableModel = MutableValue(initialModel)
    override val model: Value<SessionComponent.Model> = mutableModel
    override fun refreshToday() = Unit
    override fun startCreate(date: LocalDate) = Unit
    override fun startRepeat(date: LocalDate, templateIds: List<KitTemplateId>) = Unit
    override fun selectDate(date: LocalDate) = Unit
    override fun toggleTemplate(templateId: KitTemplateId) = Unit
    override fun createSession() = Unit
    override fun openOccupiedSession() = Unit
    override fun replaceOccupiedSession() = Unit
    override fun openSession(sessionId: PackingSessionId) = Unit
    override fun backToToday() = Unit
    override fun togglePacked(itemId: SessionPackingItemId) = Unit
    override fun startEditItem(itemId: SessionPackingItemId) = Unit
    override fun chooseBag(value: SessionComponent.EditableText?) = Unit
    override fun changeSource(value: String) = Unit
    override fun saveItemEdit() = Unit
    override fun closeItemEdit() = Unit
    override fun startAddOneOff() = Unit
    override fun changeOneOffName(value: String) = Unit
    override fun changeOneOffLocation(value: String) = Unit
    override fun addOneOffItem() = Unit
    override fun closeOneOffEditor() = Unit
    override fun removeItem(itemId: SessionPackingItemId) = Unit
    override fun undoLastChange() = Unit
    override fun completeAllPacked() = Unit
    override fun reopenSession(sessionId: PackingSessionId) = Unit
    override fun requestCompleteWithSkipped() = Unit
    override fun confirmCompleteWithSkipped() = Unit
    override fun dismissCompleteWithSkipped() = Unit
    override fun startSaveItemToTemplates(itemId: SessionPackingItemId) = Unit
    override fun toggleSaveTarget(templateId: KitTemplateId) = Unit
    override fun saveItemToTemplates() = Unit
    override fun closeSaveToTemplates() = Unit
    override fun openTemplates() = Unit
    override fun openCatalog() = Unit
    override fun refreshResultAdvertising() = Unit
    override fun chooseResultAdvertisingConsent(choice: AdvertisingConsentChoice) = Unit
    override fun openAdvertisingPrivacyPolicy() = Unit
    override fun dismissResultAdvertising() = Unit
    override fun clearError() = Unit
}
