package org.shotrush.atom.content.carcass

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Represents a tool requirement for harvesting a carcass part.
 * Uses Minecraft's tag system for flexible and extensible tool matching.
 */
sealed class ToolRequirement {
    /**
     * Check if the given item satisfies this tool requirement.
     */
    abstract fun isSatisfiedBy(item: ItemStack): Boolean

    /**
     * Get a display-friendly description of this requirement.
     */
    abstract val displayName: String

    /**
     * No tool required - can harvest with empty hand.
     */
    data object None : ToolRequirement() {
        override fun isSatisfiedBy(item: ItemStack): Boolean = true
        override val displayName: String = "No tool required"
    }

    /**
     * Requires an item matching a specific Minecraft item tag.
     *
     * Examples:
     * - `#minecraft:axes` - any vanilla axe
     * - `#atom:knives` - any knife from Atom
     * - `#forge:tools/knives` - knives from any mod (Forge convention)
     */
    data class ItemTag(val tag: String, val customDisplayName: String? = null) : ToolRequirement() {
        private val parsedKey: Key? by lazy {
            // Parse tag in format: #namespace:path
            if (!tag.startsWith("#")) return@lazy null
            val keyString = tag.substring(1) // Remove # prefix
            Key.of(keyString)
        }

        override fun isSatisfiedBy(item: ItemStack): Boolean {
            if (item.isEmpty) {
                return false
            }
            val key = parsedKey ?: return false

            // Check CraftEngine custom item tags
            val customItemId = CraftEngineItems.getCustomItemId(item)
            val customItem = customItemId?.let { CraftEngineItems.byId(it) }
            val itemTags = customItem?.settings()?.tags() ?: emptySet()

            return itemTags.contains(key)
        }

        override val displayName: String
            get() = customDisplayName ?: run {
                // Extract readable name from tag
                // #atom:knives -> "Knife"
                // #minecraft:axes -> "Axe"
                val tagName = tag.substringAfter(":")
                    .replace("_", " ")
                    .replaceFirstChar { it.uppercaseChar() }
                    .removeSuffix("s") // Remove plural
                "Requires a $tagName"
            }
    }

    /**
     * Requires a specific material type (for special cases like water buckets).
     */
    data class SpecificMaterial(val material: Material, val customDisplayName: String? = null) : ToolRequirement() {
        override fun isSatisfiedBy(item: ItemStack): Boolean {
            return item.type == material
        }

        override val displayName: String
            get() = customDisplayName ?: "Requires ${material.name.lowercase().replace("_", " ")}"
    }

    companion object {
        /**
         * Parse a tool requirement from a string.
         *
         * Formats:
         * - "none" -> None
         * - "#namespace:tag" -> ItemTag
         * - "MATERIAL_NAME" -> SpecificMaterial
         */
        fun parse(value: String): ToolRequirement {
            return when {
                value.equals("none", ignoreCase = true) -> None
                value.startsWith("#") -> ItemTag(value)
                else -> {
                    // Try to parse as material
                    val material = Material.matchMaterial(value)
                    if (material != null) {
                        SpecificMaterial(material)
                    } else {
                        // Default to none if parsing fails
                        None
                    }
                }
            }
        }
    }
}
