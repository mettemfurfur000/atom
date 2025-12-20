package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;

public class SpitAttackGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private int spitCooldown;
    private static final int COOLDOWN_TICKS = 40;
    private static final double SPIT_RANGE_MIN = 5.0;
    private static final double SPIT_RANGE_MAX = 15.0;
    
    public SpitAttackGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "spit_attack"));
        this.spitCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Llama)) return false;
        
        if (spitCooldown > 0) {
            spitCooldown--;
            return false;
        }
        
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        return distance >= SPIT_RANGE_MIN && distance <= SPIT_RANGE_MAX;
    }
    
    @Override
    public boolean shouldStayActive() {
        return false;
    }
    
    @Override
    public void start() {
        performSpit();
        spitCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void tick() {
    }
    
    private void performSpit() {
        if (!(mob instanceof Llama llama)) return;
        
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return;
        
        LlamaSpit spit = llama.launchProjectile(LlamaSpit.class);
        spit.setShooter(llama);
        
        org.bukkit.util.Vector direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();
        spit.setVelocity(direction.multiply(1.5));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_LLAMA_SPIT, 1.0f, 1.0f);
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
