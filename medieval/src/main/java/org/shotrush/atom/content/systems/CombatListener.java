package org.shotrush.atom.content.systems;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;
import org.shotrush.atom.core.util.ActionBarManager;

@RegisterSystem(
        id = "combat_listener",
        priority = 5,
        toggleable = true,
        description = "Handles loss of hunger in combat"
)
public class CombatListener implements Listener {
    
    public CombatListener(Plugin plugin) {
        // Required by SystemRegistry
    }
    
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) {
            if (player.getFoodLevel() <= 0) {
                e.setCancelled(true);
                ActionBarManager.send(player, "You're too hungry to fight!");
                return;
            }
            float currentSaturation = player.getSaturation();
            if (currentSaturation > 0) {
                player.setSaturation(Math.max(0, currentSaturation - 0.5f));
            } else {
                player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1));
            }
        }
        if (e.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                if (shooter.getFoodLevel() <= 0) {
                    e.setCancelled(true);
                    ActionBarManager.send(shooter, "You're too hungry to fight!");
                    return;
                }
                float currentSaturation = shooter.getSaturation();
                if (currentSaturation > 0) {
                    shooter.setSaturation(Math.max(0, currentSaturation - 0.5f));
                } else {
                    shooter.setFoodLevel(Math.max(0, shooter.getFoodLevel() - 1));
                }
            }
        }
    }
}
