@file:Suppress("UnstableApiUsage")

package org.shotrush.atom.blocks

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.CEWorld
import org.bukkit.entity.Player
import org.shotrush.atom.Atom
import org.shotrush.atom.toBukkitLocation

abstract class AtomBlock<BE : BlockEntity>(
    block: CustomBlock,
    val type: BlockEntityType<BlockEntity>,
    val factory: (pos: BlockPos, state: ImmutableBlockState) -> BE,
    val ticker: (suspend (BE) -> Unit)? = null,
) : AbstractBlockBehavior(block), EntityBlockBehavior {
    final override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T> =
        EntityBlockBehavior.blockEntityTypeHelper(type)

    final override fun createBlockEntity(pos: BlockPos, state: ImmutableBlockState): BlockEntity =
        factory.invoke(pos, state)

    final override fun <T : BlockEntity> createSyncBlockEntityTicker(
        level: CEWorld?,
        state: ImmutableBlockState?,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        if (ticker != null) {
            return BlockEntityTicker { world, pos, state, be ->
                Atom.instance.launch(Atom.instance.regionDispatcher(pos.toBukkitLocation(world))) {
                    ticker(be as BE)
                }
            }
        }
        return null
    }

    override fun useOnBlock(context: UseOnContext, state: ImmutableBlockState): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val sneaking = player.isSneaking

        return if (onInteract(player, sneaking)) {
            InteractionResult.SUCCESS
        } else {
            InteractionResult.PASS
        }
    }


    open fun onInteract(player: Player, sneaking: Boolean): Boolean = false
}