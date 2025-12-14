package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.shotrush.atom.core.data.PersistentData;

import org.shotrush.atom.core.api.annotation.RegisterSystem;


@RegisterSystem(
    id = "waterskin_handler",
    priority = 3,
    toggleable = true,
    description = "Handles waterskin filling and drinking"
)
public class WaterskinHandler implements Listener {
    
    private final Plugin plugin;
    
    public WaterskinHandler(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onWaterskinUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !isWaterskin(item)) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || 
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            
            org.bukkit.util.RayTraceResult result = player.rayTraceBlocks(5, org.bukkit.FluidCollisionMode.SOURCE_ONLY);
            
            if (result != null && result.getHitBlock() != null && isWaterSource(result.getHitBlock().getType())) {
                
                fillWaterskin(player, item, result.getHitBlock());
                event.setCancelled(true);
            } else if (Waterskin.getWaterAmount(item) > 0) {
                
                drinkWaterskin(player, item);
                event.setCancelled(true);
            }
        }
    }
    
    private boolean isWaterskin(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        String itemId = PersistentData.getString(item.getItemMeta(), "custom_item_id", null);
        return "waterskin".equals(itemId);
    }
    
    private boolean isWaterSource(Material material) {
        return material == Material.WATER || material == Material.WATER_CAULDRON;
    }
    
    private void fillWaterskin(Player player, ItemStack item, Block block) {
        int currentAmount = Waterskin.getWaterAmount(item);
        
        if (currentAmount >= 5) {
            player.sendMessage("§cYour waterskin is already full!");
            return;
        }
        
        
        boolean isPurified = block.getType() == Material.WATER_CAULDRON;
        
        
        Waterskin.setWater(item, 5, isPurified);
        
        if (isPurified) {
            player.sendMessage("§aFilled waterskin with purified water");
        } else {
            player.sendMessage("§eFilled waterskin with raw water");
            player.sendMessage("§7§oRaw water may cause sickness...");
        }
        
        
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);
    }
    
    private void drinkWaterskin(Player player, ItemStack item) {
        int currentAmount = Waterskin.getWaterAmount(item);
        
        if (currentAmount <= 0) {
            player.sendMessage("§cYour waterskin is empty!");
            return;
        }
        
        boolean isPurified = Waterskin.isPurified(item);
        
        
        Waterskin.drinkWater(item);
        
        
        int currentFood = player.getFoodLevel();
        float currentSaturation = player.getSaturation();
        
        player.setFoodLevel(Math.min(20, currentFood + 1));
        player.setSaturation(Math.min(20, currentSaturation + 2.0f));
        
        if (isPurified) {
            player.sendMessage("§aYou drink purified water");
            
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false));
        } else {
            player.sendMessage("§eYou drink raw water");
            
            
            if (Math.random() < 0.3) {
                player.sendMessage("§c§oYou feel sick from the raw water...");
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0, false, false));
            }
        }
        
        
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        
        int remaining = Waterskin.getWaterAmount(item);
        if (remaining > 0) {
            player.sendMessage("§7Water remaining: " + remaining + "/5");
        } else {
            player.sendMessage("§7Your waterskin is now empty");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterskinConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        
        if (isWaterskin(item)) {
            event.setCancelled(true);
        }
    }
}
