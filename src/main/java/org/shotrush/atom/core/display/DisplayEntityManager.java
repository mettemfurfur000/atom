package org.shotrush.atom.core.display;

import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;


public class DisplayEntityManager {
    
    private final Plugin plugin;
    private final Location location;
    private UUID interactionUUID;
    private UUID displayUUID;
    private CustomDisplayModel currentModel;
    
    private DisplayEntityManager(Plugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location.clone();
    }
    
    
    public static DisplayEntityManager spawn(@NonNull Plugin plugin, @NonNull Location location, @NonNull CustomDisplayModel model) {
        return spawn(plugin, location, model, true);
    }
    
    
    public static DisplayEntityManager spawn(@NonNull Plugin plugin, @NonNull Location location, @NonNull CustomDisplayModel model, boolean placeBarrier) {
        DisplayEntityManager manager = new DisplayEntityManager(plugin, location);
        manager.spawnEntities(model, placeBarrier);
        return manager;
    }
    
    
    private void spawnEntities(CustomDisplayModel model, boolean placeBarrier) {
        this.currentModel = model;
        
        
        cleanupExistingEntities();
        
        
        if (placeBarrier) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    location.getBlock().setType(Material.BARRIER);
                }
            }.runTaskLater(plugin, 1L);
        }
        
        
        ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        
        
        applyModelToDisplay(display, model);
        
        
        Interaction interaction = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        interaction.setInteractionWidth(1f);
        interaction.setInteractionHeight(1f);
        interaction.setResponsive(true);
        interaction.setInvulnerable(true);
        interaction.setGravity(false);
        
        
        interaction.addPassenger(display);
        
        
        this.interactionUUID = interaction.getUniqueId();
        this.displayUUID = display.getUniqueId();
    }
    
    
    private void applyModelToDisplay(ItemDisplay display, CustomDisplayModel model) {
        
        ItemStack item = new ItemStack(model.getBaseMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setStrings(model.getModelStrings());
            meta.setCustomModelDataComponent(component);
            
            
            if (model.getDisplayName() != null) {
                meta.setDisplayName(model.getDisplayName());
            }
            
            item.setItemMeta(meta);
        }
        
        display.setItemStack(item);
        display.setTransformation(model.createTransformation());
        
        
        if (model.isGlowing()) {
            display.setGlowing(true);
        }
        
        if (model.getBrightness() != null) {
            display.setBrightness(new Display.Brightness(model.getBrightness(), model.getBrightness()));
        }
    }
    
    
    public boolean updateModel(@NonNull CustomDisplayModel newModel) {
        if (displayUUID == null) {
            return false;
        }
        
        ItemDisplay display = getDisplay();
        if (display == null || !display.isValid()) {
            return false;
        }
        
        applyModelToDisplay(display, newModel);
        this.currentModel = newModel;
        return true;
    }
    
    
    public ItemDisplay getDisplay() {
        if (displayUUID == null || location.getWorld() == null) {
            return null;
        }
        
        return (ItemDisplay) location.getWorld().getEntity(displayUUID);
    }
    
    
    public Interaction getInteraction() {
        if (interactionUUID == null || location.getWorld() == null) {
            return null;
        }
        
        return (Interaction) location.getWorld().getEntity(interactionUUID);
    }
    
    
    public CustomDisplayModel getCurrentModel() {
        return currentModel;
    }
    
    
    public Location getLocation() {
        return location.clone();
    }
    
    
    public boolean isValid() {
        ItemDisplay display = getDisplay();
        return display != null && display.isValid();
    }
    
    
    public void remove(boolean removeBarrier) {
        
        ItemDisplay display = getDisplay();
        if (display != null) {
            display.remove();
        }
        
        
        Interaction interaction = getInteraction();
        if (interaction != null) {
            interaction.remove();
        }
        
        
        if (removeBarrier && location.getBlock().getType() == Material.BARRIER) {
            location.getBlock().setType(Material.AIR);
        }
        
        
        this.displayUUID = null;
        this.interactionUUID = null;
    }
    
    
    public void remove() {
        remove(false);
    }
    
    
    private void cleanupExistingEntities() {
        if (location.getWorld() == null) {
            return;
        }
        
        location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).forEach(entity -> {
            if (entity instanceof ItemDisplay || entity instanceof Interaction) {
                entity.remove();
            }
        });
    }
    
    
    public UUID getDisplayUUID() {
        return displayUUID;
    }
    
    
    public UUID getInteractionUUID() {
        return interactionUUID;
    }
}
