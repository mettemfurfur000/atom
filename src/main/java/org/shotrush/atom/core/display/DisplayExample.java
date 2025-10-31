package org.shotrush.atom.core.display;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

import static org.shotrush.atom.core.display.CustomDisplayModel.getRotationFromFace;


public class DisplayExample {
    
    
    public static DisplayEntityManager spawnSimpleCog(Plugin plugin, Location location) {
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.DIAMOND)
                .modelStrings(List.of("cog_small"))
                .displayName("Small Cog")
                .build();
        
        return DisplayEntityManager.spawn(plugin, location, model);
    }
    
    
    public static DisplayEntityManager spawnPoweredCog(Plugin plugin, Location location, boolean isPowered) {
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.DIAMOND)
                .modelStrings(List.of(isPowered ? "cog_small_powered" : "cog_small"))
                .displayName(isPowered ? "Powered Cog" : "Small Cog")
                .glowing(isPowered)
                .build();
        
        return DisplayEntityManager.spawn(plugin, location, model);
    }
    
    
    public static DisplayEntityManager spawnRotatedCog(Plugin plugin, Location location, BlockFace face) {
        AxisAngle4f rotation = getRotationFromFace(face);
        
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.DIAMOND)
                .modelStrings(List.of("cog_small"))
                .leftRotation(rotation)
                .build();
        
        return DisplayEntityManager.spawn(plugin, location, model);
    }
    
    
    public static DisplayEntityManager spawnScaledDisplay(Plugin plugin, Location location, float scale) {
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.DIAMOND)
                .modelStrings(List.of("cog_small"))
                .scale(scale)
                .build();
        
        return DisplayEntityManager.spawn(plugin, location, model);
    }
    
    
    public static DisplayEntityManager spawnCustomBlock(Plugin plugin, Location location) {
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.STONE) 
                .modelStrings(List.of("custom_block"))
                .translation(new Vector3f(0, 0, 0)) 
                .build();
        
        return DisplayEntityManager.spawn(plugin, location, model);
    }
    
    
    public static void togglePowerState(DisplayEntityManager manager, boolean isPowered) {
        CustomDisplayModel currentModel = manager.getCurrentModel();
        
        CustomDisplayModel newModel = currentModel
                .withModelStrings(List.of(isPowered ? "cog_small_powered" : "cog_small"))
                .withScale(isPowered ? 1.2f : 1.0f); 
        
        manager.updateModel(newModel);
    }
    
    
    public static DisplayEntityManager spawnComplexDisplay(Plugin plugin, Location location) {
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.DIAMOND)
                .modelStrings(List.of("powered", "rotating")) 
                .brightness(15) 
                .glowing(true)
                .build();
        
        return DisplayEntityManager.spawn(plugin, location, model);
    }
    
    
    public static DisplayAnimator spawnRotatingCog(Plugin plugin, Location location) {
        DisplayEntityManager manager = spawnSimpleCog(plugin, location);
        return DisplayAnimator.create(manager, plugin).rotateY(180);
    }
    
    
    public static DisplayAnimator spawnPulsingDisplay(Plugin plugin, Location location) {
        DisplayEntityManager manager = spawnSimpleCog(plugin, location);
        return DisplayAnimator.create(manager, plugin).pulse(0.8f, 1.2f, 40);
    }
    
    
    public static DisplayAnimator spawnBobbingDisplay(Plugin plugin, Location location) {
        DisplayEntityManager manager = spawnSimpleCog(plugin, location);
        return DisplayAnimator.create(manager, plugin).bob(0.2f, 60);
    }
    
    
    public static void animateScaleUp(DisplayEntityManager manager, Plugin plugin) {
        DisplayAnimator.create(manager, plugin).scale(2.0f, 20);
    }
    
    
    public static void animateRotateTo(DisplayEntityManager manager, Plugin plugin, float x, float y, float z) {
        DisplayAnimator.create(manager, plugin).rotateTo(x, y, z, 20);
    }

}
