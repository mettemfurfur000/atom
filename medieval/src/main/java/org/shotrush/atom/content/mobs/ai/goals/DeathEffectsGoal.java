package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.combat.MoraleSystem;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class DeathEffectsGoal implements Goal<Mob>, Listener {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private final MoraleSystem moraleSystem;
    private Location corpseLocation;
    private int mourningTicks;
    private boolean wasAttackDeath;
    private static final int MOURNING_DURATION = 200;
    private static final double GATHERING_RADIUS = 30.0;
    private static final double MOURN_DISTANCE = 3.0;
    
    public DeathEffectsGoal(Mob mob, Plugin plugin, HerdManager herdManager, MoraleSystem moraleSystem) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.moraleSystem = moraleSystem;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "death_effects"));
        this.mourningTicks = 0;
        this.wasAttackDeath = false;
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deceased = event.getEntity();
        if (!(deceased instanceof Animals)) return;
        
        Optional<Herd> deceasedHerdOpt = herdManager.getHerd(deceased.getUniqueId());
        if (deceasedHerdOpt.isEmpty()) return;
        
        Optional<Herd> mobHerdOpt = herdManager.getHerd(mob.getUniqueId());
        if (mobHerdOpt.isEmpty()) return;
        
        if (!deceasedHerdOpt.get().equals(mobHerdOpt.get())) return;
        
        Location deathLoc = deceased.getLocation();
        if (deathLoc == null) return;
        
        double distance = mob.getLocation().distance(deathLoc);
        if (distance > GATHERING_RADIUS) return;
        
        corpseLocation = deathLoc.clone();
        mourningTicks = 0;
        wasAttackDeath = deceased.getLastDamageCause() != null && 
                         deceased.getKiller() != null;
        
        moraleSystem.breakMorale(mob);
    }
    
    @Override
    public boolean shouldActivate() {
        return corpseLocation != null && mourningTicks < MOURNING_DURATION;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (corpseLocation == null) return false;
        if (mourningTicks >= MOURNING_DURATION) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        return true;
    }
    
    @Override
    public void start() {
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void stop() {
        if (wasAttackDeath && corpseLocation != null) {
            Location mobLoc = mob.getLocation();
            if (mobLoc != null && corpseLocation.getWorld() != null) {
                Location fleeTarget = mobLoc.clone().subtract(
                    corpseLocation.toVector().subtract(mobLoc.toVector()).normalize().multiply(20)
                );
                mob.getPathfinder().moveTo(fleeTarget, 1.5);
            }
        }
        
        corpseLocation = null;
        mourningTicks = 0;
        wasAttackDeath = false;
    }
    
    @Override
    public void tick() {
        if (corpseLocation == null) {
            stop();
            return;
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) {
            stop();
            return;
        }
        
        mourningTicks++;
        
        double distance = mobLoc.distance(corpseLocation);
        
        if (distance > MOURN_DISTANCE) {
            mob.getPathfinder().moveTo(corpseLocation, 0.8);
        } else {
            mob.getPathfinder().stopPathfinding();
            
            if (mourningTicks % 40 == 0) {
                playMourningSounds(mobLoc);
            }
            
            if (mourningTicks % 20 == 0) {
                spawnMourningParticles(corpseLocation);
            }
            
            if (mob instanceof Animals animal && mourningTicks % 60 == 0) {
                mob.lookAt(corpseLocation);
            }
        }
    }
    
    private void playMourningSounds(Location mobLoc) {
        if (mobLoc.getWorld() == null) return;
        
        Sound sound = switch (mob.getType()) {
            case COW -> Sound.ENTITY_COW_AMBIENT;
            case SHEEP -> Sound.ENTITY_SHEEP_AMBIENT;
            case PIG -> Sound.ENTITY_PIG_AMBIENT;
            case WOLF -> Sound.ENTITY_WOLF_WHINE;
            case HORSE -> Sound.ENTITY_HORSE_BREATHE;
            case CHICKEN -> Sound.ENTITY_CHICKEN_AMBIENT;
            case RABBIT -> Sound.ENTITY_RABBIT_AMBIENT;
            case LLAMA -> Sound.ENTITY_LLAMA_AMBIENT;
            default -> Sound.ENTITY_GENERIC_HURT;
        };
        
        mobLoc.getWorld().playSound(mobLoc, sound, 0.4f, 0.6f);
    }
    
    private void spawnMourningParticles(Location loc) {
        if (loc.getWorld() == null) return;
        
        loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0.01);
        loc.getWorld().spawnParticle(Particle.ASH, loc.clone().add(0, 0.1, 0), 3, 0.2, 0.1, 0.2, 0);
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.TARGET);
    }
}
