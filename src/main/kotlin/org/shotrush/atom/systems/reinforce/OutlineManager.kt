package org.shotrush.atom.systems.reinforce

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class OutlineManager(
    private val snapshotCollector: SnapshotCollector,
    private val builder: VoxelOutlineBuilder,
    private val renderer: OutlineRenderer,
    private val scope: CoroutineScope,
    private val buildCooldownTicks: Long = 10L,
    private val renderIntervalTicks: Long = 5L,
    private val defaultHalfSize: Int = 4,
    private val yBandHeight: Int = 0,
) {
    private val sessions = ConcurrentHashMap<UUID, PlayerOutlineSession>()
    private val lastBuildTick = ConcurrentHashMap<UUID, Long>()
    private var tickCounter: Long = 0

    private fun ensureSession(player: Player): PlayerOutlineSession =
        sessions.computeIfAbsent(player.uniqueId) { PlayerOutlineSession(it, scope) }

    fun tick(players: Collection<Player>) {
        tickCounter++

        players.forEach { player ->
            val session = ensureSession(player)
            val needsNewBuild = shouldRebuild(player, session)
            if (needsNewBuild && !session.isBuilding()) {
                val last = lastBuildTick[player.uniqueId] ?: 0L
                if (tickCounter - last >= buildCooldownTicks) {
                    lastBuildTick[player.uniqueId] = tickCounter
                    session.startBuild(
                        player = player,
                        halfSize = defaultHalfSize,
                        yBandHeight = yBandHeight,
                        snapshotCollector = snapshotCollector,
                        builder = builder
                    )
                }
            }
        }

        if (tickCounter % renderIntervalTicks == 0L) {
            players.forEach { player ->
                val cache = sessions[player.uniqueId]?.cache ?: return@forEach
                scope.launch {
                    renderer.render(player, cache.geometry)
                }
            }
        }
    }

    private fun shouldRebuild(player: Player, session: PlayerOutlineSession): Boolean {
        val cache = session.cache ?: return true
        val bx = player.location.blockX
        val bz = player.location.blockZ
        val movedCell = (bx != cache.originX) || (bz != cache.originZ)
        val sizeChanged = cache.halfSize != defaultHalfSize
        return movedCell || sizeChanged
    }
}