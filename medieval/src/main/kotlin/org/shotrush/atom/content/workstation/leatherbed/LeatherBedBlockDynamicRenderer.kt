package org.shotrush.atom.content.workstation.leatherbed

import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.util.HorizontalDirection
import net.momirealms.craftengine.core.util.QuaternionUtils
import org.joml.Vector3f
import org.shotrush.atom.blocks.AtomBlockEntityRenderer

class LeatherBedBlockDynamicRenderer(val entity: LeatherBedBlockEntity) : AtomBlockEntityRenderer({
    origin(entity.pos())
    item("storedItem") {
        val rotation = entity.rotation
        val translation = when (rotation) {
            HorizontalDirection.NORTH -> Vector3f(0f, -0.2f, -0.5f)
            HorizontalDirection.EAST -> Vector3f(0.5f, -0.2f, 0f)
            HorizontalDirection.SOUTH -> Vector3f(0f, -0.2f, 0.5f)
            HorizontalDirection.WEST -> Vector3f(-0.5f, -0.2f, 0f)
        }
        translation(translation)
        val idx = when (rotation) {
            HorizontalDirection.NORTH -> 0
            HorizontalDirection.EAST -> 1
            HorizontalDirection.SOUTH -> 2
            HorizontalDirection.WEST -> 3
        }
        rotation(QuaternionUtils.toQuaternionf(0.0, Math.toRadians(idx * 90.0), 0.0))
        shadow(0f, 1f)
        viewRange(64f)
        displayContext(ItemDisplayContext.NONE)
        billboard(Billboard.FIXED)
        scale(Vector3f(1f, 1f, 1f))

        displayedItem { entity.storedItem }
        autoVisibleFromItem()
    }
})