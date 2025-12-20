package org.shotrush.atom.core.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

public class ItemUtil {
    
    public static ItemStack createItemWithCustomModel(Material material, String modelName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setCustomModelName(meta, modelName);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static void setCustomModelName(ItemMeta meta, String modelName) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setStrings(java.util.List.of(modelName));
        meta.setCustomModelDataComponent(component);
    }
    
    public static void setItemModelAndCustomName(ItemMeta meta, NamespacedKey baseModel, String customModelName) {
        meta.setItemModel(baseModel);
        setCustomModelName(meta, customModelName);
    }
}
