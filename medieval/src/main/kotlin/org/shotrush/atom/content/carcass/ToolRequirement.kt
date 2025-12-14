package org.shotrush.atom.content.carcass

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.getNamespacedKey

enum class ToolRequirement(val displayName: String) {
    NONE("No tool required"),
    KNIFE("Requires a knife"),
    AXE("Requires an axe"),
    WATER_BUCKET("Requires a water bucket");

    fun isSatisfiedBy(item: ItemStack): Boolean {
        if (this == NONE) return true
        if (item.isEmpty) return false
        
        return when (this) {
            NONE -> true
            KNIFE -> isKnife(item)
            AXE -> isAxe(item)
            WATER_BUCKET -> item.type == Material.WATER_BUCKET
        }
    }

    companion object {
        private val KNIFE_REGEX = Regex("atom:[a-z]+_knife")
        private val AXE_REGEX = Regex("atom:[a-z]+_axe")
        
        private fun isKnife(item: ItemStack): Boolean {
            val key = item.getNamespacedKey()
            return key.matches(KNIFE_REGEX)
        }
        
        private fun isAxe(item: ItemStack): Boolean {
            val key = item.getNamespacedKey()
            if (key.matches(AXE_REGEX)) return true
            return item.type.name.endsWith("_AXE")
        }
    }
}
