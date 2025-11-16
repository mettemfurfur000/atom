package org.shotrush.atom.content.systems.groundstorage

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData
import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import org.shotrush.atom.Atom
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.core.data.PersistentData
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Advanced ground item handler using ItemDisplay entities
 * Automatically converts dropped items to persistent ground displays
 */
@RegisterSystem(
    id = "ground_item_display_handler",
    priority = 10,
    description = "Advanced ground items with ItemDisplay entities and auto-conversion",
    toggleable = true,
    enabledByDefault = true
)
class GroundItemDisplayHandler(private val plugin: Plugin) : Listener {

    companion object {
        private const val GROUND_ITEM_KEY = "ground_item_display"
        private const val PICKUP_RANGE = 3.0
        private const val INTERACTION_RANGE = 4.0
        private const val VELOCITY_THRESHOLD = 0.1
        private const val STACKING_RADIUS = 1.0
        private const val OCCUPANCY_RADIUS = 0.25
        private const val MAX_PLACEMENT_ATTEMPTS = 6
    }

    private val pendingItems = mutableSetOf<UUID>()
    private val conversionJobs = mutableMapOf<UUID, Job>()

    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity
        if (item.itemStack.type == Material.AIR) return

        // Mark item for conversion
        pendingItems.add(item.uniqueId)

        // Schedule conversion check using Folia's entity scheduler
        val job = Atom.instance.launch(Atom.instance.entityDispatcher(item)) {
            // Check every 5 ticks (250ms)
            while (pendingItems.contains(item.uniqueId)) {
                delay(250L)

                if (!item.isValid || item.isDead) {
                    cleanupPendingItem(item.uniqueId)
                    break
                }

                // Check if item has come to rest
                val velocity = item.velocity
                if (velocity.length() < VELOCITY_THRESHOLD && item.isOnGround) {
                    convertItemToDisplay(item)
                    break
                }
            }
        }

        conversionJobs[item.uniqueId] = job
    }

    private fun convertItemToDisplay(item: Item) {
        val location = item.location
        val itemStack = item.itemStack

        // Clean up tracking
        cleanupPendingItem(item.uniqueId)

        // Remove original item
        item.remove()

        // Try to stack with existing ground items first
        if (!tryStackWithExisting(location, itemStack)) {
            // If not stacked, create new ground item display
            createGroundItemDisplay(location, itemStack)
        }
    }

    private fun tryStackWithExisting(location: Location, itemStack: ItemStack): Boolean {
        val existingItems = findGroundItemsInRadius(location, STACKING_RADIUS)

        for (display in existingItems) {
            val existingItem = display.itemStack

            if (existingItem.isSimilar(itemStack)) {
                val existingAmount = getGroundItemAmount(display)
                val maxStackSize = existingItem.maxStackSize

                if (existingAmount < maxStackSize) {
                    val spaceLeft = maxStackSize - existingAmount
                    val amountToAdd = minOf(itemStack.amount, spaceLeft)

                    val newAmount = existingAmount + amountToAdd
                    setGroundItemAmount(display, newAmount)

                    if (amountToAdd < itemStack.amount) {
                        val remainder = itemStack.clone().apply {
                            amount = itemStack.amount - amountToAdd
                        }
                        createGroundItemDisplay(location, remainder)
                    }

                    return true
                }
            }
        }

        return false
    }

    private fun cleanupPendingItem(itemId: UUID) {
        pendingItems.remove(itemId)
        conversionJobs[itemId]?.cancel()
        conversionJobs.remove(itemId)
    }

    private fun createGroundItemDisplay(location: Location, itemStack: ItemStack) {
        val displayLocation = findFreePosition(location)

        val display = location.world?.spawn(displayLocation, ItemDisplay::class.java) ?: return

        display.setItemStack(itemStack.clone().apply { amount = 1 })
        display.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
        display.billboard = org.bukkit.entity.Display.Billboard.FIXED

        val transformation = Transformation(
            Vector3f(0f, 0f, 0f),
            AxisAngle4f(Math.PI.toFloat() / 2, 1f, 0f, 0f),
            Vector3f(0.5f, 0.5f, 0.5f),
            AxisAngle4f(0f, 0f, 0f, 0f)
        )
        display.transformation = transformation

        PersistentData.flag(display, GROUND_ITEM_KEY)

        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "original_item_type"),
            org.bukkit.persistence.PersistentDataType.STRING,
            itemStack.type.name
        )
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "original_item_amount"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            itemStack.amount
        )
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "spawn_time"),
            org.bukkit.persistence.PersistentDataType.LONG,
            System.currentTimeMillis()
        )

        location.world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f)
    }

    private fun findFreePosition(location: Location): Location {
        val block = location.block
        val blockAbove = block.getRelative(0, 1, 0)
        
        // Check if the block we're trying to place on is solid or has special properties
        val baseY = when {
            // If block is solid, place on top of it
            block.type.isSolid -> block.y + 1.0
            // If block above is solid, place on top of that (avoid spawning inside blocks)
            blockAbove.type.isSolid -> blockAbove.y + 1.0
            // Otherwise place slightly above current position
            else -> location.y + 0.1
        }
        
        val baseLocation = location.clone().apply {
            y = baseY
        }

        if (isPositionFree(baseLocation)) {
            return baseLocation
        }

        for (attempt in 1..MAX_PLACEMENT_ATTEMPTS) {
            val angle = (attempt * 60.0) * (Math.PI / 180.0)
            val radius = OCCUPANCY_RADIUS
            val x = baseLocation.x + cos(angle) * radius
            val z = baseLocation.z + sin(angle) * radius

            val testLocation = baseLocation.clone().apply {
                this.x = x
                this.z = z
            }

            if (isPositionFree(testLocation)) {
                return testLocation
            }
        }

        return baseLocation
    }

    private fun isPositionFree(location: Location): Boolean {
        val nearbyItems = findGroundItemsInRadius(location, OCCUPANCY_RADIUS)
        return nearbyItems.isEmpty()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (!event.player.isSneaking) return

        val player = event.player
        val eyeLocation = player.eyeLocation

        val direction = eyeLocation.direction
        val result = eyeLocation.world.rayTrace(
            eyeLocation,
            direction,
            INTERACTION_RANGE,
            org.bukkit.FluidCollisionMode.NEVER,
            true,
            0.1,
            { entity -> entity is ItemDisplay && isGroundItem(entity) }
        )

        val targetDisplay = result?.hitEntity as? ItemDisplay
        if (targetDisplay != null && isGroundItem(targetDisplay)) {
            event.isCancelled = true
            pickupGroundItem(player, targetDisplay)
        }
    }

    private fun pickupGroundItem(player: Player, display: ItemDisplay) {
        val itemStack = display.itemStack
        if (itemStack.type == Material.AIR) {
            display.remove()
            return
        }

        val actualAmount = getGroundItemAmount(display)
        val pickupStack = itemStack.clone().apply { amount = actualAmount }

        val remaining = player.inventory.addItem(pickupStack)

        if (remaining.isEmpty()) {
            display.location.world?.playSound(
                display.location,
                Sound.ENTITY_ITEM_PICKUP,
                0.7f,
                1.0f
            )
            display.remove()
        } else {
            val remainingAmount = remaining.values.first().amount
            setGroundItemAmount(display, remainingAmount)
            display.location.world?.playSound(
                display.location,
                Sound.ENTITY_ITEM_PICKUP,
                0.5f,
                0.8f
            )
        }
    }

    @EventHandler
    fun onItemDespawn(event: ItemDespawnEvent) {
        cleanupPendingItem(event.entity.uniqueId)
    }

    @EventHandler
    fun onItemMerge(event: ItemMergeEvent) {
        if (pendingItems.contains(event.entity.uniqueId) ||
            pendingItems.contains(event.target.uniqueId)) {
            event.isCancelled = true
        }
    }

    private fun findGroundItemsInRadius(location: Location, radius: Double): List<ItemDisplay> {
        return location.world?.getNearbyEntities(location, radius, radius, radius)
            ?.filterIsInstance<ItemDisplay>()
            ?.filter { display -> isGroundItem(display) }
            ?: emptyList()
    }

    private fun isGroundItem(display: ItemDisplay): Boolean {
        return PersistentData.isFlagged(display, GROUND_ITEM_KEY)
    }

    private fun getGroundItemAmount(display: ItemDisplay): Int {
        return display.persistentDataContainer.get(
            org.bukkit.NamespacedKey(plugin, "original_item_amount"),
            org.bukkit.persistence.PersistentDataType.INTEGER
        ) ?: 1
    }

    private fun setGroundItemAmount(display: ItemDisplay, amount: Int) {
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "original_item_amount"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            amount
        )
    }

    fun getGroundItemData(display: ItemDisplay): ItemStack? {
        return if (isGroundItem(display)) {
            val container = display.persistentDataContainer
            val typeName = container.get(
                org.bukkit.NamespacedKey(plugin, "original_item_type"),
                org.bukkit.persistence.PersistentDataType.STRING
            ) ?: return null
            val material = Material.valueOf(typeName)
            val amount = container.get(
                org.bukkit.NamespacedKey(plugin, "original_item_amount"),
                org.bukkit.persistence.PersistentDataType.INTEGER
            ) ?: 1
            ItemStack(material, amount)
        } else {
            null
        }
    }

    fun cleanup() {
        conversionJobs.values.forEach { it.cancel() }
        conversionJobs.clear()
        pendingItems.clear()
    }
}