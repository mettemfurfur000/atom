package org.shotrush.atom.blocks

import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer
import net.momirealms.craftengine.core.entity.player.Player
import org.shotrush.atom.blocks.display.RendererSceneBuilder

@Suppress("UnstableApiUsage")
abstract class AtomBlockEntityRenderer(block: RendererSceneBuilder.() -> Unit) : DynamicBlockEntityRenderer {
    val scene = RendererSceneBuilder().apply(block).build()

    override fun show(player: Player) {
        scene.show(player)
    }

    override fun hide(player: Player) {
        scene.hide(player)
    }

    override fun update(player: Player) {
        scene.update(player)
    }
}