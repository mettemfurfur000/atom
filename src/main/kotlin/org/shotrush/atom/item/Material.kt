package org.shotrush.atom.item

import org.bukkit.Color
import org.shotrush.atom.content.Age

enum class Material(val id: String, val tier: Int, val age: Age, val rgb: Color) {
    Stone("stone", 0, Age.Foraging, Color.fromRGB(128, 128, 128)),
    Copper("copper", 1, Age.Copper, Color.fromRGB(228, 128, 101)),
    Bronze("bronze", 2, Age.Bronze, Color.fromRGB(200, 100, 50)),
    Iron("iron", 3, Age.Iron, Color.fromRGB(210, 210, 210)),
    Steel("steel", 4, Age.Steel, Color.fromRGB(128, 128, 128));

    val pastTiers: List<Material> by lazy { MaterialOrderedByTier.take(tier + 1) }

    companion object {
        val MaterialById = Material.entries.associateBy { it.id }
        val MaterialOrderedByTier = Material.entries.sortedBy { it.tier }

        fun byId(id: String) = MaterialById[id] ?: error("No such Material: $id")
    }
}