package org.civlabs.atom.core.api

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.civlabs.atom.core.util.Key
import org.civlabs.atom.core.util.isCustomItem
import org.civlabs.atom.core.util.matches

sealed class SingleItemRef : ItemFilter {
    abstract fun createStack(amount: Int = 1): ItemStack

    companion object {
        fun material(material: Material) = MaterialRef(material)
        fun keyed(key: Key) = KeyedItemRef(key)
        fun atom(key: String) = keyed(Key("atom", key))
    }

    data class MaterialRef(val material: Material) : SingleItemRef() {
        override fun matches(stack: ItemStack): Boolean {
            return if (stack.isCustomItem()) false else stack.type == material
        }

        override fun createStack(amount: Int): ItemStack {
            return ItemStack(material, amount)
        }
    }

    data class KeyedItemRef(val key: Key) : SingleItemRef() {
        override fun matches(stack: ItemStack): Boolean {
            return stack.matches(key)
        }

        override fun createStack(amount: Int): ItemStack {
            val ceItem = CraftEngineItems.byId(key.toCEKey())
            if (ceItem != null) return ceItem.buildItemStack(amount)
            return Material.matchMaterial(key.toString())?.let { ItemStack(it, amount) }
                ?: error("Item not found: $key")
        }
    }
}