package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;

public class RollDefenseGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private int rollDuration;
    private int rollCooldown;
    private boolean isRolled;
    private static final int ROLL_DURATION = 60;
    private static final int COOLDOWN_TICKS = 100;
    private static final double PANIC_THRESHOLD = 0.80;
    
    public RollDefenseGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "roll_defense"));
        this.rollDuration = 0;
        this.rollCooldown = 0;
        this.isRolled = false;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Armadillo)) return false;
        
        if (rollCooldown > 0) {
            rollCooldown--;
            return false;
        }
        
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent >= PANIC_THRESHOLD) return false;
        
        return mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean();
    }
    
    @Override
    public boolean shouldStayActive() {
        return isRolled && rollDuration > 0;
    }
    
    @Override
    public void start() {
        isRolled = true;
        rollDuration = ROLL_DURATION;
        
        mob.setInvulnerable(true);
        mob.setAI(false);
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_ARMADILLO_ROLL, 1.0f, 1.0f);
            mobLoc.getWorld().spawnParticle(Particle.POOF, mobLoc, 15, 0.3, 0.3, 0.3, 0.05);
        }
    }
    
    @Override
    public void stop() {
        isRolled = false;
        mob.setInvulnerable(false);
        mob.setAI(true);
        rollCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void tick() {
        rollDuration--;
        
        if (rollDuration <= 0) {
            stop();
            return;
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null && rollDuration % 10 == 0) {
            org.bukkit.util.Vector rollDirection = mobLoc.getDirection().multiply(-0.2);
            rollDirection.setY(0);
            mob.setVelocity(rollDirection);
        }
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
