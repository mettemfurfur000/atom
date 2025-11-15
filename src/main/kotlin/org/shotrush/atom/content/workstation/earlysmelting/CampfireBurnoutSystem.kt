package org.shotrush.atom.content.systems

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.delay
import net.momirealms.craftengine.core.world.BlockPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.Lightable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.Plugin
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.core.api.annotation.RegisterSystem
import java.util.concurrent.ConcurrentHashMap

@RegisterSystem(
    id = "campfire_burnout_system",
    priority = 6,
    toggleable = true,
    description = "Makes campfires burn out after 2 minutes",
    enabledByDefault = true
)
class CampfireBurnoutSystem(private val plugin: Plugin) : Listener {

    companion object {
        private const val BURNOUT_TIME_MS = 2 * 60 * 1000L
        private val activeCampfires = ConcurrentHashMap<Location, BurnoutJob>()

        @Volatile
        private var INSTANCE: CampfireBurnoutSystem? = null

        fun getInstance(): CampfireBurnoutSystem? = INSTANCE

        data class BurnoutJob(
            val job: kotlinx.coroutines.Job,
            val startTime: Long
        )
    }

    init {
        INSTANCE = this
        plugin.server.pluginManager.registerEvents(this, plugin)

        
        val atom = Atom.instance
        atom.launch {
            delay(1000L) 
            resumeAllCampfireTimers()
        }
    }

    private fun saveToWorkstationData(location: Location, startTime: Long) {
        val pos = BlockPos(location.blockX, location.blockY, location.blockZ)
        val data = WorkstationDataManager.getWorkstationData(pos, "campfire")
        data.curingStartTime = startTime
        WorkstationDataManager.saveData()
    }

    private fun removeFromWorkstationData(location: Location) {
        val pos = BlockPos(location.blockX, location.blockY, location.blockZ)
        WorkstationDataManager.removeWorkstationData(pos)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCampfirePlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced

        if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
            val blockData = block.blockData
            if (blockData is Lightable && blockData.isLit) {
                startBurnoutTimer(block.location)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCampfireBreak(event: BlockBreakEvent) {
        val block = event.block

        if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
            cancelBurnoutTimer(block.location)
        }
    }

    fun startBurnoutTimer(location: Location) {
        cancelBurnoutTimer(location)

        val atom = Atom.instance
        val startTime = System.currentTimeMillis()

        
        saveToWorkstationData(location, startTime)

        val job = atom.launch(atom.regionDispatcher(location)) {
            delay(BURNOUT_TIME_MS)

            val block = location.block
            if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
                val blockData = block.blockData
                if (blockData is Lightable && blockData.isLit) {
                    blockData.isLit = false
                    block.blockData = blockData

                    playBurnoutEffects(location)

                    Atom.instance?.logger?.info("Campfire at ${location.blockX}, ${location.blockY}, ${location.blockZ} burned out")
                }
            }

            activeCampfires.remove(location)
            removeFromWorkstationData(location)
        }

        activeCampfires[location] = BurnoutJob(job, startTime)
    }

    fun cancelBurnoutTimer(location: Location) {
        activeCampfires.remove(location)?.job?.cancel()
        removeFromWorkstationData(location)
    }

    fun getRemainingTime(location: Location): Long? {
        val burnoutJob = activeCampfires[location] ?: return null
        val elapsed = System.currentTimeMillis() - burnoutJob.startTime
        val remaining = BURNOUT_TIME_MS - elapsed
        return if (remaining > 0) remaining else 0
    }

    private fun playBurnoutEffects(location: Location) {
        val center = location.clone().add(0.5, 0.5, 0.5)

        center.world?.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f)

        center.world?.spawnParticle(
            Particle.SMOKE,
            center,
            30,
            0.3,
            0.3,
            0.3,
            0.02
        )

        center.world?.spawnParticle(
            Particle.ASH,
            center,
            10,
            0.2,
            0.2,
            0.2,
            0.01
        )
    }

    private fun resumeAllCampfireTimers() {
        Atom.instance?.logger?.info("=== Resuming campfire burnout timers ===")
        var resumedTimers = 0
        var expiredTimers = 0

        
        WorkstationDataManager.getAllWorkstations().forEach { (_, data) ->
            if (data.type == "campfire" && data.curingStartTime != null) {
                val pos = data.position
                val startTime = data.curingStartTime!!
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = BURNOUT_TIME_MS - elapsed

                val location = org.bukkit.Location(
                    plugin.server.getWorld("world"),
                    pos.x().toDouble(),
                    pos.y().toDouble(),
                    pos.z().toDouble()
                )

                if (remaining > 0) {
                    
                    val atom = Atom.instance

                    val job = atom.launch(atom.regionDispatcher(location)) {
                        delay(remaining)

                        val block = location.block
                        if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
                            val blockData = block.blockData
                            if (blockData is Lightable && blockData.isLit) {
                                blockData.isLit = false
                                block.blockData = blockData

                                playBurnoutEffects(location)
                            }
                        }

                        activeCampfires.remove(location)
                        removeFromWorkstationData(location)
                    }

                    activeCampfires[location] = BurnoutJob(job, startTime)
                    resumedTimers++
                    val remainingSec = remaining / 1000
                    Atom.instance?.logger?.info("  ✓ Resumed campfire at (${location.blockX}, ${location.blockY}, ${location.blockZ}) - ${remainingSec}s remaining")
                } else {
                    
                    val atom = Atom.instance
                    atom.launch(atom.regionDispatcher(location)) {
                        val block = location.block
                        if (block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE) {
                            val blockData = block.blockData
                            if (blockData is Lightable && blockData.isLit) {
                                blockData.isLit = false
                                block.blockData = blockData
                            }
                        }
                        removeFromWorkstationData(location)
                    }

                    expiredTimers++
                    Atom.instance?.logger?.info("  ✗ Burning out expired campfire at (${location.blockX}, ${location.blockY}, ${location.blockZ})")
                }
            }
        }

        Atom.instance?.logger?.info("=== Campfire Resume Summary ===")
        Atom.instance?.logger?.info("  Resumed: $resumedTimers timers")
        Atom.instance?.logger?.info("  Expired: $expiredTimers campfires")
        Atom.instance?.logger?.info("===============================")
    }

    fun shutdown() {
        activeCampfires.values.forEach { it.job.cancel() }
        activeCampfires.clear()
    }
}