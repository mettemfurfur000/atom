package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PolarBear;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;

public class CubProtectionGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private boolean enraged;
    private static final double CUB_PROTECTION_RADIUS = 10.0;
    private static final double ENRAGE_DAMAGE_BONUS = 4.0;
    private static final NamespacedKey ENRAGE_KEY = new NamespacedKey("atom", "cub_protection_enrage");
    
    public CubProtectionGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "cub_protection"));
        this.enraged = false;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof PolarBear bear)) return false;
        
        Location bearLoc = mob.getLocation();
        if (bearLoc == null || bearLoc.getWorld() == null) return false;
        
        for (org.bukkit.entity.Entity nearby : bearLoc.getWorld().getNearbyEntities(bearLoc, CUB_PROTECTION_RADIUS, CUB_PROTECTION_RADIUS, CUB_PROTECTION_RADIUS)) {
            if (nearby instanceof PolarBear cub && !cub.isAdult() && cub.getUniqueId() != mob.getUniqueId()) {
                if (cub.getLastDamageCause() != null && cub.getLastDamageCause().getEntity() instanceof LivingEntity attacker) {
                    mob.setTarget(attacker);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        return enraged && mob.getTarget() != null && mob.getTarget().isValid();
    }
    
    @Override
    public void start() {
        enraged = true;
        applyEnrageBonus();
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_POLAR_BEAR_WARNING, 2.0f, 0.8f);
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mobLoc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5);
        }
    }
    
    @Override
    public void stop() {
        enraged = false;
        removeEnrageBonus();
    }
    
    @Override
    public void tick() {
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null && Math.random() < 0.05) {
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mobLoc.clone().add(0, 2, 0), 1);
        }
    }
    
    private void applyEnrageBonus() {
        AttributeInstance attackDamage = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage == null) return;
        
        AttributeModifier modifier = new AttributeModifier(
            ENRAGE_KEY,
            ENRAGE_DAMAGE_BONUS,
            AttributeModifier.Operation.ADD_NUMBER
        );
        
        attackDamage.addModifier(modifier);
    }
    
    private void removeEnrageBonus() {
        AttributeInstance attackDamage = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage == null) return;
        
        attackDamage.getModifiers().stream()
            .filter(mod -> mod.getKey().equals(ENRAGE_KEY))
            .forEach(attackDamage::removeModifier);
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}
