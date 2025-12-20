package org.shotrush.atom.blocks.display

import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.entity.player.Player
import net.momirealms.craftengine.core.world.BlockPos
import org.bukkit.inventory.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.sqrt

class ItemElementBuilder internal constructor(
    private val id: String,
) {
    private var origin: Prop<Vector3f>? = null
    private var position: Prop<Vector3f> = static(Vector3f(0f, 0f, 0f))

    private var displayedItem: Prop<ItemStack> = absent()
    private var scale: Prop<Vector3f> = absent()
    private var rotation: Prop<Quaternionf> = absent()
    private var translation: Prop<Vector3f> = absent()
    private var billboard: Prop<Billboard> = absent()
    private var displayContext: Prop<ItemDisplayContext> = absent()
    private var shadowRadius: Prop<Float> = absent()
    private var shadowStrength: Prop<Float> = absent()
    private var viewRange: Prop<Float> = absent()
    private var updatesPerSecond: Prop<Float> = absent()
    private var visible: Prop<Boolean> = static(true)

    fun origin(x: Float, y: Float, z: Float) {
        origin(Vector3f(x, y, z))
    }

    fun origin(x: Int, y: Int, z: Int) {
        origin(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    fun origin(x: Double, y: Double, z: Double) {
        origin(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    fun origin(value: Vector3f) {
        origin = static(value)
    }

    fun origin(supplier: (Player) -> Vector3f) {
        origin = dynamic(supplier)
    }

    fun position(x: Float, y: Float, z: Float) {
        position(Vector3f(x, y, z))
    }

    fun position(x: Int, y: Int, z: Int) {
        position(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    fun position(x: Double, y: Double, z: Double) {
        position(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    fun position(value: Vector3f) {
        position = static(value)
    }

    fun position(supplier: (Player) -> Vector3f) {
        position = dynamic(supplier)
    }

    fun displayedItem(value: ItemStack) {
        displayedItem = static(value)
    }

    fun displayedItem(supplier: (Player) -> ItemStack) {
        displayedItem = dynamic(supplier)
    }

    fun scale(value: Vector3f) {
        scale = static(value)
    }

    fun scale(supplier: (Player) -> Vector3f) {
        scale = dynamic(supplier)
    }

    fun rotation(value: Quaternionf) {
        rotation = static(value)
    }

    fun rotation(supplier: (Player) -> Quaternionf) {
        rotation = dynamic(supplier)
    }

    fun translation(value: Vector3f) {
        translation = static(value)
    }

    fun translation(supplier: (Player) -> Vector3f) {
        translation = dynamic(supplier)
    }

    fun billboard(value: Billboard) {
        billboard = static(value)
    }

    fun billboard(supplier: (Player) -> Billboard) {
        billboard = dynamic(supplier)
    }

    fun displayContext(value: ItemDisplayContext) {
        displayContext = static(value)
    }

    fun displayContext(supplier: (Player) -> ItemDisplayContext) {
        displayContext = dynamic(supplier)
    }

    fun shadow(radius: Float, strength: Float) {
        shadowRadius = static(radius)
        shadowStrength = static(strength)
    }

    fun shadow(radius: (Player) -> Float, strength: (Player) -> Float) {
        shadowRadius = dynamic(radius)
        shadowStrength = dynamic(strength)
    }

    fun viewRange(value: Float) {
        viewRange = static(value)
    }

    fun viewRange(supplier: (Player) -> Float) {
        viewRange = dynamic(supplier)
    }

    fun updatesPerSecond(value: Float) {
        updatesPerSecond = static(value)
    }

    fun updatesPerSecond(supplier: (Player) -> Float) {
        updatesPerSecond = dynamic(supplier)
    }

    fun visible(value: Boolean) {
        visible = static(value)
    }

    fun visible(supplier: (Player) -> Boolean) {
        visible = dynamic(supplier)
    }

    inline fun distanceBasedUPS(
        origin: Vector3f,
        maxUPS: Float = 60f,
        minUPS: Float = 1f,
        maxDistance: Float = 100f, // distance at which UPS reaches minUPS
        crossinline curve: (t: Float) -> Float = { it }, // t in [0,1], default linear
    ) {
        require(maxUPS > 0f && minUPS >= 0f && maxUPS >= minUPS)
        require(maxDistance > 0f)

        updatesPerSecond { p ->
            val dx = origin.x - p.x().toFloat()
            val dy = origin.y - p.y().toFloat()
            val dz = origin.z - p.z().toFloat()
            val dist = sqrt(dx * dx + dy * dy + dz * dz)

            val t = (dist / maxDistance).coerceIn(0f, 1f)
            val falloff = curve(t) // 0 near, 1 far for linear

            val ups = maxUPS + (minUPS - maxUPS) * falloff
            ups.coerceIn(minUPS, maxUPS)
        }
    }

    fun autoVisibleFromItem() {
        visible = dynamic { p ->
            !displayedItem.resolve(p).isEmpty
        }
    }

    internal fun build(sceneOrigin: Prop<Vector3f>): DisplayElement =
        ItemDisplayElement(
            id = id,
            origin = (origin ?: sceneOrigin),
            position = position,
            displayedItem = displayedItem,
            scale = scale,
            rotation = rotation,
            translation = translation,
            billboard = billboard,
            displayContext = displayContext,
            shadowRadius = shadowRadius,
            shadowStrength = shadowStrength,
            viewRange = viewRange,
            updatesPerSecond = updatesPerSecond,
            visible = visible
        )
}

fun BlockPos.toVector3f(): Vector3f {
    return Vector3f(x().toFloat(), y().toFloat(), z().toFloat())
}
