package org.shotrush.atom.core.util;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RegisterSystem(
    id = "action_bar_manager",
    priority = 0,
    toggleable = false,
    description = "Manages action bar messages for players"
)
public class ActionBarManager {
    
    @Getter
    private static ActionBarManager instance;
    private final Plugin plugin;
    private final Map<UUID, Map<String, String>> playerMessages = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> scheduledTasks = new HashMap<>();
    
    public ActionBarManager(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        startActionBarTick();
    }

    public void setMessage(Player player, String key, String message) {
        playerMessages.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
            .put(key, message);
    }
    
    public void removeMessage(Player player, String key) {
        Map<String, String> messages = playerMessages.get(player.getUniqueId());
        if (messages != null) {
            messages.remove(key);
        }
    }
    
    public void clearMessages(Player player) {
        playerMessages.remove(player.getUniqueId());
    }

    public static void send(Player player, String message) {
        send(player, message, 3);
    }
    public static void send(Player player, String key, String message) {
        send(player, key, message, 3);
    }

    public static void send(Player player, String message, int durationSeconds) {
        if (instance == null) return;

        String tempKey = "temp_" + System.currentTimeMillis() + "_" + message.hashCode();
        send(player, tempKey, message, durationSeconds);
    }

    public static void send(Player player, String key, String message, int durationSeconds) {
        if (instance == null) return;

        instance.setMessage(player, key, message);

        ScheduledTask task = org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(player.getLocation(), () -> {
            instance.removeMessage(player, key);
        }, durationSeconds * 20L);
        ScheduledTask old = instance.scheduledTasks.put(key, task);
        if(old != null) old.cancel();
    }

    
    public static void sendStatus(Player player, String message) {
        if (instance == null) return;
        
        instance.setMessage(player, "status", message);
    }
    
    public static void clearStatus(Player player) {
        if (instance == null) return;
        instance.removeMessage(player, "status");
    }
    
    private void startActionBarTick() {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTaskTimer(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                updateActionBar(player);
            }
        }, 1L, 5L);
    }
    
    private void updateActionBar(Player player) {
        Map<String, String> messages = playerMessages.get(player.getUniqueId());
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        List<String> orderedMessages = new ArrayList<>();

        if (messages.containsKey("status")) {
            orderedMessages.add(messages.get("status"));
        }

        Set<String> uniqueTempMessages = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            if (entry.getKey().startsWith("temp_")) {
                uniqueTempMessages.add(entry.getValue());
            }
        }
        
        
        if (!uniqueTempMessages.isEmpty()) {
            if (uniqueTempMessages.size() == 1) {
                orderedMessages.add(uniqueTempMessages.iterator().next());
            } else {
                
                orderedMessages.add(String.join(" <dark_gray>•</dark_gray> ", uniqueTempMessages));
            }
        }
        
        
        if (messages.containsKey("body_temp")) {
            orderedMessages.add(messages.get("body_temp"));
        }
        if (messages.containsKey("item_heat")) {
            orderedMessages.add(messages.get("item_heat"));
        }
        if (messages.containsKey("thirst")) {
            orderedMessages.add(messages.get("thirst"));
        }
        
        
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("body_temp") && !key.equals("item_heat") && !key.equals("thirst") 
                && !key.equals("status") && !key.startsWith("temp_")) {
                orderedMessages.add(entry.getValue());
            }
        }
        
        if (!orderedMessages.isEmpty()) {
            String combined = String.join(" <dark_gray>|</dark_gray> ", orderedMessages);
            player.sendActionBar(MiniMessage.miniMessage().deserialize(combined));
        }
    }
}
