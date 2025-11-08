package org.shotrush.atom.core.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.data.PersistentData;

import java.util.List;

@Deprecated(forRemoval = true)
public abstract class CustomItem {
    protected final Plugin plugin;
    
    public CustomItem(Plugin plugin) {
        this.plugin = plugin;
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
            PersistentData.flag(meta, getIdentifier());
            
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
        return PersistentData.isFlagged(item.getItemMeta(), getIdentifier());
    }
    
    protected boolean isUnbreakable() {
        return false;
    }
    
    protected void applyCustomMeta(ItemMeta meta) {}
}
