package org.shotrush.atom.cog;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.shotrush.atom.core.display.CustomDisplayModel;
import org.shotrush.atom.core.display.DisplayAnimator;
import org.shotrush.atom.core.display.DisplayEntityManager;

import java.util.List;

@Getter
public class Cog {
    
    private final Location location;
    private final BlockFace face;
    private final boolean isPowered;
    private final DisplayEntityManager displayManager;
    private DisplayAnimator animator;
    private boolean isRotating;
    
    public Cog(Plugin plugin, Location location, BlockFace face, boolean isPowered) {
        this.location = location.clone();
        this.face = face;
        this.isPowered = isPowered;
        
        CustomDisplayModel model = CustomDisplayModel.builder()
                .baseMaterial(Material.DIAMOND)
                .modelStrings(List.of(isPowered ? "cog_small_powered" : "cog_small"))
                .leftRotation(CustomDisplayModel.getRotationFromFace(face))
                .build();
        
        this.displayManager = DisplayEntityManager.spawn(plugin, location, model, true);
        
        if (isPowered) {
            this.animator = DisplayAnimator.create(displayManager, plugin);
            startRotation(face, false);
            this.isRotating = true;
        }
    }
    
    private void startRotation(BlockFace face, boolean reverse) {
        float speed = reverse ? -180 : 180;
        switch (face) {
            case UP, DOWN -> animator.rotateY(speed);
            case NORTH, SOUTH -> animator.rotateZ(speed);
            case EAST, WEST -> animator.rotateX(speed);
        }
    }
    
    public void toggleRotation(Plugin plugin) {
        if (!isPowered) {
            return;
        }
        
        if (isRotating) {
            stopRotation();
        } else {
            startRotationAgain(plugin);
        }
    }
    
    public void stopRotation() {
        if (animator != null) {
            animator.stop();
            isRotating = false;
        }
    }
    
    public void startRotationAgain(Plugin plugin) {
        if (!isPowered) {
            return;
        }
        
        if (animator == null) {
            animator = DisplayAnimator.create(displayManager, plugin);
        }
        startRotation(face, false);
        isRotating = true;
    }
    
    public void startReverseRotation(Plugin plugin) {
        if (animator != null) {
            animator.stop();
        }
        animator = DisplayAnimator.create(displayManager, plugin);
        startRotation(face, true);
        isRotating = true;
    }
    
    public void setPowered(Plugin plugin, boolean powered) {
        if (this.isPowered == powered) {
            return;
        }
        
        CustomDisplayModel newModel = displayManager.getCurrentModel()
                .withModelStrings(List.of(powered ? "cog_small_powered" : "cog_small"));
        displayManager.updateModel(newModel);
        
        if (powered) {
            if (animator != null) {
                animator.stop();
            }
            animator = DisplayAnimator.create(displayManager, plugin);
            startRotation(face, false);
            isRotating = true;
        } else {
            if (animator != null) {
                animator.stop();
                animator = null;
            }
            isRotating = false;
        }
    }
    
    public void remove() {
        if (animator != null) {
            animator.stop();
        }
        displayManager.remove(true);
    }
    
    public static ItemStack getCogItem(boolean isPowered) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(isPowered ? "§6Powered Cog" : "§7Small Cog");
            meta.setLore(List.of(
                "§7Place this block to spawn a cog",
                isPowered ? "§eRotates when placed" : "§7Static decoration"
            ));
            
            meta.setCustomModelData(isPowered ? 2 : 1);
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
