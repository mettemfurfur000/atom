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

public class PounceAttackGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private LivingEntity target;
    private int pounceCooldown;
    private boolean isPouncing;
    private static final int COOLDOWN_TICKS = 80;
    private static final double POUNCE_RANGE_MIN = 3.0;
    private static final double POUNCE_RANGE_MAX = 8.0;
    private static final double DAMAGE = 4.0;
    private static final double POUNCE_POWER = 1.2;
    
    public PounceAttackGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pounce_attack"));
        this.pounceCooldown = 0;
        this.isPouncing = false;
    }
    
    @Override
    public boolean shouldActivate() {
        if (pounceCooldown > 0) {
            pounceCooldown--;
            return false;
        }
        
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget == null || !currentTarget.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = currentTarget.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance >= POUNCE_RANGE_MIN && distance <= POUNCE_RANGE_MAX) {
            target = currentTarget;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        return isPouncing;
    }
    
    @Override
    public void start() {
        isPouncing = true;
        performPounce();
    }
    
    @Override
    public void stop() {
        isPouncing = false;
        target = null;
        pounceCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void tick() {
        if (target == null || !target.isValid()) {
            stop();
            return;
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null) {
            stop();
            return;
        }
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) {
            stop();
            return;
        }
        
        double distance = mobLoc.distance(targetLoc);
        if (distance < 1.5 || mob.isOnGround()) {
            if (distance < 2.0) {
                target.damage(DAMAGE, mob);
            }
            stop();
        }
    }
    
    private void performPounce() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) {
            stop();
            return;
        }
        
        if (target == null) {
            stop();
            return;
        }
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) {
            stop();
            return;
        }
        
        Vector direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();
        direction.setY(0.4);
        mob.setVelocity(direction.multiply(POUNCE_POWER));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_FOX_AGGRO, 1.0f, 1.0f);
        mobLoc.getWorld().spawnParticle(Particle.CLOUD, mobLoc, 10, 0.2, 0.2, 0.2, 0.05);
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.JUMP);
    }
}
