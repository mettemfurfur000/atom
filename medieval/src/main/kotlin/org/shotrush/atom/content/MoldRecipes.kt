package org.shotrush.atom.content

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.api.AtomAPI
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

object MoldRecipes {
    fun register(plugin: Plugin) {
        AtomAPI.Scheduler.runGlobalTask {
            val clayBallKey = NamespacedKey(plugin, "campfire_drying_clay_ball")
            val clayBallRecipe = CampfireRecipe(
                clayBallKey,
                ItemStack(Material.BRICK),
                Material.CLAY_BALL,
                0.1f,
                600
            )
            plugin.server.addRecipe(clayBallRecipe)
            plugin.logger.info("Registered campfire drying recipe for clay ball → brick")

            for (shape in MoldShape.entries) {
                val clayMold = Molds.getMold(shape, MoldType.Clay).buildItemStack()
                val firedMold = Molds.getMold(shape, MoldType.Fired).buildItemStack()

                val key = NamespacedKey(plugin, "campfire_firing_${shape.id}_mold")
                val recipe = CampfireRecipe(
                    key,
                    firedMold,
                    RecipeChoice.ExactChoice(clayMold),
                    0.35f,
                    1200
                )

                plugin.server.addRecipe(recipe)
                plugin.logger.info("Registered campfire firing recipe for ${shape.id} clay mold")
            }
        }
    }
}
