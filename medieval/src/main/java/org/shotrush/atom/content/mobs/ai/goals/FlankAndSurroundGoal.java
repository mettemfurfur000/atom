package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wolf;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class FlankAndSurroundGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private int coordinationTimer;
    private Location assignedPosition;
    private static final int COORDINATION_INTERVAL = 40;
    private static final double SURROUND_RADIUS = 8.0;
    private static final double ATTACK_RANGE = 2.5;
    
    public FlankAndSurroundGoal(Mob mob, Plugin plugin, HerdManager herdManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "flank_surround"));
        this.coordinationTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Wolf)) return false;
        if (mob.getTarget() == null || !mob.getTarget().isValid()) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd pack = herdOpt.get();
        return pack.size() >= 3;
    }
    
    @Override
    public boolean shouldStayActive() {
        return mob.getTarget() != null && mob.getTarget().isValid();
    }
    
    @Override
    public void start() {
        coordinationTimer = 0;
        assignedPosition = null;
    }
    
    @Override
    public void stop() {
        assignedPosition = null;
        mob.removeMetadata("pack_role", plugin);
    }
    
    @Override
    public void tick() {
        coordinationTimer++;
        
        if (coordinationTimer >= COORDINATION_INTERVAL) {
            coordinationTimer = 0;
            coordinateFlankingPositions();
        }
        
        if (assignedPosition != null && assignedPosition.getWorld() != null) {
            executeFlanking();
        }
    }
    
    private void coordinateFlankingPositions() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd pack = herdOpt.get();
        List<Wolf> activePackMembers = new ArrayList<>();
        
        for (java.util.UUID memberId : pack.members()) {
            Wolf packMember = (Wolf) Bukkit.getEntity(memberId);
            if (packMember == null || !packMember.isValid() || packMember.isDead()) continue;
            
            if (packMember.getLocation().distance(target.getLocation()) < SURROUND_RADIUS * 2) {
                activePackMembers.add(packMember);
                if (packMember.getTarget() == null) {
                    packMember.setTarget(target);
                }
            }
        }
        
        if (activePackMembers.size() < 2) return;
        
        assignSurroundPositions(target, activePackMembers);
    }
    
    private void assignSurroundPositions(LivingEntity target, List<Wolf> packMembers) {
        Location targetLoc = target.getLocation();
        int memberCount = packMembers.size();
        double angleIncrement = 360.0 / memberCount;
        
        for (int i = 0; i < packMembers.size(); i++) {
            Wolf packMember = packMembers.get(i);
            double angle = Math.toRadians(angleIncrement * i);
            
            Vector offset = new Vector(
                Math.cos(angle) * SURROUND_RADIUS,
                0,
                Math.sin(angle) * SURROUND_RADIUS
            );
            
            Location surroundPosition = targetLoc.clone().add(offset);
            surroundPosition.setY(targetLoc.getY());
            
            if (packMember.getUniqueId().equals(mob.getUniqueId())) {
                assignedPosition = surroundPosition;
                
                if (isTargetBehind(target, mob, 90)) {
                    mob.setMetadata("pack_role", new FixedMetadataValue(plugin, "ATTACKER"));
                } else {
                    mob.setMetadata("pack_role", new FixedMetadataValue(plugin, "DISTRACTOR"));
                }
            }
        }
    }
    
    private void executeFlanking() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        
        double distanceToPosition = mob.getLocation().distance(assignedPosition);
        double distanceToTarget = mob.getLocation().distance(target.getLocation());
        
        String role = mob.hasMetadata("pack_role") 
            ? mob.getMetadata("pack_role").get(0).asString() 
            : "ATTACKER";
        
        if (distanceToPosition > 2.0) {
            mob.getPathfinder().moveTo(assignedPosition, 1.0);
        } else if (distanceToTarget <= ATTACK_RANGE && role.equals("ATTACKER")) {
            mob.getPathfinder().stopPathfinding();
            mob.attack(target);
            
            Location mobLoc = mob.getLocation();
            if (mobLoc.getWorld() != null) {
                mobLoc.getWorld().spawnParticle(
                    Particle.CRIT,
                    mobLoc.clone().add(0, 1, 0),
                    3
                );
            }
        } else if (role.equals("DISTRACTOR")) {
            Location targetLoc = target.getLocation();
            Vector direction = targetLoc.toVector().subtract(mob.getLocation().toVector()).normalize();
            Location distractPosition = targetLoc.clone().subtract(direction.multiply(3));
            
            mob.getPathfinder().moveTo(distractPosition, 0.8);
            
            if (Math.random() < 0.1) {
                mob.lookAt(target);
            }
        }
    }
    
    private boolean isTargetBehind(LivingEntity observer, LivingEntity target, double arcAngle) {
        Vector observerDirection = observer.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(observer.getLocation().toVector()).normalize();
        
        double angle = Math.toDegrees(Math.acos(observerDirection.dot(toTarget)));
        
        return angle > (180 - arcAngle / 2);
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.TARGET);
    }
}
