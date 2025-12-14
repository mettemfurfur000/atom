package org.shotrush.atom.content.workstation.craftingbasket

import net.kyori.adventure.text.Component
import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.entity.Player
import org.bukkit.inventory.MenuType
import org.shotrush.atom.Atom
import org.shotrush.atom.blocks.AtomBlock
import org.shotrush.atom.content.workstation.Workstations

class CraftingBasketBlockBehavior(
    block: CustomBlock,
) : AtomBlock<CraftingBasketBlockEntity>(
    block,
    Workstations.CRAFTING_BASKET_ENTITY_TYPE,
    ::CraftingBasketBlockEntity
) {

    companion object {
        object Factory : BlockBehaviorFactory {
            override fun create(
                block: CustomBlock,
                arguments: Map<String?, Any?>,
            ): BlockBehavior = CraftingBasketBlockBehavior(block)
        }
    }

    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        MenuType.CRAFTING.create(player, Component.text("Crafting Basket")).open()
        return InteractionResult.SUCCESS
    }
}


class CraftingBasketBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : BlockEntity(Workstations.CRAFTING_BASKET_ENTITY_TYPE, pos, blockState) {

    init {
        Atom.instance.logger.info("CraftingBasketBlockEntity initialized at $pos")
    }

    override fun loadCustomData(tag: CompoundTag) {
        super.loadCustomData(tag)
    }

    override fun saveCustomData(tag: CompoundTag) {
        super.saveCustomData(tag)
    }
}
