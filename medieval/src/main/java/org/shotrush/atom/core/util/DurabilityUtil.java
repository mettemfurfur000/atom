package org.shotrush.atom.core.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.shotrush.atom.core.items.ItemQuality;

public class DurabilityUtil {

    public static void applyQualityBasedDamage(ItemStack item, ItemQuality quality) {
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) {
            return;
        }
        
        double durabilityMultiplier = quality != null ? quality.getDurabilityMultiplier() : 0.75;
        double damageChance = 1.0 - (durabilityMultiplier * 0.5);
        
        if (Math.random() < damageChance) {
            int currentDamage = damageable.getDamage();
            int maxDurability = item.getType().getMaxDurability();
            
            if (currentDamage + 1 >= maxDurability) {
                item.setAmount(0);
            } else {
                damageable.setDamage(currentDamage + 1);
                item.setItemMeta(damageable);
            }
        }
    }
    

    public static boolean damage(ItemStack item, int amount) {
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) {
            return false;
        }
        
        int currentDamage = damageable.getDamage();
        int maxDurability = item.getType().getMaxDurability();
        
        if (currentDamage + amount >= maxDurability) {
            item.setAmount(0);
            return true;
        } else {
            damageable.setDamage(currentDamage + amount);
            item.setItemMeta(damageable);
            return false;
        }
    }
    

    public static int getRemainingDurability(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) {
            return -1;
        }
        
        int currentDamage = damageable.getDamage();
        int maxDurability = item.getType().getMaxDurability();
        
        return maxDurability - currentDamage;
    }
    

    public static double getDurabilityPercentage(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) {
            return -1;
        }
        
        int currentDamage = damageable.getDamage();
        int maxDurability = item.getType().getMaxDurability();
        
        if (maxDurability == 0) return 1.0;
        
        return 1.0 - ((double) currentDamage / maxDurability);
    }
}
