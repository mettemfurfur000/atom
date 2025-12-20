package org.shotrush.atom.content.workstation.clay_cauldron

import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.util.QuaternionUtils
import org.joml.Vector3f
import org.shotrush.atom.blocks.AtomBlockEntityRenderer


class ClayCauldronDynamicRenderer(val entity: ClayCauldronBlockEntity) : AtomBlockEntityRenderer({
    origin(entity.pos())
    item("storedItem") {
        translation { Vector3f(0f, ((entity.fluidStored.toFloat() / entity.MAX_FLUID.toFloat()) / 2f) - 0.5f, 0f) }
        rotation(QuaternionUtils.toQuaternionf(0.0, 0.0, Math.toRadians(90.0)).conjugate())
        shadow(0f, 1f)
        viewRange(64f)
        displayContext(ItemDisplayContext.NONE)
        billboard(Billboard.FIXED)
        scale(Vector3f(0.5f))

        displayedItem { entity.storedItem }
        autoVisibleFromItem()
    }
    item("fluidLayer") {
        rotation(QuaternionUtils.toQuaternionf(0.0, 0.0, 0.0))
        shadow(0f, 1f)
        viewRange(64f)
        displayContext(ItemDisplayContext.NONE)
        billboard(Billboard.FIXED)
        scale(Vector3f(1f))

        translation { Vector3f(0f, ((entity.fluidStored.toFloat() / entity.MAX_FLUID.toFloat()) / 2f) - 0.3f, 0f) }
        displayedItem { entity.fluidAsItem }
        visible { entity.fluid != null && entity.fluidStored > 0 }
    }
})