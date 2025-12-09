package org.shotrush.atom.content.systems;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.BlockBreakSpeedAPI;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

//@RegisterSystem(
//    id = "block_break_system",
//    priority = 5,
//    toggleable = false,
//    description = "Configures block breaking speeds"
//)
public class BlockBreakSystem {
    
    private final Plugin plugin;
    
    public BlockBreakSystem(Plugin plugin) {
        this.plugin = plugin;
        
        
        configureBreakingSpeeds();
        
        plugin.getLogger().info("Block breaking speeds configured");
    }
    
    
    private void configureBreakingSpeeds() {
        
        BlockBreakSpeedAPI.setGlobalMultiplier(2.0);  
        
        
        configureMiningBlocks();
        configureWoodBlocks();
        configureFarmingBlocks();
        configureSpecialBlocks();
        
        plugin.getLogger().info("Block breaking configuration:");
        plugin.getLogger().info("- Global multiplier: " + BlockBreakSpeedAPI.getGlobalMultiplier());
        plugin.getLogger().info("- Logs: 4x slower");
        plugin.getLogger().info("- Ores: 6x slower");
        plugin.getLogger().info("- Stone: 3x slower");
        plugin.getLogger().info("- Leaves: 2x faster");
    }
    
    
    private void configureMiningBlocks() {
        
        BlockBreakSpeedAPI.setCategoryMultiplier("_ORE", 6.0);      
        BlockBreakSpeedAPI.setCategoryMultiplier("DEEPSLATE", 8.0); 
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("STONE", 3.0);     
        BlockBreakSpeedAPI.setCategoryMultiplier("GRANITE", 3.5);   
        BlockBreakSpeedAPI.setCategoryMultiplier("DIORITE", 3.5);   
        BlockBreakSpeedAPI.setCategoryMultiplier("ANDESITE", 3.5);  
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.OBSIDIAN, 15.0);        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.ANCIENT_DEBRIS, 25.0);  
        BlockBreakSpeedAPI.setBlockMultiplier(Material.NETHERITE_BLOCK, 20.0); 
        BlockBreakSpeedAPI.setBlockMultiplier(Material.DIAMOND_BLOCK, 10.0);   
        BlockBreakSpeedAPI.setBlockMultiplier(Material.EMERALD_BLOCK, 10.0);   
    }
    
    
    private void configureWoodBlocks() {
        
        BlockBreakSpeedAPI.setCategoryMultiplier("LOG", 4.0);       
        BlockBreakSpeedAPI.setCategoryMultiplier("WOOD", 3.5);      
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("PLANKS", 2.0);    
        BlockBreakSpeedAPI.setCategoryMultiplier("SLAB", 1.5);      
        BlockBreakSpeedAPI.setCategoryMultiplier("STAIRS", 1.8);    
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("LEAVES", 0.5);    
    }
    
    
    private void configureFarmingBlocks() {
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.DIRT, 0.8);           
        BlockBreakSpeedAPI.setBlockMultiplier(Material.GRASS_BLOCK, 1.2);    
        BlockBreakSpeedAPI.setBlockMultiplier(Material.FARMLAND, 0.9);       
        BlockBreakSpeedAPI.setBlockMultiplier(Material.COARSE_DIRT, 1.0);    
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.SAND, 0.7);           
        BlockBreakSpeedAPI.setBlockMultiplier(Material.GRAVEL, 0.8);         
        BlockBreakSpeedAPI.setBlockMultiplier(Material.SOUL_SAND, 1.5);      
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("CROP", 0.2);       
        BlockBreakSpeedAPI.setCategoryMultiplier("FLOWER", 0.1);     
        BlockBreakSpeedAPI.setCategoryMultiplier("GRASS", 0.1);      
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("WOOL", 0.6);       
        BlockBreakSpeedAPI.setCategoryMultiplier("CARPET", 0.3);     
    }
    
    
    private void configureSpecialBlocks() {
        
        BlockBreakSpeedAPI.setCategoryMultiplier("GLASS", 0.4);     
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("CONCRETE", 5.0);  
        BlockBreakSpeedAPI.setCategoryMultiplier("CONCRETE_POWDER", 0.6); 
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("TERRACOTTA", 2.5); 
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.ICE, 0.5);          
        BlockBreakSpeedAPI.setBlockMultiplier(Material.PACKED_ICE, 1.5);   
        BlockBreakSpeedAPI.setBlockMultiplier(Material.BLUE_ICE, 2.0);     
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.NETHERRACK, 0.3);   
        BlockBreakSpeedAPI.setBlockMultiplier(Material.SOUL_SOIL, 0.4);    
        BlockBreakSpeedAPI.setBlockMultiplier(Material.BASALT, 2.5);       
        BlockBreakSpeedAPI.setBlockMultiplier(Material.BLACKSTONE, 3.0);   
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.END_STONE, 2.0);    
        BlockBreakSpeedAPI.setBlockMultiplier(Material.PURPUR_BLOCK, 2.5); 
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.SPAWNER, 30.0);     
        BlockBreakSpeedAPI.setBlockMultiplier(Material.BEDROCK, 1000.0);   
    }
    
    
    public static void applyDifficultyModifier(double difficulty) {
        double currentGlobal = BlockBreakSpeedAPI.getGlobalMultiplier();
        BlockBreakSpeedAPI.setGlobalMultiplier(currentGlobal * difficulty);
    }
    
    
    public static void resetToVanilla() {
        BlockBreakSpeedAPI.clearAllMultipliers();
    }
    
    
    public static void applyRealisticMode() {
        
        BlockBreakSpeedAPI.clearAllMultipliers();
        BlockBreakSpeedAPI.setGlobalMultiplier(3.0);
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("_ORE", 10.0);
        BlockBreakSpeedAPI.setCategoryMultiplier("STONE", 6.0);
        BlockBreakSpeedAPI.setCategoryMultiplier("DEEPSLATE", 12.0);
        
        
        BlockBreakSpeedAPI.setCategoryMultiplier("LOG", 8.0);
        BlockBreakSpeedAPI.setCategoryMultiplier("PLANKS", 4.0);
        
        
        BlockBreakSpeedAPI.setBlockMultiplier(Material.OBSIDIAN, 50.0);
        BlockBreakSpeedAPI.setBlockMultiplier(Material.ANCIENT_DEBRIS, 100.0);
        BlockBreakSpeedAPI.setBlockMultiplier(Material.BEDROCK, 10000.0);
    }
}
