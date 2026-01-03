package org.civlabs.atom.core.system.structure

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.civlabs.atom.core.CoreAtom
import org.civlabs.atom.core.api.ChunkKey
import org.civlabs.atom.core.util.FileType
import org.civlabs.atom.core.util.LocationUtil
import org.civlabs.atom.core.util.readSerializedFileOrNull
import org.civlabs.atom.core.util.writeSerializedFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

object StructureRegistry {
    private val structuresById = ConcurrentHashMap<UUID, Structure>()
    internal val chunkIndex = ConcurrentHashMap<ChunkKey, MutableSet<UUID>>()

    private fun register(structure: Structure) : Structure {
        structuresById[structure.id] = structure
        indexStructure(structure)
        CoreAtom.instance.server.pluginManager.callEvent(StructureCreateEvent(structure))
        return structure
    }

    fun isOverlapping(structure: Structure): Boolean {
        return structuresInAabb(
            structure.world.uid, structure.minX, structure.minY, structure.minZ,
            structure.maxX, structure.maxY, structure.maxZ
        ).any { existing ->
            existing.blocks.keys.any { it in structure.blocks.keys }
        }
    }

    fun registerNonOverlapping(structure: Structure): Structure? {
        return if (isOverlapping(structure)) null else register(structure)
    }

    fun remove(structureId: UUID) {
        val s = structuresById.remove(structureId) ?: return
        CoreAtom.instance.server.pluginManager.callEvent(StructureDestroyEvent(s))
        unindexStructure(s)
    }

    fun invalidateByBlock(worldId: UUID, x: Int, y: Int, z: Int) {
        val ck = ChunkKey(worldId, x shr 4, z shr 4)
        val ids = chunkIndex[ck]?.toList() ?: emptyList()
        val packed = LocationUtil.pack(x, y, z)
        for (id in ids) {
            val s = structuresById[id] ?: continue
            if (s.blocks.containsKey(packed)) {
                remove(s.id)
            }
        }
    }

    fun structureAt(loc: Location): Structure? {
        val worldId = loc.world.uid
        val ck = ChunkKey(worldId, loc.blockX shr 4, loc.blockZ shr 4)
        val ids = chunkIndex[ck] ?: return null
        val packed = LocationUtil.pack(loc.blockX, loc.blockY, loc.blockZ)
        for (id in ids) {
            val s = structuresById[id] ?: continue
            if (s.blocks.containsKey(packed)) return s
        }
        return null
    }

    fun structuresInAabb(
        worldId: UUID,
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
    ): List<Structure> {
        val minCx = minX shr 4;
        val maxCx = maxX shr 4
        val minCz = minZ shr 4;
        val maxCz = maxZ shr 4
        val ids = HashSet<UUID>()
        for (cx in minCx..maxCx) for (cz in minCz..maxCz) {
            ids += chunkIndex[ChunkKey(worldId, cx, cz)].orEmpty()
        }
        return ids.mapNotNull { structuresById[it] }.filter { s ->
            s.world.uid == worldId &&
                    s.minX <= maxX && s.maxX >= minX &&
                    s.minY <= maxY && s.maxY >= minY &&
                    s.minZ <= maxZ && s.maxZ >= minZ
        }
    }

    private fun indexStructure(structure: Structure) {
        val worldId = structure.world.uid
        val minCx = structure.minX shr 4;
        val maxCx = structure.maxX shr 4
        val minCz = structure.minZ shr 4;
        val maxCz = structure.maxZ shr 4
        for (cx in minCx..maxCx) for (cz in minCz..maxCz) {
            val key = ChunkKey(worldId, cx, cz)
            val set = chunkIndex.computeIfAbsent(key) { CopyOnWriteArraySet() }
            set.add(structure.id)
        }
    }

    private fun unindexStructure(structure: Structure) {
        val worldId = structure.world.uid
        val minCx = structure.minX shr 4;
        val maxCx = structure.maxX shr 4
        val minCz = structure.minZ shr 4;
        val maxCz = structure.maxZ shr 4
        for (cx in minCx..maxCx) for (cz in minCz..maxCz) {
            val key = ChunkKey(worldId, cx, cz)
            val set = chunkIndex[key] ?: continue
            set.remove(structure.id)
            if (set.isEmpty()) chunkIndex.remove(key)
        }
    }

    fun saveAllToDisk() {
        for (world in Bukkit.getWorlds()) saveWorldToDisk(world)
        CoreAtom.instance.logger.info("Saved ${structuresById.size} structures to disk.")
    }

    fun saveWorldToDisk(world: World) {
        val structures = structuresById.values.filter { it.world.uid == world.uid }
        val chunks = chunkIndex.filterValues { it.any { id -> structures.any { s -> s.id == id } } }
        val saveStructures = structures.map { it.toSavedStructure() }
        val dataFile = DataFileStructures(
            saveStructures.toList(),
            chunks.map { FlatChunkStructures(it.key, it.value.toList()) }.toList()
        )
        writeSerializedFile(dataFile, world.worldPath.resolve("data/structures.dat"), FileType.NBT)

        CoreAtom.instance.logger.info("Saved ${structures.size} structures from disk for world ${world.name}..")
    }

    fun readWorldFromDisk(world: World) {
        val dataFile =
            readSerializedFileOrNull(world.worldPath.resolve("data/structures.dat"), FileType.NBT) ?: DataFileStructures(
                emptyList(),
                emptyList()
            )
        structuresById.putAll(dataFile.structures.map { Structure.fromSavedStructure(it) }.associateBy { it.id })
        chunkIndex.putAll(dataFile.chunks.associateBy { it.key }.mapValues { it.value.structures.toHashSet() })

        CoreAtom.instance.logger.info("Loaded ${dataFile.structures.size} structures from disk for world ${world.name}..")
    }

    fun readAllFromDisk() {
        chunkIndex.clear()
        structuresById.clear()

        for (world in Bukkit.getWorlds()) readWorldFromDisk(world)
        CoreAtom.instance.logger.info("Loaded ${structuresById.size} structures from disk.")
    }
}
