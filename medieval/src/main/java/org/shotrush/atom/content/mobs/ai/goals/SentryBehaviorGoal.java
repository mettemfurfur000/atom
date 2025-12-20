package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.shotrush.atom.core.data.PersistentData;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.shotrush.atom.content.mobs.herd.DominanceRank;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.EnumSet;
import java.util.Optional;

public class SentryBehaviorGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private int sentryTicks;
    private int scanDirection;
    private static final int SENTRY_DURATION = 2400;
    private static final double THREAT_DETECTION_RANGE = 20.0;
    private static final double SCAN_SPEED = 2.0;
    
    public SentryBehaviorGoal(Mob mob, Plugin plugin, HerdManager herdManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "sentry_behavior"));
        this.sentryTicks = 0;
        this.scanDirection = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals)) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        DominanceRank rank = herd.getDominanceHierarchy().getRank(mob.getUniqueId());
        
        if (rank != DominanceRank.ALPHA && rank != DominanceRank.BETA) {
            return false;
        }
        
        long timeSinceLastSentry = System.currentTimeMillis() - 
            PersistentData.getLong(mob, "last_sentry_time", 0L);
        
        return timeSinceLastSentry > SENTRY_DURATION * 50 && Math.random() < 0.1;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!mob.isValid() || mob.isDead()) return false;
        if (mob.getTarget() != null) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        if (herd.isPanicking()) return false;
        
        return sentryTicks < SENTRY_DURATION;
    }
    
    @Override
    public void start() {
        sentryTicks = 0;
        scanDirection = 0;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void stop() {
        PersistentData.set(mob, "last_sentry_time", System.currentTimeMillis());
    }
    
    @Override
    public void tick() {
        sentryTicks++;
        mob.getPathfinder().stopPathfinding();
        
        scanDirection += SCAN_SPEED;
        if (scanDirection >= 360) {
            scanDirection = 0;
        }
        
        float yaw = mob.getLocation().getYaw() + (float) scanDirection;
        Location lookTarget = mob.getEyeLocation().clone();
        lookTarget.setYaw(yaw);
        lookTarget.setPitch(0);
        
        Vector direction = lookTarget.getDirection();
        Location scanPoint = mob.getEyeLocation().add(direction.multiply(5));
        mob.lookAt(scanPoint);
        
        if (sentryTicks % 60 == 0) {
            Location loc = mob.getLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, 
                    loc.clone().add(0, mob.getHeight(), 0), 1);
            }
        }
        
        if (sentryTicks % 20 == 0) {
            detectThreats();
        }
    }
    
    private void detectThreats() {
        Location loc = mob.getLocation();
        if (loc == null || loc.getWorld() == null) return;
        
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 
                THREAT_DETECTION_RANGE, THREAT_DETECTION_RANGE, THREAT_DETECTION_RANGE)) {
            
            if (isThreat(entity)) {
                alertHerd(entity.getLocation());
                break;
            }
        }
    }
    
    private boolean isThreat(Entity entity) {
        if (entity instanceof Player) return true;
        if (entity instanceof Monster) return true;
        if (entity instanceof Wolf wolf && wolf.isAngry()) return true;
        return false;
    }
    
    private void alertHerd(Location threatLocation) {
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd herd = herdOpt.get();
        herdManager.broadcastPanic(herd, threatLocation, 5000);
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_GOAT_SCREAMING_AMBIENT, 1.5f, 1.0f);
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, 
                mobLoc.clone().add(0, mob.getHeight(), 0), 5, 0.5, 0.5, 0.5);
        }
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
