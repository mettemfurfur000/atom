package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.shotrush.atom.Atom

object RecipeUnlockHandler : Listener {
    private val recipeIngredients: MutableMap<NamespacedKey, MutableList<RecipeChoice>> =
        HashMap()
    private const val REQUIRE_ALL = false

    init {
        Atom.instance.launch {
            delay(100)
            extractRecipeIngredients()
        }
    }

    fun register(atom: Atom) {
        val eventDispatcher = mapOf(
            eventDef<EntityPickupItemEvent> { atom.entityDispatcher(it.entity) },
            eventDef<InventoryClickEvent> { atom.entityDispatcher(it.whoClicked) },
            eventDef<InventoryCloseEvent> { atom.entityDispatcher(it.player) },
            eventDef<PlayerJoinEvent> { atom.entityDispatcher(it.player) },
        )
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    private fun extractRecipeIngredients() {
        val recipeIterator = Bukkit.recipeIterator()

        while (recipeIterator.hasNext()) {
            val recipe = recipeIterator.next()
            if (recipe !is Keyed) continue
            val key = recipe.key
            if (key.namespace != "atom") continue

            val ingredients: MutableList<RecipeChoice> = mutableListOf()

            if (recipe is ShapedRecipe) {
                val choiceMap = recipe.getChoiceMap()
                ingredients.addAll(choiceMap.values)
            } else if (recipe is ShapelessRecipe) {
                ingredients.addAll(recipe.getChoiceList())
            }

            if (!ingredients.isEmpty()) {
                recipeIngredients[key] = ingredients
                Atom.instance.logger.info("Extracted ${ingredients.size} ingredients from recipe: ${key.key}")
            }
        }
    }

    @EventHandler fun onPickupItem(event: EntityPickupItemEvent) {
        if (event.entity is Player) {
            checkAndUnlockRecipes(event.entity as Player)
        }
    }

    @EventHandler fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked is Player) {
            checkAndUnlockRecipes(event.whoClicked as Player)
        }
    }

    @EventHandler fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.player is Player) {
            checkAndUnlockRecipes(event.player as Player)
        }
    }

    @EventHandler fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.getPlayer()
        checkAndUnlockRecipes(player)
    }

    private fun checkAndUnlockRecipes(player: Player) {
        for (entry in recipeIngredients.entries) {
            val key: NamespacedKey = entry.key
            val ingredients: MutableList<RecipeChoice> = entry.value

            if (player.hasDiscoveredRecipe(key)) continue

            if (hasRequiredIngredients(player, ingredients)) {
                player.discoverRecipe(key)
                player.sendMessage("§a§l✓ §aYou discovered a new recipe!")
            }
        }
    }

    private fun hasRequiredIngredients(player: Player, requiredIngredients: List<RecipeChoice>): Boolean {
        val contents = player.inventory.contents

        for (choice in requiredIngredients) {
            var found = false

            for (item in contents) {
                if (item?.isEmpty != false) continue

                if (choice.test(item)) {
                    found = true
                    if (!REQUIRE_ALL) return true
                    break
                }
            }
            if (!found) return false
        }

        return true
    }
}
