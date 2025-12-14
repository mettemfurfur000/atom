package org.shotrush.atom.core.blocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;


public interface BlockType {
    
    
    String getIdentifier();

    
    String getDisplayName();

    
    Material getItemMaterial();

    
    String[] getLore();

    
    CustomBlock createBlock(Location spawnLocation, Location blockLocation, BlockFace blockFace);

    
    CustomBlock deserialize(String data);

    
    boolean requiresUpdate();
    
    
    default ItemStack getDropItem() {
        return null;
    }

    default double getBreakTime() {
        return 20.0;
    }
}
