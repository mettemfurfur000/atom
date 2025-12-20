package org.shotrush.atom.core.util;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RegisterSystem(
    id = "right_click_detector",
    priority = 0,
    toggleable = false,
    description = "Detects and tracks player right-click events"
)
public class RightClickDetector implements Listener {
    
    public RightClickDetector(Plugin plugin) {
        
    }
    
    private static class ClickData {
        long lastAllowTime = 0;
        long lastClickTime = 0;
    }
    
    private static final Map<UUID, ClickData> clickData = new ConcurrentHashMap<>();
    private static final long ALLOW_INTERVAL_MS = 1000;
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick()) {
            UUID playerId = event.getPlayer().getUniqueId();
            ClickData data = clickData.computeIfAbsent(playerId, k -> new ClickData());
            data.lastClickTime = System.currentTimeMillis();
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clickData.remove(event.getPlayer().getUniqueId());
    }
    
    public static boolean isRightClicking(UUID playerId) {
        ClickData data = clickData.get(playerId);
        if (data == null) return false;
        
        long currentTime = System.currentTimeMillis();
        return currentTime - data.lastClickTime <= 1500;
    }
    
    public static void clear(UUID playerId) {
        clickData.remove(playerId);
    }
}
