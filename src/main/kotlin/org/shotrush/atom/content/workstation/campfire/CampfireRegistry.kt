package org.shotrush.atom.content.workstation.campfire

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Lightable
import org.bukkit.plugin.Plugin
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import net.momirealms.craftengine.core.world.BlockPos
import java.util.concurrent.ConcurrentHashMap

class CampfireRegistry(private val plugin: Plugin) {
    companion object {
        const val BASE_BURN_MS = 2 * 60 * 1000L
        private const val WS_TYPE = "campfire"
    }

    data class CampfireState(
        val location: Location,
        var lit: Boolean,
        var startTime: Long?,          // when became lit (for burnout + mold)
        var burnoutJob: Job? = null
    )

    interface Listener {
        fun onCampfirePlaced(state: CampfireState) {}
        fun onCampfireLit(state: CampfireState) {}
        fun onCampfireExtinguished(state: CampfireState, reason: String) {}
        fun onCampfireBroken(state: CampfireState) {}
        fun onFuelAdded(state: CampfireState, addedMs: Long, newEndTimeMs: Long) {}
        fun onResumeTimerScheduled(state: CampfireState, remainingMs: Long) {}
        fun onResumeTimerExpired(state: CampfireState) {}
    }

    private val atom get() = Atom.instance
    private val listeners = mutableListOf<Listener>()
    private val active = ConcurrentHashMap<Location, CampfireState>()

    fun addListener(l: Listener) = listeners.add(l)
    fun removeListener(l: Listener) = listeners.remove(l)

    fun isTracked(loc: Location) = active.containsKey(loc)
    fun getState(loc: Location) = active[loc]
    
    /**
     * Get all active campfire states for iteration.
     */
    fun getAllStates(): Collection<CampfireState> = active.values

    fun trackOnPlace(loc: Location, lit: Boolean) {
        val fixed = fix(loc)
        val state = CampfireState(fixed, lit, startTime = null)
        active[fixed] = state
        listeners.forEach { it.onCampfirePlaced(state) }
        if (lit) light(state, fromEvent = "place")
    }

    fun lightAt(loc: Location) {
        val state = active.getOrPut(fix(loc)) { CampfireState(fix(loc), false, null) }
        light(state, fromEvent = "interact")
    }

    private fun light(state: CampfireState, fromEvent: String) {
        val block = state.location.block
        val data = block.blockData
        if (data is Lightable && !data.isLit) {
            data.isLit = true
            block.blockData = data
        }
        if (!state.lit) {
            state.lit = true
            state.startTime = System.currentTimeMillis()
            persistStartTime(state)
            scheduleBurnout(state)
            listeners.forEach { it.onCampfireLit(state) }
            atom.logger.info("Campfire lit at ${xyz(state)} (via $fromEvent)")
        }
    }

    fun extinguishAt(loc: Location, reason: String) {
        val state = active[fix(loc)] ?: return
        if (!state.lit) return

        atom.launch(atom.regionDispatcher(state.location)) {
            cancelBurnout(state)
            setLit(state.location, false)
            state.lit = false
            listeners.forEach { it.onCampfireExtinguished(state, reason) }
            clearPersistence(state)
            atom.logger.info("Campfire extinguished at ${xyz(state)} ($reason)")
        }
    }

    fun brokenAt(loc: Location) {
        val state = active.remove(fix(loc)) ?: return
        atom.launch(atom.regionDispatcher(state.location)) {
            cancelBurnout(state)
            listeners.forEach { it.onCampfireBroken(state) }
            clearPersistence(state)
            atom.logger.info("Campfire broken at ${xyz(state)}")
        }
    }

    fun addFuel(loc: Location, addedMs: Long): Long? {
        val fixed = fix(loc)
        val state = active[fixed] ?: return null
        if (!state.lit || state.startTime == null) return null
        val now = System.currentTimeMillis()
        val elapsed = now - state.startTime!!
        val remaining = BASE_BURN_MS - elapsed
        if (remaining <= 0) return null

        val newDuration = remaining + addedMs
        val newStart = now - (BASE_BURN_MS - newDuration)
        state.startTime = newStart

        persistStartTime(state)

        // reschedule burnout
        atom.launch(atom.regionDispatcher(state.location)) {
            cancelBurnout(state)
            scheduleBurnout(state)
        }

        val endTime = newStart + BASE_BURN_MS
        listeners.forEach { it.onFuelAdded(state, addedMs, endTime) }
        atom.logger.info("Fuel added at ${xyz(state)}: +${addedMs / 1000}s")
        return endTime
    }

    fun resumeFromDisk(targetWorld: org.bukkit.World) {
        atom.logger.info("=== Resuming campfires for world ${targetWorld.name} ===")
        var resumed = 0
        var expired = 0

        WorkstationDataManager.getAllWorkstations().forEach { (_, data) ->
            if (data.type == WS_TYPE && data.curingStartTime != null) {
                val loc = Location(
                    targetWorld,
                    data.position.x().toDouble(),
                    data.position.y().toDouble(),
                    data.position.z().toDouble()
                )
                
                org.bukkit.Bukkit.getServer().regionScheduler.run(plugin, loc) { _ ->
                    try {
                        val block = loc.block
                        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) {
                            clearKey(data.position)
                            return@run
                        }
                        
                        val lightable = block.blockData as? Lightable
                        if (lightable == null) {
                            clearKey(data.position)
                            return@run
                        }

                        val state = CampfireState(loc, lightable.isLit, data.curingStartTime)
                        val elapsed = System.currentTimeMillis() - data.curingStartTime!!
                        val remaining = BASE_BURN_MS - elapsed

                        if (remaining > 0 && lightable.isLit) {
                            active[fix(loc)] = state
                            scheduleBurnout(state, remainingOverride = remaining)
                            listeners.forEach { it.onResumeTimerScheduled(state, remaining) }
                            atom.logger.info("  ✓ Resume (${xyz(state)}) ${remaining / 1000}s")
                        } else {
                            setLit(loc, false)
                            listeners.forEach { it.onResumeTimerExpired(state) }
                            clearPersistence(state)
                            atom.logger.info("  ✗ Expired (${xyz(state)})")
                        }
                    } catch (e: Exception) {
                        atom.logger.warning("  ⊗ Error resuming campfire at (${data.position.x()},${data.position.y()},${data.position.z()}): ${e.message}")
                    }
                }
            }
        }

        atom.logger.info("Resume summary: resumed=$resumed expired=$expired (final counts may differ due to async processing)")
    }

    private fun scheduleBurnout(state: CampfireState, remainingOverride: Long? = null) {
        val start = state.startTime ?: return
        val now = System.currentTimeMillis()
        val remaining = remainingOverride ?: (BASE_BURN_MS - (now - start)).coerceAtLeast(0)
        if (remaining <= 0) {
            // immediate extinguish
            extinguishAt(state.location, "burnout")
            return
        }

        val job = atom.launch(atom.regionDispatcher(state.location)) {
            delay(remaining)
            // double-check lit
            val lightable = state.location.block.blockData as? Lightable
            if (lightable != null && lightable.isLit) {
                setLit(state.location, false)
                state.lit = false
                listeners.forEach { it.onCampfireExtinguished(state, "burnout") }
                clearPersistence(state)
                atom.logger.info("Campfire burned out at ${xyz(state)}")
            }
        }
        state.burnoutJob = job
    }

    private suspend fun cancelBurnout(state: CampfireState) {
        state.burnoutJob?.cancel()
        state.burnoutJob?.cancelAndJoin()
        state.burnoutJob = null
    }

    private fun setLit(loc: Location, value: Boolean) {
        val block = loc.block
        val data = block.blockData
        if (data is Lightable) {
            if (data.isLit != value) {
                data.isLit = value
                block.blockData = data
            }
        }
    }

    private fun persistStartTime(state: CampfireState) {
        val pos = BlockPos(state.location.blockX, state.location.blockY, state.location.blockZ)
        val d = WorkstationDataManager.getWorkstationData(pos, WS_TYPE)
        d.curingStartTime = state.startTime
        WorkstationDataManager.saveData()
    }

    private fun clearPersistence(state: CampfireState) {
        val pos = BlockPos(state.location.blockX, state.location.blockY, state.location.blockZ)
        WorkstationDataManager.removeWorkstationData(pos)
    }

    private fun clearKey(pos: BlockPos) {
        WorkstationDataManager.removeWorkstationData(pos)
    }

    private fun fix(l: Location) = Location(l.world, l.blockX.toDouble(), l.blockY.toDouble(), l.blockZ.toDouble())
    private fun xyz(s: CampfireState) = "${s.location.blockX},${s.location.blockY},${s.location.blockZ}"
}