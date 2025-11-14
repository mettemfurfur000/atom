package org.shotrush.atom.content.base

import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer
import net.momirealms.craftengine.core.entity.player.Player

@Suppress("UnstableApiUsage")
abstract class AtomBlockEntityRenderer : DynamicBlockEntityRenderer {
    abstract override fun show(player: Player)

    abstract override fun hide(player: Player)

    abstract override fun update(player: Player)
}