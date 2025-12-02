package org.shotrush.atom.content.workstation.core

import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import java.util.*


data class PlacedItem(
    val item: ItemStack,
    val position: Vector3f,
    val yaw: Float,
    var displayUUID: UUID? = null,
)

