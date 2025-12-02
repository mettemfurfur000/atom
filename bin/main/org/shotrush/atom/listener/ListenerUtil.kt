package org.shotrush.atom.listener

import org.bukkit.event.Event
import kotlin.coroutines.CoroutineContext

inline fun <reified T : Event> eventDef(noinline runner: (event: T) -> CoroutineContext): Pair<Class<out Event>, (event: Event) -> CoroutineContext> {
    return (T::class.java to runner) as Pair<Class<out Event>, (event: Event) -> CoroutineContext>
}
