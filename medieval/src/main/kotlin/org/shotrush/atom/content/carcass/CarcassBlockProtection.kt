package org.shotrush.atom.content.carcass

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.util.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.api.annotation.RegisterSystem

@RegisterSystem(
    id = "carcass_block_protection",
    priority = 10,
    toggleable = false,
    description = "Prevents players from breaking carcass blocks directly"
)
class CarcassBlockProtection(plugin: Plugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (isCarcassBlock(event.block)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { isCarcassBlock(it) }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { isCarcassBlock(it) }
    }

    private fun isCarcassBlock(block: org.bukkit.block.Block): Boolean {
        val state = CraftEngineBlocks.getCustomBlockState(block) ?: return false
        return state.owner().matchesKey(CarcassBlock.CARCASS_KEY)
    }
}
