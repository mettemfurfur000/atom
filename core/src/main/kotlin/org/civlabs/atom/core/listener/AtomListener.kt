package org.civlabs.atom.core.listener

import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.civlabs.atom.core.CoreAtom
import kotlin.coroutines.CoroutineContext

typealias EventClass = Class<out Event>
typealias EventRunner = (event: Event) -> CoroutineContext

inline fun <reified T : Event> eventDef(noinline runner: (event: T) -> CoroutineContext) =
    (T::class.java as EventClass) to (runner as EventRunner)

interface AtomListener : Listener {
    val eventDefs: Map<EventClass, EventRunner>
}


fun AtomListener.register() =
    CoreAtom.instance.server.pluginManager.registerSuspendingEvents(this, CoreAtom.instance, eventDefs)