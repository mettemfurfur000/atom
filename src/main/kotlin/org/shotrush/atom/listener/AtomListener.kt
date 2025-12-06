package org.shotrush.atom.listener

import org.bukkit.event.Event
import org.bukkit.event.Listener
import kotlin.coroutines.CoroutineContext

interface AtomListener : Listener {
    val eventDefs: Map<Class<out Event>, (event: Event) -> CoroutineContext>
}

inline fun <reified T : Event> eventDef(noinline runner: (event: T) -> CoroutineContext): Pair<Class<out Event>, (event: Event) -> CoroutineContext> {
    return (T::class.java to runner) as Pair<Class<out Event>, (event: Event) -> CoroutineContext>
}
