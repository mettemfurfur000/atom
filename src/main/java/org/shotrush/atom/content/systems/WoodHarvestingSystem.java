package org.shotrush.atom.content.systems;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.foragingage.items.SharpenedFlint;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.util.ActionBarManager;

import java.util.HashMap;
import java.util.Map;
import org.shotrush.atom.core.api.annotation.RegisterSystem;
import org.shotrush.atom.item.Items;
import org.shotrush.atom.item.ItemsKt;

@RegisterSystem(
    id = "wood_harvesting_system",
    priority = 2,
    toggleable = true,
    description = "Handles tree chopping mechanics"
)
public class WoodHarvestingSystem implements Listener {
    
    private final Plugin plugin;
    private final Map<Location, Integer> woodDamageStates = new HashMap<>();
    
    public WoodHarvestingSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        Location loc = placed.getLocation();
        woodDamageStates.remove(loc);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace direction = event.getDirection();
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.POINTED_DRIPSTONE) {
                Block targetBlock = block.getRelative(direction);
                if (isWoodBlock(targetBlock.getType())) {
                    plugin.getLogger().info("[WoodHarvesting] Piston pushing dripstone into wood!");
                    splitWoodBlock(targetBlock);
                }
            }
        }
    }
    
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || !isWoodBlock(block.getType())) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isUsingSharpenedFlint(item)) {
            ActionBarManager.send(player, "§cYou need a Sharpened Flint to harvest wood!");
            return;
        }
        handleWoodDamageStage(block, player);
        damageFlint(item, player);
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        String typeName = broken.getType().name();

        if (isWoodBlock(broken.getType()) || typeName.contains("STRIPPED") || 
            typeName.contains("SANDSTONE_WALL") || typeName.contains("FENCE")) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (isUsingSharpenedFlint(item)) {
                event.setCancelled(true);
                handleWoodDamageStage(broken, player);
                damageFlint(item, player);
            } else if (isUsingAxe(item)) {
                event.setDropItems(true);
            } else {
                event.setDropItems(false);
                ActionBarManager.send(player, "§cYou need an Axe or Sharpened Flint to harvest wood!");
            }
        }
    }
    
    private boolean isUsingSharpenedFlint(ItemStack item) {
        return SharpenedFlint.isSharpenedFlint(item);
    }
    
    private boolean isUsingAxe(ItemStack item) {
        if (item == null) return false;
        String typeName = item.getType().name();
        return typeName.endsWith("_AXE");
    }
    
    private void damageFlint(ItemStack item, Player player) {
        SharpenedFlint.damageItem(item, player, 0.15);
    }

    public boolean isWoodBlock(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD");
    }
    
    private void splitWoodBlock(Block woodBlock) {
        Material woodType = woodBlock.getType();
        Material plankType = getPlankTypeFromWood(woodType);
        
        if (plankType == null) return;
        
        Location loc = woodBlock.getLocation();
        woodBlock.setType(Material.AIR);

        ItemStack planks = new ItemStack(plankType, 1);
        loc.getWorld().dropItemNaturally(loc.add(0.5, 0.5, 0.5), planks);

        loc.getWorld().playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);

        loc.getWorld().spawnParticle(Particle.BLOCK, 
            loc, 20, 0.3, 0.3, 0.3, 0.05, 
            woodType.createBlockData());
    }
    
    private Material getPlankTypeFromWood(Material woodType) {
        String woodName = woodType.name();

        if (woodName.contains("OAK")) return Material.OAK_PLANKS;
        if (woodName.contains("SPRUCE")) return Material.SPRUCE_PLANKS;
        if (woodName.contains("BIRCH")) return Material.BIRCH_PLANKS;
        if (woodName.contains("JUNGLE")) return Material.JUNGLE_PLANKS;
        if (woodName.contains("ACACIA")) return Material.ACACIA_PLANKS;
        if (woodName.contains("DARK_OAK")) return Material.DARK_OAK_PLANKS;
        if (woodName.contains("MANGROVE")) return Material.MANGROVE_PLANKS;
        if (woodName.contains("CHERRY")) return Material.CHERRY_PLANKS;
        if (woodName.contains("BAMBOO")) return Material.BAMBOO_PLANKS;
        if (woodName.contains("CRIMSON")) return Material.CRIMSON_PLANKS;
        if (woodName.contains("WARPED")) return Material.WARPED_PLANKS;

        return Material.OAK_PLANKS;
    }
    
    private void handleWoodDamageStage(Block block, Player player) {
        Location loc = block.getLocation();
        Material currentType = block.getType();
        String typeName = currentType.name();
        int currentStage = 0;
        if (typeName.contains("FENCE")) {
            currentStage = 3;
        } else if (typeName.contains("SANDSTONE_WALL")) {
            currentStage = 2;
        } else if (typeName.contains("STRIPPED")) {
            currentStage = 1;
        } else if (isWoodBlock(currentType)) {
            currentStage = woodDamageStates.getOrDefault(loc, 0);
        }
        
        currentStage++;
        plugin.getLogger().info("[WoodHarvesting] Block: " + currentType + ", Stage: " + currentStage);
        
        switch (currentStage) {
            case 1:
                Material strippedType = getStrippedWoodType(currentType);
                if (strippedType != null) {
                    block.setType(strippedType);
                    woodDamageStates.put(loc, currentStage);
                    
                    block.getWorld().playSound(loc, Sound.ITEM_AXE_STRIP, 1.0f, 1.0f);
                    block.getWorld().spawnParticle(Particle.BLOCK, 
                        loc.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.05,
                        currentType.createBlockData());
                    
                    ActionBarManager.send(player,"§7Wood partially damaged (1/4)");
                }
                break;
                
            case 2:
                block.setType(Material.SANDSTONE_WALL);
                woodDamageStates.put(loc, currentStage);
                
                block.getWorld().playSound(loc, Sound.BLOCK_WOOD_HIT, 1.0f, 0.9f);
                block.getWorld().spawnParticle(Particle.BLOCK, 
                    loc.add(0.5, 0.5, 0.5), 12, 0.25, 0.25, 0.25, 0.05,
                    currentType.createBlockData());

                ActionBarManager.send(player,"§7Wood damaged (2/4)");
                break;
                
            case 3:
                Material fenceType = getFenceType(currentType);
                if (fenceType != null) {
                    block.setType(fenceType);
                    woodDamageStates.put(loc, currentStage);
                    
                    block.getWorld().playSound(loc, Sound.BLOCK_WOOD_HIT, 1.0f, 0.8f);
                    block.getWorld().spawnParticle(Particle.BLOCK, 
                        loc.add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05,
                        currentType.createBlockData());
                    ActionBarManager.send(player,"§7Wood heavily damaged (3/4)");
                }
                break;
                
            case 4:
            default:
                Material plankType = getPlankTypeFromWood(currentType);
                if (plankType != null) {
                    woodDamageStates.remove(loc);
                    
                    block.setType(Material.AIR);
                    
                    ItemStack planks = new ItemStack(plankType, 1);
                    block.getWorld().dropItemNaturally(loc.add(0.5, 0.5, 0.5), planks);
                    
                    block.getWorld().playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
                    block.getWorld().spawnParticle(Particle.BLOCK, 
                        loc, 20, 0.3, 0.3, 0.3, 0.1,
                        currentType.createBlockData());

                    ActionBarManager.send(player,"§aWood harvested!");
                }
                break;
        }
    }
    
    private Material getStrippedWoodType(Material woodType) {
        String name = woodType.name();
        
        if (name.contains("STRIPPED")) return null;
        
        if (name.endsWith("_LOG")) {
            String prefix = name.substring(0, name.length() - 4);
            try {
                return Material.valueOf("STRIPPED_" + prefix + "_LOG");
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        if (name.endsWith("_WOOD")) {
            String prefix = name.substring(0, name.length() - 5);
            try {
                return Material.valueOf("STRIPPED_" + prefix + "_WOOD");
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        return null;
    }
    
    private Material getFenceType(Material woodType) {
        String name = woodType.name();
        
        if (name.contains("OAK")) return Material.OAK_FENCE;
        if (name.contains("SPRUCE")) return Material.SPRUCE_FENCE;
        if (name.contains("BIRCH")) return Material.BIRCH_FENCE;
        if (name.contains("JUNGLE")) return Material.JUNGLE_FENCE;
        if (name.contains("ACACIA")) return Material.ACACIA_FENCE;
        if (name.contains("DARK_OAK")) return Material.DARK_OAK_FENCE;
        if (name.contains("MANGROVE")) return Material.MANGROVE_FENCE;
        if (name.contains("CHERRY")) return Material.CHERRY_FENCE;
        if (name.contains("BAMBOO")) return Material.BAMBOO_FENCE;
        if (name.contains("CRIMSON")) return Material.CRIMSON_FENCE;
        if (name.contains("WARPED")) return Material.WARPED_FENCE;
        
        return Material.OAK_FENCE;
    }
}
