package org.shotrush.atom.systems.blockbreak

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.shotrush.atom.Atom
import org.shotrush.atom.api.BlockRef
import org.shotrush.atom.asReference
import org.shotrush.atom.listener.AtomListener
import org.shotrush.atom.listener.EventClass
import org.shotrush.atom.listener.EventRunner
import org.shotrush.atom.listener.eventDef

object BlockBreakSystem : AtomListener {
    override val eventDefs: Map<EventClass, EventRunner> = mapOf(
        eventDef<BlockDamageEvent> {
            Atom.instance.regionDispatcher(it.block.location)
        },
        eventDef<BlockDamageAbortEvent> {
            Atom.instance.regionDispatcher(it.block.location)
        }
    )

    var globalSpeed = 4.0
    const val MODIFIER_KEY: String = "block_break_speed"
    val blockMultipliers = HashMap<BlockRef, Double>()

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onBlockDamage(event: BlockDamageEvent) {
        applySpeedModifier(event.player, event.block)
    }

    @EventHandler
    private fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        clearSpeedModifier(event.player)
    }

    private fun applySpeedModifier(player: Player, block: Block) {
        val attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return
        clearSpeedModifier(player)
        val multiplier = getMultiplier(block)
        if (multiplier == 1.0) return
        val attributeValue = (1.0 / multiplier) - 1.0
        val key = NamespacedKey(Atom.instance, MODIFIER_KEY)
        val modifier = AttributeModifier(
            key,
            attributeValue,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.ANY
        )
        attribute.addModifier(modifier)
    }

    private fun clearSpeedModifier(player: Player) {
        val attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return

        attribute.modifiers.stream()
            .filter { mod -> mod.key.key == MODIFIER_KEY }
            .forEach { modifier -> attribute.removeModifier(modifier) }
    }

    fun getMultiplier(block: Block): Double {
        var multiplier = globalSpeed
        val ref = block.asReference()
        if (blockMultipliers.containsKey(ref)) multiplier *= blockMultipliers[ref]!!
        return multiplier
    }
}