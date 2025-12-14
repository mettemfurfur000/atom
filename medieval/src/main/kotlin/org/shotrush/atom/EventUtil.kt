package org.shotrush.atom

import org.bukkit.event.HandlerList

interface EventObject {
    fun getHandlerList(): HandlerList
}