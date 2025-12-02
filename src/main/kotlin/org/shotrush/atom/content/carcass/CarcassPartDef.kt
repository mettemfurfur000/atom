package org.shotrush.atom.content.carcass

import org.bukkit.Material

data class CarcassPartDef(
    val id: String,
    val displayName: String,
    val requiredTool: ToolRequirement,
    val itemId: String,
    val minAmount: Int,
    val maxAmount: Int,
    val guiSlot: Int,
    val displayMaterial: Material = Material.PAPER,
    val external: Boolean = false
)

data class CarcassPartState(
    val partId: String,
    var remainingAmount: Int
)
