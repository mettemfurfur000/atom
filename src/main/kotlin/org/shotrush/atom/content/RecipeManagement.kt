package org.shotrush.atom.content

import net.minecraft.data.recipes.ShapedRecipeBuilder
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import org.shotrush.atom.Atom
import org.shotrush.atom.core.data.PersistentData.key
import org.shotrush.atom.item.Material
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.item.Molds
import org.shotrush.atom.item.Tools


object RecipeManagement {
    fun handle(atom: Atom) {
        val server = atom.server
        val toolMaterials = listOf(
            "wood",
            "stone",
            "iron",
            "gold",
            "diamond",
            "netherite"
        )
        val tools = listOf(
            "shovel",
            "pickaxe",
            "axe",
            "hoe",
            "sword"
        )
        tools.forEach { tool ->
            toolMaterials.forEach { material ->
                server.removeRecipe(NamespacedKey("minecraft", "${material}_$tool"))
            }
        }
        MoldShape.entries.forEach { shape ->
            Material.entries.forEach { material ->
                val k = NamespacedKey("atom", "${material.id}_${shape.name.lowercase()}")
                val tool = Tools.getTool(shape, material)
                val toolHead = Molds.getToolHead(shape, material)
                val recipe = ShapedRecipe(k, tool)
                recipe.shape("A", "B", "C")
                recipe.setIngredient('A', toolHead.buildItemStack())
                recipe.setIngredient('B', org.bukkit.Material.VINE)
                recipe.setIngredient('C', org.bukkit.Material.STICK)
                server.addRecipe(recipe)
            }
        }
    }
}