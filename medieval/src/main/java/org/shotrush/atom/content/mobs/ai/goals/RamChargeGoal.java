package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class RamChargeGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private LivingEntity target;
    private Location chargeStart;
    private boolean isCharging;
    private boolean isWindingUp;
    private boolean isStunned;
    private int chargeCooldown;
    private int stunDuration;
    private static final int COOLDOWN_TICKS = 100;
    private static final double CHARGE_DISTANCE = 5.0;
    private static final double CHARGE_SPEED = 2.0;
    private static final double DAMAGE = 6.0;
    private static final double KNOCKBACK = 1.2;
    private static final double RECOIL_DAMAGE = 4.0;
    private static final int STUN_TICKS = 40;
    
    public RamChargeGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "ram_charge"));
        this.isCharging = false;
        this.chargeCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (isStunned) {
            stunDuration--;
            if (stunDuration <= 0) {
                isStunned = false;
            }
            return false;
        }
        
        if (chargeCooldown > 0) {
            chargeCooldown--;
            return false;
        }
        
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget == null || !currentTarget.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = currentTarget.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance >= CHARGE_DISTANCE && distance <= 15.0) {
            target = currentTarget;
            chargeStart = mobLoc.clone();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (isWindingUp) return true;
        if (!isCharging) return false;
        if (target == null || !target.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null) return false;
        
        if (chargeStart == null) return false;
        
        double distanceTraveled = mobLoc.distance(chargeStart);
        return distanceTraveled < 10.0;
    }
    
    @Override
    public void start() {
        isWindingUp = true;
        mob.getPathfinder().stopPathfinding();
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.5f);
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mobLoc.clone().add(0, mob.getHeight() + 0.5, 0), 3, 0.3, 0.3, 0.3, 0);
        }
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(mob, () -> {
            isWindingUp = false;
            isCharging = true;
            Location loc = mob.getLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_STEP, 1.0f, 0.8f);
            }
        }, 20L);
    }
    
    @Override
    public void stop() {
        isCharging = false;
        isWindingUp = false;
        target = null;
        chargeStart = null;
        if (!isStunned) {
            chargeCooldown = COOLDOWN_TICKS;
        }
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        if (isWindingUp) {
            mob.getPathfinder().stopPathfinding();
            return;
        }
        
        if (isStunned) {
            mob.getPathfinder().stopPathfinding();
            Location mobLoc = mob.getLocation();
            if (mobLoc != null && mobLoc.getWorld() != null && stunDuration % 5 == 0) {
                mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, 
                    mobLoc.clone().add(0, mob.getHeight() + 0.8, 0), 3, 0.2, 0.2, 0.2, 0);
            }
            return;
        }
        
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
            performRamHit();
            stop();
            return;
        }
        
        checkWallCollision(mobLoc);
        
        mob.getPathfinder().moveTo(targetLoc, CHARGE_SPEED);
        
        if (Math.random() < 0.2) {
            mobLoc.getWorld().spawnParticle(Particle.CLOUD, mobLoc, 5, 0.3, 0.2, 0.3, 0.03);
        }
    }
    
    private void performRamHit() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        target.damage(DAMAGE, mob);
        
        Vector knockbackDirection = target.getLocation().toVector().subtract(mobLoc.toVector()).normalize();
        knockbackDirection.setY(0.15);
        target.setVelocity(knockbackDirection.multiply(KNOCKBACK));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
        mobLoc.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 3, 0.3, 0.3, 0.3, 0);
        mobLoc.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.6, 0.5, 0.6, 0.15);
    }
    
    private void checkWallCollision(Location mobLoc) {
        Vector facing = mobLoc.getDirection().normalize();
        Location checkLoc = mobLoc.clone().add(facing.multiply(1.5));
        Block block = checkLoc.getBlock();
        
        if (block.getType().isSolid() && !block.getType().equals(Material.AIR)) {
            isStunned = true;
            stunDuration = STUN_TICKS;
            isCharging = false;
            
            mob.damage(RECOIL_DAMAGE);
            mob.getPathfinder().stopPathfinding();
            
            mobLoc.getWorld().playSound(mobLoc, Sound.BLOCK_STONE_HIT, 1.0f, 0.5f);
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_RAVAGER_HURT, 1.0f, 1.0f);
            mobLoc.getWorld().spawnParticle(Particle.BLOCK, checkLoc, 30, 0.3, 0.3, 0.3, block.getBlockData());
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mobLoc.clone().add(0, mob.getHeight() + 0.8, 0), 5, 0.3, 0.3, 0.3, 0);
            
            chargeCooldown = COOLDOWN_TICKS + STUN_TICKS;
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
