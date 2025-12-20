package org.civlabs.atom.core.system.room

import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.World
import org.civlabs.atom.core.api.ChunkKey
import org.civlabs.atom.core.util.UUIDSerializer
import java.util.*

data class Room(
    val id: UUID,
    val world: World,
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int,
    val blocks: Set<Long>,
) {

    val x: IntRange = minX..maxX
    val y: IntRange = minY..maxY
    val z: IntRange = minZ..maxZ
    val size: Int = blocks.size

    fun toSavedRoom() = SavedRoom(
        id = id,
        world = world.uid,
        minX = minX, minY = minY, minZ = minZ,
        maxX = maxX, maxY = maxY, maxZ = maxZ,
        blocks = blocks
    )

    companion object {
        fun fromSavedRoom(savedRoom: SavedRoom) = Room(
            id = savedRoom.id,
            world = Bukkit.getWorld(savedRoom.world)
                ?: throw IllegalArgumentException("World not found: ${savedRoom.world}"),
            minX = savedRoom.minX, minY = savedRoom.minY, minZ = savedRoom.minZ,
            maxX = savedRoom.maxX, maxY = savedRoom.maxY, maxZ = savedRoom.maxZ,
            blocks = savedRoom.blocks
        )
    }
}

@Serializable
data class SavedRoom(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val world: UUID,
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int,
    val blocks: Set<Long>,
)

@Serializable
data class FlatChunk(
    val key: ChunkKey,
    val rooms: List<@Serializable(with = UUIDSerializer::class) UUID>,
)

@Serializable
data class DataFile(
    val rooms: List<SavedRoom>,
    val chunks: List<FlatChunk>,
)