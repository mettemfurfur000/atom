package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class KickAttackGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private int kickCooldown;
    private static final int COOLDOWN_TICKS = 60;
    private static final double KICK_RANGE = 3.0;
    private static final double DAMAGE = 5.0;
    private static final double KNOCKBACK = 1.5;
    private static final double BEHIND_ARC_ANGLE = 90.0;
    
    public KickAttackGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "kick_attack"));
        this.kickCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (kickCooldown > 0) {
            kickCooldown--;
            return false;
        }
        
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance >= KICK_RANGE) return false;
        
        return isTargetBehind(mob, target, BEHIND_ARC_ANGLE);
    }
    
    private boolean isTargetBehind(Mob mob, LivingEntity target, double arcAngle) {
        Vector mobDirection = mob.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize();
        
        double angle = Math.toDegrees(Math.acos(mobDirection.dot(toTarget)));
        
        return angle > (180 - arcAngle / 2);
    }
    
    @Override
    public boolean shouldStayActive() {
        return false;
    }
    
    @Override
    public void start() {
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            Vector upwardVelocity = new Vector(0, 0.3, 0);
            mob.setVelocity(mob.getVelocity().add(upwardVelocity));
        }
        
        mob.getScheduler().runDelayed(plugin, task -> {
            performKick();
        }, null, 5);
        
        kickCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void tick() {
    }
    
    private void performKick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return;
        
        double distance = mobLoc.distance(targetLoc);
        if (distance >= KICK_RANGE) return;
        
        target.damage(DAMAGE, mob);
        
        Vector knockbackDirection = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();
        knockbackDirection.setY(0.2);
        target.setVelocity(knockbackDirection.multiply(KNOCKBACK));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_HORSE_ANGRY, 1.0f, 0.8f);
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 1.2f);
        
        Vector behindDirection = mobLoc.getDirection().multiply(-1);
        Location particleLoc = mobLoc.clone().add(behindDirection.multiply(0.5));
        mobLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 2, 0.3, 0.3, 0.3, 0);
        mobLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.3, 0.5, 0.3, 0.1);
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
