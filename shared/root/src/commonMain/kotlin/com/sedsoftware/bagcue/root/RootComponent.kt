package com.sedsoftware.bagcue.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.templates.TemplateComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val selectedPrimaryDestination: Value<PrimaryDestination>

    fun selectPrimaryDestination(destination: PrimaryDestination)

    fun showSession()
    fun showHistory()
    fun showTemplates()
    fun showCatalog()
    fun showSettings()

    enum class PrimaryDestination {
        Today,
        Sessions,
        Templates,
        Settings,
    }

    sealed interface Child {
        data class Session(val component: SessionComponent) : Child
        data class History(val component: HistoryComponent) : Child
        data class Templates(val component: TemplateComponent) : Child
        data class Catalog(val component: CatalogComponent) : Child
        data class Settings(val component: SettingsComponent) : Child
    }
}
