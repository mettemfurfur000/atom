package org.shotrush.atom.content.mobs.ai.debug;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.Optional;

@CommandAlias("mobai")
@Description("Debug commands for mob AI system")
@CommandPermission("atom.debug.mobai")
public class MobAIDebugCommand extends BaseCommand {
    
    private final VisualDebugger visualDebugger;
    private final HerdManager herdManager;
    
    public MobAIDebugCommand(VisualDebugger visualDebugger, HerdManager herdManager) {
        this.visualDebugger = visualDebugger;
        this.herdManager = herdManager;
    }
    
    @Subcommand("debug")
    @Description("Set global debug level")
    @CommandCompletion("OFF|MINIMAL|NORMAL|VERBOSE")
    public void onDebugGlobal(Player player, String levelStr) {
        try {
            DebugLevel level = DebugLevel.valueOf(levelStr.toUpperCase());
            DebugManager.setGlobalLevel(level);
            player.sendMessage(Component.text("Global debug level set to: ", NamedTextColor.GREEN)
                .append(Component.text(level.name(), NamedTextColor.YELLOW)));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid debug level. Use: OFF, MINIMAL, NORMAL, or VERBOSE", NamedTextColor.RED));
        }
    }
    
    @Subcommand("debug")
    @Description("Set category debug level")
    @CommandCompletion("GOALS|NEEDS|MEMORY|COMBAT|SOCIAL|ENVIRONMENTAL OFF|MINIMAL|NORMAL|VERBOSE")
    public void onDebugCategory(Player player, String categoryStr, String levelStr) {
        try {
            DebugCategory category = DebugCategory.valueOf(categoryStr.toUpperCase());
            DebugLevel level = DebugLevel.valueOf(levelStr.toUpperCase());
            DebugManager.setCategoryLevel(category, level);
            player.sendMessage(Component.text("Debug level for ", NamedTextColor.GREEN)
                .append(Component.text(category.getDisplayName(), category.getColor()))
                .append(Component.text(" set to: ", NamedTextColor.GREEN))
                .append(Component.text(level.name(), NamedTextColor.YELLOW)));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid category or level.", NamedTextColor.RED));
        }
    }
    
    @Subcommand("info")
    @Description("Show detailed info about target entity")
    public void onInfo(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Mob mob)) {
            player.sendMessage(Component.text("You must be looking at a mob!", NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("=== Mob AI Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(mob.getType().name(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("ID: ", NamedTextColor.GRAY)
            .append(Component.text("#" + mob.getEntityId(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY)
            .append(Component.text(mob.getUniqueId().toString().substring(0, 8) + "...", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("Health: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f / %.1f", 
                mob.getHealth(), 
                mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()), 
                NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Location: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f, %.1f, %.1f", 
                mob.getLocation().getX(), 
                mob.getLocation().getY(), 
                mob.getLocation().getZ()), 
                NamedTextColor.WHITE)));
        
        if (mob instanceof Animals animal) {
            displayHerdInfo(player, animal);
        }
    }
    
    @Subcommand("goals")
    @Description("List active goals for target entity")
    public void onGoals(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Mob mob)) {
            player.sendMessage(Component.text("You must be looking at a mob!", NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("=== Active Goals ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Goal inspection requires server-side API access.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Enable VERBOSE debugging to see goal activations in real-time.", NamedTextColor.GRAY));
    }
    
    @Subcommand("herd")
    @Description("Show herd info and hierarchy")
    public void onHerd(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Animals animal)) {
            player.sendMessage(Component.text("You must be looking at an animal!", NamedTextColor.RED));
            return;
        }
        
        displayHerdInfo(player, animal);
    }
    
    private void displayHerdInfo(Player player, Animals animal) {
        Optional<Herd> herdOpt = herdManager.getHerd(animal.getUniqueId());
        
        if (herdOpt.isEmpty()) {
            player.sendMessage(Component.text("This animal is not in a herd.", NamedTextColor.YELLOW));
            return;
        }
        
        Herd herd = herdOpt.get();
        boolean isLeader = herd.leader().equals(animal.getUniqueId());
        
        player.sendMessage(Component.text("=== Herd Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Herd Size: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(herd.size()), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Role: ", NamedTextColor.GRAY)
            .append(Component.text(isLeader ? "LEADER" : "FOLLOWER", 
                isLeader ? NamedTextColor.YELLOW : NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Panicking: ", NamedTextColor.GRAY)
            .append(Component.text(herd.isPanicking() ? "Yes" : "No", 
                herd.isPanicking() ? NamedTextColor.RED : NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Herd ID: ", NamedTextColor.GRAY)
            .append(Component.text(herd.id().toString().substring(0, 8) + "...", NamedTextColor.DARK_GRAY)));
    }
    
    @Subcommand("track")
    @Description("Toggle visual tracking for target entity")
    public void onTrack(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Mob mob)) {
            player.sendMessage(Component.text("You must be looking at a mob!", NamedTextColor.RED));
            return;
        }
        
        visualDebugger.toggleTracking(mob.getUniqueId());
        visualDebugger.enableVisualsForPlayer(player.getUniqueId());
        
        boolean isTracking = visualDebugger.isTracking(mob.getUniqueId());
        player.sendMessage(Component.text(
            isTracking ? "Now tracking " : "Stopped tracking ",
            isTracking ? NamedTextColor.GREEN : NamedTextColor.YELLOW
        ).append(Component.text(mob.getType().name() + " #" + mob.getEntityId(), NamedTextColor.WHITE)));
    }
    
    @Subcommand("performance")
    @Description("Show performance statistics")
    public void onPerformance(Player player) {
        PerformanceMonitor.displayStats(player);
    }
    
    @Subcommand("reset")
    @Description("Reset performance metrics")
    public void onReset(Player player) {
        PerformanceMonitor.reset();
        DebugManager.resetPerformanceMetrics();
        player.sendMessage(Component.text("Performance metrics reset.", NamedTextColor.GREEN));
    }
}
