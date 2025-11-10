package org.shotrush.atom

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.item.CustomItem
import net.momirealms.craftengine.core.util.Key
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.item.isItem

fun ItemStack.isCustomItem() = CraftEngineItems.isCustomItem(this)
fun ItemStack.getNamespacedKey(): String = if(isCustomItem()) {
    CraftEngineItems.getCustomItemId(this).toString()
} else {
    type.key.toString()
}
fun ItemStack.matches(regex: Regex) = getNamespacedKey().matches(regex)
fun ItemStack.matches(key: Key) = getNamespacedKey() == key.toString()
fun ItemStack.matches(key: String) = getNamespacedKey() == key
fun ItemStack.matches(namespace: String, path: String) = getNamespacedKey() == "$namespace:$path"
fun ItemStack.matches(item: CustomItem<ItemStack>) = item.isItem(this)

fun Block.matches(key: Key) = CraftEngineBlocks.getCustomBlockState(this)?.owner()?.matchesKey(key) ?: false
fun Block.matches(key: String) = matches(Key.of(key))
fun Block.matches(namespace: String, path: String) = matches(Key.of(namespace, path))