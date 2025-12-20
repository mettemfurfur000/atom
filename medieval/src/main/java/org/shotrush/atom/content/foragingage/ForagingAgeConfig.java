package org.shotrush.atom.content.foragingage;

import org.bukkit.Material;
import org.shotrush.atom.core.api.BlockBreakSpeedAPI;


public class ForagingAgeConfig {
    
    public static void configureBlockBreaking() {
        
        
        BlockBreakSpeedAPI.builder()
            .global(1.5)                              
            .category("LOG", 3.0)                    
            .category("_ORE", 5.0)                   
            .category("STONE", 2.0)                  
            .category("DEEPSLATE", 7.0)              
            .category("LEAVES", 0.5)                 
            .category("WOOL", 0.7)                   
            .block(Material.OBSIDIAN, 10.0)          
            .block(Material.ANCIENT_DEBRIS, 20.0)    
            .block(Material.GRASS_BLOCK, 1.2)        
            .block(Material.DIRT, 0.8)               
            .apply();
    }
    
    
    public static void applyHardMode() {
        
        BlockBreakSpeedAPI.setGlobalMultiplier(
            BlockBreakSpeedAPI.getFinalMultiplier(Material.STONE) * 0.5
        );
    }
    
    
    public static void resetToVanilla() {
        BlockBreakSpeedAPI.clearAllMultipliers();
    }
}
