package org.shotrush.atom.cog;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CogListener implements Listener {
    
    private final Atom plugin;
    private final Map<String, Cog> cogs = new ConcurrentHashMap<>();
    
    public CogListener(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        
        if (item.getType() != Material.BARRIER || !item.hasItemMeta()) {
            return;
        }
        
        Integer customModelData = item.getItemMeta().getCustomModelData();
        if (customModelData == null || (customModelData != 1 && customModelData != 2)) {
            return;
        }
        
        event.setCancelled(true);
        
        Block block = event.getBlockPlaced();
        BlockFace face = event.getBlockAgainst().getFace(block);
        if (face == null) {
            face = BlockFace.UP;
        }
        boolean isPowered = customModelData == 2;
        
        Cog cog = new Cog(plugin, block.getLocation().add(0.5, 0.5, 0.5), face, isPowered);
        cogs.put(locationKey(block.getLocation()), cog);
        
        if (isPowered && cog.isRotating()) {
            propagateRotationToAdjacent(block.getLocation(), true);
        }
        
        item.setAmount(item.getAmount() - 1);
        
        Player player = event.getPlayer();
        player.sendMessage(isPowered ? "§aPlaced powered cog!" : "§7Placed small cog!");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) {
            return;
        }
        
        String key = locationKey(block.getLocation());
        Cog cog = cogs.get(key);
        
        if (cog != null && cog.isPowered()) {
            cog.toggleRotation(plugin);
            event.setCancelled(true);
            
            Player player = event.getPlayer();
            if (cog.isRotating()) {
                player.sendMessage("§aStarted cog rotation!");
                propagateRotationToAdjacent(block.getLocation(), true);
            } else {
                player.sendMessage("§7Stopped cog rotation!");
                stopAdjacentCogs(block.getLocation());
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (block.getType() != Material.BARRIER) {
            return;
        }
        
        String key = locationKey(block.getLocation());
        Cog cog = cogs.remove(key);
        
        if (cog != null) {
            cog.remove();
            
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5),
                Cog.getCogItem(cog.isPowered())
            );
            
            event.setCancelled(true);
            event.getPlayer().sendMessage("§7Removed cog!");
        }
    }
    
    private void propagateRotationToAdjacent(org.bukkit.Location loc, boolean reverse) {
        List<org.bukkit.Location> adjacent = getAdjacentLocations(loc);
        
        for (org.bukkit.Location adjLoc : adjacent) {
            String key = locationKey(adjLoc);
            Cog adjCog = cogs.get(key);
            
            if (adjCog != null && adjCog.isPowered() && !adjCog.isRotating()) {
                if (reverse) {
                    adjCog.startReverseRotation(plugin);
                } else {
                    adjCog.startRotationAgain(plugin);
                }
            }
        }
    }
    
    private void stopAdjacentCogs(org.bukkit.Location loc) {
        List<org.bukkit.Location> adjacent = getAdjacentLocations(loc);
        
        for (org.bukkit.Location adjLoc : adjacent) {
            String key = locationKey(adjLoc);
            Cog adjCog = cogs.get(key);
            
            if (adjCog != null && adjCog.isPowered() && adjCog.isRotating()) {
                adjCog.stopRotation();
            }
        }
    }
    
    private List<org.bukkit.Location> getAdjacentLocations(org.bukkit.Location loc) {
        List<org.bukkit.Location> adjacent = new ArrayList<>();
        adjacent.add(loc.clone().add(1, 0, 0));
        adjacent.add(loc.clone().add(-1, 0, 0));
        adjacent.add(loc.clone().add(0, 1, 0));
        adjacent.add(loc.clone().add(0, -1, 0));
        adjacent.add(loc.clone().add(0, 0, 1));
        adjacent.add(loc.clone().add(0, 0, -1));
        return adjacent;
    }
    
    private String locationKey(org.bukkit.Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
    
    public void removeAll() {
        cogs.values().forEach(Cog::remove);
        cogs.clear();
    }
}
