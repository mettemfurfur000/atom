package org.shotrush.atom.content.systems

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
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
    description = "Handles campfire lighting with pebbles, unlit placement, and straw fuel extension",
    enabledByDefault = true
)
@CustomBlockDrops(
    blocks = [Material.CAMPFIRE],
    drops = [
        CustomBlockDrops.Drop(material = Material.CHARCOAL, chance = 0.05, min = 1, max = 1),
        CustomBlockDrops.Drop(customItemId = "straw", chance = 0.05, min = 1, max = 1)
    ],
    replaceVanillaDrops = true,
    ages = ["foraging_age"]
)
class CampfireSystem(private val plugin: Plugin) : Listener {

    companion object {
        private val activeLighting = mutableMapOf<Player, LightingJob>()
        private const val STRAW_EXTENSION_MS = 2 * 60 * 1000L 
        private val strawBurnJobs = mutableMapOf<org.bukkit.Location, kotlinx.coroutines.Job>() 

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

        
        if (item.matches("atom:straw")) {
            if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
                val blockData = block.blockData
                if (blockData is Lightable && blockData.isLit) {
                    
                    event.isCancelled = true
                    if (extendCampfireBurnTime(block.location, player)) {
                        
                        item.subtract(1)

                        
                        val location = block.location.add(0.5, 0.5, 0.5)
                        location.world?.playSound(location, Sound.ITEM_CROP_PLANT, 1.0f, 0.8f)
                        location.world?.spawnParticle(
                            Particle.FLAME,
                            location,
                            15,
                            0.3,
                            0.3,
                            0.3,
                            0.02
                        )

                        val burnoutSystem = CampfireBurnoutSystem.getInstance()
                        val remainingTime = burnoutSystem?.getRemainingTime(block.location)
                        if (remainingTime != null) {
                            val minutes = (remainingTime / 60000L).toInt()
                            val seconds = ((remainingTime % 60000L) / 1000L).toInt()
                            ActionBarManager.send(player, "§aAdded fuel! Time remaining: §e${minutes}m ${seconds}s")
                            player.sendMessage("§aAdded fuel! Time remaining: §e${minutes}m ${seconds}s")
                        } else {
                            ActionBarManager.send(player, "§aAdded fuel to campfire!")
                        }
                    }
                    return
                }
            }
        }

        
        if (!item.matches("atom:pebble")) return
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) return

        val blockData = block.blockData
        if (blockData !is Lightable) return
        if (blockData.isLit) return

        if (!isLighting(player)) {
            startLighting(player, block, blockData)
        }
    }

    private fun extendCampfireBurnTime(location: org.bukkit.Location, player: Player): Boolean {
        val burnoutSystem = CampfireBurnoutSystem.getInstance() ?: return false

        
        val currentBurnoutJob = CampfireBurnoutSystem.activeCampfires[location]

        if (currentBurnoutJob == null) {
            Atom.instance?.logger?.warning("No active burn timer found for campfire at $location")
            return false
        }

        
        val elapsed = System.currentTimeMillis() - currentBurnoutJob.startTime
        val remainingTime = CampfireBurnoutSystem.BURNOUT_TIME_MS - elapsed

        if (remainingTime <= 0) {
            Atom.instance?.logger?.warning("Campfire at $location has already expired")
            return false
        }

        
        val campfireState = location.block.state as? org.bukkit.block.Campfire
        if (campfireState == null) {
            Atom.instance?.logger?.warning("Block at $location is not a valid Campfire state")
            return false
        }

        
        var currentStrawCount = 0
        for (i in 0 until campfireState.size) {
            val item = campfireState.getItem(i)
            if (item != null && !item.type.isAir) {
                currentStrawCount++
            }
        }

        if (currentStrawCount >= 4) {
            ActionBarManager.send(player, "§cThis campfire is fully fueled! (Maximum 4 straw)")
            return false
        }

        
        currentBurnoutJob.job.cancel()
        CampfireBurnoutSystem.activeCampfires.remove(location)

        
        val strawItem = org.bukkit.inventory.ItemStack(Material.WHEAT, 1)
        var addedToSlot = -1
        for (i in 0 until campfireState.size) {
            if (campfireState.getItem(i) == null || campfireState.getItem(i)?.type?.isAir == true) {
                addedToSlot = i
                break
            }
        }

        if (addedToSlot != -1) {
            campfireState.setItem(addedToSlot, strawItem)
            
            campfireState.setCookTime(addedToSlot, 0)
            campfireState.setCookTimeTotal(addedToSlot, Integer.MAX_VALUE) 
            campfireState.update()
        }

        
        val newDuration = remainingTime + STRAW_EXTENSION_MS
        val newStartTime = System.currentTimeMillis() - (CampfireBurnoutSystem.BURNOUT_TIME_MS - newDuration)

        
        val pos = net.momirealms.craftengine.core.world.BlockPos(location.blockX, location.blockY, location.blockZ)
        val data = org.shotrush.atom.content.workstation.core.WorkstationDataManager.getWorkstationData(pos, "campfire")
        data.curingStartTime = newStartTime
        org.shotrush.atom.content.workstation.core.WorkstationDataManager.saveData()

        
        val atom = Atom.instance

        
        if (currentStrawCount == 0 && addedToSlot != -1) {
            
            strawBurnJobs[location]?.cancel()

            val strawBurnJob = atom.launch(atom.regionDispatcher(location)) {
                var currentBurningSlot = addedToSlot

                while (isActive) {
                    delay(STRAW_EXTENSION_MS)

                    val campfire = location.block.state as? org.bukkit.block.Campfire
                    if (campfire != null && currentBurningSlot != -1) {
                        
                        campfire.setItem(currentBurningSlot, org.bukkit.inventory.ItemStack(Material.AIR))
                        campfire.update()

                        
                        currentBurningSlot = -1
                        for (i in 0 until campfire.size) {
                            val item = campfire.getItem(i)
                            if (item != null && !item.type.isAir) {
                                currentBurningSlot = i
                                break
                            }
                        }

                        
                        if (currentBurningSlot == -1) {
                            break
                        }
                    } else {
                        break
                    }
                }

                strawBurnJobs.remove(location)
            }

            strawBurnJobs[location] = strawBurnJob
        }

        val job = atom.launch(atom.regionDispatcher(location)) {
            delay(newDuration)

            val block = location.block
            if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
                val blockData = block.blockData
                if (blockData is Lightable && blockData.isLit) {
                    blockData.isLit = false
                    block.blockData = blockData

                    
                    strawBurnJobs[location]?.cancel()
                    strawBurnJobs.remove(location)

                    
                    val finalCampfireState = block.state as? org.bukkit.block.Campfire
                    finalCampfireState?.let {
                        for (i in 0 until it.size) {
                            it.setItem(i, org.bukkit.inventory.ItemStack(Material.AIR))
                        }
                        it.update()
                    }

                    burnoutSystem.playBurnoutEffects(location)

                    Atom.instance?.logger?.info("Campfire at ${location.blockX}, ${location.blockY}, ${location.blockZ} burned out")
                }
            }

            CampfireBurnoutSystem.activeCampfires.remove(location)

            val pos = net.momirealms.craftengine.core.world.BlockPos(location.blockX, location.blockY, location.blockZ)
            org.shotrush.atom.content.workstation.core.WorkstationDataManager.removeWorkstationData(pos)
        }

        CampfireBurnoutSystem.activeCampfires[location] = CampfireBurnoutSystem.Companion.BurnoutJob(job, newStartTime)

        Atom.instance?.logger?.info("Extended campfire burn time at $location by ${STRAW_EXTENSION_MS / 1000}s (new total: ${newDuration / 1000}s)")
        return true
    }

    private fun isLighting(player: Player): Boolean {
        return activeLighting.containsKey(player)
    }

    private fun startLighting(player: Player, block: org.bukkit.block.Block, blockData: Lightable) {
        activeLighting[player]?.job?.cancel()

        val strikeCount = 10 + Random.nextInt(6) 

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

                if (!player.isOnline || player.location.distance(block.location) > 5.0) {
                    break
                }

                player.scheduler.run(Atom.instance!!, { _ ->
                    playLightingEffects(player, block)
                }, null)

                currentStrike++

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