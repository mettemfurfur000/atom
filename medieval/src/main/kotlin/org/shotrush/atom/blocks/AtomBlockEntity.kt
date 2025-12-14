package org.shotrush.atom.blocks

import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.ChunkPos
import org.bukkit.Location
import org.bukkit.World
import org.shotrush.atom.content.workstation.WorkstationDef

abstract class AtomBlockEntity(
    type: BlockEntityType<BlockEntity>,
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : BlockEntity(type, pos, blockState) {
    constructor(type: WorkstationDef, pos: BlockPos, blockState: ImmutableBlockState) : this(
        type.type,
        pos,
        blockState
    )

    val location: Location
        get() = Location(
            world.world.platformWorld() as World,
            pos.x().toDouble(),
            pos.y().toDouble(),
            pos.z().toDouble()
        )

    val bukkitWorld: World
        get() = world.world.platformWorld() as World

    fun getChunk() = world?.getChunkAtIfLoaded(ChunkPos(pos))

    fun markDirty(updateRenderer: Boolean = true) {
        getChunk()?.setDirty(true)
        if (updateRenderer)
            updateRender()
    }

    fun updateRender() {
        val render = blockEntityRenderer ?: return
        val chunk = getChunk() ?: return
        val tracking = chunk.trackedBy
        tracking.forEach(render::update)
    }
}