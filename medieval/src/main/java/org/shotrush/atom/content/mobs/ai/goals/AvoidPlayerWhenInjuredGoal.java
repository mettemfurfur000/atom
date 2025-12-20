package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;

import java.util.EnumSet;

public class AvoidPlayerWhenInjuredGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final SpeciesBehavior behavior;
    private Player nearestPlayer;
    private int scanTimer;
    private static final int SCAN_INTERVAL = 10;
    private static final double FLEE_DISTANCE = 10.0;
    
    public AvoidPlayerWhenInjuredGoal(Mob mob, Plugin plugin, SpeciesBehavior behavior) {
        this.mob = mob;
        this.behavior = behavior;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "avoid_player_injured"));
        this.scanTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        
        if (healthPercent >= behavior.panicHealthThreshold()) {
            return false;
        }
        
        if (mob.hasMetadata("aggressive") && mob.getMetadata("aggressive").get(0).asBoolean()) {
            return false;
        }
        
        nearestPlayer = findNearestPlayer();
        return nearestPlayer != null;
    }
    
    @Override
    public boolean shouldStayActive() {
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        
        if (healthPercent >= behavior.panicHealthThreshold() * 1.2) {
            return false;
        }
        
        if (nearestPlayer == null || !nearestPlayer.isValid()) {
            return false;
        }
        
        return mob.getLocation().distance(nearestPlayer.getLocation()) < FLEE_DISTANCE * 2;
    }
    
    @Override
    public void start() {
        mob.setTarget(null);
        scanTimer = 0;
    }
    
    @Override
    public void stop() {
        nearestPlayer = null;
    }
    
    @Override
    public void tick() {
        scanTimer++;
        if (scanTimer >= SCAN_INTERVAL) {
            scanTimer = 0;
            nearestPlayer = findNearestPlayer();
        }
        
        if (nearestPlayer == null || !nearestPlayer.isValid()) return;
        
        Location current = mob.getLocation();
        if (current == null || current.getWorld() == null) return;
        
        Location playerLoc = nearestPlayer.getLocation();
        if (playerLoc == null) return;
        
        org.bukkit.util.Vector awayFromPlayer = current.toVector().subtract(playerLoc.toVector()).normalize();
        Location fleeTarget = current.clone().add(awayFromPlayer.multiply(15.0));
        
        if (fleeTarget == null || fleeTarget.getWorld() == null) return;
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor((Animals) mob);
        double speed = behavior.getFleeSpeed(domesticationFactor) * 0.8;
        
        mob.getPathfinder().moveTo(fleeTarget, speed);
    }
    
    private Player findNearestPlayer() {
        Player nearest = null;
        double nearestDist = FLEE_DISTANCE;
        
        for (Player player : mob.getLocation().getNearbyPlayers(FLEE_DISTANCE)) {
            double dist = player.getLocation().distance(mob.getLocation());
            if (dist < nearestDist) {
                nearest = player;
                nearestDist = dist;
            }
        }
        
        return nearest;
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
