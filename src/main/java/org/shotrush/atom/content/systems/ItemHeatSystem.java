package org.shotrush.atom.content.systems;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.core.data.PersistentData;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.util.ActionBarManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

@RegisterSystem(
    id = "item_heat_system",
    priority = 3,
    dependencies = {"action_bar_manager"},
    toggleable = true,
    description = "Manages item temperature and heat mechanics"
)
public class ItemHeatSystem implements Listener {
    @Getter
    public static ItemHeatSystem instance;
    private final Atom plugin;
    private static final NamespacedKey HEAT_MODIFIER_KEY = new NamespacedKey("atom", "heat_modifier");
    
    private static final Map<UUID, Map<Integer, Double>> playerItemHeatCache = new HashMap<>();
    private static final Map<UUID, ItemStack> lastKnownItems = new HashMap<>();
    
    public ItemHeatSystem(org.bukkit.plugin.Plugin plugin) {
        this.plugin = (Atom) plugin;
        instance = this;
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startHeatTickForPlayer(player);
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
        if (slotHeatMap != null) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    Double cachedHeat = slotHeatMap.get(slot);
                    if (cachedHeat != null) {
                        setItemHeat(item, cachedHeat);
                    }
                }
            }
        }
        
        
        playerItemHeatCache.remove(playerId);
        lastKnownItems.remove(playerId);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        
        int previousSlot = event.getPreviousSlot();
        ItemStack previousItem = player.getInventory().getItem(previousSlot);
        if (previousItem != null && previousItem.getType() != Material.AIR) {
            UUID playerId = player.getUniqueId();
            Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
            if (slotHeatMap != null) {
                Double cachedHeat = slotHeatMap.get(previousSlot);
                if (cachedHeat != null) {
                    setItemHeat(previousItem, cachedHeat);
                }
            }
        }
        
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item != null && item.getType() != Material.AIR) {
            applyHeatEffect(player, item);
        } else {
            removeHeatEffect(player);
        }
    }
    
    private void startHeatTickForPlayer(Player player) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(player, task -> {
            if (!player.isOnline()) {
                task.cancel();
                playerItemHeatCache.remove(player.getUniqueId());
                lastKnownItems.remove(player.getUniqueId());
                return;
            }
            
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() != Material.AIR) {
                updateItemHeatInCache(player, heldItem);
                applyHeatEffectFromCache(player, heldItem);
                displayHeatActionBarFromCache(player, heldItem);
            }
        }, 1L, 20L);
    }
    
    private void updateItemHeatInCache(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        
        ItemStack lastItem = lastKnownItems.get(playerId);
        if (lastItem == null || !item.isSimilar(lastItem)) {
            
            double heat = getItemHeat(item);
            playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>()).put(slot, heat);
            lastKnownItems.put(playerId, item.clone());
        }
        
        
        double currentHeat = playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>())
            .computeIfAbsent(slot, k -> getItemHeat(item));
        
        org.bukkit.Location loc = player.getLocation();
        
        
        double bodyTemp = 37.0;
        PlayerTemperatureSystem tempSystem = PlayerTemperatureSystem.getInstance();
        if (tempSystem != null) {
            bodyTemp = tempSystem.getPlayerTemperature(player);
        }
        
        
        double targetTemp = bodyTemp - 17.0; 
        
        
        double heatFromSources = org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
            .getNearbyHeatSources(loc, 6);
        targetTemp += heatFromSources * 10;
        
        
        double heatDifference = targetTemp - currentHeat;
        double heatChange = heatDifference * 0.08; 
        double newHeat = currentHeat + heatChange;
        
        newHeat = Math.max(-100, Math.min(500, newHeat));
        
        playerItemHeatCache.get(playerId).put(slot, newHeat);
    }
    
    private void applyHeatEffectFromCache(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        Double heat = playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>())
            .get(slot);
        if (heat == null) {
            heat = getItemHeat(item);
        }
        
        boolean hasProtection = org.shotrush.atom.core.api.combat.ArmorProtectionAPI.hasLeatherChestplate(player);
        
        if (heat != 0) {
            double speedModifier = -Math.abs(heat) * 0.001;
            org.shotrush.atom.core.api.player.AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
        } else {
            org.shotrush.atom.core.api.player.AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            );
        }
        
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyHeatDamage(player, heat, hasProtection);
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyColdDamage(player, heat, hasProtection);
    }
    
    private void displayHeatActionBarFromCache(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        Double heat = playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>())
            .get(slot);
        if (heat == null) {
            heat = getItemHeat(item);
        }
        
        ActionBarManager manager = ActionBarManager.getInstance();
        if (manager == null) return;
        
        
        if (Math.abs(heat) < 5.0) {
            manager.removeMessage(player, "item_heat");
            return;
        }
        
        String color;
        String descriptor;
        if (heat > 200) {
            color = "§4";
            descriptor = "Burning";
        } else if (heat > 100) {
            color = "§c";
            descriptor = "Very Hot";
        } else if (heat > 50) {
            color = "§6";
            descriptor = "Hot";
        } else if (heat > 20) {
            color = "§e";
            descriptor = "Warm";
        } else if (heat < -50) {
            color = "§b";
            descriptor = "Freezing";
        } else if (heat < -20) {
            color = "§3";
            descriptor = "Cold";
        } else {
            color = "§7";
            descriptor = "Cool";
        }
        
        String message = String.format("§7Item: %s%s §7(%.0f°C)", color, descriptor, heat);
        manager.setMessage(player, "item_heat", message);
    }
    
    private void displayHeatActionBar(Player player, ItemStack item) {
        double heat = getItemHeat(item);
        
        ActionBarManager manager = ActionBarManager.getInstance();
        if (manager == null) return;
        
        
        if (Math.abs(heat) < 5.0) {
            manager.removeMessage(player, "item_heat");
            return;
        }
        
        String color;
        String descriptor;
        if (heat > 200) {
            color = "§4";
            descriptor = "Burning";
        } else if (heat > 100) {
            color = "§c";
            descriptor = "Very Hot";
        } else if (heat > 50) {
            color = "§6";
            descriptor = "Hot";
        } else if (heat > 20) {
            color = "§e";
            descriptor = "Warm";
        } else if (heat < -50) {
            color = "§b";
            descriptor = "Freezing";
        } else if (heat < -20) {
            color = "§3";
            descriptor = "Cold";
        } else {
            color = "§7";
            descriptor = "Cool";
        }
        
        String message = String.format("§7Item: %s%s §7(%.0f°C)", color, descriptor, heat);
        manager.setMessage(player, "item_heat", message);
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();
        ItemStack item = droppedItem.getItemStack();
        
        saveCachedHeatToItem(player, item);
        droppedItem.setItemStack(item);
        
        double heat = getItemHeat(item);

        if (heat > 200.0) {
            startDroppedItemFireTracking(droppedItem);
        }
        
        if (heat >= 50) {
            ItemStack chestplate = player.getInventory().getChestplate();
            boolean hasProtection = chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE;
            
            if (!hasProtection) {
                player.setFireTicks(40); 
            }
        }
        
        startDroppedItemHeatTracking(droppedItem);
    }
    
    private void startDroppedItemHeatTracking(Item droppedItem) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(droppedItem, task -> {
            if (droppedItem.isDead() || !droppedItem.isValid()) {
                task.cancel();
                return;
            }
            
            ItemStack itemStack = droppedItem.getItemStack();
            double currentHeat = getItemHeat(itemStack);
            org.bukkit.Location loc = droppedItem.getLocation();
            
            double heatChange = org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
                .getNearbyHeatSources(loc, 6);
            
            double ambientTemp = 20.0;
            if (currentHeat > ambientTemp) {
                heatChange -= 0.5;
            }
            
            double newHeat = Math.max(0, currentHeat + heatChange * 0.1);
            setItemHeat(itemStack, newHeat);
            droppedItem.setItemStack(itemStack);
            
            if (newHeat >= 200) {
                double fireChance = Math.min(0.5, (newHeat - 200) / 600);
                
                if (Math.random() < fireChance) {
                    org.bukkit.block.Block below = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                    if (below.getType().isBurnable() || below.getType() == Material.AIR) {
                        loc.getBlock().setType(Material.FIRE);
                    }
                }
            }
        }, 20L, 20L);
    }
    
    private void updateItemHeatFromEnvironment(Player player, ItemStack item) {
        double currentHeat = getItemHeat(item);
        org.bukkit.Location loc = player.getLocation();
        
        
        double tempChange = 0.0;
        
        
        org.bukkit.block.Biome biome = loc.getBlock().getBiome();
        tempChange += org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getBiomeTemperature(biome);
        tempChange += org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getDayNightModifier(loc.getWorld());
        tempChange += org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getLightLevelModifier(loc);
        tempChange += org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getWaterIceModifier(player, loc);
        tempChange += org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getNearbyHeatSources(loc, 5);
        
        
        double targetTemp = 20.0 + (tempChange * 2.0);
        
        
        double heatDifference = targetTemp - currentHeat;
        double heatChange = heatDifference * 0.08; 
        
        double newHeat = currentHeat + heatChange;
        
        
        newHeat = Math.max(-150, Math.min(600, newHeat));
        
        
        if (Math.abs(newHeat - currentHeat) > 0.5) {
            setItemHeat(item, newHeat);
        }
    }
    
    private void applyHeatEffect(Player player, ItemStack item) {
        double heat = getItemHeat(item);
        boolean hasProtection = org.shotrush.atom.core.api.combat.ArmorProtectionAPI.hasLeatherChestplate(player);
        
        if (heat != 0) {
            double speedModifier = -Math.abs(heat) * 0.001;
            org.shotrush.atom.core.api.player.AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
        } else {
            org.shotrush.atom.core.api.player.AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            );
        }
        
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyHeatDamage(player, heat, hasProtection);
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyColdDamage(player, heat, hasProtection);
    }
    
    private void removeHeatEffect(Player player) {
        org.shotrush.atom.core.api.player.AttributeModifierAPI.removeModifier(
            player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
        );
    }
    
    public static double getItemHeat(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        
        return PersistentData.getDouble(item.getItemMeta(), "item_heat", 0.0);
    }
    
    public static void setItemHeat(ItemStack item, double heat) {
        if (item == null || item.getType() == Material.AIR) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentData.set(meta, "item_heat", heat);
        item.setItemMeta(meta);
    }
    
    
    private void saveCachedHeatToItem(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
        if (slotHeatMap != null) {
            Double cachedHeat = slotHeatMap.get(slot);
            if (cachedHeat != null) {
                setItemHeat(item, cachedHeat);
            }
        }
    }
    
    public void saveHeatForSlot(Player player, int slot, ItemStack item) {
        UUID playerId = player.getUniqueId();
        Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
        if (slotHeatMap != null) {
            Double cachedHeat = slotHeatMap.get(slot);
            if (cachedHeat != null) {
                setItemHeat(item, cachedHeat);
            }
        }
    }
    
    public static void startItemDisplayHeatTracking(org.bukkit.entity.ItemDisplay itemDisplay) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(itemDisplay, task -> {
            if (itemDisplay.isDead() || !itemDisplay.isValid()) {
                task.cancel();
                return;
            }
            
            ItemStack itemStack = itemDisplay.getItemStack();
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                task.cancel();
                return;
            }
            
            double currentHeat = getItemHeat(itemStack);
            org.bukkit.Location loc = itemDisplay.getLocation();
            
            double heatFromSources = org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
                .getNearbyHeatSources(loc, 6);
            
            double ambientTemp = 20.0;
            double targetTemp = ambientTemp + (heatFromSources * 10);
            
            double heatDifference = targetTemp - currentHeat;
            double heatChange = heatDifference * 0.05;
            double newHeat = currentHeat + heatChange;
            
            newHeat = Math.max(-100, Math.min(500, newHeat));
            
            if (Math.abs(newHeat - currentHeat) > 0.5) {
                setItemHeat(itemStack, newHeat);
                itemDisplay.setItemStack(itemStack);
            }
        }, 20L, 20L);
    }
    
    
    public static void startDroppedItemFireTracking(org.bukkit.entity.Item item) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(item, task -> {
            if (item.isDead() || !item.isValid()) {
                task.cancel();
                return;
            }
            
            ItemStack itemStack = item.getItemStack();
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                task.cancel();
                return;
            }
            
            double currentHeat = getItemHeat(itemStack);
            
            
            if (currentHeat > 200.0) {
                igniteNearbyBlocks(item.getLocation(), currentHeat);
            }
            
            
            double ambientTemp = 20.0;
            double heatDifference = ambientTemp - currentHeat;
            double heatChange = heatDifference * 0.02; 
            double newHeat = currentHeat + heatChange;
            
            newHeat = Math.max(-100, Math.min(500, newHeat));
            
            if (Math.abs(newHeat - currentHeat) > 0.5) {
                setItemHeat(itemStack, newHeat);
                item.setItemStack(itemStack);
            }
        }, 10L, 10L); 
    }
    
    
    private static void igniteNearbyBlocks(org.bukkit.Location location, double temperature) {
        if (location.getWorld() == null) return;
        
        
        int radius = (int) Math.min(3, (temperature - 200) / 100);
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.Location checkLoc = location.clone().add(x, y, z);
                    org.bukkit.block.Block block = checkLoc.getBlock();
                    
                    
                    if (isFlammable(block) && block.getRelative(org.bukkit.block.BlockFace.UP).getType() == Material.AIR) {
                        
                        double igniteChance = Math.min(0.3, (temperature - 200) / 1000);
                        if (Math.random() < igniteChance) {
                            block.getRelative(org.bukkit.block.BlockFace.UP).setType(Material.FIRE);
                        }
                    }
                }
            }
        }
    }
    
    
    private static boolean isFlammable(org.bukkit.block.Block block) {
        Material type = block.getType();
        return type == Material.OAK_PLANKS || type == Material.SPRUCE_PLANKS ||
               type == Material.BIRCH_PLANKS || type == Material.JUNGLE_PLANKS ||
               type == Material.ACACIA_PLANKS || type == Material.DARK_OAK_PLANKS ||
               type == Material.MANGROVE_PLANKS || type == Material.CHERRY_PLANKS ||
               type == Material.BAMBOO_PLANKS || type == Material.CRIMSON_PLANKS ||
               type == Material.WARPED_PLANKS ||
               type == Material.OAK_LOG || type == Material.SPRUCE_LOG ||
               type == Material.BIRCH_LOG || type == Material.JUNGLE_LOG ||
               type == Material.ACACIA_LOG || type == Material.DARK_OAK_LOG ||
               type == Material.MANGROVE_LOG || type == Material.CHERRY_LOG ||
               type == Material.OAK_LEAVES || type == Material.SPRUCE_LEAVES ||
               type == Material.BIRCH_LEAVES || type == Material.JUNGLE_LEAVES ||
               type == Material.ACACIA_LEAVES || type == Material.DARK_OAK_LEAVES ||
               type == Material.MANGROVE_LEAVES || type == Material.CHERRY_LEAVES ||
               type == Material.AZALEA_LEAVES || type == Material.FLOWERING_AZALEA_LEAVES ||
               type == Material.GRASS_BLOCK || type == Material.TALL_GRASS ||
               type == Material.FERN || type == Material.LARGE_FERN ||
               type == Material.DEAD_BUSH || type == Material.DANDELION ||
               type == Material.POPPY || type == Material.BLUE_ORCHID ||
               type.name().endsWith("_WOOL") || type.name().endsWith("_CARPET") ||
               type == Material.HAY_BLOCK || type == Material.DRIED_KELP_BLOCK ||
               type == Material.TNT || type == Material.BOOKSHELF;
    }
}
