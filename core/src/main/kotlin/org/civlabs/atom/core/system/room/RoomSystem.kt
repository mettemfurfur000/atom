package org.civlabs.atom.core.system.room

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.mainDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.civlabs.atom.core.CoreAtom
import org.civlabs.atom.core.listener.AtomListener
import org.civlabs.atom.core.listener.eventDef
import org.joml.Vector3i

object RoomSystem : AtomListener {
    override val eventDefs = mapOf(
        eventDef<ChunkLoadEvent> {
            CoreAtom.instance.regionDispatcher(it.world, it.chunk.x, it.chunk.z)
        },
        eventDef<ChunkUnloadEvent> {
            CoreAtom.instance.regionDispatcher(it.world, it.chunk.x, it.chunk.z)
        },
        eventDef<BlockBreakEvent> {
            CoreAtom.instance.regionDispatcher(it.block.location)
        },
        eventDef<BlockPlaceEvent> {
            CoreAtom.instance.regionDispatcher(it.block.location)
        },
        eventDef<WorldSaveEvent> {
            CoreAtom.instance.mainDispatcher
        }
    )

    fun onModifiedChunk(world: World, chunkX: Int, chunkZ: Int) {
        WorldEpochs.bump(world.uid, chunkX, chunkZ)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val w = event.block.world
        onModifiedChunk(w, event.block.chunk.x, event.block.chunk.z)

        CoreAtom.instance.launch(CoreAtom.instance.regionDispatcher(event.block.location)) {
            // Invalidate nearby rooms
            RoomRegistry.invalidateByBlock(w.uid, event.block.x, event.block.y, event.block.z)
            // Try scan from adjacent air cells (break leaves air)
            val seeds = arrayOf(
                Vector3i(event.block.x + 1, event.block.y, event.block.z),
                Vector3i(event.block.x - 1, event.block.y, event.block.z),
                Vector3i(event.block.x, event.block.y + 1, event.block.z),
                Vector3i(event.block.x, event.block.y - 1, event.block.z),
                Vector3i(event.block.x, event.block.y, event.block.z + 1),
                Vector3i(event.block.x, event.block.y, event.block.z - 1),
            )
            for (s in seeds) {
                RoomScanner.scanAt(w, s)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun onBlockPlace(event: BlockPlaceEvent) {
        val w = event.block.world
        onModifiedChunk(w, event.block.chunk.x, event.block.chunk.z)

        // Invalidate nearby rooms
        RoomRegistry.invalidateByBlock(w.uid, event.block.x, event.block.y, event.block.z)
        // Try scan from adjacent air cells (room may have just sealed)
        val b = event.block
        val seeds = arrayOf(
            Vector3i(b.x + 1, b.y, b.z),
            Vector3i(b.x - 1, b.y, b.z),
            Vector3i(b.x, b.y + 1, b.z),
            Vector3i(b.x, b.y - 1, b.z),
            Vector3i(b.x, b.y, b.z + 1),
            Vector3i(b.x, b.y, b.z - 1),
        )
        for (s in seeds) {
            RoomScanner.scanAt(w, s)
        }
    }

    @EventHandler
    fun onWorldSave(event: WorldSaveEvent) {
        RoomRegistry.saveAllToDisk()
    }
}