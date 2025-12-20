package org.civlabs.atom.core.util

import org.bukkit.event.HandlerList

interface EventObject {
    fun getHandlerList(): HandlerList
}