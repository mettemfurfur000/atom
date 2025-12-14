package org.shotrush.atom.blocks.display

import net.momirealms.craftengine.core.entity.player.Player

sealed interface Prop<out T> {
    fun isPresent(): Boolean
    fun isDynamic(): Boolean
    fun resolve(player: Player): T

    data object Absent : Prop<Nothing> {
        override fun isPresent() = false
        override fun isDynamic() = false
        override fun resolve(player: Player): Nothing =
            throw IllegalStateException("Absent property has no value")
    }

    data class Static<T>(val value: T) : Prop<T> {
        override fun isPresent() = true
        override fun isDynamic() = false
        override fun resolve(player: Player): T = value
    }

    class Dynamic<T>(private val supplier: (Player) -> T) : Prop<T> {
        override fun isPresent() = true
        override fun isDynamic() = true
        override fun resolve(player: Player): T = supplier(player)
    }
}

fun <T> absent(): Prop<T> = Prop.Absent
fun <T> static(value: T): Prop<T> = Prop.Static(value)
fun <T> dynamic(f: (Player) -> T): Prop<T> = Prop.Dynamic(f)