package org.shotrush.atom.core.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CustomItemRegistry {
    private final Plugin plugin;
    private final Map<String, CustomItem> items = new HashMap<>();
    
    public CustomItemRegistry(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void register(CustomItem item) {
        items.put(item.getIdentifier(), item);
        plugin.getLogger().info("Registered custom item: " + item.getIdentifier());
    }
    
    public CustomItem getItem(String identifier) {
        return items.get(identifier);
    }
    
    public CustomItem getItem(ItemStack itemStack) {
        for (CustomItem item : items.values()) {
            if (item.isCustomItem(itemStack)) {
                return item;
            }
        }
        return null;
    }
    
    public ItemStack createItem(String identifier) {
        CustomItem item = items.get(identifier);
        return item != null ? item.create() : null;
    }
}
