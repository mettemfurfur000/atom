package org.shotrush.atom.content.mobs.ai.debug;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.util.ActionBarManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisualDebugger {
    
    private final Atom plugin;
    private final Map<UUID, TrackedMob> trackedMobs;
    private final Set<UUID> trackingPlayers;
    
    public enum GoalState {
        AGGRESSIVE(Color.RED, "Combat"),
        PEACEFUL(Color.GREEN, "Peaceful"),
        NEEDS_DRIVEN(Color.BLUE, "Needs"),
        ENVIRONMENTAL(Color.YELLOW, "Environmental"),
        SOCIAL(Color.fromRGB(170, 85, 255), "Social"),
        IDLE(Color.WHITE, "Idle");
        
        private final Color color;
        private final String displayName;
        
        GoalState(Color color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
        
        public Color getColor() {
            return color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private static class TrackedMob {
        UUID mobId;
        String currentGoal;
        GoalState state;
        double hunger;
        double thirst;
        double energy;
        BossBar bossBar;
        
        TrackedMob(UUID mobId) {
            this.mobId = mobId;
            this.currentGoal = "Idle";
            this.state = GoalState.IDLE;
            this.hunger = 100;
            this.thirst = 100;
            this.energy = 100;
        }
    }
    
    public VisualDebugger(Atom plugin) {
        this.plugin = plugin;
        this.trackedMobs = new ConcurrentHashMap<>();
        this.trackingPlayers = ConcurrentHashMap.newKeySet();
        startVisualUpdateTask();
    }
    
    public void trackMob(UUID mobId) {
        trackedMobs.computeIfAbsent(mobId, TrackedMob::new);
    }
    
    public void untrackMob(UUID mobId) {
        TrackedMob tracked = trackedMobs.remove(mobId);
        if (tracked != null && tracked.bossBar != null) {
            for (UUID playerId : trackingPlayers) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    player.hideBossBar(tracked.bossBar);
                }
            }
        }
    }
    
    public boolean isTracking(UUID mobId) {
        return trackedMobs.containsKey(mobId);
    }
    
    public void toggleTracking(UUID mobId) {
        if (isTracking(mobId)) {
            untrackMob(mobId);
        } else {
            trackMob(mobId);
        }
    }
    
    public void enableVisualsForPlayer(UUID playerId) {
        trackingPlayers.add(playerId);
    }
    
    public void disableVisualsForPlayer(UUID playerId) {
        trackingPlayers.remove(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            for (TrackedMob tracked : trackedMobs.values()) {
                if (tracked.bossBar != null) {
                    player.hideBossBar(tracked.bossBar);
                }
            }
        }
    }
    
    public void updateMobState(UUID mobId, String goalName, GoalState state) {
        TrackedMob tracked = trackedMobs.get(mobId);
        if (tracked == null) return;
        
        tracked.currentGoal = goalName;
        tracked.state = state;
    }
    
    public void updateMobNeeds(UUID mobId, double hunger, double thirst, double energy) {
        TrackedMob tracked = trackedMobs.get(mobId);
        if (tracked == null) return;
        
        tracked.hunger = hunger;
        tracked.thirst = thirst;
        tracked.energy = energy;
    }
    
    private void startVisualUpdateTask() {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTaskTimer(() -> {
            for (TrackedMob tracked : trackedMobs.values()) {
                Mob mob = (Mob) plugin.getServer().getEntity(tracked.mobId);
                if (mob == null || !mob.isValid()) {
                    untrackMob(tracked.mobId);
                    continue;
                }
                
                updateVisuals(mob, tracked);
            }
        }, 5L, 5L);
    }
    
    private void updateVisuals(Mob mob, TrackedMob tracked) {
        Location loc = mob.getLocation().clone().add(0, mob.getHeight() + 0.5, 0);
        
        if (loc.getWorld() != null) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(
                    tracked.state.getColor().getRed(),
                    tracked.state.getColor().getGreen(),
                    tracked.state.getColor().getBlue()
                ), 
                1.0f
            );
            loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.2, 0.2, 0.2, 0, dustOptions);
        }
        
        for (UUID playerId : trackingPlayers) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) continue;
            
            if (player.getLocation().distanceSquared(mob.getLocation()) < 1600) {
                String color = tracked.state == GoalState.AGGRESSIVE ? "§c" :
                    tracked.state == GoalState.PEACEFUL ? "§a" :
                    tracked.state == GoalState.NEEDS_DRIVEN ? "§9" :
                    tracked.state == GoalState.ENVIRONMENTAL ? "§e" :
                    tracked.state == GoalState.SOCIAL ? "§d" :
                    "§f";
                ActionBarManager.send(player,
                    color + mob.getType().name() + "#" + mob.getEntityId() + ": " + tracked.currentGoal);
                
                updateBossBar(player, tracked, mob);
            }
        }
    }
    
    private void updateBossBar(Player player, TrackedMob tracked, Mob mob) {
        if (tracked.bossBar == null) {
            tracked.bossBar = BossBar.bossBar(
                Component.text(mob.getType().name() + " Needs"),
                0.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
            );
            player.showBossBar(tracked.bossBar);
        }
        
        double avgNeeds = (tracked.hunger + tracked.thirst + tracked.energy) / 3.0;
        float progress = Math.max(0.0f, Math.min(1.0f, (float) (avgNeeds / 100.0)));
        
        BossBar.Color barColor;
        if (avgNeeds < 25) {
            barColor = BossBar.Color.RED;
        } else if (avgNeeds < 50) {
            barColor = BossBar.Color.YELLOW;
        } else {
            barColor = BossBar.Color.GREEN;
        }
        
        tracked.bossBar.progress(progress);
        tracked.bossBar.color(barColor);
        tracked.bossBar.name(Component.text(
            String.format("%s | H:%.0f%% T:%.0f%% E:%.0f%%", 
                mob.getType().name(), tracked.hunger, tracked.thirst, tracked.energy),
            barColor == BossBar.Color.RED ? NamedTextColor.RED :
            barColor == BossBar.Color.YELLOW ? NamedTextColor.YELLOW :
            NamedTextColor.GREEN
        ));
    }
    
    public Set<UUID> getTrackedMobs() {
        return trackedMobs.keySet();
    }
}
