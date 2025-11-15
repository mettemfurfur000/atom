package org.shotrush.atom.content.systems

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.delay
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
        
        val job = atom.launch(atom.regionDispatcher(location)) {
            val startTime = System.currentTimeMillis()

            
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
        }

        activeCampfires[location] = BurnoutJob(job, System.currentTimeMillis())
    }

    
    fun cancelBurnoutTimer(location: Location) {
        activeCampfires.remove(location)?.job?.cancel()
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

    
    fun shutdown() {
        activeCampfires.values.forEach { it.job.cancel() }
        activeCampfires.clear()
    }
}