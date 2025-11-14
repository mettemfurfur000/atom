package org.shotrush.atom.content.workstation.leatherbed

import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.util.HorizontalDirection
import net.momirealms.craftengine.core.util.QuaternionUtils
import org.joml.Vector3f
import org.shotrush.atom.content.base.AtomBlockEntityRenderer
import org.shotrush.atom.content.base.display.toVector3f

class LeatherBedBlockDynamicRenderer(val entity: LeatherBedBlockEntity) : AtomBlockEntityRenderer({
    origin(entity.pos())
    item("storedItem") {
        position(0.5, 1.0, 0.5)
        val rotation = entity.rotation
        val idx = when (rotation) {
            HorizontalDirection.NORTH -> 0
            HorizontalDirection.EAST -> 1
            HorizontalDirection.SOUTH -> 2
            HorizontalDirection.WEST -> 3
        }
//        translation(
//            Vector3f(
//                rotation.stepX() * 0.5f,
//                0f,
//                rotation.stepZ() * 0.5f
//            )
//        )
        shadow(0f, 1f)
        viewRange(64f)
        displayContext(ItemDisplayContext.NONE)
        billboard(Billboard.FIXED)
        scale(Vector3f(1f, 1f, 1f))

        displayedItem { entity.storedItem }



        rotation { QuaternionUtils.toQuaternionf(0.0, Math.toRadians(((System.currentTimeMillis() / 10.0)  % 360)), 0.0) }
        distanceBasedUPS(entity.pos().toVector3f(), maxDistance = 10f)
    }
})