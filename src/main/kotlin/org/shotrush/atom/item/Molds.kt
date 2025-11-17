package org.shotrush.atom.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DyedItemColor
import io.papermc.paper.datacomponent.item.TooltipDisplay
import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.item.CustomItem
import net.momirealms.craftengine.core.util.Key
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.shotrush.atom.getNamespacedKey

object Molds {
    fun getMold(tool: MoldShape, variant: MoldType): CustomItem<ItemStack> {
        return CraftEngineItems.byId(Key.of("atom", "${variant.id}_mold_${tool.mold}"))!!
    }

    fun getToolHead(tool: MoldShape, material: Material): CustomItem<ItemStack> {
        val key = "${material.id}_${tool.id}_head"
        return CraftEngineItems.byId(Key.of("atom", key)) ?: error("No tool head found for $key")
    }

    fun getIngot(material: Material): ItemStack {
        if (material == Material.Iron) return ItemStack(org.bukkit.Material.IRON_INGOT)
        if (material == Material.Copper) return ItemStack(org.bukkit.Material.COPPER_INGOT)
        return CraftEngineItems.byId(Key.of("atom", "ingot_${material.id}"))!!.buildItemStack()
    }

    fun getFilledMold(shape: MoldShape, variant: MoldType, material: Material): ItemStack {
        if (variant != MoldType.Wax && variant != MoldType.Fired) throw IllegalArgumentException("Only Wax and Fired molds can be filled!")
        val item = CraftEngineItems.byId(Key.of("atom", "filled_${variant.id}_mold_${shape.mold}"))!!
        val stack = item.buildItemStack()
        
        // TODO: Replace MATERIAL_HERE placeholder in lore with actual material name
        // CraftEngine applies client-bound-data.lore after buildItemStack() so we can't override it here
        
        stack.setData(
            DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(
                material.rgb
            )
        )
        stack.setData(
            DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .hiddenComponents(setOf(DataComponentTypes.DYED_COLOR))
                .build()
        )
        stack.editPersistentDataContainer {
            it.set(NamespacedKey("atom", "mold_type"), PersistentDataType.STRING, variant.id)
            it.set(NamespacedKey("atom", "mold_shape"), PersistentDataType.STRING, shape.id)
            it.set(NamespacedKey("atom", "mold_fill"), PersistentDataType.STRING, material.id)
        }
        return stack
    }

    val FilledRegex = Regex("atom:filled_(.+)_mold_(.+)")
    val EmptyRegex = Regex("atom:(fired|wax)_mold_(.+)")
    val UnfiredRegex = Regex("atom:clay_mold_(.+)")
    val FullRegex = Regex("atom:(\\w+)_mold_(.+)")

    fun isFilledMold(stack: ItemStack): Boolean {
        if (!stack.getNamespacedKey().matches(FilledRegex)) return false
        if (!stack.persistentDataContainer.has("mold_type")) return false
        if (!stack.persistentDataContainer.has("mold_shape")) return false
        if (!stack.persistentDataContainer.has("mold_fill")) return false
        return true
    }

    fun isEmptyMold(stack: ItemStack): Boolean {
        return stack.getNamespacedKey().matches(EmptyRegex)
    }

    fun isMold(stack: ItemStack): Boolean {
        return stack.getNamespacedKey().matches(FullRegex)
    }

    fun getMoldType(stack: ItemStack): MoldType {
        val key = stack.getNamespacedKey()
        if (key.startsWith("atom:wax_")) return MoldType.Wax
        if (key.startsWith("atom:fired_")) return MoldType.Fired
        if (key.startsWith("atom:clay_")) return MoldType.Clay
        if (key.startsWith("atom:filled_")) {
            val moldTypeId = stack.persistentDataContainer.getString("mold_type") ?: error("No mold type found!")
            val moldType = MoldType.byId(moldTypeId)
            return moldType
        }
        throw IllegalArgumentException("Item is not a mold!")
    }

    fun getMoldShape(stack: ItemStack): MoldShape {
        val key = stack.getNamespacedKey()
        if (key.matches(FullRegex)) {
            val regex = FullRegex.find(key)!!
            val moldShapeId = regex.groupValues[2]
            return MoldShape.byMold(moldShapeId)
        }
        error("Item is not a mold!")
    }

    fun emptyMold(stack: ItemStack): Pair<ItemStack, ItemStack> {
        if (!isFilledMold(stack)) throw IllegalArgumentException("Item is not a filled mold!")
        val moldTypeId = stack.persistentDataContainer.getString("mold_type") ?: error("No mold type found!")
        val moldShapeId = stack.persistentDataContainer.getString("mold_shape") ?: error("No mold shape found!")
        val materialId = stack.persistentDataContainer.getString("mold_fill") ?: error("No material found!")

        val moldType = MoldType.byId(moldTypeId)
        val moldShape = MoldShape.byId(moldShapeId)
        val material = Material.byId(materialId)

        val emptyMold = if (moldType == MoldType.Wax) {
            ItemStack.empty()
        } else {
            getMold(moldShape, moldType).buildItemStack()
        }
        val toolHead = if (moldShape == MoldShape.Ingot) {
            getIngot(material)
        } else {
            getToolHead(moldShape, material).buildItemStack()
        }
        return Pair(emptyMold, toolHead)
    }
}

fun PersistentDataContainerView.has(key: String) =
    this.has(NamespacedKey("atom", key))

fun PersistentDataContainerView.getString(key: String) =
    this.get(NamespacedKey("atom", key), PersistentDataType.STRING)

fun PersistentDataContainerView.getString(key: NamespacedKey) =
    this.get(key, PersistentDataType.STRING)