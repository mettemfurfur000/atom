package org.civlabs.atom.core.system.structure

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.civlabs.atom.core.util.EventObject

abstract class StructureEvent(structure: Structure) : Event(false) {

}

class StructureCreateEvent(val structure: Structure) : StructureEvent(structure) {
    companion object : EventObject {
        val HANDLER_LIST: HandlerList = HandlerList()

        @JvmStatic
        override fun getHandlerList(): HandlerList = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}

class StructureDestroyEvent(val structure: Structure) : StructureEvent(structure) {
    companion object : EventObject {
        val HANDLER_LIST: HandlerList = HandlerList()

        @JvmStatic
        override fun getHandlerList(): HandlerList = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}
