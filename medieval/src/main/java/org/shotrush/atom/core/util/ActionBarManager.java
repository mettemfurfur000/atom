package org.shotrush.atom.core.util;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    public static ActionBarManager getInstance() {
        return instance;
    }

    private static ActionBarManager instance;
    private final Plugin plugin;
    private final Map<UUID, Map<String, String>> playerMessages = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> scheduledTasks = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.legacySection();

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

    /**
     * Parses a message that may contain either MiniMessage format or traditional color codes
     * @param message The message to parse
     * @return Component with proper formatting applied
     */
    private Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // Check if message contains § (section sign) - legacy Bukkit color codes
        if (message.contains("§")) {
            return sectionSerializer.deserialize(message);
        }

        // Check if message contains & - legacy color codes with ampersand
        if (message.contains("&")) {
            return legacySerializer.deserialize(message);
        }

        // Check if message contains MiniMessage tags (< and >)
        if (message.contains("<") && message.contains(">")) {
            try {
                return miniMessage.deserialize(message);
            } catch (Exception e) {
                // If MiniMessage parsing fails, treat as plain text
                return Component.text(message);
            }
        }

        // No formatting detected, treat as plain text
        return Component.text(message);
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

        List<Component> orderedComponents = new ArrayList<>();
        Component separator = miniMessage.deserialize(" <dark_gray>|</dark_gray> ");
        Component bulletSeparator = miniMessage.deserialize(" <dark_gray>•</dark_gray> ");

        // Add status message if present
        if (messages.containsKey("status")) {
            orderedComponents.add(parseMessage(messages.get("status")));
        }

        // Collect unique temp messages
        Set<String> uniqueTempMessages = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            if (entry.getKey().startsWith("temp_")) {
                uniqueTempMessages.add(entry.getValue());
            }
        }

        // Add temp messages
        if (!uniqueTempMessages.isEmpty()) {
            if (uniqueTempMessages.size() == 1) {
                orderedComponents.add(parseMessage(uniqueTempMessages.iterator().next()));
            } else {
                // Combine multiple temp messages with bullet separator
                Component combined = Component.empty();
                Iterator<String> iterator = uniqueTempMessages.iterator();
                while (iterator.hasNext()) {
                    combined = combined.append(parseMessage(iterator.next()));
                    if (iterator.hasNext()) {
                        combined = combined.append(bulletSeparator);
                    }
                }
                orderedComponents.add(combined);
            }
        }

        // Add specific stat messages in order
        if (messages.containsKey("body_temp")) {
            orderedComponents.add(parseMessage(messages.get("body_temp")));
        }
        if (messages.containsKey("item_heat")) {
            orderedComponents.add(parseMessage(messages.get("item_heat")));
        }
        if (messages.containsKey("thirst")) {
            orderedComponents.add(parseMessage(messages.get("thirst")));
        }

        // Add all other messages
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("body_temp") && !key.equals("item_heat") && !key.equals("thirst")
                    && !key.equals("status") && !key.startsWith("temp_")) {
                orderedComponents.add(parseMessage(entry.getValue()));
            }
        }

        // Combine all components with separator
        if (!orderedComponents.isEmpty()) {
            Component finalComponent = orderedComponents.get(0);
            for (int i = 1; i < orderedComponents.size(); i++) {
                finalComponent = finalComponent.append(separator).append(orderedComponents.get(i));
            }
            player.sendActionBar(finalComponent);
        }
    }
}