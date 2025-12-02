package org.shotrush.atom.content.systems.groundstorage

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.ItemFrame
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.data.PersistentData

/**
 * Utility class for working with ground items from external systems
 * @deprecated Use GroundItemDisplayUtils instead for ItemDisplay-based ground items
 */
@Deprecated(
    message = "Use GroundItemDisplayUtils instead for ItemDisplay-based ground items",
    replaceWith = ReplaceWith("GroundItemDisplayUtils", "org.shotrush.atom.content.systems.groundstorage.GroundItemDisplayUtils")
)
object GroundItemUtils {
    
    private const val GROUND_ITEM_KEY = "ground_item_frame"
    private const val GROUND_ITEM_DISPLAY_KEY = "ground_item_display"
    
    /**
     * Find all ground items within a specified radius of a location
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return List of ItemFrames that are ground items
     */
    fun findGroundItemsInRadius(center: Location, radius: Double = 0.6): List<ItemFrame> {
        return center.world.getNearbyEntities(center, radius, radius, radius)
            .filterIsInstance<ItemFrame>()
            .filter { frame ->
                PersistentData.isFlagged(frame, GROUND_ITEM_KEY)
            }
    }

    /**
     * Find all ground item displays within a specified radius of a location
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return List of ItemDisplays that are ground items
     */
    fun findGroundItemDisplaysInRadius(center: Location, radius: Double = 1.5): List<org.bukkit.entity.ItemDisplay> {
        return center.world.getNearbyEntities(center, radius, radius, radius)
            .filterIsInstance<org.bukkit.entity.ItemDisplay>()
            .filter { display ->
                PersistentData.isFlagged(display, GROUND_ITEM_DISPLAY_KEY)
            }
    }
    
    /**
     * Find ground item directly above a block (on its top surface)
     * @param block The block to check above
     * @return ItemFrame if found, null otherwise
     */
    fun findGroundItemAbove(block: Block): ItemFrame? {
        val searchLocation = block.location.add(0.5, 1.0, 0.5)
        return findGroundItemsInRadius(searchLocation, 0.6).firstOrNull()
    }

    fun isObstructed(frame: ItemFrame, vanilla: Material? = null, customKey: Key? = null): ItemStack? {
        val block = frame.location.block

        if (vanilla != null && block.type == vanilla) {
            return getGroundItem(frame)
        }
        if (customKey != null) {
            val customState = CraftEngineBlocks.getCustomBlockState(block)
            if (customState != null) {
                val owner = customState.owner()
                if (owner.matchesKey(customKey)) {
                    return getGroundItem(frame)
                }
            }
        }

        return null
    }
    
    /**
     * Find ground item directly below a location (on the block surface below)
     * @param location The location to check below
     * @return ItemFrame if found, null otherwise
     */
    fun findGroundItemBelow(location: Location): ItemFrame? {
        val searchLocation = location.clone().add(0.0, -1.0, 0.0)
        return findGroundItemsInRadius(searchLocation, 0.6).firstOrNull()
    }
    
    /**
     * Find all ground items on the same block as the given location
     * @param location The location to check
     * @return List of ItemFrames on the same block
     */
    fun findGroundItemsOnBlock(location: Location): List<ItemFrame> {
        val blockLocation = location.block.location
        return findGroundItemsInRadius(blockLocation.add(0.5, 1.0, 0.5), 0.8)
            .filter { frame ->
                val frameBlock = frame.location.block.location
                frameBlock.blockX == blockLocation.blockX &&
                frameBlock.blockZ == blockLocation.blockZ &&
                frameBlock.blockY == blockLocation.blockY + 1
            }
    }
    
    /**
     * Get the item from a ground item frame
     * @param frame The ItemFrame to get the item from
     * @return ItemStack if the frame contains an item, null otherwise
     */
    fun getGroundItem(frame: ItemFrame): ItemStack? {
        val item = frame.item
        return if (item.type != Material.AIR) item else null
    }
    
    /**
     * Check if an ItemFrame is a ground item
     * @param frame The ItemFrame to check
     * @return true if it's a ground item, false otherwise
     */
    fun isGroundItem(frame: ItemFrame): Boolean {
        return PersistentData.isFlagged(frame, GROUND_ITEM_KEY)
    }

    /**
     * Check if an ItemDisplay is a ground item
     * @param display The ItemDisplay to check
     * @return true if it's a ground item, false otherwise
     */
    fun isGroundItem(display: org.bukkit.entity.ItemDisplay): Boolean {
        return PersistentData.isFlagged(display, GROUND_ITEM_DISPLAY_KEY)
    }
    
    /**
     * Remove a ground item and drop its contents
     * @param frame The ItemFrame to remove
     * @param dropNaturally Whether to drop the item naturally or at exact location
     */
    fun removeGroundItem(frame: ItemFrame, dropNaturally: Boolean = true) {
        val item = getGroundItem(frame)
        if (item != null) {
            if (dropNaturally) {
                frame.world.dropItemNaturally(frame.location, item)
            } else {
                frame.world.dropItem(frame.location, item)
            }
        }
        frame.remove()
    }

    /**
     * Remove a ground item display and drop its contents
     * @param display The ItemDisplay to remove
     * @param dropNaturally Whether to drop the item naturally or at exact location
     */
    fun removeGroundItem(display: org.bukkit.entity.ItemDisplay, dropNaturally: Boolean = true) {
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
     * Find ground items containing specific materials within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @param materials The materials to search for
     * @return List of ItemFrames containing the specified materials
     */
    fun findGroundItemsWithMaterials(
        center: Location, 
        radius: Double, 
        materials: Set<Material>
    ): List<ItemFrame> {
        return findGroundItemsInRadius(center, radius)
            .filter { frame ->
                val item = getGroundItem(frame)
                item != null && materials.contains(item.type)
            }
    }
    
    /**
     * Find the closest ground item to a location within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return The closest ItemFrame or null if none found
     */
    fun findClosestGroundItem(center: Location, radius: Double = 0.6): ItemFrame? {
        return findGroundItemsInRadius(center, radius)
            .minByOrNull { frame -> frame.location.distance(center) }
    }
    
    /**
     * Count ground items within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return Number of ground items found
     */
    fun countGroundItems(center: Location, radius: Double): Int {
        return findGroundItemsInRadius(center, radius).size
    }
    
    /**
     * Check if there are any ground items within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return true if any ground items found, false otherwise
     */
    fun hasGroundItemsInRadius(center: Location, radius: Double): Boolean {
        return findGroundItemsInRadius(center, radius).isNotEmpty()
    }
    
    /**
     * Find all ground items on the same block as the given location
     * @param location The location to check
     * @return List of ItemFrames on the same block
     */
    fun findAllGroundItems(location: Location): List<ItemFrame> {
        return findGroundItemsOnBlock(location)
    }
    
    /**
     * Set the item contents of a ground item frame
     * @param frame The ItemFrame to modify
     * @param newItem The new ItemStack to place in the frame (null or AIR to clear)
     * @param playSound Whether to play a sound effect when changing the item
     * @return true if the item was successfully changed, false if not a ground item
     */
    fun setGroundItem(frame: ItemFrame, newItem: ItemStack?, playSound: Boolean = true): Boolean {
        if (!isGroundItem(frame)) return false
        
        val itemToSet = when {
            newItem == null -> ItemStack(Material.AIR)
            newItem.type == Material.AIR -> ItemStack(Material.AIR)
            else -> newItem.clone().apply { amount = 1 } 
        }
        
        frame.setItem(itemToSet, playSound)
        return true
    }
    
    /**
     * Replace the item in a ground item frame with a new item
     * @param frame The ItemFrame to modify
     * @param newItem The new ItemStack to place in the frame
     * @param playSound Whether to play a sound effect when changing the item
     * @return The previous ItemStack that was in the frame, or null if frame was empty or not a ground item
     */
    fun replaceGroundItem(frame: ItemFrame, newItem: ItemStack, playSound: Boolean = true): ItemStack? {
        if (!isGroundItem(frame)) return null
        
        val previousItem = getGroundItem(frame)
        setGroundItem(frame, newItem, playSound)
        return previousItem
    }
    
    /**
     * Clear the contents of a ground item frame (set to AIR)
     * @param frame The ItemFrame to clear
     * @param playSound Whether to play a sound effect when clearing
     * @return The ItemStack that was removed, or null if frame was empty or not a ground item
     */
    fun clearGroundItem(frame: ItemFrame, playSound: Boolean = true): ItemStack? {
        if (!isGroundItem(frame)) return null
        
        val previousItem = getGroundItem(frame)
        frame.setItem(ItemStack(Material.AIR), playSound)
        return previousItem
    }
    
    /**
     * Find ground item displays containing specific materials within a radius
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @param materials The materials to search for
     * @return List of ItemDisplays containing the specified materials
     */
    fun findGroundItemDisplaysWithMaterials(
        center: Location,
        radius: Double,
        materials: Set<Material>
    ): List<org.bukkit.entity.ItemDisplay> {
        return findGroundItemDisplaysInRadius(center, radius)
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
    fun findClosestGroundItemDisplay(center: Location, radius: Double = 1.5): org.bukkit.entity.ItemDisplay? {
        return findGroundItemDisplaysInRadius(center, radius)
            .minByOrNull { display -> display.location.distance(center) }
    }

    /**
     * Get the item from a ground item display
     * @param display The ItemDisplay to get the item from
     * @return ItemStack if the display contains an item, null otherwise
     */
    fun getGroundItem(display: org.bukkit.entity.ItemDisplay): ItemStack? {
        val item = display.itemStack
        return if (item.type != Material.AIR) item else null
    }

    /**
     * Set the item contents of a ground item display
     * @param display The ItemDisplay to modify
     * @param newItem The new ItemStack to display (null or AIR to clear)
     * @param playSound Whether to play a sound effect when changing the item
     * @return true if the item was successfully changed, false if not a ground item
     */
    fun setGroundItem(display: org.bukkit.entity.ItemDisplay, newItem: ItemStack?, playSound: Boolean = true): Boolean {
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
     * Transform the item in a ground item frame using a transformation function
     * @param frame The ItemFrame to modify
     * @param transform Function that takes the current ItemStack and returns the new one
     * @param playSound Whether to play a sound effect when changing the item
     * @return true if the transformation was applied, false if not a ground item
     */
    fun transformGroundItem(frame: ItemFrame, transform: (ItemStack?) -> ItemStack?, playSound: Boolean = true): Boolean {
        if (!isGroundItem(frame)) return false
        
        val currentItem = getGroundItem(frame)
        val newItem = transform(currentItem)
        setGroundItem(frame, newItem, playSound)
        return true
    }
    
    /**
     * Swap the contents of two ground item frames
     * @param frame1 The first ItemFrame
     * @param frame2 The second ItemFrame
     * @param playSound Whether to play sound effects when swapping
     * @return true if the swap was successful, false if either frame is not a ground item
     */
    fun swapGroundItems(frame1: ItemFrame, frame2: ItemFrame, playSound: Boolean = true): Boolean {
        if (!isGroundItem(frame1) || !isGroundItem(frame2)) return false
        
        val item1 = getGroundItem(frame1)
        val item2 = getGroundItem(frame2)
        
        setGroundItem(frame1, item2, playSound)
        setGroundItem(frame2, item1, playSound)
        return true
    }
}
