package org.shotrush.atom.core.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

@Deprecated(forRemoval = true)
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
    
    public CustomItem getCustomItem(ItemStack itemStack) {
        return getItem(itemStack);
    }
    
    public ItemStack createItem(String identifier) {
        CustomItem item = items.get(identifier);
        return item != null ? item.create() : null;
    }
    
    public Collection<CustomItem> getAllItems() {
        return items.values();
    }
    
    public Set<String> getAllIdentifiers() {
        return items.keySet();
    }
}
