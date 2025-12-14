package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.shotrush.atom.Atom
import org.shotrush.atom.item.Molds

/**
 * Eventually this will move to a item behaviour in CE, but for now its this.
 */
object MoldListener : Listener {
    fun register(atom: Atom) {
        val eventDispatcher = mapOf(eventDef<PlayerInteractEvent> {
            atom.entityDispatcher(it.player)
        })
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (!event.player.isSneaking) return
        if (Molds.isFilledMold(item)) {
            event.isCancelled = true
            val (mold, head) = Molds.emptyMold(item)
            event.player.inventory.remove(item.clone().apply { amount = 1 })
            if (!mold.isEmpty) event.player.inventory.addItem(mold)
            event.player.inventory.addItem(head)
        }
    }
}