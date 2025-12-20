package org.shotrush.atom.core.skin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.util.Objects;

//@RegisterSystem(
//    id = "skin_listener",
//    priority = 1,
//    toggleable = true,
//    description = "Applies default skins to players on join"
//)
public class SkinListener implements Listener {
    
    public SkinListener(Plugin plugin) {
        
    }
    
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
