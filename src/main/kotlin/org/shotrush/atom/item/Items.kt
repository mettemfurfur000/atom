package org.shotrush.atom.item

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.item.CustomItem
import net.momirealms.craftengine.core.util.Key
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.content.AnimalProduct
import org.shotrush.atom.content.AnimalType
import kotlin.reflect.KProperty

object Items {
    val SharpenedFlint by item("atom:sharpened_flint")
    val UI_Molding by item("atom:ui_molding")
    val UI_MoldingPressed by item("atom:ui_molding_pressed")

    fun getAnimalProduct(type: AnimalType, product: AnimalProduct): CustomItem<ItemStack> {
        return CraftEngineItems.byId(Key.of("atom", "animal_${product.id}_${type.id}"))!!
    }

    fun getMold(tool: String, variant: String): CustomItem<ItemStack> {
        return CraftEngineItems.byId(Key.of("atom", "${variant}_mold_${tool}"))!!
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