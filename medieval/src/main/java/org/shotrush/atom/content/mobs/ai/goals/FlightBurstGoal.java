package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class FlightBurstGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private int flightCooldown;
    private static final int COOLDOWN_TICKS = 60;
    private static final double PANIC_THRESHOLD = 0.70;
    private static final double FLIGHT_POWER = 0.8;
    
    public FlightBurstGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "flight_burst"));
        this.flightCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (flightCooldown > 0) {
            flightCooldown--;
            return false;
        }
        
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent >= PANIC_THRESHOLD) return false;
        
        if (!mob.isOnGround()) return false;
        
        return mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean();
    }
    
    @Override
    public boolean shouldStayActive() {
        return false;
    }
    
    @Override
    public void start() {
        performFlightBurst();
        flightCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void tick() {
    }
    
    private void performFlightBurst() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        Vector velocity = mob.getVelocity();
        velocity.setY(FLIGHT_POWER);
        
        double angle = Math.random() * 2 * Math.PI;
        velocity.setX(Math.cos(angle) * 0.3);
        velocity.setZ(Math.sin(angle) * 0.3);
        
        mob.setVelocity(velocity);
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_CHICKEN_HURT, 1.0f, 1.5f);
        mobLoc.getWorld().spawnParticle(Particle.POOF, mobLoc, 10, 0.3, 0.3, 0.3, 0.05);
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
