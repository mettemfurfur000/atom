package org.shotrush.atom.content.systems;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

@RegisterSystem(
    id = "player_attribute_modifier",
    priority = 1,
    toggleable = false,
    description = "Modifies player attributes on join"
)
public class PlayerAttributeModifier implements Listener {
    
    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("atom", "default_modifier");
    
    public PlayerAttributeModifier(Plugin plugin) {
        
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyAttributeModifiers(player);
    }
    
    public static void applyAttributeModifiers(Player player) {
        modifyAttribute(player, Attribute.BLOCK_BREAK_SPEED, -0.767, AttributeModifier.Operation.MULTIPLY_SCALAR_1); 
        modifyAttribute(player, Attribute.BLOCK_INTERACTION_RANGE, -1.0, AttributeModifier.Operation.ADD_NUMBER);
        modifyAttribute(player, Attribute.ENTITY_INTERACTION_RANGE, -1.0, AttributeModifier.Operation.ADD_NUMBER);
        modifyAttribute(player, Attribute.MOVEMENT_SPEED, -0.2, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        modifyAttribute(player, Attribute.ATTACK_SPEED, -0.3, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        modifyAttribute(player, Attribute.ATTACK_DAMAGE, -0.25, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }
    
    private static void modifyAttribute(Player player, Attribute attribute, double value, AttributeModifier.Operation operation) {
        org.shotrush.atom.core.api.player.AttributeModifierAPI.applyModifier(player, attribute, MODIFIER_KEY, value, operation);
    }
}
