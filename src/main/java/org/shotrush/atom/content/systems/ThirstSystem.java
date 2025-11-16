package org.shotrush.atom.content.systems;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.shotrush.atom.core.api.annotation.RegisterSystem;
import org.shotrush.atom.core.util.ActionBarManager;

@RegisterSystem(
    id = "thirst_system",
    priority = 2,
    dependencies = {"action_bar_manager"},
    toggleable = true,
    description = "Manages player thirst mechanics"
)
public class ThirstSystem implements Listener {
    
    @Getter
    public static ThirstSystem instance;
    
    private final Plugin plugin;
    private final Map<UUID, Integer> thirstLevels = new HashMap<>();
    private final Map<UUID, Long> thirstAccelerationEnd = new HashMap<>();
    private final Map<UUID, Integer> thirstDamageTicks = new HashMap<>();
    private final Map<UUID, Integer> thirstTickCounter = new HashMap<>();
    
    private static final int MAX_THIRST = 20;
    private static final int THIRST_DECREASE_INTERVAL = 1;
    private static final int THIRST_DRAIN_RATE = 600;
    private static final int THIRST_DAMAGE_INTERVAL = 80;
    
    public ThirstSystem(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        int savedThirst = org.shotrush.atom.core.api.player.PlayerDataAPI.getInt(player, "thirst.level", MAX_THIRST);
        
        thirstLevels.put(playerId, savedThirst);
        updateThirstDisplay(player);
        startThirstTickForPlayer(player);
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        int thirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        org.shotrush.atom.core.api.player.PlayerDataAPI.setInt(player, "thirst.level", thirst);
        
        thirstLevels.remove(playerId);
        thirstAccelerationEnd.remove(playerId);
    }
    
    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        
        // Reset thirst to maximum on death
        thirstLevels.put(playerId, MAX_THIRST);
        org.shotrush.atom.core.api.player.PlayerDataAPI.setInt(player, "thirst.level", MAX_THIRST);
    }
    
    private void startThirstTickForPlayer(Player player) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(player, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            
            UUID playerId = player.getUniqueId();
            int currentThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
            
            int tickCounter = thirstTickCounter.getOrDefault(playerId, 0) + 1;
            thirstTickCounter.put(playerId, tickCounter);
            
            if (tickCounter >= THIRST_DRAIN_RATE) {
                thirstTickCounter.put(playerId, 0);
                
                int decreaseAmount = 1;
                
                Long accelerationEnd = thirstAccelerationEnd.get(playerId);
                if (accelerationEnd != null && System.currentTimeMillis() < accelerationEnd) {
                    decreaseAmount = 2;
                } else {
                    thirstAccelerationEnd.remove(playerId);
                }
                
                currentThirst -= decreaseAmount;
                thirstLevels.put(playerId, Math.max(0, currentThirst));
            }
            
            if (currentThirst <= 0) {
                player.setRemainingAir(0);
                
                int damageTicks = thirstDamageTicks.getOrDefault(playerId, 0);
                damageTicks++;
                thirstDamageTicks.put(playerId, damageTicks);
                
                if (damageTicks >= THIRST_DAMAGE_INTERVAL) {
                    player.damage(1.0);
                    thirstDamageTicks.put(playerId, 0);
                }
            } else {
                thirstDamageTicks.remove(playerId);
                
                if (currentThirst <= 5) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0, false, false
                    ));
                }
            }
            
            int targetAir = (currentThirst * 300) / MAX_THIRST;
            if (!player.isInWater() && player.getEyeLocation().getBlock().getType() != Material.WATER) {
                if (player.getRemainingAir() > targetAir) {
                    player.setRemainingAir(targetAir);
                }
            }
            
            checkWaterPurification(player);
            
            updateThirstDisplay(player);
        }, THIRST_DECREASE_INTERVAL, THIRST_DECREASE_INTERVAL);
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onEntityAirChange(org.bukkit.event.entity.EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        int thirst = thirstLevels.getOrDefault(player.getUniqueId(), MAX_THIRST);
        int targetAir = (thirst * 300) / MAX_THIRST;
        
        if (thirst <= 0) {
            event.setCancelled(true);
            return;
        }
        
        if (player.isInWater() || player.getEyeLocation().getBlock().getType() == Material.WATER) {
            if (event.getAmount() > targetAir) {
                event.setAmount(targetAir);
            }
        } else {
            if (event.getAmount() > targetAir) {
                event.setAmount(targetAir);
            }
        }
    }
    
    @EventHandler
    public void onPlayerItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item.getType() == Material.POTION) {
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            if (meta != null) {
                if (meta.hasCustomEffect(PotionEffectType.REGENERATION)) {
                    drinkPurifiedWater(player);
                } else if (meta.getBasePotionType() == org.bukkit.potion.PotionType.WATER) {
                    drinkRawWater(player);
                }
            }
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return;
        
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() == Material.AIR || item.getType() == Material.BUCKET) {
            org.bukkit.util.RayTraceResult result = player.rayTraceBlocks(5, org.bukkit.FluidCollisionMode.ALWAYS);
            if (result != null && result.getHitBlock() != null && isWaterBlock(result.getHitBlock())) {
                drinkRawWater(player);
                event.setCancelled(true);
                return;
            }
            
            if (event.getClickedBlock() != null && isWaterBlock(event.getClickedBlock())) {
                drinkRawWater(player);
                event.setCancelled(true);
                return;
            }
        }
    }
    
    private boolean isWaterBlock(org.bukkit.block.Block block) {
        return block.getType() == Material.WATER || 
               block.getBlockData() instanceof org.bukkit.block.data.Waterlogged waterlogged && waterlogged.isWaterlogged();
    }
    
    private void drinkRawWater(Player player) {
        UUID playerId = player.getUniqueId();
        
        int currentThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        addThirst(player, 5);
        int newThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        int gained = newThirst - currentThirst;
        
        if (gained > 0) {
            ActionBarManager.send(player, "<cyan>+<white>" + gained + "</white> Thirst</cyan>");
        }
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0, false, false));
        
        thirstAccelerationEnd.put(playerId, System.currentTimeMillis() + 30000);
    }
    
    private void drinkPurifiedWater(Player player) {
        UUID playerId = player.getUniqueId();
        
        int currentThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        addThirst(player, 10);
        int newThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        int gained = newThirst - currentThirst;
        
        if (gained > 0) {
            ActionBarManager.send(player, "<cyan>+<white>" + gained + "</white> Thirst</cyan> <gray>(Purified)</gray>");
        }
    }
    
    private void checkWaterPurification(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.POTION) return;
        
        double heat = ItemHeatSystem.getItemHeat(item);
        if (heat < 100.0) return;
        
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null && !meta.hasCustomEffect(PotionEffectType.REGENERATION)) {
            org.shotrush.atom.core.items.CustomItem purifiedWater = 
                org.shotrush.atom.Atom.getInstance().getItemRegistry().getItem("purified_water");
            if (purifiedWater != null) {
                ItemStack newItem = purifiedWater.create();
                ItemHeatSystem.setItemHeat(newItem, heat);
                player.getInventory().setItemInMainHand(newItem);
            }
        }
    }
    
    private void updateThirstDisplay(Player player) {
        int thirst = thirstLevels.getOrDefault(player.getUniqueId(), MAX_THIRST);
        
        player.setMaximumAir(300);
        
        int airValue = (thirst * 300) / MAX_THIRST;
        
        if (player.isInWater() || player.getEyeLocation().getBlock().getType() == Material.WATER) {
            player.setRemainingAir(Math.min(300, airValue));
        } else {
            
            player.setRemainingAir(airValue);
        }
        
    }
    
    public void addThirst(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int currentThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        int newThirst = Math.min(currentThirst + amount, MAX_THIRST);
        thirstLevels.put(playerId, newThirst);
        updateThirstDisplay(player);
    }
    
    public int getThirst(Player player) {
        return thirstLevels.getOrDefault(player.getUniqueId(), MAX_THIRST);
    }
}
