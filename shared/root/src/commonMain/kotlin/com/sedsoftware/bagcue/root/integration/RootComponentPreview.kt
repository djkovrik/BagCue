package com.sedsoftware.bagcue.root.integration

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.root.RootComponent
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.templates.TemplateComponent

class RootComponentPreview(
    private val sessionComponent: SessionComponent,
    private val historyComponent: HistoryComponent,
    private val templateComponent: TemplateComponent,
    private val catalogComponent: CatalogComponent,
    private val settingsComponent: SettingsComponent,
    initialDestination: RootComponent.PrimaryDestination = RootComponent.PrimaryDestination.Today,
    initialCatalog: Boolean = false,
) : RootComponent {
    private val mutableStack = MutableValue(
        singleChildStack(
            if (initialCatalog) RootComponent.Child.Catalog(catalogComponent) else childFor(initialDestination),
        ),
    )
    private val mutableDestination = MutableValue(initialDestination)

    override val stack: Value<ChildStack<*, RootComponent.Child>> = mutableStack
    override val selectedPrimaryDestination: Value<RootComponent.PrimaryDestination> = mutableDestination

    override fun selectPrimaryDestination(destination: RootComponent.PrimaryDestination) {
        mutableDestination.value = destination
        mutableStack.value = singleChildStack(childFor(destination))
    }

    override fun showSession() = selectPrimaryDestination(RootComponent.PrimaryDestination.Today)
    override fun showHistory() = selectPrimaryDestination(RootComponent.PrimaryDestination.Sessions)
    override fun showTemplates() = selectPrimaryDestination(RootComponent.PrimaryDestination.Templates)
    override fun showSettings() = selectPrimaryDestination(RootComponent.PrimaryDestination.Settings)

    override fun showCatalog() {
        mutableDestination.value = RootComponent.PrimaryDestination.Templates
        mutableStack.value = singleChildStack(RootComponent.Child.Catalog(catalogComponent))
    }

    private fun childFor(destination: RootComponent.PrimaryDestination): RootComponent.Child = when (destination) {
        RootComponent.PrimaryDestination.Today -> RootComponent.Child.Session(sessionComponent)
        RootComponent.PrimaryDestination.Sessions -> RootComponent.Child.History(historyComponent)
        RootComponent.PrimaryDestination.Templates -> RootComponent.Child.Templates(templateComponent)
        RootComponent.PrimaryDestination.Settings -> RootComponent.Child.Settings(settingsComponent)
    }

    private fun singleChildStack(child: RootComponent.Child): ChildStack<Unit, RootComponent.Child> =
        ChildStack(active = Child.Created(configuration = Unit, instance = child, key = child::class.toString()))
}
