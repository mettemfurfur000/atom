package org.shotrush.atom.content.systems;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.item.ItemQualityAPI;
import org.shotrush.atom.core.api.item.QualityInheritanceAPI;
import org.shotrush.atom.core.items.ItemQuality;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

@RegisterSystem(
    id = "quality_inheritance_listener",
    priority = 5,
    toggleable = true,
    description = "Handles item quality inheritance in crafting"
)
public class QualityInheritanceListener implements Listener {
    
    public QualityInheritanceListener(Plugin plugin) {
        
    }
    
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        
        if (result == null) return;
        
        ItemStack[] matrix = inventory.getMatrix();
        ItemQuality inherited = QualityInheritanceAPI.inheritQuality(matrix);
        
        if (inherited != null) {
            ItemStack modifiedResult = result.clone();
            ItemQualityAPI.setQuality(modifiedResult, inherited);
            inventory.setResult(modifiedResult);
        }
    }
    
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return;
        
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        
        QualityInheritanceAPI.applyInheritedQuality(result, matrix);
    }
}
