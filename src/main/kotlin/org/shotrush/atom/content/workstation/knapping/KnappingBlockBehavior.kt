package org.shotrush.atom.content.workstation.knapping

import dev.triumphteam.gui.paper.builder.item.ItemBuilder
import dev.triumphteam.gui.paper.kotlin.builder.buildGui
import dev.triumphteam.gui.paper.kotlin.builder.chestContainer
import dev.triumphteam.nova.getValue
import dev.triumphteam.nova.mutableListStateOf
import dev.triumphteam.nova.setValue
import net.kyori.adventure.text.Component
import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.Material
import org.bukkit.entity.Player
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.item.Items

class KnappingBlockBehavior(block: CustomBlock) : AbstractBlockBehavior(block), EntityBlockBehavior {
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

    override fun useWithoutItem(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        println("useWithoutItem")

        val default = ((1..25).map { false })

        val gui = buildGui {
            val clicksState = mutableListStateOf(*default.toTypedArray())
            val clicks by clicksState
            containerType = chestContainer { rows = 5 }
            title(Component.text("Knapping Station"))
            component {
                remember(clicksState)
                render { container ->
                    for (r in 1..5) {
                        for (c in 2..6) {
                            val slot = (r - 1) + (c - 2) * 5
                            val clicked = clicks.getOrNull(slot) ?: false
                            val material = if (!clicked) Items.UI_Molding.buildItemStack() else Items.UI_MoldingPressed.buildItemStack()
                            container[r, c] = ItemBuilder.from(material)
                                .name(Component.text("Click to mold"))
                                .asGuiItem { _, _ ->
                                    clicks[slot] = !clicked
                                }
                        }
                    }
                }
            }

            component {
                remember(clicksState)
                render { container ->
                    val result = KnappingRecipes.getResult(clicks)
                    if(result != null) {
                        container[3, 8] = ItemBuilder.from(result)
                            .asGuiItem { player, ctx ->
                                player.inventory.addItem(result)
                                clicks.fill(false)
                                ctx.guiView.close()
                            }
                    }
                }
            }
        }

        if (context.player != null) {
            gui.open(context.player!!.platformPlayer()!! as Player)
        }

        return InteractionResult.SUCCESS
    }
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