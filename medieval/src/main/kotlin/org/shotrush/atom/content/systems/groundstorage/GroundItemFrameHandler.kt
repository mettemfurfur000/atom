package org.shotrush.atom.content.systems.groundstorage

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Rotation
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import kotlin.random.Random
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.core.data.PersistentData

/**
 * Simple ground item handler with random rotation
 */
@RegisterSystem(
    id = "ground_item_frame_handler",
    priority = 15,
    description = "Place items on ground with random rotation [DEPRECATED - Use GroundItemDisplayHandler]",
    toggleable = true,
    enabledByDefault = false
)
class GroundItemFrameHandler(private val plugin: Plugin) : Listener {

    companion object {
        private const val GROUND_ITEM_KEY = "ground_item_frame"
    }
    

    // Right-click placement mechanic disabled - use GroundItemDisplayHandler instead
    // @EventHandler
    // fun onPlayerInteract(event: PlayerInteractEvent) {
    //     if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
    //
    //     val block = event.clickedBlock ?: return
    //     val player = event.player
    //     val hand = player.inventory.itemInMainHand
    //
    //     if (event.blockFace != BlockFace.UP || !player.isSneaking || hand.type == Material.AIR) return
    //     if (!block.type.isSolid) return
    //
    //     event.isCancelled = true
    //     val location = block.location.add(0.5, 1.0625, 0.5)
    //
    //     createGroundItem(location, hand, player)
    // }
    //
    // private fun createGroundItem(location: Location, hand: ItemStack, player: Player) {
    //     location.world.spawn(location, ItemFrame::class.java).apply {
    //         setFacingDirection(BlockFace.UP, true)
    //         isVisible = false
    //         isFixed = false
    //         setItem(hand.clone().apply { amount = 1 }, false)
    //         PersistentData.flag(this, GROUND_ITEM_KEY)
    //
    //         rotation = Rotation.values()[Random.nextInt(Rotation.values().size)]
    //     }
    //     location.world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f)
    //     consumeItem(player, hand)
    // }
    //
    // private fun consumeItem(player: Player, item: ItemStack) {
    //     if (item.amount > 1) item.amount-- else player.inventory.setItemInMainHand(null)
    // }

    @EventHandler
    fun onFrameBreak(event: HangingBreakEvent) {
        val frame = event.entity as? ItemFrame ?: return
        if (!isGroundItem(frame)) return
        
        if (event.cause == HangingBreakEvent.RemoveCause.OBSTRUCTION) {
            event.isCancelled = true
            return
        }
        
        dropItem(frame)
        event.isCancelled = true
        frame.remove()
    }

    @EventHandler
    fun onFrameDamage(event: EntityDamageByEntityEvent) {
        val frame = event.entity as? ItemFrame ?: return
        if (!isGroundItem(frame)) return
        
        dropItem(frame)
        event.isCancelled = true
        frame.remove()
    }

    @EventHandler
    fun onFrameInteract(event: PlayerInteractEntityEvent) {
        val frame = event.rightClicked as? ItemFrame ?: return
        if (!isGroundItem(frame)) return
        
        
        event.isCancelled = true
    }
    
    private fun isGroundItem(frame: ItemFrame) = 
        PersistentData.isFlagged(frame, GROUND_ITEM_KEY)
    
    private fun dropItem(frame: ItemFrame) {
        val item = frame.item
        if (item.type != Material.AIR) {
            frame.world.dropItemNaturally(frame.location, item)
        }
    }
    
}
