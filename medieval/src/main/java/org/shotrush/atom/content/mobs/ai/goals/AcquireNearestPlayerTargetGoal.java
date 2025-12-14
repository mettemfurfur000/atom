package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;

import java.util.EnumSet;

public class AcquireNearestPlayerTargetGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final SpeciesBehavior behavior;
    private int scanTimer;
    private static final int SCAN_INTERVAL = 20;
    private static final double FIGHT_THRESHOLD = 0.30;
    
    public AcquireNearestPlayerTargetGoal(Mob mob, Plugin plugin, SpeciesBehavior behavior) {
        this.mob = mob;
        this.behavior = behavior;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "acquire_player_target"));
        this.scanTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!mob.hasMetadata("aggressive")) return false;
        if (!mob.getMetadata("aggressive").get(0).asBoolean()) return false;
        
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent < FIGHT_THRESHOLD) return false;
        
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return false;
        }
        
        if (mob.getTarget() != null) return false;
        
        Player nearest = findNearestPlayer();
        if (nearest != null) {
            mob.setTarget(nearest);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (mob.getTarget() == null) return false;
        
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent < FIGHT_THRESHOLD) {
            mob.setTarget(null);
            return false;
        }
        
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            mob.setTarget(null);
            return false;
        }
        
        return mob.getTarget().isValid() && mob.getLocation().distance(mob.getTarget().getLocation()) < behavior.aggroRadius() * 2;
    }
    
    @Override
    public void start() {
        scanTimer = 0;
    }
    
    @Override
    public void stop() {
        mob.setTarget(null);
    }
    
    @Override
    public void tick() {
        scanTimer++;
        if (scanTimer >= SCAN_INTERVAL) {
            scanTimer = 0;
            Player nearest = findNearestPlayer();
            if (nearest != null && (mob.getTarget() == null || 
                mob.getLocation().distance(nearest.getLocation()) < mob.getLocation().distance(mob.getTarget().getLocation()))) {
                mob.setTarget(nearest);
            }
        }
    }
    
    private Player findNearestPlayer() {
        Player nearest = null;
        double nearestDist = behavior.aggroRadius();
        
        for (Player player : mob.getLocation().getNearbyPlayers(behavior.aggroRadius())) {
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }
            
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
        return EnumSet.of(GoalType.TARGET);
    }
}
