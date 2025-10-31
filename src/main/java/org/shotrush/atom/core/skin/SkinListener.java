package org.shotrush.atom.core.skin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SkinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SkinAPI.setDefaultBlackSkin(event.getPlayer());
    }
}
