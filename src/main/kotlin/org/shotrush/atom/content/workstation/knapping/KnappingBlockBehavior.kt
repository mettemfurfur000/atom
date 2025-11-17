package org.shotrush.atom.content.workstation.knapping

import dev.triumphteam.gui.paper.builder.item.ItemBuilder
import dev.triumphteam.gui.paper.kotlin.builder.buildGui
import dev.triumphteam.gui.paper.kotlin.builder.chestContainer
import dev.triumphteam.nova.getValue
import dev.triumphteam.nova.mutableListStateOf
import dev.triumphteam.nova.setValue
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
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
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.getNamespacedKey
import org.shotrush.atom.isCustomItem
import org.shotrush.atom.item.Items
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

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

    fun openUI(
        ui: KnappingUIItem,
        player: Player,
        transformer: (shape: MoldShape) -> ItemStack,
        invert: Boolean = false,
        onCraftComplete: (() -> Unit)? = null,
    ) {
        val default = ((1..25).map { invert })
        val itemA = ui.getItem(invert)
        val itemB = ui.getItem(!invert)

        val gui = buildGui {
            val clicksState = mutableListStateOf(*default.toTypedArray())
            val clicks by clicksState
            containerType = chestContainer { rows = 5 }
            title(
                MiniMessage.miniMessage().deserialize("<shift:-8><white><image:atom:ui_knapping_table><shift:-170><dark_gray>Knapping Station")
            )
            component {
                remember(clicksState)
                render { container ->
                    for (r in 1..5) {
                        for (c in 1..5) {
                            val slot = (r - 1) + (c - 1) * 5
                            val clicked = clicks.getOrNull(slot) ?: false
                            val material = if (!clicked) itemA else itemB
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
                    if (result != null) {
                        val stack = transformer(result)
                        container[3, 8] = ItemBuilder.from(stack)
                            .asGuiItem { player, ctx ->
                                player.inventory.addItem(stack)
                                clicks.fill(false)
                                ctx.guiView.close()
                                onCraftComplete?.invoke()
                            }
                    }
                }
            }
        }

        gui.open(player)
    }


    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as Player? ?: return InteractionResult.PASS
        val item = context.item.item
        if (item !is ItemStack) return InteractionResult.PASS
        val key = item.getNamespacedKey()

        if (!item.isCustomItem()) {
            if (item.type == Material.CLAY_BALL) {
                openUI(KnappingUIItem.Clay, player, { shape ->
                    // Check if this shape produces a vanilla item directly
                    if (shape.isVanillaItem()) {
                        ItemStack(shape.vanillaItem!!)
                    } else {
                        Molds.getMold(shape, MoldType.Clay).buildItemStack()
                    }
                }) {
                    if (player.gameMode != GameMode.CREATIVE)
                        context.item.count(context.item.count() - 1)
                }
                return InteractionResult.SUCCESS
            } else if (item.type == Material.HONEYCOMB) {
                openUI(KnappingUIItem.Wax, player, { shape ->
                    if (shape.isVanillaItem()) {
                        ItemStack(shape.vanillaItem!!)
                    } else {
                        Molds.getMold(shape, MoldType.Wax).buildItemStack()
                    }
                }) {
                    if (player.gameMode != GameMode.CREATIVE)
                        context.item.count(context.item.count() - 1)
                }
                return InteractionResult.SUCCESS
            }
        } else {
            if (key == "atom:pebble") {
                openUI(
                    KnappingUIItem.Stone,
                    player,
                    { shape ->
                        if (shape.isVanillaItem()) {
                            ItemStack(shape.vanillaItem!!)
                        } else {
                            Molds.getToolHead(shape, org.shotrush.atom.item.Material.Stone).buildItemStack()
                        }
                    }, true
                ) {
                    if (player.gameMode != GameMode.CREATIVE)
                        context.item.count(context.item.count() - 1)
                }
                return InteractionResult.SUCCESS
            }
        }
        return super.useOnBlock(context, state)
    }

    class KnappingBlockEntity(
        pos: BlockPos,
        blockState: ImmutableBlockState,
    ) : BlockEntity(Workstations.KNAPPING_STATION_ENTITY_TYPE, pos, blockState) {

        init {
            Atom.instance.logger.info("KnappingBlockEntity init at $pos")
        }

        override fun loadCustomData(tag: CompoundTag) {
            super.loadCustomData(tag)
        }

        override fun saveCustomData(tag: CompoundTag) {
            super.saveCustomData(tag)
        }
    }

    enum class KnappingUIItem(val getItem: (pressed: Boolean) -> ItemStack) {
        Clay({ if (it) Items.UI_MoldingClayPressed.buildItemStack() else Items.UI_MoldingClay.buildItemStack() }),
        Wax({ if (it) Items.UI_MoldingWaxPressed.buildItemStack() else Items.UI_MoldingWax.buildItemStack() }),
        Stone({ if (it) Items.UI_MoldingStonePressed.buildItemStack() else Items.UI_MoldingStone.buildItemStack() })
    }
}