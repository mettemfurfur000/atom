package org.shotrush.atom.content.systems

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.Lightable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import org.shotrush.atom.Atom
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.core.blocks.annotation.CustomBlockDrops
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.matches
import kotlin.random.Random

@RegisterSystem(
    id = "campfire_system",
    priority = 5,
    toggleable = true,
    description = "Handles campfire lighting with pebbles and unlit placement",
    enabledByDefault = true
)
@CustomBlockDrops(
    blocks = [Material.CAMPFIRE
    ],
    drops = [CustomBlockDrops.Drop(material = Material.CHARCOAL, chance = 0.05, min = 1, max = 1), CustomBlockDrops.Drop(
        customItemId = "straw",
        chance = 0.05,
        min = 1,
        max = 1
    )],
    replaceVanillaDrops = true,
    ages = ["foraging_age"]
)
class CampfireSystem(private val plugin: Plugin) : Listener {

    companion object {
        private val activeLighting = mutableMapOf<Player, LightingJob>()

        data class LightingJob(
            val job: kotlinx.coroutines.Job
        )
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCampfirePlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced

        if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
            val blockData = block.blockData
            if (blockData is Lightable) {
                blockData.isLit = false
                block.blockData = blockData
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val block = event.clickedBlock ?: return
        val player = event.player
        val item = player.inventory.itemInMainHand

        if (!item.matches("atom:pebble")) return
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) return
        val blockData = block.blockData
        if (blockData !is Lightable) return
        if (blockData.isLit) return
        if (!isLighting(player)) {
            startLighting(player, block, blockData)
        }
    }

    private fun isLighting(player: Player): Boolean {
        return activeLighting.containsKey(player)
    }

    private fun startLighting(player: Player, block: org.bukkit.block.Block, blockData: Lightable) {
        activeLighting[player]?.job?.cancel()

        val strikeCount = 10 + Random.nextInt(6) // 10-15 strikes

        val job = GlobalScope.launch {
            var currentStrike = 0

            ActionBarManager.sendStatus(player, "§7Lighting campfire... Strike repeatedly")

            while (currentStrike < strikeCount && isActive) {
                delay(250)
                if (!player.hasActiveItem() || !player.activeItem.matches("atom:pebble")) {
                    ActionBarManager.sendStatus(player, "§cLighting cancelled - pebble lowered")
                    delay(1000)
                    break
                }

                // Check if player is still online and near the campfire
                if (!player.isOnline || player.location.distance(block.location) > 5.0) {
                    break
                }

                // Play lighting effects
                player.scheduler.run(Atom.instance!!, { _ ->
                    playLightingEffects(player, block)
                }, null)

                currentStrike++

                // Show progress
                val progress = (currentStrike.toFloat() / strikeCount * 100).toInt()
                ActionBarManager.sendStatus(player, "§7Lighting... §e$progress%")
            }

            if (currentStrike >= strikeCount) {
                player.scheduler.run(Atom.instance!!, { _ ->
                    finishLighting(player, block, blockData)
                }, null)
            }

            activeLighting.remove(player)
            ActionBarManager.clearStatus(player)
        }

        activeLighting[player] = LightingJob(job)
    }

    private fun playLightingEffects(player: Player, block: org.bukkit.block.Block) {
        val location = block.location.add(0.5, 0.5, 0.5)

        // Play spark sound
        location.world?.playSound(location, Sound.BLOCK_STONE_HIT, 1.0f, 1.5f)

        location.world?.spawnParticle(
            Particle.DUST_PLUME,
            location,
            3,
            0.2,
            0.2,
            0.2,
            0.02
        )

        if (Random.nextDouble() < 0.3) {
            location.world?.spawnParticle(
                Particle.SMOKE,
                location,
                2,
                0.1,
                0.1,
                0.1,
                0.01
            )
        }
    }

    private fun finishLighting(player: Player, block: org.bukkit.block.Block, blockData: Lightable) {
        val location = block.location.add(0.5, 0.5, 0.5)

        val successChance = 0.7

        if (Random.nextDouble() < successChance) {
            blockData.isLit = true
            block.blockData = blockData

            ActionBarManager.send(player, "§aSuccessfully lit the campfire!")
            location.world?.playSound(location, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f)
            location.world?.spawnParticle(
                Particle.FLAME,
                location,
                20,
                0.3,
                0.3,
                0.3,
                0.02
            )


            CampfireBurnoutSystem.getInstance()?.startBurnoutTimer(block.location)

        } else {
            ActionBarManager.send(player, "§cFailed to light the campfire. Try again!")
            location.world?.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 2.0f)
            location.world?.spawnParticle(
                Particle.SMOKE,
                location,
                10,
                0.2,
                0.2,
                0.2,
                0.02
            )
        }
    }
}