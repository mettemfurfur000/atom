package org.shotrush.atom.api

import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.shotrush.atom.util.UUIDSerializer
import java.util.*

@Serializable
data class ChunkKey(
    @Serializable(with = UUIDSerializer::class)
    val worldId: UUID,
    val x: Int, val z: Int,
)

fun Location.chunkKey() = ChunkKey(world.uid, chunk.x, chunk.z)