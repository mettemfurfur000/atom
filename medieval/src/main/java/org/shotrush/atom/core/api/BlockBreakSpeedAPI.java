package org.shotrush.atom.core.api;

import lombok.Getter;
import lombok.Setter;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;

import java.util.HashMap;
import java.util.Map;

public class BlockBreakSpeedAPI implements Listener {
    
    private static BlockBreakSpeedAPI instance;
    @Getter
    @Setter
    private static double globalMultiplier = 4.0;
    private static final Map<Material, Double> blockMultipliers = new HashMap<>();
    private static final Map<String, Double> categoryMultipliers = new HashMap<>();
    private static final String MODIFIER_KEY = "block_break_speed";
    
    
    private BlockBreakSpeedAPI() {}
    
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new BlockBreakSpeedAPI();
            plugin.getServer().getPluginManager().registerEvents(instance, plugin);
        }
    }
    

    public static void setBlockMultiplier(Material material, double multiplier) {
        if (multiplier == 1.0) {
            blockMultipliers.remove(material);
        } else {
            blockMultipliers.put(material, multiplier);
        }
    }
    
    
    public static void setCategoryMultiplier(String category, double multiplier) {
        if (multiplier == 1.0) {
            categoryMultipliers.remove(category.toUpperCase());
        } else {
            categoryMultipliers.put(category.toUpperCase(), multiplier);
        }
    }
    
    
    public static void removeBlockMultiplier(Material material) {
        blockMultipliers.remove(material);
    }
    
    
    public static void removeCategoryMultiplier(String category) {
        categoryMultipliers.remove(category.toUpperCase());
    }
    
    
    public static void clearAllMultipliers() {
        blockMultipliers.clear();
        categoryMultipliers.clear();
        globalMultiplier = 1.0;
    }
    
    
    public static double getFinalMultiplier(Material material) {
        double multiplier = globalMultiplier;
        
        
        if (blockMultipliers.containsKey(material)) {
            multiplier *= blockMultipliers.get(material);
        } else {
            
            String name = material.name();
            for (Map.Entry<String, Double> entry : categoryMultipliers.entrySet()) {
                if (name.contains(entry.getKey())) {
                    multiplier *= entry.getValue();
                    break;
                }
            }
        }
        
        return multiplier;
    }
    
    public static class Builder {
        private double global = 1.0;
        private final Map<Material, Double> blocks = new HashMap<>();
        private final Map<String, Double> categories = new HashMap<>();
        
        public Builder global(double multiplier) {
            this.global = multiplier;
            return this;
        }
        
        public Builder block(Material material, double multiplier) {
            blocks.put(material, multiplier);
            return this;
        }
        
        public Builder category(String pattern, double multiplier) {
            categories.put(pattern.toUpperCase(), multiplier);
            return this;
        }
        
        
        public void apply() {
            setGlobalMultiplier(global);
            blocks.forEach(BlockBreakSpeedAPI::setBlockMultiplier);
            categories.forEach(BlockBreakSpeedAPI::setCategoryMultiplier);
        }
    }
    
    
    public static Builder builder() {
        return new Builder();
    }
    
    
    
    @EventHandler(priority = EventPriority.LOWEST)
    private void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        clearSpeedModifier(player);
        if (CraftEngineBlocks.getCustomBlockState(block) != null) {
            return;
        }
        applySpeedModifier(player, block.getType());
    }
    
    @EventHandler
    private void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        
        
        clearSpeedModifier(player);
    }
    
    private void applySpeedModifier(Player player, Material blockType) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return;
        
        
        clearSpeedModifier(player);
        
        
        double multiplier = getFinalMultiplier(blockType);
        if (multiplier == 1.0) return;
        
        
        
        
        
        double attributeValue = (1.0 / multiplier) - 1.0;
        
        
        NamespacedKey key = new NamespacedKey(Atom.getInstance(), MODIFIER_KEY);
        AttributeModifier modifier = new AttributeModifier(
            key,
            attributeValue,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.ANY
        );
        
        attribute.addModifier(modifier);
    }
    
    private void clearSpeedModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return;
        
        attribute.getModifiers().stream()
            .filter(mod -> mod.getKey().getKey().equals(MODIFIER_KEY))
            .forEach(attribute::removeModifier);
    }
}
