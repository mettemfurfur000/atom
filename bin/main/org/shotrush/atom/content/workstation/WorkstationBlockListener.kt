package org.shotrush.atom.content.workstation

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.core.api.annotation.RegisterSystem


@RegisterSystem(
    id = "workstation_block_listener",
    priority = 5,
    toggleable = false,
    description = "Handles workstation block break events and cleanup"
)
class WorkstationBlockListener(private val plugin: Plugin) : Listener {
    
    companion object {
        private val WORKSTATION_TYPES = setOf(
            "knapping_station",
            "leather_bed",
            "crafting_basket"
        )
    }
    
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val workstationType = getWorkstationType(block) ?: return
        
        plugin.logger.info("Workstation broken: $workstationType at ${block.location}")
        
        
        val pos = BlockPos(block.x, block.y, block.z)
        
        
        val workstationData = WorkstationDataManager.getWorkstationData(pos, workstationType)
        val placedItems = workstationData.placedItems.toList() 
        
        
        if (placedItems.isNotEmpty()) {
            val dropLocation = block.location.add(0.5, 0.5, 0.5)
            placedItems.forEach { placedItem ->
                
                block.world.dropItemNaturally(dropLocation, placedItem.item)
                
                
                placedItem.displayUUID?.let { uuid ->
                    org.bukkit.Bukkit.getEntity(uuid)?.remove()
                }
            }
            plugin.logger.info("Dropped ${placedItems.size} items from workstation")
        }
        
        
        cleanupNearbyEntities(block.location)
        
        
        WorkstationDataManager.removeWorkstationData(pos)
        
        
        WorkstationDataManager.saveData()
    }
    
    
    private fun getWorkstationType(block: Block): String? {
        val state = CraftEngineBlocks.getCustomBlockState(block) ?: return null
        
        for (type in WORKSTATION_TYPES) {
            if (state.owner().matchesKey(Key.of("atom:$type"))) {
                return type
            }
        }
        return null
    }
    
    
    private fun cleanupNearbyEntities(location: Location, radius: Double = 1.5) {
        val centerLocation = location.clone().add(0.5, 0.5, 0.5)
        
        location.world?.getNearbyEntities(centerLocation, radius, radius, radius)?.forEach { entity ->
            when (entity) {
                is ItemDisplay -> {
                    
                    if (entity.location.distance(centerLocation) < radius) {
                        plugin.logger.info("Removing orphaned ItemDisplay at ${entity.location}")
                        entity.remove()
                    }
                }
                is Interaction -> {
                    
                    if (entity.location.distance(centerLocation) < radius) {
                        plugin.logger.info("Removing orphaned Interaction at ${entity.location}")
                        entity.remove()
                    }
                }
            }
        }
    }
}
