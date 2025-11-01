package org.shotrush.atom.core.skin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.shotrush.atom.Atom;

import java.util.Objects;

public class SkinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Atom")),
            task -> {
                String configuredSkin = Atom.getInstance().getDataStorage()
                    .getServerData().getString("default_skin_username");
                
                if (configuredSkin != null && !configuredSkin.isEmpty()) {
                    SkinAPI.setSkinFromUsername(player, configuredSkin);
                } else {
                    SkinAPI.setDefaultSkin(player);
                }
            },
            null,
            20L
        );
    }
}
