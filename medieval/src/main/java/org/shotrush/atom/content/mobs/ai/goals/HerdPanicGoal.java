package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.ai.combat.MoraleSystem;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.EnumSet;
import java.util.Optional;

public class HerdPanicGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private final SpeciesBehavior behavior;
    private final MoraleSystem moraleSystem;
    private Location fleeTarget;
    private int repathTimer;
    private static final int REPATH_INTERVAL = 20;
    
    public HerdPanicGoal(Mob mob, Plugin plugin, HerdManager herdManager, SpeciesBehavior behavior, MoraleSystem moraleSystem) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.behavior = behavior;
        this.moraleSystem = moraleSystem;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "herd_panic"));
        this.repathTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (moraleSystem != null && moraleSystem.isMoraleBroken(mob)) {
            return true;
        }
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent() && herdOpt.get().isPanicking()) {
            return true;
        }
        
        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent < behavior.panicHealthThreshold()) {
            return true;
        }
        
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!mob.isValid() || mob.isDead()) return false;

        if (moraleSystem != null && moraleSystem.isMoraleBroken(mob)) {
            return true;
        }
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent() && herdOpt.get().isPanicking()) {
            return true;
        }

        double healthPercent = mob.getHealth() / mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (healthPercent < behavior.panicHealthThreshold() * 1.5) {
            return true;
        }

        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void start() {
        mob.setMetadata("fleeing", new FixedMetadataValue(plugin, true));
        computeFleeTarget();
        repathTimer = 0;
    }
    
    @Override
    public void stop() {
        mob.removeMetadata("fleeing", plugin);
        fleeTarget = null;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        if (fleeTarget == null) {
            computeFleeTarget();
        }
        
        repathTimer++;
        if (repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            computeFleeTarget();
        }
        
        if (fleeTarget == null || fleeTarget.getWorld() == null) {
            return;
        }
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor((Animals) mob);
        double speed = behavior.getFleeSpeed(domesticationFactor);
        
        drainStamina();
        
        
        if (fleeTarget != null && fleeTarget.getWorld() != null) {
            
            if (mob.getLocation().distance(fleeTarget) < 3.0) {
                computeFleeTarget();
            }
            mob.getPathfinder().moveTo(fleeTarget, speed);
        }
    }
    
    private void computeFleeTarget() {
        if (!mob.isValid() || mob.isDead()) {
            fleeTarget = null;
            return;
        }
        
        Location current = mob.getLocation();
        if (current == null || current.getWorld() == null) {
            fleeTarget = null;
            return;
        }
        
        Location threatLocation = null;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent()) {
            threatLocation = herdOpt.get().lastThreatLocation();
        }
        
        if (threatLocation == null && mob.getLastDamageCause() != null) {
            threatLocation = mob.getLastDamageCause().getEntity() != null 
                ? mob.getLastDamageCause().getEntity().getLocation() 
                : current;
        }
        
        if (threatLocation == null) {
            threatLocation = current.clone().add(Math.random() * 10 - 5, 0, Math.random() * 10 - 5);
        }
        
        org.bukkit.util.Vector awayFromThreat = current.toVector().subtract(threatLocation.toVector()).normalize();
        double distance = 20.0 + (Math.random() * 10.0);
        
        fleeTarget = current.clone().add(awayFromThreat.multiply(distance));
        fleeTarget.setY(current.getY());
        
        if (fleeTarget.getWorld() == null) {
            fleeTarget = null;
        }
    }
    
    private void drainStamina() {
        if (!mob.hasMetadata("stamina")) {
            double maxStamina = 100 + (Math.random() * 100);
            mob.setMetadata("maxStamina", new FixedMetadataValue(plugin, maxStamina));
            mob.setMetadata("stamina", new FixedMetadataValue(plugin, maxStamina));
            return;
        }
        
        double stamina = mob.getMetadata("stamina").get(0).asDouble();
        double drain = 0.3;
        
        stamina = Math.max(0, stamina - drain);
        mob.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        
        if (stamina <= 0) {
            stop();
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
