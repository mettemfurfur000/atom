package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 1)
public class PurifiedWater extends CustomItem {
    
    public PurifiedWater(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "purified_water";
    }
    
    @Override
    public Material getMaterial() {
        return Material.POTION;
    }
    
    @Override
    public String getDisplayName() {
        return "§bPurified Water";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7Clean, boiled water",
            "§7Safe to drink",
            "§a+10 Thirst"
        );
    }
    
    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 1, 0), true);
        }
    }
}
