package org.shotrush.atom.content.mobs.ai.combat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class FatigueSystem {
    
    private final Plugin plugin;
    private static final int FATIGUE_THRESHOLD = 100;
    private static final double STAMINA_DRAIN_COMBAT = 0.9;
    private static final double FATIGUE_DAMAGE_MULTIPLIER = 0.6;
    private static final double FATIGUE_ATTACK_SPEED_MULTIPLIER = 0.7;
    
    public FatigueSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void trackCombat(Mob mob) {
        if (!mob.hasMetadata("combat_ticks")) {
            mob.setMetadata("combat_ticks", new FixedMetadataValue(plugin, 0));
        }
        
        int combatTicks = mob.getMetadata("combat_ticks").get(0).asInt();
        combatTicks++;
        
        mob.setMetadata("combat_ticks", new FixedMetadataValue(plugin, combatTicks));
        
        drainStamina(mob);
        
        if (combatTicks >= FATIGUE_THRESHOLD) {
            mob.setMetadata("fatigued", new FixedMetadataValue(plugin, true));
        }
    }
    
    public void resetCombat(Mob mob) {
        mob.removeMetadata("combat_ticks", plugin);
        
        if (!mob.hasMetadata("fatigue_recovery_timer")) {
            mob.setMetadata("fatigue_recovery_timer", new FixedMetadataValue(plugin, 0));
        }
        
        int recoveryTimer = mob.getMetadata("fatigue_recovery_timer").get(0).asInt();
        recoveryTimer++;
        
        if (recoveryTimer >= 200) {
            mob.removeMetadata("fatigued", plugin);
            mob.removeMetadata("fatigue_recovery_timer", plugin);
        } else {
            mob.setMetadata("fatigue_recovery_timer", new FixedMetadataValue(plugin, recoveryTimer));
        }
    }
    
    public boolean isFatigued(Mob mob) {
        return mob.hasMetadata("fatigued") && mob.getMetadata("fatigued").get(0).asBoolean();
    }
    
    public void applyFatigueDebuff(Mob mob) {
        if (!isFatigued(mob)) {
            return;
        }
        
        mob.setMetadata("fatigue_damage_multiplier", new FixedMetadataValue(plugin, FATIGUE_DAMAGE_MULTIPLIER));
        mob.setMetadata("fatigue_attack_speed_multiplier", new FixedMetadataValue(plugin, FATIGUE_ATTACK_SPEED_MULTIPLIER));
        
        if (mob.getTicksLived() % 40 == 0) {
            spawnFatigueParticles(mob);
        }
    }
    
    public double getDamageMultiplier(Mob mob) {
        if (isFatigued(mob) && mob.hasMetadata("fatigue_damage_multiplier")) {
            return mob.getMetadata("fatigue_damage_multiplier").get(0).asDouble();
        }
        return 1.0;
    }
    
    public double getAttackSpeedMultiplier(Mob mob) {
        if (isFatigued(mob) && mob.hasMetadata("fatigue_attack_speed_multiplier")) {
            return mob.getMetadata("fatigue_attack_speed_multiplier").get(0).asDouble();
        }
        return 1.0;
    }
    
    private void drainStamina(Mob mob) {
        if (!mob.hasMetadata("stamina")) {
            double maxStamina = 100 + (Math.random() * 100);
            mob.setMetadata("maxStamina", new FixedMetadataValue(plugin, maxStamina));
            mob.setMetadata("stamina", new FixedMetadataValue(plugin, maxStamina));
            return;
        }
        
        double stamina = mob.getMetadata("stamina").get(0).asDouble();
        stamina = Math.max(0, stamina - STAMINA_DRAIN_COMBAT);
        
        mob.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        
        if (stamina <= 0) {
            mob.setMetadata("fatigued", new FixedMetadataValue(plugin, true));
        }
    }
    
    private void spawnFatigueParticles(Mob mob) {
        Location loc = mob.getLocation();
        if (loc.getWorld() == null) return;
        
        loc.getWorld().spawnParticle(
            Particle.SWEEP_ATTACK,
            loc.clone().add(0, 1.0, 0),
            3,
            0.3, 0.3, 0.3,
            0.0
        );
    }
}
