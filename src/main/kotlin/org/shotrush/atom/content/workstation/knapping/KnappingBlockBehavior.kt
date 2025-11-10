package org.shotrush.atom.content.workstation.knapping

import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.Workstations

class KnappingBlockBehavior(val block: CustomBlock) : BlockBehavior(), EntityBlockBehavior {
    override fun block(): CustomBlock = block

    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = KnappingBlockBehavior(block)
    }

    override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T> =
        EntityBlockBehavior.blockEntityTypeHelper(Workstations.KNAPPING_STATION_ENTITY_TYPE)

    override fun createBlockEntity(
        pos: BlockPos,
        state: ImmutableBlockState,
    ): BlockEntity = KnappingBlockEntity(pos, state)
}

class KnappingBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : BlockEntity(Workstations.KNAPPING_STATION_ENTITY_TYPE, pos, blockState) {

    init {
        Atom.instance?.logger?.info("KnappingBlockEntity init at $pos")
    }

    override fun loadCustomData(tag: CompoundTag) {
        super.loadCustomData(tag)
    }

    override fun saveCustomData(tag: CompoundTag) {
        super.saveCustomData(tag)
    }
}