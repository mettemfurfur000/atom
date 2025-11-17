package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.getNamespacedKey
import org.shotrush.atom.getNamespacedPath
import org.shotrush.atom.item.Material
import org.shotrush.atom.item.ToolShape

object PlayerMiningListener : Listener {

    fun register(atom: Atom) {
        val eventDispatcher = mapOf(
            eventDef<BlockBreakEvent> { atom.regionDispatcher(it.block.location) },
            eventDef<PlayerInteractEvent> {
                if (it.clickedBlock != null) atom.regionDispatcher(it.clickedBlock!!.location)
                else atom.entityDispatcher(it.player)
            }
        )
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    // Single allowed shape and minimum tier per block
    // Expand as needed or load from config/datapack.
// Single allowed shape and minimum tier per block
    private val miningRequirements: Map<String, Pair<ToolShape, Material>> = buildMap {
        // Stone family (overworld)
        put("minecraft:stone", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:cobblestone", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:andesite", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:diorite", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:granite", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:tuff", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:calcite", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:dripstone_block", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:packed_mud", ToolShape.Shovel to Material.Stone)

        // Deepslate family
        put("minecraft:deepslate", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:cobbled_deepslate", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:polished_deepslate", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_bricks", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_tiles", ToolShape.Pickaxe to Material.Steel)

        // Ores (stone)
        put("minecraft:coal_ore", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:copper_ore", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:iron_ore", ToolShape.Pickaxe to Material.Copper)
        put("minecraft:gold_ore", ToolShape.Pickaxe to Material.Iron)
        put("minecraft:redstone_ore", ToolShape.Pickaxe to Material.Iron)
        put("minecraft:lapis_ore", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:diamond_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:emerald_ore", ToolShape.Pickaxe to Material.Steel)

        // Ores (deepslate variants)
        put("minecraft:deepslate_coal_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_copper_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_iron_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_gold_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_redstone_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_lapis_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_diamond_ore", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:deepslate_emerald_ore", ToolShape.Pickaxe to Material.Steel)

        // Raw ore blocks
        put("minecraft:raw_iron_block", ToolShape.Pickaxe to Material.Copper)
        put("minecraft:raw_copper_block", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:raw_gold_block", ToolShape.Pickaxe to Material.Iron)

        // Metal/valuable blocks
        put("minecraft:iron_block", ToolShape.Pickaxe to Material.Copper)
        put("minecraft:copper_block", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:gold_block", ToolShape.Pickaxe to Material.Iron)
        put("minecraft:lapis_block", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:diamond_block", ToolShape.Pickaxe to Material.Iron)
        put("minecraft:emerald_block", ToolShape.Pickaxe to Material.Iron)
        put("minecraft:redstone_block", ToolShape.Pickaxe to Material.Iron)
        put("minecraft:netherite_block", ToolShape.Pickaxe to Material.Steel)

        // Obsidian and similar
        put("minecraft:obsidian", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:crying_obsidian", ToolShape.Pickaxe to Material.Steel)
        put("minecraft:ancient_debris", ToolShape.Pickaxe to Material.Steel)

        // Overworld building stones/bricks
        put("minecraft:stone_bricks", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:mossy_stone_bricks", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:smooth_stone", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:cobblestone_wall", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:cobblestone_stairs", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:cobblestone_slab", ToolShape.Pickaxe to Material.Stone)

        // Terracotta and glazed terracotta
        put("minecraft:terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:white_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:orange_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:magenta_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:light_blue_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:yellow_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:lime_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:pink_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:gray_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:light_gray_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:cyan_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:purple_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:blue_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:brown_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:green_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:red_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:black_glazed_terracotta", ToolShape.Pickaxe to Material.Stone)

        // Bricks
        put("minecraft:bricks", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:nether_bricks", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:red_nether_bricks", ToolShape.Pickaxe to Material.Stone)

        // Misc hard blocks
        put("minecraft:quartz_block", ToolShape.Pickaxe to Material.Copper)
        put("minecraft:prismarine", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:packed_ice", ToolShape.Pickaxe to Material.Stone)
        put("minecraft:blue_ice", ToolShape.Pickaxe to Material.Stone)
    }

    private fun isRelevantBlock(blockKey: String): Boolean =
        miningRequirements.containsKey(blockKey)

    private fun requiredFor(blockKey: String): Pair<ToolShape, Material>? =
        miningRequirements[blockKey]

    // Compare tiers; ensure enum order reflects progression
    private fun isTierAtLeast(actual: Material, required: Material): Boolean =
        actual.ordinal >= required.ordinal

    // Tool parsing using "<tier>_<shape>" convention
    private fun parseTool(stack: ItemStack): Pair<Material, ToolShape>? {
        val path = stack.getNamespacedPath()
        val shape = ToolShape.entries.firstOrNull { path.endsWith("_${it.id}") } ?: return null
        val tier = Material.entries.firstOrNull { path.startsWith("${it.id}_") } ?: return null
        return tier to shape
    }

    // Optional early feedback on left-click
    @EventHandler
    suspend fun on(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK) return
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return
        val block = event.clickedBlock ?: return

        val blockKey = block.getNamespacedKey()
        if (!isRelevantBlock(blockKey)) return

        val item = event.item ?: ItemStack.empty()
        val tool = parseTool(item)

        val (requiredShape, requiredTier) = requiredFor(blockKey) ?: return

        if (tool == null) {
            player.feedback(needTool = true, blockKey = blockKey)
            return
        }

        val (tier, shape) = tool
        if (shape != requiredShape) {
            player.feedback(wrongShape = true, shape = shape, blockKey = blockKey)
            return
        }

        if (!isTierAtLeast(tier, requiredTier)) {
            player.feedback(
                tooLowTier = true,
                requiredTier = requiredTier,
                shape = requiredShape,
                blockKey = blockKey
            )
        }
    }

    @EventHandler
    suspend fun on(event: BlockBreakEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val blockKey = block.getNamespacedKey()
        if (!isRelevantBlock(blockKey)) return

        val item = player.inventory.itemInMainHand
        val tool = parseTool(item)

        val (requiredShape, requiredTier) = requiredFor(blockKey) ?: return

        if (tool == null) {
            event.isCancelled = true
            player.feedback(needTool = true, blockKey = blockKey)
            return
        }

        val (tier, shape) = tool
        if (shape != requiredShape) {
            event.isCancelled = true
            player.feedback(wrongShape = true, shape = shape, blockKey = blockKey)
            return
        }

        if (!isTierAtLeast(tier, requiredTier)) {
            event.isCancelled = true
            player.feedback(
                tooLowTier = true,
                requiredTier = requiredTier,
                shape = requiredShape,
                blockKey = blockKey
            )
        }
    }

    // Simple feedback helper (actionbar + sound)
    private fun Player.feedback(
        needTool: Boolean = false,
        wrongShape: Boolean = false,
        tooLowTier: Boolean = false,
        requiredTier: Material? = null,
        shape: ToolShape? = null,
        blockKey: String,
    ) {
        val msg = when {
            needTool -> "You need a proper tool to mine $blockKey."
            wrongShape -> "Wrong tool type${shape?.let { " ($it)" } ?: ""} for $blockKey."
            tooLowTier -> "Your tool is too weak. Need <l10n:material.${requiredTier?.id}.name> or better."
            else -> null
        }
        msg?.let { ActionBarManager.send(this, "mining", it, 4) }
        playSound(location, Sound.BLOCK_ANVIL_PLACE, 0.7f, 1.7f)
    }
}