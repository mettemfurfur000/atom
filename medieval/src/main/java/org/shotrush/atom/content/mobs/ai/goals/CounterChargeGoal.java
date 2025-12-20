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

public class CounterChargeGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private LivingEntity target;
    private int chargeCooldown;
    private boolean isCharging;
    private static final int COOLDOWN_TICKS = 120;
    private static final double HEALTH_THRESHOLD = 0.20;
    private static final double CHARGE_SPEED = 1.8;
    private static final double DAMAGE = 5.0;
    private static final double KNOCKBACK = 1.5;
    
    public CounterChargeGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "counter_charge"));
        this.chargeCooldown = 0;
        this.isCharging = false;
    }
    
    @Override
    public boolean shouldActivate() {
        if (chargeCooldown > 0) {
            chargeCooldown--;
            return false;
        }
        
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent >= HEALTH_THRESHOLD) return false;
        
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget == null || !currentTarget.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = currentTarget.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance >= 3.0 && distance <= 8.0) {
            target = currentTarget;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!isCharging) return false;
        if (target == null || !target.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null) return false;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        return distance > 1.5;
    }
    
    @Override
    public void start() {
        isCharging = true;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_RAVAGER_STUNNED, 0.7f, 1.8f);
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mobLoc.clone().add(0, 1, 0), 3);
        }
    }
    
    @Override
    public void stop() {
        isCharging = false;
        target = null;
        chargeCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void tick() {
        if (target == null || !target.isValid()) {
            stop();
            return;
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) {
            stop();
            return;
        }
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) {
            stop();
            return;
        }
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance < 2.0) {
            performHit();
            stop();
            return;
        }
        
        mob.getPathfinder().moveTo(targetLoc, CHARGE_SPEED);
    }
    
    private void performHit() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        if (target == null) return;
        
        target.damage(DAMAGE, mob);
        
        Vector knockbackDirection = target.getLocation().toVector().subtract(mobLoc.toVector()).normalize();
        knockbackDirection.setY(0.15);
        target.setVelocity(knockbackDirection.multiply(KNOCKBACK));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_HOGLIN_ATTACK, 1.0f, 1.2f);
        mobLoc.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 15, 0.3, 0.5, 0.3, 0.1);
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
