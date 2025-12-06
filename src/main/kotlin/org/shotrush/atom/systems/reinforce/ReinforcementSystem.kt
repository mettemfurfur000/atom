package org.shotrush.atom.systems.reinforce

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.shotrush.atom.Atom
import org.shotrush.atom.listener.AtomListener
import org.shotrush.atom.listener.eventDef

object ReinforcementSystem : AtomListener {
    override val eventDefs = mapOf(
        eventDef<BlockBreakEvent> {
            Atom.instance.regionDispatcher(it.block.location)
        }
    )

    fun getReinforcementLevel(location: Location): ReinforceType? {
        return null
    }

    fun setReinforcementLevel(location: Location, level: ReinforceType?) {

    }

    @EventHandler
    suspend fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val pos = block.location
        val level = getReinforcementLevel(pos) ?: return
        event.isCancelled = true
        pos.world?.dropItemNaturally(pos, level.itemRef.createStack())
        setReinforcementLevel(pos, null)
    }
}