package org.shotrush.atom.systems.room

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ScanReservations {
    private val owners = ConcurrentHashMap<Long, UUID>()

    fun tryReserve(cellKey: Long, scanId: UUID): Boolean {
        val prev = owners.putIfAbsent(cellKey, scanId)
        return prev == null || prev == scanId
    }

    fun releaseCells(keys: Collection<Long>, scanId: UUID) {
        for (k in keys) owners.remove(k, scanId)
    }
}