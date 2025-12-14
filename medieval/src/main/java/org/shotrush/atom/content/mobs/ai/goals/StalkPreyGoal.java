package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class StalkPreyGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private LivingEntity prey;
    private static final double POUNCE_DISTANCE = 5.0;
    private static final double STALK_SPEED = 0.5;
    private static final double BLIND_SPOT_ANGLE = 120.0;
    
    public StalkPreyGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "stalk_prey"));
    }
    
    @Override
    public boolean shouldActivate() {
        if (mob.getTarget() == null || !mob.getTarget().isValid()) {
            return false;
        }
        
        prey = mob.getTarget();
        
        if (mob.hasMetadata("in_combat") && mob.getMetadata("in_combat").get(0).asBoolean()) {
            return false;
        }
        
        double distance = mob.getLocation().distance(prey.getLocation());
        return distance > POUNCE_DISTANCE && distance < 20.0;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (prey == null || !prey.isValid()) {
            return false;
        }
        
        double distance = mob.getLocation().distance(prey.getLocation());
        
        if (distance <= POUNCE_DISTANCE) {
            mob.setMetadata("ready_to_pounce", new FixedMetadataValue(plugin, true));
            return false;
        }
        
        return distance < 20.0;
    }
    
    @Override
    public void start() {
        mob.setMetadata("stalking", new FixedMetadataValue(plugin, true));
        mob.setSneaking(true);
    }
    
    @Override
    public void stop() {
        mob.removeMetadata("stalking", plugin);
        mob.setSneaking(false);
    }
    
    @Override
    public void tick() {
        if (prey == null || !prey.isValid()) return;
        
        Location mobLoc = mob.getLocation();
        Location preyLoc = prey.getLocation();
        
        if (mobLoc.getWorld() == null || preyLoc.getWorld() == null) return;
        
        Location stalkTarget = calculateBlindSpotPosition(mobLoc, preyLoc);
        
        if (stalkTarget != null) {
            mob.getPathfinder().moveTo(stalkTarget, STALK_SPEED);
        }
        
        if (canSee(prey, mob)) {
            mob.getPathfinder().stopPathfinding();
            
            if (Math.random() < 0.3) {
                mob.setMetadata("in_combat", new FixedMetadataValue(plugin, true));
                stop();
            }
        }
    }
    
    private Location calculateBlindSpotPosition(Location mobLoc, Location preyLoc) {
        Vector preyDirection = preyLoc.getDirection().normalize();
        Vector mobToPreyDirection = preyLoc.toVector().subtract(mobLoc.toVector()).normalize();
        
        double angleToPreyFacing = Math.toDegrees(preyDirection.angle(mobToPreyDirection));
        
        if (angleToPreyFacing < BLIND_SPOT_ANGLE / 2) {
            Vector rightVector = preyDirection.clone().rotateAroundY(Math.toRadians(90));
            if (Math.random() < 0.5) {
                rightVector.multiply(-1);
            }
            
            Location targetPos = preyLoc.clone().add(rightVector.multiply(3));
            targetPos.setY(preyLoc.getY());
            return targetPos;
        }
        
        return preyLoc.clone();
    }
    
    private boolean canSee(LivingEntity observer, LivingEntity target) {
        Vector observerDirection = observer.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(observer.getLocation().toVector()).normalize();
        
        double angle = Math.toDegrees(Math.acos(observerDirection.dot(toTarget)));
        
        return angle < 90;
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
