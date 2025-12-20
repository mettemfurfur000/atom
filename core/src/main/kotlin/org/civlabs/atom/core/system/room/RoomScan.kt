package org.civlabs.atom.core.system.room

interface RoomScan {
    suspend fun scan(): Boolean

    fun toRoom(): Room?
}