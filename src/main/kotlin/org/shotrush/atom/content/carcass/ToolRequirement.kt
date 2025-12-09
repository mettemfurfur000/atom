package org.shotrush.atom.content.carcass

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
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
        private val parsedTag: Tag<Material>? by lazy {
            // Parse tag in format: #namespace:path
            if (!tag.startsWith("#")) return@lazy null

            val keyString = tag.substring(1) // Remove # prefix
            val key = try {
                NamespacedKey.fromString(keyString)
            } catch (e: Exception) {
                null
            }

            // Try to get the tag from Bukkit's tag registry
            key?.let { Bukkit.getTag(Tag.REGISTRY_ITEMS, it, Material::class.java) }
        }

        override fun isSatisfiedBy(item: ItemStack): Boolean {
            if (item.isEmpty) return false

            val tag = parsedTag ?: return false
            return tag.isTagged(item.type)
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
