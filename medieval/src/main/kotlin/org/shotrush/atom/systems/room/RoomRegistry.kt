package org.shotrush.atom.systems.room

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.joml.Vector3i
import org.shotrush.atom.Atom
import org.shotrush.atom.FileType
import org.shotrush.atom.api.ChunkKey
import org.shotrush.atom.readSerializedFileOrNull
import org.shotrush.atom.util.LocationUtil
import org.shotrush.atom.writeSerializedFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


object RoomRegistry {
    private val roomsById = ConcurrentHashMap<UUID, Room>()
    internal val chunkIndex = ConcurrentHashMap<ChunkKey, MutableSet<UUID>>()

    fun register(room: Room) {
        roomsById[room.id] = room
        indexRoom(room)
        Atom.instance.server.pluginManager.callEvent(RoomCreateEvent(room))
    }

    fun tryRegisterDedup(room: Room): Boolean {
        // Check overlap with existing in intersecting chunks
        val candidates = roomsInAabb(
            room.world.uid, room.minX, room.minY, room.minZ,
            room.maxX, room.maxY, room.maxZ
        )
        val overlaps = candidates.any { existing ->
            // Fast AABB already true; check voxel intersection
            existing.blocks.any { it in room.blocks }
        }
        if (overlaps) return false
        register(room)
        return true
    }

    fun remove(roomId: UUID) {
        val room = roomsById.remove(roomId) ?: return
        Atom.instance.server.pluginManager.callEvent(RoomDestroyEvent(room))
        unindexRoom(room)
    }

    private val neighbors = arrayOf(
        Vector3i(1, 0, 0), Vector3i(-1, 0, 0),
        Vector3i(0, 1, 0), Vector3i(0, -1, 0),
        Vector3i(0, 0, 1), Vector3i(0, 0, -1),
    )

    fun invalidateByBlock(worldId: UUID, x: Int, y: Int, z: Int) {
        val ck = ChunkKey(worldId, x shr 4, z shr 4)
        val ids = chunkIndex[ck]?.toList() ?: emptyList()
        for (id in ids) {
            val r = roomsById[id] ?: continue
            if (x in r.x && y in r.y && z in r.z) {
                if (r.blocks.contains(LocationUtil.pack(x, y, z))) {
                    remove(r.id)
                }
            }

            for (n in neighbors) {
                if (x + n.x in r.x && y + n.y in r.y && z + n.z in r.z) {
                    if (r.blocks.contains(LocationUtil.pack(x + n.x, y + n.y, z + n.z))) {
                        remove(r.id)
                    }
                }
            }
        }
    }

    fun roomAt(loc: Location): Room? {
        val worldId = loc.world.uid
        val ck = ChunkKey(worldId, loc.blockX shr 4, loc.blockZ shr 4)
        val ids = chunkIndex[ck] ?: return null
        val x = loc.blockX;
        val y = loc.blockY;
        val z = loc.blockZ
        for (id in ids) {
            val r = roomsById[id] ?: continue
            if (x in r.x && y in r.y && z in r.z) {
                if (r.blocks.contains(LocationUtil.pack(x, y, z))) {
                    return r
                }
            }
        }
        return null
    }

    fun roomsInAabb(
        worldId: UUID,
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
    ): List<Room> {
        val minCx = minX shr 4;
        val maxCx = maxX shr 4
        val minCz = minZ shr 4;
        val maxCz = maxZ shr 4
        val ids = HashSet<UUID>()
        for (cx in minCx..maxCx) for (cz in minCz..maxCz) {
            ids += chunkIndex[ChunkKey(worldId, cx, cz)].orEmpty()
        }
        return ids.mapNotNull { roomsById[it] }.filter { r ->
            r.world.uid == worldId &&
                    r.minX <= maxX && r.maxX >= minX &&
                    r.minY <= maxY && r.maxY >= minY &&
                    r.minZ <= maxZ && r.maxZ >= minZ
        }
    }

    private fun indexRoom(room: Room) {
        val worldId = room.world.uid
        val minCx = room.minX shr 4;
        val maxCx = room.maxX shr 4
        val minCz = room.minZ shr 4;
        val maxCz = room.maxZ shr 4
        for (cx in minCx..maxCx) for (cz in minCz..maxCz) {
            val key = ChunkKey(worldId, cx, cz)
            val set = chunkIndex.computeIfAbsent(key) { CopyOnWriteArraySet() }
            set.add(room.id)
        }
    }

    private fun unindexRoom(room: Room) {
        val worldId = room.world.uid
        val minCx = room.minX shr 4;
        val maxCx = room.maxX shr 4
        val minCz = room.minZ shr 4;
        val maxCz = room.maxZ shr 4
        for (cx in minCx..maxCx) for (cz in minCz..maxCz) {
            val key = ChunkKey(worldId, cx, cz)
            val set = chunkIndex[key] ?: continue
            set.remove(room.id)
            if (set.isEmpty()) chunkIndex.remove(key)
        }
    }

    fun saveAllToDisk() {
        for (world in Bukkit.getWorlds()) saveWorldToDisk(world)
        Atom.instance.logger.info("Saved ${roomsById.size} rooms to disk.")
    }

    fun saveWorldToDisk(world: World) {
        val rooms = roomsById.values.filter { it.world.uid == world.uid }
        val chunks = chunkIndex.filterValues { it.any { id -> rooms.any { room -> room.id == id } } }
        val saveRoomsById = rooms.map { it.toSavedRoom() }
        val dataFile = DataFile(saveRoomsById.toList(), chunks.map { FlatChunk(it.key, it.value.toList()) }.toList())
        writeSerializedFile(dataFile, world.worldPath.resolve("data/rooms.dat"), FileType.NBT)

        Atom.instance.logger.info("Saved ${rooms.size} rooms from disk for world ${world.name}..")
    }

    fun readWorldFromDisk(world: World) {
        val dataFile =
            readSerializedFileOrNull(world.worldPath.resolve("data/rooms.dat"), FileType.NBT) ?: DataFile(
                emptyList(),
                emptyList()
            )
        roomsById.putAll(dataFile.rooms.map { Room.fromSavedRoom(it) }.associateBy { it.id })
        chunkIndex.putAll(dataFile.chunks.associateBy { it.key }.mapValues { it.value.rooms.toHashSet() })

        Atom.instance.logger.info("Loaded ${dataFile.rooms.size} rooms from disk for world ${world.name}..")
    }

    fun readAllFromDisk() {
        chunkIndex.clear()
        roomsById.clear()

        for (world in Bukkit.getWorlds()) readWorldFromDisk(world)
        Atom.instance.logger.info("Loaded ${roomsById.size} rooms from disk.")
    }
}