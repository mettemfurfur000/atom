package org.shotrush.atom.systems.room

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.shotrush.atom.EventObject

abstract class RoomEvent(room: Room) : Event(false) {

}

class RoomCreateEvent(val room: Room) : RoomEvent(room) {
    companion object : EventObject {
        val HANDLER_LIST: HandlerList = HandlerList()

        @JvmStatic
        override fun getHandlerList(): HandlerList = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}

class RoomDestroyEvent(val room: Room) : RoomEvent(room) {
    companion object : EventObject {
        val HANDLER_LIST: HandlerList = HandlerList()

        @JvmStatic
        override fun getHandlerList(): HandlerList = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}