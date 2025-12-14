package org.shotrush.atom.core.api.item;

import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.core.items.ItemQuality;

import java.util.ArrayList;
import java.util.List;

public class QualityInheritanceAPI {
    
    public static ItemQuality inheritQuality(ItemStack... ingredients) {
        if (ingredients == null || ingredients.length == 0) return null;
        
        List<ItemQuality> qualities = new ArrayList<>();
        
        for (ItemStack ingredient : ingredients) {
            if (ingredient != null) {
                ItemQuality quality = ItemQualityAPI.getQuality(ingredient);
                if (quality != null) {
                    qualities.add(quality);
                }
            }
        }
        
        if (qualities.isEmpty()) return null;
        
        return ItemQuality.average(qualities.toArray(new ItemQuality[0]));
    }
    
    public static void applyInheritedQuality(ItemStack result, ItemStack... ingredients) {
        ItemQuality inherited = inheritQuality(ingredients);
        if (inherited != null) {
            ItemQualityAPI.setQuality(result, inherited);
        }
    }
    
    public static int getModifiedDurability(ItemStack item, int baseDurability) {
        ItemQuality quality = ItemQualityAPI.getQuality(item);
        if (quality == null) return baseDurability;
        
        return (int) (baseDurability * quality.getDurabilityMultiplier());
    }
    
    public static double getModifiedEfficiency(ItemStack item, double baseEfficiency) {
        ItemQuality quality = ItemQualityAPI.getQuality(item);
        if (quality == null) return baseEfficiency;
        
        return baseEfficiency * quality.getEfficiencyMultiplier();
    }
    
    public static int getProcessingCost(ItemStack item, int baseCost) {
        ItemQuality quality = ItemQualityAPI.getQuality(item);
        if (quality == null) return baseCost;
        
        return (int) Math.ceil(baseCost * quality.getProcessingCostMultiplier());
    }
    
    public static int getSmeltingTime(ItemStack item, int baseTime) {
        return getProcessingCost(item, baseTime);
    }
    
    public static int getFoodCost(ItemStack item, int baseFoodCost) {
        return getProcessingCost(item, baseFoodCost);
    }
    
    public static int getThirstCost(ItemStack item, int baseThirstCost) {
        return getProcessingCost(item, baseThirstCost);
    }
    
    public static int getEnergyCost(ItemStack item, int baseEnergyCost) {
        return getProcessingCost(item, baseEnergyCost);
    }
}
