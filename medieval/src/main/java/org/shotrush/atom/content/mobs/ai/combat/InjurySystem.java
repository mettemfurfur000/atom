package org.shotrush.atom.content.mobs.ai.combat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class InjurySystem {
    
    private final Plugin plugin;
    private static final double WOUNDED_THRESHOLD = 0.70;
    private static final double CRITICAL_THRESHOLD = 0.30;
    private static final double WOUNDED_SPEED_MULTIPLIER = 0.7;
    private static final double CRITICAL_SPEED_MULTIPLIER = 0.5;
    
    public InjurySystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public InjuryLevel getInjuryLevel(Mob mob) {
        if (!mob.isValid() || mob.isDead()) {
            return InjuryLevel.HEALTHY;
        }
        
        double maxHealth = mob.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = mob.getHealth();
        double healthPercent = currentHealth / maxHealth;
        
        if (healthPercent < CRITICAL_THRESHOLD) {
            return InjuryLevel.CRITICALLY_INJURED;
        } else if (healthPercent < WOUNDED_THRESHOLD) {
            return InjuryLevel.WOUNDED;
        } else {
            return InjuryLevel.HEALTHY;
        }
    }
    
    public void applyInjuryEffects(Mob mob) {
        InjuryLevel level = getInjuryLevel(mob);
        
        if (level == InjuryLevel.HEALTHY) {
            mob.removeMetadata("injury_level", plugin);
            mob.removeMetadata("injury_speed_multiplier", plugin);
            return;
        }
        
        mob.setMetadata("injury_level", new FixedMetadataValue(plugin, level.name()));
        
        double speedMultiplier = switch (level) {
            case WOUNDED -> WOUNDED_SPEED_MULTIPLIER;
            case CRITICALLY_INJURED -> CRITICAL_SPEED_MULTIPLIER;
            default -> 1.0;
        };
        
        mob.setMetadata("injury_speed_multiplier", new FixedMetadataValue(plugin, speedMultiplier));
        
        if (level == InjuryLevel.WOUNDED || level == InjuryLevel.CRITICALLY_INJURED) {
            applyLimpingEffect(mob, level);
        }
    }
    
    public void spawnBloodTrail(Mob mob) {
        InjuryLevel level = getInjuryLevel(mob);
        
        if (level == InjuryLevel.HEALTHY) {
            return;
        }
        
        Location loc = mob.getLocation();
        if (loc.getWorld() == null) return;
        
        int particleCount = switch (level) {
            case WOUNDED -> 2;
            case CRITICALLY_INJURED -> 5;
            default -> 0;
        };
        
        if (particleCount > 0) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(139, 0, 0), 
                1.0f
            );
            loc.getWorld().spawnParticle(
                Particle.DUST, 
                loc.clone().add(0, 0.2, 0), 
                particleCount, 
                0.2, 0.1, 0.2, 
                0.0, 
                dustOptions
            );
        }
    }
    
    public double getSpeedMultiplier(Mob mob) {
        if (mob.hasMetadata("injury_speed_multiplier")) {
            return mob.getMetadata("injury_speed_multiplier").get(0).asDouble();
        }
        return 1.0;
    }
    
    private void applyLimpingEffect(Mob mob, InjuryLevel level) {
        if (!mob.hasMetadata("limp_timer")) {
            mob.setMetadata("limp_timer", new FixedMetadataValue(plugin, 0));
        }
        
        int timer = mob.getMetadata("limp_timer").get(0).asInt();
        timer++;
        
        int limpInterval = level == InjuryLevel.CRITICALLY_INJURED ? 10 : 20;
        
        if (timer >= limpInterval) {
            timer = 0;
            
            if (mob.getVelocity().length() > 0.1) {
                org.bukkit.util.Vector velocity = mob.getVelocity();
                velocity.multiply(0.3);
                mob.setVelocity(velocity);
            }
        }
        
        mob.setMetadata("limp_timer", new FixedMetadataValue(plugin, timer));
    }
    
    public enum InjuryLevel {
        HEALTHY,
        WOUNDED,
        CRITICALLY_INJURED
    }
}
