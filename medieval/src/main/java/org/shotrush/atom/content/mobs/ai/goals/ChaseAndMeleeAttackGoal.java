package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.ai.combat.FatigueSystem;
import org.shotrush.atom.content.mobs.ai.combat.InjurySystem;
import org.shotrush.atom.content.mobs.ai.combat.MoraleSystem;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;

import java.util.EnumSet;

public class ChaseAndMeleeAttackGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final SpeciesBehavior behavior;
    private final FatigueSystem fatigueSystem;
    private final InjurySystem injurySystem;
    private final MoraleSystem moraleSystem;
    private int attackCooldown;
    private static final int ATTACK_INTERVAL = 20;
    private static final double ATTACK_RANGE = 2.0;
    
    public ChaseAndMeleeAttackGoal(Mob mob, Plugin plugin, SpeciesBehavior behavior, FatigueSystem fatigueSystem, InjurySystem injurySystem, MoraleSystem moraleSystem) {
        this.mob = mob;
        this.behavior = behavior;
        this.fatigueSystem = fatigueSystem;
        this.injurySystem = injurySystem;
        this.moraleSystem = moraleSystem;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "chase_melee_attack"));
        this.attackCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null && mob.getTarget().isValid();
    }
    
    @Override
    public boolean shouldStayActive() {
        if (mob.getTarget() == null || !mob.getTarget().isValid()) return false;
        
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return false;
        }
        
        if (moraleSystem != null && moraleSystem.isMoraleBroken(mob)) {
            return false;
        }
        
        double distance = mob.getLocation().distance(mob.getTarget().getLocation());
        return distance < behavior.aggroRadius() * 2.5;
    }
    
    @Override
    public void start() {
        attackCooldown = 0;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return;
        
        Location current = mob.getLocation();
        if (current == null || current.getWorld() == null) return;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return;
        
        if (fatigueSystem != null) {
            fatigueSystem.trackCombat(mob);
            fatigueSystem.applyFatigueDebuff(mob);
        }
        
        if (injurySystem != null) {
            injurySystem.applyInjuryEffects(mob);
        }
        
        if (moraleSystem != null) {
            moraleSystem.checkMorale(mob);
        }
        
        double distance = current.distance(targetLoc);
        
        if (distance > ATTACK_RANGE) {
            double domesticationFactor = AnimalDomestication.getDomesticationFactor((Animals) mob);
            double speed = behavior.getChaseSpeed(domesticationFactor);
            
            if (injurySystem != null) {
                speed *= injurySystem.getSpeedMultiplier(mob);
            }
            
            mob.getPathfinder().moveTo(targetLoc, speed);
        } else {
            mob.getPathfinder().stopPathfinding();
            
            int attackInterval = ATTACK_INTERVAL;
            if (fatigueSystem != null && fatigueSystem.isFatigued(mob)) {
                attackInterval = (int) (ATTACK_INTERVAL / fatigueSystem.getAttackSpeedMultiplier(mob));
            }
            
            if (attackCooldown <= 0) {
                performAttack(target);
                attackCooldown = attackInterval;
            }
        }
        
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }
    
    private void performAttack(LivingEntity target) {
        mob.lookAt(target);
        
        try {
            mob.attack(target);
        } catch (IllegalArgumentException e) {
            double damage = 2.0;
            target.damage(damage, mob);
        }
        
        mob.swingMainHand();
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
