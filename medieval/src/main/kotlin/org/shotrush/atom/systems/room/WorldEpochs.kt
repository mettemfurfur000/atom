package org.shotrush.atom.systems.room

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object WorldEpochs {
    private val chunkEpoch = java.util.concurrent.ConcurrentHashMap<Pair<UUID, Long>, AtomicLong>()

    fun key(worldId: UUID, chunkX: Int, chunkZ: Int): Pair<UUID, Long> {
        val k = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFF_FFFFL)
        return worldId to k
    }

    fun bump(worldId: UUID, chunkX: Int, chunkZ: Int) {
        val k = key(worldId, chunkX, chunkZ)
        chunkEpoch.computeIfAbsent(k) { AtomicLong(0) }.incrementAndGet()
    }

    fun get(worldId: UUID, chunkX: Int, chunkZ: Int): Long {
        val k = key(worldId, chunkX, chunkZ)
        return chunkEpoch[k]?.get() ?: 0L
    }
}