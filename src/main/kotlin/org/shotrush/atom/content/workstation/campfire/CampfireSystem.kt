package org.shotrush.atom.content.workstation.campfire

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import com.github.shynixn.mccoroutine.folia.ticks
import kotlinx.coroutines.delay
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.Lightable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.campfire.features.BurnoutFeature
import org.shotrush.atom.content.workstation.campfire.features.MoldFiringFeature
import org.shotrush.atom.content.workstation.campfire.features.StrawFuelFeature
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.matches
import kotlin.random.Random

@RegisterSystem(
    id = "campfire_system",
    priority = 5,
    toggleable = true,
    description = "Unified campfire lifecycle (lighting, burnout, straw fuel, mold firing)",
    enabledByDefault = true
)
class CampfireSystem(private val plugin: Plugin) : Listener {

    private val registry = CampfireRegistry(plugin)
    private val burnout = BurnoutFeature()
    private val straw = StrawFuelFeature()
    private val mold = MoldFiringFeature()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        registry.addListener(burnout)
        registry.addListener(straw)
        registry.addListener(mold)
    }
    
    private val resumedWorlds = mutableSetOf<String>()
    
    @EventHandler
    fun onWorldLoad(event: org.bukkit.event.world.WorldLoadEvent) {
        if (resumedWorlds.add(event.world.name)) {
            Atom.instance.launch {
                delay(100L)
                registry.resumeFromDisk(event.world)
            }
        }
    }
    
    @EventHandler
    fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        val world = event.player.world
        if (resumedWorlds.add(world.name)) {
            Atom.instance.launch {
                delay(2000L)
                registry.resumeFromDisk(world)
            }
        }
    }

    // Ensure campfires placed are unlit by default but tracked
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val b = event.blockPlaced
        if (b.type != Material.CAMPFIRE && b.type != Material.SOUL_CAMPFIRE) return

        val data = b.blockData
        if (data is Lightable) {
            data.isLit = false
            b.blockData = data
        }
        registry.trackOnPlace(b.location, lit = false)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) return

        val data = block.blockData as? Lightable ?: return
        val player = event.player
        val item = player.inventory.itemInMainHand

        // Straw fuel add when lit
        if (item.matches("atom:straw") && data.isLit) {
            val end = straw.tryAddStrawFuel(registry, block.location)
            if (end != null) {
                event.isCancelled = true
                item.subtract(1)
                val remaining = (end - System.currentTimeMillis()).coerceAtLeast(0)
                val min = (remaining / 60000L).toInt()
                val sec = ((remaining % 60000L) / 1000L).toInt()
                ActionBarManager.send(player, "campfire", "<green>Added fuel! Time remaining:</green> <yellow>${min}m ${sec}s</yellow>")
            } else {
                ActionBarManager.send(player, "campfire","<red>Could not add fuel.</red>")
            }
            return
        }

        // Lighting with pebble when unlit
        if (item.matches("atom:pebble") && !data.isLit) {
            // simulate multiple strikes with progress
            val strikesNeeded = 10 + Random.nextInt(6)
            startStrikeTask(player, block.location, strikesNeeded) {
                registry.lightAt(block.location)
                ActionBarManager.send(player, "campfire","<green>Successfully lit the campfire!</green>")
            }
            return
        }

        // Manual extinguish if player toggles and it becomes unlit
        Atom.instance.launch(Atom.instance.regionDispatcher(block.location)) {
            delay(50L)
            val updated = block.location.block.blockData as? Lightable ?: return@launch
            if (!updated.isLit) {
                registry.extinguishAt(block.location, "player")
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val b = event.block
        if (b.type != Material.CAMPFIRE && b.type != Material.SOUL_CAMPFIRE) return
        
        registry.brokenAt(b.location)
        
        val campfire = b.state as? org.bukkit.block.Campfire
        if (campfire != null) {
            for (i in 0 until campfire.size) {
                campfire.setItem(i, null)
            }
            campfire.update(false)
        }
    }

    private fun startStrikeTask(
        player: Player,
        campfireLoc: Location,
        strikesNeeded: Int,
        onSuccess: () -> Unit
    ) {
        val atom = Atom.instance
        atom.launch(atom.regionDispatcher(campfireLoc)) {
            var strikes = 0
            ActionBarManager.sendStatus(player, "<gray>Lighting campfire... Strike repeatedly</gray>")
            while (strikes < strikesNeeded) {
                delay(5.ticks)
                val data = campfireLoc.block.blockData
                if (data is Lightable && data.isLit) {
                    return@launch
                }
                // Basic validations
                if (!player.isOnline || player.location.world != campfireLoc.world ||
                    player.location.distance(campfireLoc) > 5.0 || !player.hasActiveItem()
                ) {
                    ActionBarManager.sendStatus(player, "<red>Lighting cancelled</red>")
                    delay(800)
                    ActionBarManager.clearStatus(player)
                    return@launch
                }
                // Feedback
                player.world.playSound(campfireLoc.clone().add(0.5, 0.5, 0.5), Sound.BLOCK_STONE_HIT, 1.0f, 1.5f)
                player.world.spawnParticle(Particle.DUST_PLUME, campfireLoc.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02)
                strikes++
                val progress = (strikes.toFloat() / strikesNeeded * 100).toInt()

                val str = buildString {
                    append("<yellow>$progress%</yellow> ")
                    append("<dark_gray>[")
                    append("<green>")
                    val total = 20
                    val bars = (total * (progress / 100.0)).toInt()
                    repeat(bars) {
                        append("|")
                    }
                    append("</green><gray>")
                    repeat(total - bars) {
                        append("|")
                    }
                    append("</gray>")
                    append("]")
                    append("<dark_gray>")
                }
                ActionBarManager.sendStatus(player, str)
            }
            ActionBarManager.clearStatus(player)
            // Chance of success
            if (Random.nextDouble() < 0.7) {
                onSuccess()
                val center = campfireLoc.clone().add(0.5, 0.5, 0.5)
                center.world?.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f)
                center.world?.spawnParticle(Particle.FLAME, center, 20, 0.3, 0.3, 0.3, 0.02)
            } else {
                val center = campfireLoc.clone().add(0.5, 0.5, 0.5)
                center.world?.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 2.0f)
                center.world?.spawnParticle(Particle.SMOKE, center, 10, 0.2, 0.2, 0.2, 0.02)
                ActionBarManager.send(player, "campfire", "<red>Failed to light the campfire. Try again!</red>")
            }
        }
    }
}