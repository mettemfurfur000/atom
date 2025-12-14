package org.shotrush.atom.content.carcass

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType
import org.shotrush.atom.Atom
import org.shotrush.atom.core.util.ActionBarManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CarcassGui : Listener {
    
    private val openViews = ConcurrentHashMap<UUID, OpenCarcassView>()
    
    private data class OpenCarcassView(
        val blockEntity: CarcassBlockEntity,
        val config: CarcassAnimalConfig,
        val view: InventoryView
    )
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, Atom.instance)
    }
    
    fun open(player: Player, blockEntity: CarcassBlockEntity) {
        val config = blockEntity.getConfig() ?: return
        
        val menuType = when (config.guiRows) {
            1 -> MenuType.GENERIC_9X1
            2 -> MenuType.GENERIC_9X2
            3 -> MenuType.GENERIC_9X3
            4 -> MenuType.GENERIC_9X4
            5 -> MenuType.GENERIC_9X5
            else -> MenuType.GENERIC_9X6
        }
        
        val view = menuType.builder()
            .title(Component.text("${config.displayName} Carcass", NamedTextColor.DARK_GRAY))
            .build(player)
        
        populateInventory(view.topInventory, blockEntity, config, player)
        
        openViews[player.uniqueId] = OpenCarcassView(blockEntity, config, view)
        view.open()
    }
    
    private fun populateInventory(
        inventory: Inventory,
        blockEntity: CarcassBlockEntity,
        config: CarcassAnimalConfig,
        player: Player
    ) {
        inventory.clear()
        
        config.parts.forEach { def ->
            val partState = blockEntity.getPartState(def.id) ?: return@forEach
            val slot = def.guiSlot
            
            if (slot < 0 || slot >= inventory.size) return@forEach
            
            val remaining = partState.remainingAmount
            
            val item = if (remaining <= 0) {
                createHarvestedItem(def)
            } else {
                createPartItem(def, remaining)
            }
            
            inventory.setItem(slot, item)
        }
    }
    
    private fun createPartItem(def: CarcassPartDef, remaining: Int): ItemStack {
        val customItem = CraftEngineItems.byId(Key.of(def.itemId))
        val item = customItem?.buildItemStack(remaining.coerceIn(1, 64)) 
            ?: ItemStack(Material.BARRIER, remaining.coerceIn(1, 64))
        
        val lore = mutableListOf<Component>()
        lore.add(Component.empty())
        lore.add(Component.text("Remaining: $remaining", NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false))
        lore.add(Component.empty())
        
        if (def.requiredTool == ToolRequirement.NONE) {
            lore.add(Component.text("⬆ Pick up and click to harvest", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false))
        } else {
            lore.add(Component.text("Tool: ${def.requiredTool.displayName}", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("⬆ Pick up tool and click to harvest", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false))
        }
        
        item.editMeta { meta ->
            meta.displayName(Component.text(def.displayName, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false))
            meta.lore(lore)
        }
        
        return item
    }
    
    private fun createHarvestedItem(def: CarcassPartDef): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        
        item.editMeta { meta ->
            meta.displayName(Component.text(def.displayName, NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.STRIKETHROUGH, true))
            meta.lore(listOf(
                Component.empty(),
                Component.text("✓ Fully harvested", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            ))
        }
        
        return item
    }
    
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val openView = openViews[player.uniqueId] ?: return
        
        // Only cancel clicks in the top (carcass) inventory, allow player inventory interaction
        if (event.clickedInventory == event.view.topInventory) {
            event.isCancelled = true
        } else {
            // Allow moving items in player inventory, but prevent shift-clicking into carcass
            if (event.isShiftClick) {
                event.isCancelled = true
            }
            return
        }
        
        val slot = event.slot
        val blockEntity = openView.blockEntity
        val config = openView.config
        
        if (blockEntity.decomposed) {
            player.closeInventory()
            return
        }
        
        // Find which part was clicked
        val clickedPart = config.parts.find { it.guiSlot == slot } ?: return
        val partState = blockEntity.getPartState(clickedPart.id) ?: return
        
        if (partState.remainingAmount <= 0) return
        
        // Check tool requirement based on CURSOR item (what player picked up and is clicking with)
        val cursorItem = event.cursor
        if (!clickedPart.requiredTool.isSatisfiedBy(cursorItem)) {
            val toolName = if (clickedPart.requiredTool == ToolRequirement.NONE) {
                "your hand"
            } else {
                clickedPart.requiredTool.displayName.lowercase()
            }
            ActionBarManager.send(player, "carcass", "<red>Click with $toolName to harvest</red>")
            return
        }
        
        // Damage the tool if it has durability
        if (clickedPart.requiredTool != ToolRequirement.NONE && cursorItem.type.maxDurability > 0) {
            cursorItem.damage(1, player)
        }
        
        // Harvest the part
        blockEntity.harvestPart(player, clickedPart.id)
        
        if (blockEntity.isEmpty()) {
            player.closeInventory()
        } else {
            // Refresh the inventory
            populateInventory(event.view.topInventory, blockEntity, config, player)
        }
    }
    
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        openViews.remove(player.uniqueId)
    }
}
