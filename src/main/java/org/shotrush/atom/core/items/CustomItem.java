package org.shotrush.atom.core.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public abstract class CustomItem {
    protected final Plugin plugin;
    protected final NamespacedKey key;
    
    public CustomItem(Plugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, getIdentifier());
    }
    
    public abstract String getIdentifier();
    public abstract Material getMaterial();
    public abstract String getDisplayName();
    public abstract List<String> getLore();
    
    public ItemStack create() {
        ItemStack item = new ItemStack(getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(getDisplayName());
            meta.setLore(getLore());
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            
            if (isUnbreakable()) {
                meta.setUnbreakable(true);
            }
            
            applyCustomMeta(meta);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    
    protected boolean isUnbreakable() {
        return false;
    }
    
    protected void applyCustomMeta(ItemMeta meta) {}
    
    public NamespacedKey getKey() {
        return key;
    }
}
