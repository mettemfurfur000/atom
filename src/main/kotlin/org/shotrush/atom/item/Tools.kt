package org.shotrush.atom.item

import net.kyori.adventure.text.minimessage.MiniMessage
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.util.Key
import org.bukkit.inventory.ItemStack
import org.bukkit.Material as BukkitMat

object Tools {
    private val ClassicMaterials = setOf(Material.Stone, Material.Iron)
    private val ClassicToolIds = setOf("pickaxe", "shovel", "hoe", "sword", "axe")

    private fun isClassic(shape: MoldShape, material: Material): Boolean {
        val toolId = shape.id
        return material in ClassicMaterials && toolId in ClassicToolIds
    }

    private fun atomToolKey(shape: MoldShape, material: Material): String {
        val toolId = shape.id
        return "${material.id}_${toolId}"
    }

    private fun buildVanillaTool(shape: MoldShape, material: Material): ItemStack {
        return when (shape) {
            MoldShape.Pickaxe -> when (material) {
                Material.Stone -> ItemStack(BukkitMat.STONE_PICKAXE)
                Material.Iron -> ItemStack(BukkitMat.IRON_PICKAXE)
                else -> error("Not a classic pair")
            }

            MoldShape.Shovel -> when (material) {
                Material.Stone -> ItemStack(BukkitMat.STONE_SHOVEL)
                Material.Iron -> ItemStack(BukkitMat.IRON_SHOVEL)
                else -> error("Not a classic pair")
            }
//            MoldShape.Hoe -> when (material) {
//                Material.Stone -> ItemStack(BukkitMat.STONE_HOE)
//                Material.Iron -> ItemStack(BukkitMat.IRON_HOE)
//                else -> error("Not a classic pair")
//            }
            MoldShape.Sword -> when (material) {
                Material.Stone -> ItemStack(BukkitMat.STONE_SWORD)
                Material.Iron -> ItemStack(BukkitMat.IRON_SWORD)
                else -> error("Not a classic pair")
            }

            MoldShape.Axe -> when (material) {
                Material.Stone -> ItemStack(BukkitMat.STONE_AXE)
                Material.Iron -> ItemStack(BukkitMat.IRON_AXE)
                else -> error("Not a classic pair")
            }

            else -> error("Shape ${shape.id} is not a classic vanilla tool")
        }.apply {
            val age = when (material) {
                Material.Stone -> "foraging"
                Material.Iron -> "iron"
                else -> ""
            }
            val lore = listOf(
                "<!i><gray><lang:item.tool.common.lore>",
                "",
                "<!i><white><image:atom:badge_tool> <image:atom:badge_age_$age>"
            )
            lore(lore.map { MiniMessage.miniMessage().deserialize(it) })
        }
    }

    fun getTool(shape: MoldShape, material: Material): ItemStack {
        if (isClassic(shape, material)) {
            return buildVanillaTool(shape, material)
        }

        val key = atomToolKey(shape, material)
        val custom = CraftEngineItems.byId(Key.of("atom", key))
            ?: error("Atom tool not found for atom:$key")
        return custom.buildItemStack()
    }
}