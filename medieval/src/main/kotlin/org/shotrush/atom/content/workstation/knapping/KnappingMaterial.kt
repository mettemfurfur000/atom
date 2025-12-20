package org.shotrush.atom.content.workstation.knapping

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.util.Key
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.item.Material
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

enum class KnappingMaterial(val id: String, val buildFinalItem: (shape: MoldShape) -> ItemStack) {
    Clay("clay", { Molds.getMold(it, MoldType.Clay).buildItemStack() }),
    Wax("wax", { Molds.getMold(it, MoldType.Clay).buildItemStack() }),
    Stone("stone", { Molds.getToolHead(it, Material.Stone).buildItemStack() });

    fun getItem(pressed: Boolean): ItemStack = if (pressed) pressedItem else unpressedItem


    val pressedItem: ItemStack by lazy {
        CraftEngineItems.byId(Key.of("atom", "ui_molding_${id}_pressed"))?.buildItemStack()
            ?: error("No pressed item found for $id")
    }
    val unpressedItem: ItemStack by lazy {
        CraftEngineItems.byId(Key.of("atom", "ui_molding_$id"))?.buildItemStack()
            ?: error("No pressed item found for $id")
    }

    companion object {
        val ALL = entries.toSet()
        val MOLDS_ONLY = setOf(Clay, Wax)
    }
}