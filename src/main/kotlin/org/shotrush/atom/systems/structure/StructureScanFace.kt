package org.shotrush.atom.systems.structure

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.minecraft.core.Direction
import org.bukkit.World
import org.joml.Vector3i
import org.shotrush.atom.Atom
import org.shotrush.atom.systems.room.WorldEpochs
import org.shotrush.atom.util.LocationUtil
import java.util.*
import kotlin.collections.ArrayDeque

/**
 * Scans for structures made of predefined material blocks.
 * Treats non-matching blocks as boundaries.
 * Records material type for each block and controller block positions.
 */
class StructureScanFace(
    private val world: World,
    private val startPosition: Vector3i,
    private var definition: StructureDefinition,
    private val maxVolume: Int = 128,
    private val epochCheckInterval: Int = 64,
    private val nodesPerChunkHop: Int = 64,
) : StructureScan {
    private val scanId = UUID.randomUUID()
    private var hasScanned = false

    // map of packed position -> material name
    val scannedBlocks = mutableMapOf<Long, String>()

    // controller blocks found during scan
    val controllerBlocks = mutableSetOf<Long>()
    val volume: Int get() = scannedBlocks.size

    private fun inRangeY(y: Int): Boolean =
        y in world.minHeight..<world.maxHeight

    private fun tryPack(x: Int, y: Int, z: Int): Long? =
        try {
            LocationUtil.pack(x, y, z)
        } catch (_: IllegalArgumentException) {
            null
        }

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

    private data class ChunkQueues(
        val active: ArrayDeque<Vector3i> = ArrayDeque(),
        val pending: ArrayDeque<Vector3i> = ArrayDeque()
    )

    override suspend fun scan(): Boolean {
        val ok = try {
            Atom.instance.logger.info { "DEBUH: running batched" }
            runBatchedScan()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        hasScanned = true
        return ok
    }

    private suspend fun runBatchedScan(): Boolean = coroutineScope {
        val sx = startPosition.x()
        val sy = startPosition.y()
        val sz = startPosition.z()
        if (!inRangeY(sy)) {
            Atom.instance.logger.info { "DEBUH: not in Y range $sx $sy $sz" }
            return@coroutineScope false
        }

        // Start block must be part of the structure
        if (!definition.isPartOfStructure(world, sx, sy, sz)) {
            Atom.instance.logger.info { "DEBUH: not a part of the structure $sx $sy $sz" }
            return@coroutineScope false
        }

        val startKey = tryPack(sx, sy, sz) ?: return@coroutineScope false
        recordBlock(sx, sy, sz, startKey)

        val startChunkX = sx shr 4
        val startChunkZ = sz shr 4
        rememberEpoch(startChunkX, startChunkZ)

        val frontier = HashMap<Long, ChunkQueues>()
        fun queues(cx: Int, cz: Int) =
            frontier.computeIfAbsent(packChunk(cx, cz)) { ChunkQueues() }

        queues(startChunkX, startChunkZ).active.add(Vector3i(sx, sy, sz))

        var stepsSinceEpochCheck = 0

        Atom.instance.logger.info { "DEBUH: running loopy loops" }

        while (frontier.isNotEmpty()) {
            val keys = frontier.keys.toList()

            for (pk in keys) {
                val cx = (pk shr 32).toInt()
                val cz = (pk and 0xFFFF_FFFFL).toInt()
                val q = frontier[pk] ?: continue

                val keepBucket = withContext(Atom.instance.regionDispatcher(world, cx, cz)) {
                    rememberEpoch(cx, cz)

                    var processed = 0
                    val budget = nodesPerChunkHop.coerceAtLeast(1)
                    val drainBudget = (budget / 2).coerceAtLeast(1)
                    val expandBudget = budget - drainBudget

                    // Drain pending into active, bounded
                    var drained = 0
                    while (q.pending.isNotEmpty() && drained < drainBudget && processed < budget) {
                        val p = q.pending.removeFirst()
                        val px = p.x();
                        val py = p.y();
                        val pz = p.z()
                        if (!inRangeY(py)) {
                            drained++; processed++
                            continue
                        }

                        val key = tryPack(px, py, pz)
                        if (key == null) {
                            drained++; processed++
                            continue
                        }

                        if (!definition.isPartOfStructure(world, px, py, pz)) {
                            Atom.instance.logger.info { "DEBUH: iter: not a structure $px $py $pz" }
                            drained++; processed++
                            continue
                        }

                        if (!scannedBlocks.containsKey(key)) {
                            recordBlock(px, py, pz, key)
                            if (volume > maxVolume) {
                                Atom.instance.logger.info { "DEBUH: too big, bigger than $maxVolume" }
                                return@withContext false
                            }
                            q.active.add(Vector3i(px, py, pz))
                        }
                        drained++; processed++
                    }

                    // Expand within this chunk, bounded
                    var expanded = 0
                    while (q.active.isNotEmpty()
                        && expanded < expandBudget
                        && processed < budget
                    ) {
                        if (volume > maxVolume) {
                            Atom.instance.logger.info { "DEBUH: too big, bigger than $maxVolume" }
                            return@withContext false
                        }

                        stepsSinceEpochCheck++
                        if (stepsSinceEpochCheck >= epochCheckInterval) {
                            stepsSinceEpochCheck = 0
                            if (!epochsStable()) return@withContext false
                        }

                        val p = q.active.removeFirst()
                        val px = p.x();
                        val py = p.y();
                        val pz = p.z()

                        if (!definition.isPartOfStructure(world, px, py, pz)) {
                            expanded++; processed++
                            continue
                        }

                        for (dir in Direction.entries) {
                            val nx = px + dir.stepX
                            val ny = py + dir.stepY
                            val nz = pz + dir.stepZ
                            if (!inRangeY(ny)) continue

                            val key = tryPack(nx, ny, nz) ?: continue
                            if (scannedBlocks.containsKey(key)) continue

                            // Neighbor must be part of the structure
                            if (!definition.isPartOfStructure(world, nx, ny, nz)) {
                                continue
                            }

                            val nChunkX = nx shr 4
                            val nChunkZ = nz shr 4

                            // Accept immediately
                            recordBlock(nx, ny, nz, key)
                            if (volume > maxVolume) {
                                Atom.instance.logger.info { "DEBUH: too big, bigger than $maxVolume" }
                                return@withContext false
                            }

                            if (nChunkX == cx && nChunkZ == cz) {
                                q.active.add(Vector3i(nx, ny, nz))
                            } else {
                                rememberEpoch(nChunkX, nChunkZ)
                                val tgt = queues(nChunkX, nChunkZ)
                                tgt.active.add(Vector3i(nx, ny, nz))
                            }
                        }
                        expanded++; processed++
                    }

                    true
                }

                if (!keepBucket) return@coroutineScope false

                val q2 = frontier[pk]
                if (q2 != null && q2.active.isEmpty() && q2.pending.isEmpty()) {
                    frontier.remove(pk)
                }
            }
        }

        return@coroutineScope epochsStable()
    }

    private fun recordBlock(x: Int, y: Int, z: Int, packed: Long) {
        val block = world.getBlockAt(x, y, z)
        val blockKey = block.type.key.toString()
        scannedBlocks[packed] = blockKey

        Atom.instance.logger.info { "DEBUH: iter: added $x $y $z $blockKey" }

        // Check if this is a controller block
        if (definition.isControllerBlock(world, x, y, z)) {
            controllerBlocks.add(packed)
            Atom.instance.logger.info { "DEBUH: iter: as a controller" }
        }
    }

    override fun toStructure(): Structure? {
        if (!hasScanned) return null
        if (volume == 0 || volume > maxVolume) return null

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        for (cell in scannedBlocks.keys) {
            val (x, y, z) = LocationUtil.unpack(cell)
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        return Structure(
            id = UUID.randomUUID(),
            world = world,
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ,
            blocks = scannedBlocks.toMap(),
            controllers = controllerBlocks.toSet(),
            defName = definition.name
        )
    }
}
