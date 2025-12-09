package org.shotrush.atom.api

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.isCustomItem
import org.shotrush.atom.util.Key

sealed interface ItemFilter {
    fun matches(stack: ItemStack): Boolean

    companion object {
        fun ofTag(tag: Key): ItemFilter = TagFilter(tag)
        fun ofTag(tag: String): ItemFilter {
            if (tag.startsWith("#")) {
                return TagFilter(Key.of(tag.substring(1)))
            }
            return TagFilter(Key.of(tag))
        }
    }
}

data class TagFilter(val tag: Key) : ItemFilter {
    override fun matches(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.isCustomItem()) {
            val stack = CraftEngineItems.byItemStack(stack) ?: return false
            return stack.settings().tags().contains(tag.toCEKey())
        } else {
            val tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tag.toBukkitKey(), Material::class.java) ?: return false
            return tag.isTagged(stack.type)
        }
    }
}
