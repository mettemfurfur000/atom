package org.shotrush.atom.core.workstations;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

//@RegisterSystem(
//    id = "workstation_listener",
//    priority = 6,
//    toggleable = true,
//    description = "Generic listener for CraftEngine workstation blocks"
//)
public class WorkstationListener implements Listener {
    private static final java.util.Set<String> WORKSTATION_TYPES = java.util.Set.of(
        "knapping_station", "leather_bed", "crafting_basket"
    );
    
    public WorkstationListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        String workstationType = getWorkstationType(block);
        if (workstationType == null) return;
        
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean sneaking = player.isSneaking();
        
        handleWorkstation(event, block, player, hand, sneaking, workstationType);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Block block = event.getBlock();
        WorkstationManager manager = WorkstationManager.getInstance();

        
        String workstationType = getWorkstationType(block);
        
        if (manager != null) {
            WorkstationData data = manager.get(block);
            Location blockLoc = block.getLocation();
            event.getPlayer().sendMessage("§eBlock break at " + blockLoc.getBlockX() + "," + blockLoc.getBlockY() + "," + blockLoc.getBlockZ());
            event.getPlayer().sendMessage("§eBlock break - data: " + (data != null ? "found" : "null") + ", items: " + (data != null ? data.getPlacedItems().size() : 0));
            if (data != null) {
                
                for (WorkstationData.PlacedItem item : data.getPlacedItems()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item.getItem());
                    event.getPlayer().sendMessage("§eDropped: " + item.getItem().getType());
                }
                
                data.removeAllDisplays();
                manager.remove(block);
                event.getPlayer().sendMessage("§eRemoved workstation data");
            }
        }

        if (workstationType != null && !event.isDropItems()) {
            event.getPlayer().sendMessage("§eDEBUG: Dropping workstation block: " + workstationType);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of("atom:" + workstationType));
            if (customItem != null) {
                ItemStack workstation = customItem.buildItemStack();
                if (workstation != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), workstation);
                    event.getPlayer().sendMessage("§eDEBUG: Dropped workstation block!");
                } else {
                    event.getPlayer().sendMessage("§cDEBUG: Failed to build ItemStack!");
                }
            } else {
                event.getPlayer().sendMessage("§cDEBUG: CustomItem not found for " + workstationType);
            }
        }
    }
    
    private String getWorkstationType(Block block) {
        var state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null) return null;
        
        for (String type : WORKSTATION_TYPES) {
            if (state.owner().matchesKey(Key.of("atom:" + type))) {
                return type;
            }
        }
        return null;
    }
    
    private void handleWorkstation(PlayerInteractEvent event, Block block, Player player, ItemStack hand, boolean sneaking, String type) {
        
        if ("crafting_basket".equals(type)) {
            event.setCancelled(true);
            player.openWorkbench(block.getLocation(), true);
            return;
        }
        
        WorkstationManager manager = WorkstationManager.getInstance();
        if (manager == null) {
            player.sendMessage("§cWorkstationManager is null!");
            return;
        }
        // TODO: Please fix the fricking bug where this shit doesn't drop anything, also
        // TODO: Exempt ALL CraftEngine Blocks from the BlockBreakSpeed API cause they have a fuckass mismatch.
        player.sendMessage("§eDEBUG: getOrCreate at " + block.getX() + "," + block.getY() + "," + block.getZ());
        WorkstationData data = manager.getOrCreate(block, type);
        player.sendMessage("§aWorkstation data: " + (data != null ? "found" : "null") + ", items: " + (data != null ? data.getPlacedItems().size() : 0));
        player.sendMessage("§eDEBUG: Map now has " + manager.getWorkstationCount() + " entries");
        
        WorkstationHandler<?> handler = WorkstationData.getHandler(type);
        player.sendMessage("§aHandler: " + (handler != null ? handler.getClass().getSimpleName() : "null"));
        
        
        if (handler != null && handler.handleWrenchInteraction(player, hand, sneaking, data, block)) {
            event.setCancelled(true);
            return;
        }
        //e

        if (handler != null && handler.isValidTool(hand)) {
            handler.handleInteraction(event, block, player, hand, data);
            return;
        }
        
        
        if (sneaking && hand.getType() == Material.AIR) {
            event.setCancelled(true);
            ItemStack removed = data.removeLastItem();
            if (removed != null) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), removed);
                player.swingMainHand();
            }
            return;
        }
        
        
        if (handler != null) {
            handler.handleInteraction(event, block, player, hand, data);
        }
    }
}
