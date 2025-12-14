package org.shotrush.atom.content.carcass

data class CarcassPartDef(
    val id: String,
    val displayName: String,
    val requiredTool: ToolRequirement,
    val itemId: String,
    val minAmount: Int,
    val maxAmount: Int,
    val guiSlot: Int,
    val external: Boolean = false
)

data class CarcassPartState(
    val partId: String,
    var remainingAmount: Int
)
