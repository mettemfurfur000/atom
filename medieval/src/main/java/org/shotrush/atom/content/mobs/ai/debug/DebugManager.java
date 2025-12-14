package org.shotrush.atom.content.mobs.ai.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DebugManager {
    
    private static DebugLevel globalLevel = DebugLevel.OFF;
    private static final Map<DebugCategory, DebugLevel> categoryLevels = new ConcurrentHashMap<>();
    private static final Map<String, Long> performanceMetrics = new ConcurrentHashMap<>();
    private static final Map<String, Integer> performanceCounts = new ConcurrentHashMap<>();
    private static final Atom plugin = Atom.getInstance();
    
    static {
        for (DebugCategory category : DebugCategory.values()) {
            categoryLevels.put(category, DebugLevel.OFF);
        }
    }
    
    public static void setGlobalLevel(DebugLevel level) {
        globalLevel = level;
        plugin.getLogger().info("Global debug level set to: " + level);
    }
    
    public static void setCategoryLevel(DebugCategory category, DebugLevel level) {
        categoryLevels.put(category, level);
        plugin.getLogger().info("Debug level for " + category.getDisplayName() + " set to: " + level);
    }
    
    public static DebugLevel getGlobalLevel() {
        return globalLevel;
    }
    
    public static DebugLevel getCategoryLevel(DebugCategory category) {
        return categoryLevels.getOrDefault(category, DebugLevel.OFF);
    }
    
    public static boolean isEnabled(DebugCategory category, DebugLevel requiredLevel) {
        DebugLevel catLevel = categoryLevels.get(category);
        if (catLevel != null && catLevel.isAtLeast(requiredLevel)) {
            return true;
        }
        return globalLevel.isAtLeast(requiredLevel);
    }
    
    public static void log(String message, DebugCategory category) {
        log(message, category, DebugLevel.NORMAL);
    }
    
    public static void log(String message, DebugCategory category, DebugLevel level) {
        if (!isEnabled(category, level)) return;
        
        Component msg = Component.text("[MobAI|", NamedTextColor.GRAY)
            .append(Component.text(category.getDisplayName(), category.getColor()))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text(message, NamedTextColor.WHITE));
        
        Bukkit.getConsoleSender().sendMessage(msg);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("atom.debug.view")) {
                player.sendMessage(msg);
            }
        }
    }
    
    public static void logGoalActivation(Mob mob, String goalName, DebugCategory category) {
        if (!isEnabled(category, DebugLevel.NORMAL)) return;
        
        String message = String.format("%s#%d activated %s", 
            mob.getType().name(), 
            mob.getEntityId(),
            goalName);
        
        log(message, category);
    }
    
    public static void logGoalDeactivation(Mob mob, String goalName, DebugCategory category) {
        if (!isEnabled(category, DebugLevel.VERBOSE)) return;
        
        String message = String.format("%s#%d deactivated %s", 
            mob.getType().name(), 
            mob.getEntityId(),
            goalName);
        
        log(message, category, DebugLevel.VERBOSE);
    }
    
    public static void logNeedsChange(Mob mob, String need, double value, String status) {
        if (!isEnabled(DebugCategory.NEEDS, DebugLevel.NORMAL)) return;
        
        String message = String.format("%s#%d %s: %.1f%% (%s)", 
            mob.getType().name(), 
            mob.getEntityId(),
            need,
            value,
            status);
        
        log(message, DebugCategory.NEEDS);
    }
    
    public static void logCriticalNeed(Mob mob, String need, double value) {
        if (!isEnabled(DebugCategory.NEEDS, DebugLevel.MINIMAL)) return;
        
        String message = String.format("%s#%d is %s (%s: %.1f%%)", 
            mob.getType().name(), 
            mob.getEntityId(),
            getUrgencyString(value),
            need,
            value);
        
        log(message, DebugCategory.NEEDS, DebugLevel.MINIMAL);
    }
    
    private static String getUrgencyString(double value) {
        if (value < 15) return "CRITICAL";
        if (value < 30) return "STARVING";
        if (value < 50) return "VERY HUNGRY";
        return "HUNGRY";
    }
    
    public static void logMemory(Mob mob, String memoryType, String details) {
        if (!isEnabled(DebugCategory.MEMORY, DebugLevel.NORMAL)) return;
        
        String message = String.format("%s#%d [%s] %s", 
            mob.getType().name(), 
            mob.getEntityId(),
            memoryType,
            details);
        
        log(message, DebugCategory.MEMORY);
    }
    
    public static void logCombat(Mob mob, String action, String details) {
        if (!isEnabled(DebugCategory.COMBAT, DebugLevel.NORMAL)) return;
        
        String message = String.format("%s#%d %s - %s", 
            mob.getType().name(), 
            mob.getEntityId(),
            action,
            details);
        
        log(message, DebugCategory.COMBAT);
    }
    
    public static void logSocial(Mob mob, String event, String details) {
        if (!isEnabled(DebugCategory.SOCIAL, DebugLevel.NORMAL)) return;
        
        String message = String.format("%s#%d [SOCIAL] %s - %s", 
            mob.getType().name(), 
            mob.getEntityId(),
            event,
            details);
        
        log(message, DebugCategory.SOCIAL);
    }
    
    public static void logEnvironmental(Mob mob, String event, String details) {
        if (!isEnabled(DebugCategory.ENVIRONMENTAL, DebugLevel.NORMAL)) return;
        
        String message = String.format("%s#%d [ENV] %s - %s", 
            mob.getType().name(), 
            mob.getEntityId(),
            event,
            details);
        
        log(message, DebugCategory.ENVIRONMENTAL);
    }
    
    public static void startPerformanceTracking(String operation) {
        if (!globalLevel.isAtLeast(DebugLevel.VERBOSE)) return;
        
        performanceMetrics.put(operation, System.nanoTime());
    }
    
    public static void endPerformanceTracking(String operation) {
        if (!globalLevel.isAtLeast(DebugLevel.VERBOSE)) return;
        
        Long startTime = performanceMetrics.remove(operation);
        if (startTime == null) return;
        
        long duration = System.nanoTime() - startTime;
        double ms = duration / 1_000_000.0;
        
        performanceCounts.merge(operation, 1, Integer::sum);
        
        if (ms > 5.0) {
            log(String.format("PERFORMANCE WARNING: %s took %.2fms", operation, ms), 
                DebugCategory.GOALS, DebugLevel.MINIMAL);
        } else if (globalLevel.isAtLeast(DebugLevel.VERBOSE)) {
            log(String.format("%s completed in %.2fms", operation, ms), 
                DebugCategory.GOALS, DebugLevel.VERBOSE);
        }
    }
    
    public static Map<String, Integer> getPerformanceCounts() {
        return new ConcurrentHashMap<>(performanceCounts);
    }
    
    public static void resetPerformanceMetrics() {
        performanceMetrics.clear();
        performanceCounts.clear();
    }
}
