package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.content.mobs.herd.HerdRole;

import java.util.EnumSet;
import java.util.Optional;

public class HerdLeaderWanderGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final HerdManager herdManager;
    private Location wanderTarget;
    private int cooldown;
    private static final int WANDER_COOLDOWN = 200;
    private static final int MIN_WANDER_DISTANCE = 5;
    private static final int MAX_WANDER_DISTANCE = 15;
    
    public HerdLeaderWanderGoal(Mob mob, Plugin plugin, HerdManager herdManager) {
        this.mob = mob;
        this.herdManager = herdManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "herd_leader_wander"));
        this.cooldown = 0;
        this.wanderTarget = null;
    }
    
    @Override
    public boolean shouldActivate() {
        if (herdManager.getRole(mob.getUniqueId()) != HerdRole.LEADER) {
            return false;
        }
        
        if (mob.getTarget() != null) return false;
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return false;
        }
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent() && herdOpt.get().isPanicking()) {
            return false;
        }
        
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (wanderTarget == null) return false;
        if (mob.getTarget() != null) return false;
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return false;
        }
        
        double distance = mob.getLocation().distance(wanderTarget);
        return distance > 2.0;
    }
    
    @Override
    public void start() {
        computeWanderTarget();
        cooldown = WANDER_COOLDOWN + (int) (Math.random() * 100);
    }
    
    @Override
    public void stop() {
        wanderTarget = null;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        Location current = mob.getLocation();
        if (current == null || current.getWorld() == null) return;
        
        if (wanderTarget == null || wanderTarget.getWorld() == null) return;
        
        mob.getPathfinder().moveTo(wanderTarget, 0.8);
    }
    
    private void computeWanderTarget() {
        Location current = mob.getLocation();
        Location centroid = null;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent()) {
            centroid = herdManager.getHerdCentroid(herdOpt.get());
        }
        
        if (centroid != null && Math.random() < 0.4) {
            org.bukkit.util.Vector towardsCentroid = centroid.toVector().subtract(current.toVector()).normalize();
            double distance = MIN_WANDER_DISTANCE + (Math.random() * (MAX_WANDER_DISTANCE - MIN_WANDER_DISTANCE));
            wanderTarget = current.clone().add(towardsCentroid.multiply(distance));
        } else {
            double angle = Math.random() * 2 * Math.PI;
            double distance = MIN_WANDER_DISTANCE + (Math.random() * (MAX_WANDER_DISTANCE - MIN_WANDER_DISTANCE));
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            wanderTarget = current.clone().add(x, 0, z);
        }
        
        wanderTarget.setY(current.getY());
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
