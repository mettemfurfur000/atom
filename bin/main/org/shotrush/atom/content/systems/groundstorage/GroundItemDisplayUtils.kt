package org.shotrush.atom.content.systems.groundstorage

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.data.PersistentData
import java.util.*

/**
 * Utility class for working with ground item displays from external systems
 * Replaces the old ItemFrame-based utilities with ItemDisplay-based ones
 */
object GroundItemDisplayUtils {
    
    private const val GROUND_ITEM_KEY = "ground_item_display"
    
    /**
     * Find all ground item displays within a specified radius of a location
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return List of ItemDisplays that are ground items
     */
    fun findGroundItemsInRadius(center: Location, radius: Double = 1.5): List<ItemDisplay> {
        return center.world.getNearbyEntities(center, radius, radius, radius)
            .filterIsInstance<ItemDisplay>()
            .filter { display ->
                PersistentData.isFlagged(display, GROUND_ITEM_KEY)
            }
    }
    
    /**
     * Find ground item displays directly above a block (on its top surface)
     * @param block The block to check above
     * @return List of ItemDisplays found above the block
     */
    fun findGroundItemsAbove(block: Block): List<ItemDisplay> {
        val searchLocation = block.location.add(0.5, 0.5, 0.5)
        return findGroundItemsInRadius(searchLocation, 1.0)
            .filter { display ->
                val displayBlock = display.location.block
                displayBlock.x == block.x && displayBlock.z == block.z && displayBlock.y >= block.y
            }
    }

    /**
     * Find ground item displays directly below a location (on the block surface below)
     * @param location The location to check below
     * @return List of ItemDisplays found below the location
     */
    fun findGroundItemsBelow(location: Location): List<ItemDisplay> {
        val searchLocation = location.clone().add(0.0, -0.5, 0.0)
        return findGroundItemsInRadius(searchLocation, 1.0)
            .filter { display ->
                val displayBlock = display.location.block
                val targetBlock = location.block
                displayBlock.x == targetBlock.x && 
                displayBlock.z == targetBlock.z && 
                displayBlock.y <= targetBlock.y
            }
    }
    
    /**
     * Find all ground item displays on the same block as the given location
     * @param location The location to check
     * @return List of ItemDisplays on the same block
     */
    fun findGroundItemsOnBlock(location: Location): List<ItemDisplay> {
        val blockLocation = location.block.location
        val searchLocation = blockLocation.add(0.5, 0.5, 0.5)
        return findGroundItemsInRadius(searchLocation, 1.0)
            .filter { display ->
                val displayBlock = display.location.block.location
                displayBlock.blockX == blockLocation.blockX &&
                displayBlock.blockZ == blockLocation.blockZ &&
                displayBlock.blockY >= blockLocation.blockY
            }
    }
    
    /**
     * Check if a block is obstructed by ground items
     * @param display The ItemDisplay to check
     * @param vanilla Optional vanilla material to check against
     * @param customKey Optional custom block key to check against
     * @return ItemStack if obstructed, null otherwise
     */
    fun isObstructed(display: ItemDisplay, vanilla: Material? = null, customKey: Key? = null): ItemStack? {
        val block = display.location.block

        if (vanilla != null && block.type == vanilla) {
            return getGroundItem(display)
        }
        if (customKey != null) {
            val customState = CraftEngineBlocks.getCustomBlockState(block)
            if (customState != null) {
                val owner = customState.owner()
                if (owner.matchesKey(customKey)) {
                    return getGroundItem(display)
                }
            }
        }

        return null
    }
    
    /**
     * Get the item from a ground item display
     * @param display The ItemDisplay to get the item from
     * @return ItemStack if the display contains an item, null otherwise
     */
    fun getGroundItem(display: ItemDisplay): ItemStack? {
        val item = display.itemStack
        return if (item.type != Material.AIR) item else null
    }
    
    /**
     * Check if an ItemDisplay is a ground item
     * @param display The ItemDisplay to check
     * @return true if it's a ground item, false otherwise
     */
    fun isGroundItem(display: ItemDisplay): Boolean {
        return PersistentData.isFlagged(display, GROUND_ITEM_KEY)
    }
    
    /**
     * Remove a ground item display and drop its contents
     * @param display The ItemDisplay to remove
     * @param dropNaturally Whether to drop the item naturally or at exact location
     */
    fun removeGroundItem(display: ItemDisplay, dropNaturally: Boolean = true) {
        val item = getGroundItem(display)
        if (item != null) {
            if (dropNaturally) {
                display.world.dropItemNaturally(display.location, item)
            } else {
                display.world.dropItem(display.location, item)
            }
        }
        display.remove()
    }
    
    /**
     * Find ground item displays containing specific materials within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @param materials The materials to search for
     * @return List of ItemDisplays containing the specified materials
     */
    fun findGroundItemsWithMaterials(
        center: Location, 
        radius: Double, 
        materials: Set<Material>
    ): List<ItemDisplay> {
        return findGroundItemsInRadius(center, radius)
            .filter { display ->
                val item = getGroundItem(display)
                item != null && materials.contains(item.type)
            }
    }
    
    /**
     * Find the closest ground item display to a location within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return The closest ItemDisplay or null if none found
     */
    fun findClosestGroundItem(center: Location, radius: Double = 1.5): ItemDisplay? {
        return findGroundItemsInRadius(center, radius)
            .minByOrNull { display -> display.location.distance(center) }
    }
    
    /**
     * Count ground item displays within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return Number of ground items found
     */
    fun countGroundItems(center: Location, radius: Double): Int {
        return findGroundItemsInRadius(center, radius).size
    }
    
    /**
     * Check if there are any ground item displays within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return true if any ground items found, false otherwise
     */
    fun hasGroundItemsInRadius(center: Location, radius: Double): Boolean {
        return findGroundItemsInRadius(center, radius).isNotEmpty()
    }
    
    /**
     * Set the item contents of a ground item display
     * @param display The ItemDisplay to modify
     * @param newItem The new ItemStack to display (null or AIR to clear)
     * @param playSound Whether to play a sound effect when changing the item
     * @return true if the item was successfully changed, false if not a ground item
     */
    fun setGroundItem(display: ItemDisplay, newItem: ItemStack?, playSound: Boolean = true): Boolean {
        if (!isGroundItem(display)) return false
        
        val itemToSet = when {
            newItem == null -> ItemStack(Material.AIR)
            newItem.type == Material.AIR -> ItemStack(Material.AIR)
            else -> newItem.clone().apply { amount = 1 } 
        }
        
        display.setItemStack(itemToSet)
        
        if (playSound && itemToSet.type != Material.AIR) {
            display.world.playSound(display.location, Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 0.5f, 1.0f)
        }
        
        return true
    }
    
    /**
     * Replace the item in a ground item display with a new item
     * @param display The ItemDisplay to modify
     * @param newItem The new ItemStack to display
     * @param playSound Whether to play a sound effect when changing the item
     * @return The previous ItemStack that was in the display, or null if display was empty or not a ground item
     */
    fun replaceGroundItem(display: ItemDisplay, newItem: ItemStack, playSound: Boolean = true): ItemStack? {
        if (!isGroundItem(display)) return null
        
        val previousItem = getGroundItem(display)
        setGroundItem(display, newItem, playSound)
        return previousItem
    }
    
    /**
     * Clear the contents of a ground item display (set to AIR)
     * @param display The ItemDisplay to clear
     * @param playSound Whether to play a sound effect when clearing
     * @return The ItemStack that was removed, or null if display was empty or not a ground item
     */
    fun clearGroundItem(display: ItemDisplay, playSound: Boolean = true): ItemStack? {
        if (!isGroundItem(display)) return null
        
        val previousItem = getGroundItem(display)
        display.setItemStack(ItemStack(Material.AIR))
        
        if (playSound) {
            display.world.playSound(display.location, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 0.5f, 1.0f)
        }
        
        return previousItem
    }
    
    /**
     * Transform the item in a ground item display using a transformation function
     * @param display The ItemDisplay to modify
     * @param transform Function that takes the current ItemStack and returns the new one
     * @param playSound Whether to play a sound effect when changing the item
     * @return true if the transformation was applied, false if not a ground item
     */
    fun transformGroundItem(display: ItemDisplay, transform: (ItemStack?) -> ItemStack?, playSound: Boolean = true): Boolean {
        if (!isGroundItem(display)) return false
        
        val currentItem = getGroundItem(display)
        val newItem = transform(currentItem)
        setGroundItem(display, newItem, playSound)
        return true
    }
    
    /**
     * Swap the contents of two ground item displays
     * @param display1 The first ItemDisplay
     * @param display2 The second ItemDisplay
     * @param playSound Whether to play sound effects when swapping
     * @return true if the swap was successful, false if either display is not a ground item
     */
    fun swapGroundItems(display1: ItemDisplay, display2: ItemDisplay, playSound: Boolean = true): Boolean {
        if (!isGroundItem(display1) || !isGroundItem(display2)) return false
        
        val item1 = getGroundItem(display1)
        val item2 = getGroundItem(display2)
        
        setGroundItem(display1, item2, playSound)
        setGroundItem(display2, item1, playSound)
        return true
    }
    
    /**
     * Get all ground item displays in a chunk
     * @param location Any location within the target chunk
     * @return List of ItemDisplays in the chunk
     */
    fun getGroundItemsInChunk(location: Location): List<ItemDisplay> {
        val chunk = location.chunk
        return chunk.entities
            .filterIsInstance<ItemDisplay>()
            .filter { display -> isGroundItem(display) }
    }
    
    /**
     * Remove all ground item displays in a chunk
     * @param location Any location within the target chunk
     * @param dropNaturally Whether to drop items naturally or at exact locations
     * @return Number of items removed
     */
    fun clearGroundItemsInChunk(location: Location, dropNaturally: Boolean = true): Int {
        val items = getGroundItemsInChunk(location)
        items.forEach { display ->
            removeGroundItem(display, dropNaturally)
        }
        return items.size
    }
    
    /**
     * Get the spawn time of a ground item display
     * @param display The ItemDisplay to check
     * @return The timestamp when the item was spawned, or null if not a ground item
     */
    fun getSpawnTime(display: ItemDisplay): Long? {
        return if (isGroundItem(display)) {
            display.persistentDataContainer.get(
                org.bukkit.NamespacedKey(display.world?.name ?: return null, "spawn_time"),
                org.bukkit.persistence.PersistentDataType.LONG
            )
        } else {
            null
        }
    }
    
    /**
     * Check if a ground item display is old enough to despawn
     * @param display The ItemDisplay to check
     * @param maxAgeMinutes Maximum age in minutes before despawning
     * @return true if the item should despawn, false otherwise
     */
    fun shouldDespawn(display: ItemDisplay, maxAgeMinutes: Int): Boolean {
        val spawnTime = getSpawnTime(display) ?: return false
        val ageMinutes = (System.currentTimeMillis() - spawnTime) / (1000 * 60)
        return ageMinutes >= maxAgeMinutes
    }
    
    /**
     * Find all ground item displays on the same block as the given location
     * @param location The location to check
     * @return List of ItemDisplays on the same block
     */
    fun findAllGroundItems(location: Location): List<ItemDisplay> {
        return findGroundItemsOnBlock(location)
    }
}