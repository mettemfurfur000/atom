package org.shotrush.atom.blocks.display

import net.momirealms.craftengine.core.entity.player.Player
import net.momirealms.craftengine.core.world.BlockPos
import org.joml.Vector3f

class RendererScene internal constructor(
    private val elements: Map<String, DisplayElement>,
) {
    private val tickManager = TickManager()

    fun show(player: Player) = elements.values.forEach {
        it.show(player)
        it.startTicking(player, tickManager)
    }

    fun update(player: Player) = elements.values.forEach {
        it.update(player)
        it.startTicking(player, tickManager)
    }

    fun hide(player: Player) = elements.values.forEach {
        it.stopTicking(player, tickManager)
        it.hide(player)
    }

    fun element(id: String): DisplayElement? = elements[id]
    fun item(id: String): ItemDisplayElement? = element(id) as? ItemDisplayElement
}

class RendererSceneBuilder {
    private var origin: Prop<Vector3f> = static(Vector3f(0f, 0f, 0f))
    private val elementBuilders = mutableListOf<(Prop<Vector3f>) -> DisplayElement>()

    fun origin(pos: BlockPos) = origin(pos.x(), pos.y(), pos.z())

    fun origin(x: Int, y: Int, z: Int) = origin(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))

    fun origin(x: Double, y: Double, z: Double) = origin(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))

    fun origin(value: Vector3f) {
        origin = static(value)
    }

    fun origin(supplier: (Player) -> Vector3f) {
        origin = dynamic(supplier)
    }

    fun item(id: String, block: ItemElementBuilder.() -> Unit) {
        val b = ItemElementBuilder(id).apply(block)
        elementBuilders += { sceneOrigin -> b.build(sceneOrigin) }
    }

    internal fun build(): RendererScene {
        val elems = elementBuilders.map { it(origin) }
        return RendererScene(elems.associateBy { it.id })
    }
}