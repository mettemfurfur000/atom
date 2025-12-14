package org.shotrush.atom.content.workstation.knapping

import com.github.shynixn.mccoroutine.folia.launch
import dev.triumphteam.gui.GuiView
import dev.triumphteam.gui.paper.builder.item.ItemBuilder
import dev.triumphteam.gui.paper.kotlin.builder.buildGui
import dev.triumphteam.gui.paper.kotlin.builder.chestContainer
import dev.triumphteam.nova.getValue
import dev.triumphteam.nova.mutableListStateOf
import dev.triumphteam.nova.mutableStateOf
import dev.triumphteam.nova.setValue
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine
import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.ItemBuildContext
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.plugin.context.ContextHolder
import net.momirealms.craftengine.core.plugin.gui.GuiParameters
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.blocks.AtomBlock
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.format
import org.shotrush.atom.getNamespacedKey
import org.shotrush.atom.isCustomItem
import org.shotrush.atom.item.Items
import org.shotrush.atom.item.MoldShape
import kotlin.time.Duration.Companion.seconds

private const val N = 5

private fun idxColMajor(r: Int, c: Int): Int = r + c * N

class KnappingBlockBehavior(block: CustomBlock) : AtomBlock<KnappingBlockBehavior.KnappingBlockEntity>(
    block,
    Workstations.KNAPPING_STATION_ENTITY_TYPE,
    ::KnappingBlockEntity
) {
    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = KnappingBlockBehavior(block)
    }
    // ---------- Utilities for recipe rendering ----------

    private data class RecipeEntry(
        val id: String,
        val patterns: List<Pattern>,
        val resultShape: MoldShape,
    )

    private fun buildRecipeEntries(material: KnappingMaterial): List<RecipeEntry> {
        return KnappingRecipes.allRecipes.mapNotNull { kr ->
            if (kr.patterns.isEmpty() || material !in kr.allowed) null
            else RecipeEntry(id = kr.id, patterns = kr.patterns, resultShape = kr.result)
        }.sortedBy { it.id }
    }

    // Top-left aligned pattern into 5x5 boolean mask
    private fun patternToGrid5x5(p: Pattern): List<Boolean> {
        val grid = MutableList(N * N) { false }
        for (r in 0 until p.height) {
            val row = p[r]
            for (c in 0 until p.width) {
                val filled = row[c] == '#'
                grid[idxColMajor(r, c)] = filled
            }
        }
        return grid
    }

    // Centered pattern into 5x5 boolean mask (nicer for display)
    private fun patternToCenteredGrid5x5(p: Pattern): List<Boolean> {
        val grid = MutableList(N * N) { false }
        val offsetR = (N - p.height) / 2
        val offsetC = (N - p.width) / 2
        for (r in 0 until p.height) {
            val row = p[r]
            for (c in 0 until p.width) {
                val filled = row[c] == '#'
                grid[idxColMajor(offsetR + r, offsetC + c)] = filled
            }
        }
        return grid
    }

    // ---------- Main Knapping UI (craftable) ----------

    fun openUI(
        ui: KnappingMaterial,
        player: Player,
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
                MiniMessage.miniMessage()
                    .deserialize("<shift:-8><white><image:atom:ui_knapping_table><shift:-170><dark_gray>Knapping Station")
            )
            component {
                remember(clicksState)
                render { container ->
                    for (r in 1..5) {
                        for (c in 2..6) {
                            val slot = (r - 1) + (c - 2) * 5
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
                    val result = KnappingRecipes.getResult(clicks, ui)
                    if (result != null) {
                        val stack = ui.buildFinalItem(result)
                        container[3, 8] = ItemBuilder.from(stack)
                            .asGuiItem { player0, ctx ->
                                player0.inventory.addItem(stack)
                                clicks.fill(false)
                                ctx.guiView.close()
                                onCraftComplete?.invoke()
                            }
                    }
                }
            }

            // Recipe Book button (wired)
            component {
                render { container ->
                    container[4, 8] = ItemBuilder.from(Material.BOOK)
                        .name(format("<green>Knapping Table Recipes</green>"))
                        .lore(
                            format("<gray>Click to view the knapping recipes.</gray>")
                        )
                        .asGuiItem { _, ctx ->
                            openRecipeBook(
                                ui = ui,
                                player = player,
                                originalUi = ctx.guiView,
                                invert = invert
                            )
                        }
                }
            }
        }

        gui.open(player)
    }

    // ---------- Recipe Book UI (read-only, paginated) ----------

    fun openRecipeBook(
        ui: KnappingMaterial,
        player: Player,
        originalUi: GuiView,
        invert: Boolean,
    ) {
        openRecipeBookWithInitialPage(
            ui = ui,
            player = player,
            originalUi = originalUi,
            invert = invert,
            initialPage = 0
        )
    }

    fun openRecipeBookWithInitialPage(
        ui: KnappingMaterial,
        player: Player,
        originalUi: GuiView,
        invert: Boolean,
        initialPage: Int,
    ) {
        val entries = buildRecipeEntries(ui)
        if (entries.isEmpty()) {
            player.sendMessage(Component.text("No knapping recipes registered."))
            originalUi.open()
            return
        }

        val itemA = ui.getItem(invert)
        val itemB = ui.getItem(!invert)

        val gui = buildGui {
            spamPreventionDuration = 0.seconds
            containerType = chestContainer { rows = 5 }
            title(
                MiniMessage.miniMessage()
                    .deserialize("<shift:-8><white><image:atom:ui_knapping_table><shift:-170><dark_gray>Knapping Recipes")
            )

            val pageState = mutableStateOf(initialPage.coerceIn(0, entries.lastIndex))
            var page by pageState
            val patternIndexState = mutableStateOf(0)
            var patternIndex by patternIndexState

            val job = Atom.instance.launch {
                while (true) {
                    delay(3.seconds)
                    val pats = entries[page].patterns
                    if (pats.size <= 1) continue // no need to cycle
                    val next = (patternIndex + 1) % pats.size
                    patternIndex = next
                }
            }
            onClose {
                job.cancel()
            }

            component {
                remember(pageState)
                remember(patternIndexState)
                render { container ->
                    val entry = entries[page]

                    // Render 5x5 area from centered mask
                    val mask = patternToCenteredGrid5x5(entry.patterns[patternIndex])
                    for (r in 1..5) {
                        for (c in 2..6) {
                            val slot = (r - 1) + (c - 2) * 5
                            val filled = mask.getOrNull(slot) ?: false
                            val material = if (!filled) itemA else itemB
                            container[r, c] = ItemBuilder.from(material)
                                .name(Component.text(" "))
                                .asGuiItem() // read-only
                        }
                    }

                    val stack = ui.buildFinalItem(entry.resultShape)
                    container[3, 8] = ItemBuilder.from(stack).asGuiItem()

                    val hasPrev = page > 0
                    val hasNext = page < entries.lastIndex

                    val displayPage = (page + 1).toString()
                    val displayMax = (entries.lastIndex + 1).toString()

                    val prevItem = if (hasPrev) Items.UI.ARROW_LEFT_AVAILABLE else Items.UI.ARROW_LEFT_BLOCKED

                    container[4, 7] = ItemBuilder.from(
                        CraftEngineItems.byId(prevItem)!!.buildItemStack(
                            ItemBuildContext.of(
                                BukkitCraftEngine.instance().adapt(player), ContextHolder.builder().withParameter(
                                    GuiParameters.CURRENT_PAGE, displayPage
                                ).withParameter(GuiParameters.MAX_PAGE, displayMax)
                            )
                        )
                    ).asGuiItem { _, _ ->
                        if (hasPrev) {
                            patternIndex = 0
                            page -= 1;
                        }
                    }

                    container[4, 8] = ItemBuilder.from(CraftEngineItems.byId(Items.UI.ARROW_BACK)!!.buildItemStack())
                        .name(format("<!i><#DAA520>Back"))
                        .lore(format("<!i><gray>Return to knapping table.</gray>"))
                        .asGuiItem { _, ctx ->
                            originalUi.open()
                        }

                    val item = if (hasNext) Items.UI.ARROW_RIGHT_AVAILABLE else Items.UI.ARROW_RIGHT_BLOCKED

                    container[4, 9] = ItemBuilder.from(
                        CraftEngineItems.byId(item)!!.buildItemStack(
                            ItemBuildContext.of(
                                BukkitCraftEngine.instance().adapt(player), ContextHolder.builder().withParameter(
                                    GuiParameters.CURRENT_PAGE, displayPage
                                ).withParameter(GuiParameters.MAX_PAGE, displayMax)
                            )
                        )
                    )
                        .asGuiItem { _, _ ->
                            if (hasNext) {
                                patternIndex = 0
                                page += 1;
                            }
                        }
                }
            }
        }

        gui.open(player)
    }

    // Jump to a specific recipe id (optional helper)
    fun openRecipeAt(
        ui: KnappingMaterial,
        player: Player,
        originalUi: GuiView,
        invert: Boolean,
        recipeId: String,
    ) {
        val entries = buildRecipeEntries(ui)
        val index = entries.indexOfFirst { it.id == recipeId }.let { if (it == -1) 0 else it }
        openRecipeBookWithInitialPage(ui, player, originalUi, invert, index)
    }

    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as Player? ?: return InteractionResult.PASS
        val item = context.item.item
        if (item !is ItemStack) return InteractionResult.PASS
        val key = item.getNamespacedKey()

        val consumeItem = {
            if (player.gameMode != GameMode.CREATIVE) {
                if (player.inventory.itemInMainHand.isSimilar(item)) {
                    player.inventory.itemInMainHand.subtract()
                } else if (player.inventory.itemInOffHand.isSimilar(item)) {
                    player.inventory.itemInOffHand.subtract()
                }
            }
        }

        if (!item.isCustomItem()) {
            if (item.type == Material.CLAY_BALL) {
                openUI(KnappingMaterial.Clay, player, onCraftComplete = consumeItem)
                return InteractionResult.SUCCESS
            } else if (item.type == Material.HONEYCOMB) {
                openUI(KnappingMaterial.Wax, player, onCraftComplete = consumeItem)
                return InteractionResult.SUCCESS
            }
        } else {
            if (key == "atom:pebble") {
                openUI(
                    KnappingMaterial.Stone,
                    player,
                    invert = true,
                    onCraftComplete = consumeItem
                )
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

}

