package org.shotrush.atom.content.workstation.clay_cauldron

import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.blocks.AtomBlock
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.item.Molds


class ClayCauldronBlockBehavior(block: CustomBlock) : AtomBlock<ClayCauldronBlockEntity>(
    block,
    Workstations.CLAY_CAULDRON.type,
    ::ClayCauldronBlockEntity,
    ClayCauldronBlockEntity::tick
) {
    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = ClayCauldronBlockBehavior(block)
    }

    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val item = context.item.item
        if (item !is ItemStack) return InteractionResult.PASS
        val pos = context.clickedPos

        val blockEntity = context.level.storageWorld().getBlockEntityAtIfLoaded(pos)

        if (blockEntity !is ClayCauldronBlockEntity) return InteractionResult.PASS

        if (blockEntity.canStoreItem(item)) {
            val amountToTake = blockEntity.amountToStore(item)
            if (amountToTake == 0) return InteractionResult.PASS
            val clone = item.clone().apply { amount = amountToTake }
            blockEntity.storeItem(clone)
            item.amount -= amountToTake
            return InteractionResult.SUCCESS
        } else if (Molds.isEmptyMold(item)) {
            val type = Molds.getMoldType(item)
            val shape = Molds.getMoldShape(item)
            return blockEntity.fillMold(player, item, type, shape)
        }

        return InteractionResult.PASS
    }
}