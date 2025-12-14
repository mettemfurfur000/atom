package org.shotrush.atom.systems.room

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.mainDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.joml.Vector3i
import org.shotrush.atom.Atom

object RoomSystem : org.shotrush.atom.listener.AtomListener {
    override val eventDefs = mapOf(
        org.shotrush.atom.listener.eventDef<ChunkLoadEvent> {
            Atom.instance.regionDispatcher(it.world, it.chunk.x, it.chunk.z)
        },
        org.shotrush.atom.listener.eventDef<ChunkUnloadEvent> {
            Atom.instance.regionDispatcher(it.world, it.chunk.x, it.chunk.z)
        },
        org.shotrush.atom.listener.eventDef<BlockBreakEvent> {
            Atom.instance.regionDispatcher(it.block.location)
        },
        org.shotrush.atom.listener.eventDef<BlockPlaceEvent> {
            Atom.instance.regionDispatcher(it.block.location)
        },
        org.shotrush.atom.listener.eventDef<WorldSaveEvent> {
            Atom.instance.mainDispatcher
        }
    )

    fun onModifiedChunk(world: org.bukkit.World, chunkX: Int, chunkZ: Int) {
        WorldEpochs.bump(world.uid, chunkX, chunkZ)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val w = event.block.world
        onModifiedChunk(w, event.block.chunk.x, event.block.chunk.z)

        Atom.instance.launch(Atom.instance.regionDispatcher(event.block.location)) {
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