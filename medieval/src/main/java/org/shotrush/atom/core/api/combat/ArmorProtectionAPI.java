package org.shotrush.atom.core.api.combat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArmorProtectionAPI {
    
    public static boolean hasLeatherChestplate(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE;
    }
    
    public static double getInsulationValue(Player player) {
        double insulation = 0.0;
        
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        
        if (helmet != null && helmet.getType() == Material.LEATHER_HELMET) insulation += 0.15;
        if (chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE) insulation += 0.30;
        if (leggings != null && leggings.getType() == Material.LEATHER_LEGGINGS) insulation += 0.25;
        if (boots != null && boots.getType() == Material.LEATHER_BOOTS) insulation += 0.15;
        
        if (helmet != null && isMetalArmor(helmet.getType())) insulation += 0.05;
        if (chestplate != null && isMetalArmor(chestplate.getType())) insulation += 0.10;
        if (leggings != null && isMetalArmor(leggings.getType())) insulation += 0.08;
        if (boots != null && isMetalArmor(boots.getType())) insulation += 0.05;
        
        return Math.min(0.85, insulation);
    }
    
    public static boolean isMetalArmor(Material material) {
        return material == Material.IRON_HELMET || material == Material.IRON_CHESTPLATE || 
               material == Material.IRON_LEGGINGS || material == Material.IRON_BOOTS ||
               material == Material.GOLDEN_HELMET || material == Material.GOLDEN_CHESTPLATE ||
               material == Material.GOLDEN_LEGGINGS || material == Material.GOLDEN_BOOTS ||
               material == Material.DIAMOND_HELMET || material == Material.DIAMOND_CHESTPLATE ||
               material == Material.DIAMOND_LEGGINGS || material == Material.DIAMOND_BOOTS ||
               material == Material.NETHERITE_HELMET || material == Material.NETHERITE_CHESTPLATE ||
               material == Material.NETHERITE_LEGGINGS || material == Material.NETHERITE_BOOTS ||
               material == Material.CHAINMAIL_HELMET || material == Material.CHAINMAIL_CHESTPLATE ||
               material == Material.CHAINMAIL_LEGGINGS || material == Material.CHAINMAIL_BOOTS;
    }
    
    public static boolean isWearingFullSet(Player player, Material armorType) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        
        String materialPrefix = armorType.name().replace("_CHESTPLATE", "");
        
        return helmet != null && helmet.getType().name().startsWith(materialPrefix) &&
               chestplate != null && chestplate.getType().name().startsWith(materialPrefix) &&
               leggings != null && leggings.getType().name().startsWith(materialPrefix) &&
               boots != null && boots.getType().name().startsWith(materialPrefix);
    }
}
