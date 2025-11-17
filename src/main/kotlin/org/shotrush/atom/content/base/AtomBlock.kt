package org.shotrush.atom.content.base

import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import java.util.UUID

abstract class AtomBlock(
    block: CustomBlock
) : AbstractBlockBehavior(block), EntityBlockBehavior {
    abstract override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T>


    abstract override fun createBlockEntity(pos: BlockPos, state: ImmutableBlockState): BlockEntity

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