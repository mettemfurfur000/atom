package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.combat.InjurySystem;

import java.util.EnumSet;
import java.util.List;

public class TrackWoundedPreyGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final InjurySystem injurySystem;
    private LivingEntity woundedTarget;
    private static final double DETECTION_RADIUS = 32.0;
    private static final double AGGRO_RANGE_MULTIPLIER = 1.5;
    
    public TrackWoundedPreyGoal(Mob mob, Plugin plugin, InjurySystem injurySystem) {
        this.mob = mob;
        this.plugin = plugin;
        this.injurySystem = injurySystem;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "track_wounded_prey"));
    }
    
    @Override
    public boolean shouldActivate() {
        if (mob.getTarget() != null && mob.getTarget().isValid()) {
            return false;
        }
        
        woundedTarget = findNearestWoundedPrey();
        return woundedTarget != null;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (woundedTarget == null || !woundedTarget.isValid() || woundedTarget.isDead()) {
            return false;
        }
        
        InjurySystem.InjuryLevel level = injurySystem.getInjuryLevel((Mob) woundedTarget);
        if (level == InjurySystem.InjuryLevel.HEALTHY) {
            return false;
        }
        
        double distance = mob.getLocation().distance(woundedTarget.getLocation());
        return distance < DETECTION_RADIUS * AGGRO_RANGE_MULTIPLIER;
    }
    
    @Override
    public void start() {
        if (woundedTarget != null) {
            mob.setTarget(woundedTarget);
        }
    }
    
    @Override
    public void stop() {
        woundedTarget = null;
    }
    
    @Override
    public void tick() {
        if (woundedTarget == null || !woundedTarget.isValid()) return;
        
        double distance = mob.getLocation().distance(woundedTarget.getLocation());
        if (distance < 3.0) {
            mob.attack(woundedTarget);
        }
    }
    
    private LivingEntity findNearestWoundedPrey() {
        Location mobLoc = mob.getLocation();
        if (mobLoc.getWorld() == null) return null;
        
        List<LivingEntity> nearbyEntities = mobLoc.getWorld().getLivingEntities().stream()
            .filter(entity -> entity instanceof Animals)
            .filter(entity -> !entity.getUniqueId().equals(mob.getUniqueId()))
            .filter(entity -> entity.getLocation().distance(mobLoc) <= DETECTION_RADIUS)
            .filter(entity -> entity instanceof Mob)
            .toList();
        
        LivingEntity nearestWounded = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (LivingEntity entity : nearbyEntities) {
            Mob potentialPrey = (Mob) entity;
            InjurySystem.InjuryLevel level = injurySystem.getInjuryLevel(potentialPrey);
            
            if (level == InjurySystem.InjuryLevel.WOUNDED || level == InjurySystem.InjuryLevel.CRITICALLY_INJURED) {
                double distance = mobLoc.distance(entity.getLocation());
                
                if (level == InjurySystem.InjuryLevel.CRITICALLY_INJURED) {
                    distance *= 0.7;
                }
                
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestWounded = entity;
                }
            }
        }
        
        return nearestWounded;
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
    }
}
