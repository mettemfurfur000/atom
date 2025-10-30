package org.shotrush.atom.listener;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;

public class ModelPlaceListener implements Listener {
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(Atom.getInstance(), "model_id");
        
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }
        
        String modelId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        
        event.setCancelled(true);
        
        Location placeLoc = event.getBlockPlaced().getLocation().clone();
        placeLoc.add(0.5, 0.5, 0.5);
        
        float yaw = event.getPlayer().getLocation().getYaw();
        float pitch = event.getPlayer().getLocation().getPitch();
        
        yaw = ((yaw % 360) + 360) % 360;
        float snappedYaw = Math.round(yaw / 90.0f) * 90.0f;
        float snappedPitch = Math.round(pitch / 90.0f) * 90.0f;
        snappedPitch = Math.max(-90, Math.min(90, snappedPitch));
        
        Atom.getInstance().getSchedulerManager().runAtLocationDelayed(placeLoc, () -> {
            try {
                org.shotrush.atom.display.DisplayGroup group = Atom.getInstance().getModelManager().spawnModel(modelId, placeLoc);
                
                if (group != null && Math.abs(snappedYaw) > 0.1f) {
                    Atom.getInstance().getSchedulerManager().runAtLocationDelayed(placeLoc, () -> {
                        group.rotate(snappedYaw);
                    }, 1);
                }
                
                if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
            } catch (Exception e) {
                event.getPlayer().sendMessage(net.kyori.adventure.text.Component.text(
                    "✗ Failed to place model: " + e.getMessage(),
                    net.kyori.adventure.text.format.NamedTextColor.RED
                ));
            }
        }, 1);
    }
}
