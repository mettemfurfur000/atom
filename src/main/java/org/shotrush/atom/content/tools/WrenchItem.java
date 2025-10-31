package org.shotrush.atom.content.tools;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 1)
public class WrenchItem extends CustomItem {
    
    public WrenchItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "wrench";
    }
    
    @Override
    public Material getMaterial() {
        return Material.WOODEN_HOE;
    }
    
    @Override
    public String getDisplayName() {
        return "§e🔧 Mechanical Wrench";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A tool for working with blocks",
            "§8• Right-click: Interact",
            "§8• Shift + Right-click: Remove",
            "§8[Engineering Tool]"
        );
    }
    
    @Override
    protected boolean isUnbreakable() {
        return true;
    }
}
