package org.shotrush.atom.content.foraging.items

import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.item.Items
import org.shotrush.atom.item.isItem
import org.shotrush.atom.matches

object SharpenedFlint {
    fun isSharpenedFlint(stack: ItemStack): Boolean = stack.matches(Items.SharpenedFlint)

    fun damageItem(item: ItemStack?, player: Player, breakChance: Double = 0.4) {
        if (item == null || item.amount <= 0) return

        val currentAmount = item.amount

        if (Math.random() < breakChance) {
            item.amount = currentAmount - 1

            if (currentAmount - 1 <= 0) {
                player.world.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                ActionBarManager.send(player, "<red>Your Sharpened Flint broke!</red>")
            } else {
                player.world.playSound(player.location, Sound.BLOCK_STONE_HIT, 0.5f, 1.2f)
            }
        }
    }
}