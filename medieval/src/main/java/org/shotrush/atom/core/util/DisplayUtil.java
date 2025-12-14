package org.shotrush.atom.core.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayUtil {
    
    public static ItemDisplay spawnItemBlock(Location location, Material blockType, float yaw, float scale) {
        return location.getWorld().spawn(
            location,
            ItemDisplay.class,
            display -> {
                display.setItemStack(new ItemStack(blockType));
                display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                
                Quaternionf rotation = new Quaternionf()
                    .rotateY((float) Math.toRadians(-yaw));
                
                Transformation transform = new Transformation(
                    new Vector3f(0, 0, 0),
                    rotation,
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
                );
                display.setTransformation(transform);
            }
        );
    }
    
    public static ItemDisplay spawnItemBlock(Location location, Material blockType, float yaw) {
        return spawnItemBlock(location, blockType, yaw, 0.5f);
    }
    
    public static ItemDisplay spawnItemBlock(Location location, Material blockType) {
        return spawnItemBlock(location, blockType, 0.0f, 0.5f);
    }
}
