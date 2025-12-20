package org.shotrush.atom.core.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.shotrush.atom.core.data.PersistentData;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        resetPlayerDataOnDeath(player);
    }

    private void resetPlayerDataOnDeath(Player player) {
        // Reset thirst
        PersistentData.remove(player, "thirst.level");
        
        // Reset temperature
        PersistentData.remove(player, "temperature.body");
        
        // Keep age_announcement.last_seen_age as it's not death-related
        // PersistentData.remove(player, "age_announcement.last_seen_age");
    }
}