package org.shotrush.atom.content.workstation.leatherbed

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
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.matches

class LeatherBedBlockBehavior(block: CustomBlock) : AtomBlock<LeatherBedBlockEntity>(
    block,
    Workstations.LEATHER_BED.type,
    ::LeatherBedBlockEntity,
    LeatherBedBlockEntity::tick
) {

    companion object {
        fun isScrapingTool(item: ItemStack): Boolean {
            return item.matches("atom:sharpened_rock") || item.matches("atom:knife")
        }
    }

    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = LeatherBedBlockBehavior(block)
    }

    fun getFullMessage(): String = "<red>Leather bed is full!</red>"

    fun getEmptyMessage(): String = "<red>Place leather first!</red>"


    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val item = context.item.item as? ItemStack ?: return InteractionResult.PASS
        val pos = context.clickedPos

        val blockEntity = context.level.storageWorld().getBlockEntityAtIfLoaded(pos)

        if (blockEntity !is LeatherBedBlockEntity) return InteractionResult.PASS

        if (isScrapingTool(item)) {
            if (!blockEntity.hasItem()) {
                ActionBarManager.send(player, getEmptyMessage())
                return InteractionResult.SUCCESS
            } else {
                blockEntity.startScraping(player, item)
            }
            return InteractionResult.SUCCESS
        } else {
            return if (player.isSneaking) {
                blockEntity.tryEmptyItems(player, item)
            } else {
                blockEntity.tryPlaceItem(player, item)
            }
        }
    }
}