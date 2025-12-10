package org.shotrush.atom.systems.room

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.bukkit.World
import org.joml.Vector3i
import org.shotrush.atom.Atom
import org.shotrush.atom.util.LocationUtil
import java.util.UUID
import kotlin.collections.ArrayDeque

class RoomScanSimple(
    val world: World,
    val startPosition: Vector3i,
    private val maxVolume: Int = 200,
    private val epochCheckInterval: Int = 64,
    // Batch tuning: how many nodes to process per chunk hop
    private val nodesPerChunkHop: Int = 256,
) : RoomScan {
    private val scanId = UUID.randomUUID()
    private var hasScanned = false

    val scannedPositions = mutableSetOf<Long>()
    val volume: Int get() = scannedPositions.size

    private val neighbors = arrayOf(
        Vector3i(1, 0, 0), Vector3i(-1, 0, 0),
        Vector3i(0, 1, 0), Vector3i(0, -1, 0),
        Vector3i(0, 0, 1), Vector3i(0, 0, -1),
    )

    private fun inRangeY(y: Int) = y in -128..512

    private fun tryPack(x: Int, y: Int, z: Int): Long? =
        try {
            LocationUtil.pack(x, y, z)
        } catch (_: IllegalArgumentException) {
            null
        }

    // Epoch tracking per chunk
    private val touchedChunks = HashMap<Long, Long>()
    private fun packChunk(cx: Int, cz: Int): Long =
        (cx.toLong() shl 32) or (cz.toLong() and 0xFFFF_FFFFL)

    private fun rememberEpoch(cx: Int, cz: Int) {
        val pk = packChunk(cx, cz)
        touchedChunks.computeIfAbsent(pk) {
            WorldEpochs.get(world.uid, cx, cz)
        }
    }

    private fun epochsStable(): Boolean {
        for ((packed, epoch) in touchedChunks) {
            val cx = (packed shr 32).toInt()
            val cz = (packed and 0xFFFF_FFFFL).toInt()
            val now = WorldEpochs.get(world.uid, cx, cz)
            if (now != epoch) return false
        }
        return true
    }

    override suspend fun scan(): Boolean {
        val ok = try {
            runBatchedScan()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        if(!ok) {
            println("Room scan failed! Scanned $volume blocks.")
        }
        hasScanned = true
        ScanReservations.releaseCells(scannedPositions, scanId)
        return ok
    }

    /**
     * Batched by chunk:
     * - frontierBuckets: chunkKey -> deque of positions
     * - process up to nodesPerChunkHop per chunk hop
     * - enqueue neighbors into their chunk bucket
     */
    private suspend fun runBatchedScan(): Boolean = coroutineScope {
        val sx = startPosition.x()
        val sy = startPosition.y()
        val sz = startPosition.z()
        if (!inRangeY(sy)) return@coroutineScope false

        val startKey = tryPack(sx, sy, sz) ?: return@coroutineScope false
        // Reserve the starting voxel first; fail-fast on contention
        if (!ScanReservations.tryReserve(startKey, scanId)) return@coroutineScope false

        val startChunkX = sx shr 4
        val startChunkZ = sz shr 4
        rememberEpoch(startChunkX, startChunkZ)

        scannedPositions.add(startKey)

        // Buckets per chunk
        val frontierBuckets = HashMap<Long, ArrayDeque<Vector3i>>()
        fun bucketKey(cx: Int, cz: Int) = packChunk(cx, cz)
        fun getBucket(cx: Int, cz: Int) =
            frontierBuckets.computeIfAbsent(bucketKey(cx, cz)) { ArrayDeque() }

        getBucket(startChunkX, startChunkZ).add(Vector3i(sx, sy, sz))

        var stepsSinceEpochCheck = 0

        while (frontierBuckets.isNotEmpty()) {
            // Iterate over a snapshot of keys to allow mutation during iteration
            val keys = frontierBuckets.keys.toList()
            var progressed = false

            for (pk in keys) {
                val cx = (pk shr 32).toInt()
                val cz = (pk and 0xFFFF_FFFFL).toInt()
                val bucket = frontierBuckets[pk] ?: continue
                if (bucket.isEmpty()) {
                    frontierBuckets.remove(pk)
                    continue
                }

                // Process a batch inside the correct region dispatcher
                val disp = Atom.instance.regionDispatcher(world, cx, cz)
                val keepBucket = withContext(disp) {
                    rememberEpoch(cx, cz)

                    // Two queues for this chunk:
                    // - bucket: validated positions ready to expand (already visited)
                    // - pending: positions reserved but not yet validated (from other chunks)
                    // We will store the pending queue in the same map with a suffix key.
                    val pendingKey = (pk xor 0xFFFF_FFFFL) // any stable derivation; different from pk
                    val pending = frontierBuckets.computeIfAbsent(pendingKey) { ArrayDeque() }

                    var processed = 0
                    // First, validate pending positions that belong to this chunk
                    while (pending.isNotEmpty() && processed < nodesPerChunkHop) {
                        val p = pending.removeFirst()
                        val px = p.x(); val py = p.y(); val pz = p.z()

                        // Validate within this chunk context
                        val nonSolid = !world.getBlockAt(px, py, pz).type.isSolid
                        val key = tryPack(px, py, pz)
                        if (key == null) {
                            // Out of range; drop and release reservation
                            ScanReservations.releaseCells(listOf(LocationUtil.pack(px, py, pz)), scanId)
                        } else if (!nonSolid) {
                            ScanReservations.releaseCells(listOf(key), scanId)
                        } else if (!scannedPositions.contains(key)) {
                            scannedPositions.add(key)
                            if (volume > maxVolume) return@withContext false
                            bucket.add(Vector3i(px, py, pz))
                        }
                        processed++
                    }

                    // Then, expand validated positions in this chunk
                    while (bucket.isNotEmpty() && processed < nodesPerChunkHop) {
                        if (volume > maxVolume) return@withContext false

                        stepsSinceEpochCheck++
                        if (stepsSinceEpochCheck >= epochCheckInterval) {
                            stepsSinceEpochCheck = 0
                            if (!epochsStable()) return@withContext false
                        }

                        val p = bucket.removeFirst()
                        val px = p.x(); val py = p.y(); val pz = p.z()

                        for (n in neighbors) {
                            val nx = px + n.x()
                            val ny = py + n.y()
                            val nz = pz + n.z()
                            if (!inRangeY(ny)) continue

                            val key = tryPack(nx, ny, nz) ?: continue
                            if (scannedPositions.contains(key)) continue

                            // Reserve neighbor before any world access
                            if (!ScanReservations.tryReserve(key, scanId)) return@withContext false

                            val nChunkX = nx shr 4
                            val nChunkZ = nz shr 4

                            if (nChunkX == cx && nChunkZ == cz) {
                                // Validate immediately in current chunk
                                val nonSolid = !world.getBlockAt(nx, ny, nz).type.isSolid
                                if (!nonSolid) {
                                    ScanReservations.releaseCells(listOf(key), scanId)
                                    continue
                                }
                                scannedPositions.add(key)
                                if (volume > maxVolume) return@withContext false
                                bucket.add(Vector3i(nx, ny, nz))
                            } else {
                                // Defer validation to the target chunk's pass
                                rememberEpoch(nChunkX, nChunkZ)
                                val tgtKey = bucketKey(nChunkX, nChunkZ)
                                val tgtPendingKey = (tgtKey xor 0xFFFF_FFFFL)
                                val tgtPending = frontierBuckets.computeIfAbsent(tgtPendingKey) { ArrayDeque() }
                                tgtPending.add(Vector3i(nx, ny, nz))
                            }
                        }
                        processed++
                    }

                    // Clean empty pending bucket
                    if (pending.isEmpty()) frontierBuckets.remove(pendingKey)

                    true
                }

                progressed = true
                if (!keepBucket) return@coroutineScope false

                if (bucket.isEmpty()) {
                    frontierBuckets.remove(pk)
                }
            }

            if (!progressed) {
                // No progress? Defensive break to avoid infinite loop
                break
            }
        }

        // Final epoch validation
        return@coroutineScope epochsStable()
    }

    override fun toRoom(): Room? {
        if (!hasScanned) return null
        if (volume == 0 || volume > maxVolume) return null

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        for (cell in scannedPositions) {
            val (x, y, z) = LocationUtil.unpack(cell)
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        return Room(
            id = UUID.randomUUID(),
            world = world,
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ,
            blocks = scannedPositions.toSet()
        )
    }
}