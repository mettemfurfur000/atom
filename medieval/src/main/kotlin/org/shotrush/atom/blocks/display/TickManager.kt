package org.shotrush.atom.blocks.display

import kotlinx.coroutines.Job
import net.momirealms.craftengine.core.entity.player.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TickManager {
    private val jobs = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Job>>()

    fun startOrReplace(player: Player, elementId: String, job: Job) {
        val map = jobs.computeIfAbsent(player.uuid()) { ConcurrentHashMap() }
        map.put(elementId, job)?.cancel()
    }

    fun stop(player: Player, elementId: String) {
        jobs[player.uuid()]?.remove(elementId)?.cancel()
    }

    fun stopAll(player: Player) {
        jobs.remove(player.uuid())?.values?.forEach { it.cancel() }
    }
}