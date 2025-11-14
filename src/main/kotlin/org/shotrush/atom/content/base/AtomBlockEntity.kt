package org.shotrush.atom.content.base

import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.ChunkPos
import org.bukkit.Location
import org.bukkit.World

abstract class AtomBlockEntity(
    type: BlockEntityType<BlockEntity>,
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : BlockEntity(type, pos, blockState) {
    val location: Location
        get() = Location(
            world.world.platformWorld() as World,
            pos.x().toDouble(),
            pos.y().toDouble(),
            pos.z().toDouble()
        )

    fun getChunk() = world?.getChunkAtIfLoaded(ChunkPos(pos))

    fun markDirty() {
        getChunk()?.setDirty(true)
        updateRender()
    }

    fun updateRender() {
        val render = blockEntityRenderer ?: return
        val chunk = getChunk() ?: return
        val tracking = chunk.trackedBy
        tracking.forEach(render::update)
    }
}