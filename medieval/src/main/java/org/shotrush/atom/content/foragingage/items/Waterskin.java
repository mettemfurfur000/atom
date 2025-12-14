package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.data.PersistentData;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 1)
public class Waterskin extends CustomItem {
    
    private static final int MAX_WATER = 5;
    
    public Waterskin(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "waterskin";
    }
    
    @Override
    public Material getMaterial() {
        return Material.POTION;
    }
    
    @Override
    public String getDisplayName() {
        return "§6Waterskin";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A leather container for water",
            "§7Right-click water to fill",
            "§7Right-click to drink",
            "§8Capacity: 5 uses"
        );
    }
    
    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "waterskin");
        PersistentData.set(meta, "water_amount", 0);
        PersistentData.set(meta, "water_purified", false);
    }
    
    public static int getWaterAmount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return PersistentData.getInt(item.getItemMeta(), "water_amount", 0);
    }
    
    public static boolean isPurified(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return PersistentData.getBoolean(item.getItemMeta(), "water_purified", false);
    }
    
    public static void setWater(ItemStack item, int amount, boolean purified) {
        if (item == null || item.getType() != Material.POTION) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentData.set(meta, "water_amount", Math.min(amount, MAX_WATER));
        PersistentData.set(meta, "water_purified", purified);
        
        updateLore(meta, amount, purified);
        item.setItemMeta(meta);
    }
    
    public static void addWater(ItemStack item, int amount, boolean purified) {
        int current = getWaterAmount(item);
        boolean currentPurified = isPurified(item);
        boolean newPurified = currentPurified || purified;
        setWater(item, current + amount, newPurified);
    }
    
    public static void drinkWater(ItemStack item) {
        int current = getWaterAmount(item);
        if (current > 0) {
            setWater(item, current - 1, isPurified(item));
        }
    }
    
    private static void updateLore(ItemMeta meta, int amount, boolean purified) {
        List<String> lore = new ArrayList<>();
        lore.add("§7A leather container for water");
        lore.add("§7Right-click water to fill");
        lore.add("§7Right-click to drink");
        lore.add("§8Capacity: 5 uses");
        lore.add("");
        lore.add("§7Water: " + amount + "/" + MAX_WATER);
        
        if (purified) {
            lore.add("§aPurified Water");
        } else if (amount > 0) {
            lore.add("§cRaw Water");
        }
        
        meta.setLore(lore);
    }
}
