package org.shotrush.atom.item

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.item.CustomItem
import net.momirealms.craftengine.core.util.Key
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.content.AnimalProduct
import org.shotrush.atom.content.AnimalType
import org.shotrush.atom.getNamespacedKey
import org.shotrush.atom.getNamespacedPath
import kotlin.reflect.KProperty

object Items {
    val SharpenedFlint by item("atom:sharpened_rock")

    fun getAnimalProduct(type: AnimalType, product: AnimalProduct): CustomItem<ItemStack> {
        return CraftEngineItems.byId(Key.of("atom", "animal_${product.id}_${type.id}"))!!
    }

    fun getAnimalFromProduct(product: ItemStack): AnimalType {
        val ns = product.getNamespacedKey()
        val (_, key) = ns.split(":")
        val animal = key.split("_").last()
        return AnimalType.byId(animal) ?: throw IllegalStateException("Animal $animal not found!")
    }

    fun getAnimalProductFromItem(item: ItemStack): AnimalProduct {
        return AnimalProduct.decodeFromItemKey(item.getNamespacedPath())
    }

    fun isAnimalProduct(item: ItemStack): Boolean {
        return item.getNamespacedKey().startsWith("atom:animal_")
    }

    object UI {
        val ARROW_LEFT_AVAILABLE = Key.of("internal", "previous_recipe_0")
        val ARROW_LEFT_BLOCKED = Key.of("internal", "previous_recipe_1")
        val ARROW_RIGHT_AVAILABLE = Key.of("internal", "next_recipe_0")
        val ARROW_RIGHT_BLOCKED = Key.of("internal", "next_recipe_1")
        val ARROW_BACK = Key.of("internal", "return")
    }
}

fun item(key: Key) = CEItem(key)
fun item(key: String) = item(Key.of(key))

data class CEItem(val key: Key) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): CustomItem<ItemStack> {
        return CraftEngineItems.byId(key) ?: throw IllegalStateException("Item $key not found!")
    }
}

fun CustomItem<ItemStack>.isItem(item: ItemStack): Boolean {
    return CraftEngineItems.getCustomItemId(item) == this.id()
}