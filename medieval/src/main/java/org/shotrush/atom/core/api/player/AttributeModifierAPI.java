package org.shotrush.atom.core.api.player;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttributeModifierAPI {
    
    public static void applyModifier(Player player, Attribute attribute, NamespacedKey key, 
                                    double value, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        removeModifier(player, attribute, key);
        
        AttributeModifier modifier = new AttributeModifier(key, value, operation);
        instance.addModifier(modifier);
    }
    
    public static void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        instance.getModifiers().stream()
            .filter(mod -> mod.getKey().equals(key))
            .forEach(instance::removeModifier);
    }
    
    public static boolean hasModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return false;
        
        return instance.getModifiers().stream()
            .anyMatch(mod -> mod.getKey().equals(key));
    }
    
    public static void removeAllModifiers(Player player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        instance.getModifiers().forEach(instance::removeModifier);
    }
}
