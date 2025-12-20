package org.civlabs.atom.core.system.structure

import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.World
import org.civlabs.atom.core.api.ChunkKey
import org.civlabs.atom.core.util.UUIDSerializer
import java.util.*

data class Structure(
    val id: UUID,           //
    val world: World,       //
    val defName: String,    // so I know what the structure actually is
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int,
    // map of packed location -> material name
    val blocks: Map<Long, String>,
    // explicit controller block positions (packed)
    val controllers: Set<Long>
) {

    val x: IntRange = minX..maxX
    val y: IntRange = minY..maxY
    val z: IntRange = minZ..maxZ
    val size: Int = blocks.size

    fun toSavedStructure() = SavedStructure(
        id = id,
        world = world.uid,
        minX = minX, minY = minY, minZ = minZ,
        maxX = maxX, maxY = maxY, maxZ = maxZ,
        blocks = blocks,
        controllers = controllers,
        defName = defName
    )

    companion object {
        fun fromSavedStructure(saved: SavedStructure) = Structure(
            id = saved.id,
            world = Bukkit.getWorld(saved.world)
                ?: throw IllegalArgumentException("World not found: ${saved.world}"),
            minX = saved.minX, minY = saved.minY, minZ = saved.minZ,
            maxX = saved.maxX, maxY = saved.maxY, maxZ = saved.maxZ,
            blocks = saved.blocks,
            controllers = saved.controllers,
            defName = saved.defName
        )
    }
}

@Serializable
data class SavedStructure(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val world: UUID,
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int,
    val defName: String,
    val blocks: Map<Long, String>,
    val controllers: Set<Long>
)

@Serializable
data class FlatChunkStructures(
    val key: ChunkKey,
    val structures: List<@Serializable(with = UUIDSerializer::class) UUID>,
)

@Serializable
data class DataFileStructures(
    val structures: List<SavedStructure>,
    val chunks: List<FlatChunkStructures>,
)
