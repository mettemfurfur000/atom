package org.shotrush.atom.content.carcass

import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import org.bukkit.entity.Player
import org.shotrush.atom.content.base.AtomBlock

class CarcassBlockBehavior(block: CustomBlock) : AtomBlock<CarcassBlockEntity>(
    block,
    CarcassBlock.CARCASS_DEF.type,
    ::CarcassBlockEntity,
    CarcassBlockEntity::tick
) {
    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = CarcassBlockBehavior(block)
    }

    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val pos = context.clickedPos
        val blockEntity = context.level.storageWorld().getBlockEntityAtIfLoaded(pos)

        if (blockEntity !is CarcassBlockEntity) return InteractionResult.PASS

        if (blockEntity.decomposed) {
            return InteractionResult.PASS
        }

        if (!blockEntity.opened) {
            val cfg = blockEntity.getConfig()
            if (cfg != null && cfg.requiresButchering) {
                val heldItem = player.inventory.itemInMainHand
                val hasKnife = ToolRequirement.KNIFE.isSatisfiedBy(heldItem)
                
                if (!hasKnife) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text("You need a knife to open this carcass", net.kyori.adventure.text.format.NamedTextColor.RED))
                    return InteractionResult.PASS
                }
            }
            
            blockEntity.markOpened()
        }
        
        CarcassGui.open(player, blockEntity)
        return InteractionResult.SUCCESS
    }
}
