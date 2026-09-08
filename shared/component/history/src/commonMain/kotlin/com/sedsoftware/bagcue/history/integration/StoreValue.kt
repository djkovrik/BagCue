package com.sedsoftware.bagcue.history.integration

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.Store

internal fun <State : Any> Store<*, State, *>.asValue(): Value<State> = object : Value<State>() {
    override val value: State get() = state
    override fun subscribe(observer: (State) -> Unit): Cancellation {
        val disposable = states(com.arkivanov.mvikotlin.core.rx.observer(onNext = observer))
        return Cancellation(disposable::dispose)
    }
}
