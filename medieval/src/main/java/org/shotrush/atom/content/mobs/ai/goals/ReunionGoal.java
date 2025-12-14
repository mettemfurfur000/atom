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
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.EnumSet;
import java.util.Optional;

public class ReunionGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private final SpeciesBehavior behavior;
    private int callOutTimer;
    private static final int CALL_INTERVAL = 60;
    private static final double SEPARATION_MULTIPLIER = 3.0;
    private static final double REUNION_SPEED = 1.4;
    
    public ReunionGoal(Mob mob, Plugin plugin, HerdManager herdManager, SpeciesBehavior behavior) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.behavior = behavior;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "reunion"));
        this.callOutTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        Location herdCentroid = herdManager.getHerdCentroid(herd);
        
        if (herdCentroid == null) return false;
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
        double cohesionRadius = behavior.getCohesionRadius(domesticationFactor);
        double separationThreshold = cohesionRadius * SEPARATION_MULTIPLIER;
        
        double distance = mob.getLocation().distance(herdCentroid);
        return distance > separationThreshold;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!mob.isValid() || mob.isDead()) return false;
        if (mob.getTarget() != null) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        Location herdCentroid = herdManager.getHerdCentroid(herd);
        
        if (herdCentroid == null) return false;
        
        if (!(mob instanceof Animals animal)) return false;
        double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
        double cohesionRadius = behavior.getCohesionRadius(domesticationFactor);
        double reunionThreshold = cohesionRadius * 1.5;
        
        double distance = mob.getLocation().distance(herdCentroid);
        return distance > reunionThreshold;
    }
    
    @Override
    public void start() {
        callOutTimer = 0;
    }
    
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        callOutTimer++;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd herd = herdOpt.get();
        Location herdCentroid = herdManager.getHerdCentroid(herd);
        
        if (herdCentroid == null) return;
        
        mob.getPathfinder().moveTo(herdCentroid, REUNION_SPEED);
        
        if (callOutTimer >= CALL_INTERVAL) {
            callOutTimer = 0;
            performCallOut();
        }
        
        if (mob.getTicksLived() % 10 == 0) {
            Location loc = mob.getLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().spawnParticle(Particle.NOTE, 
                    loc.clone().add(0, mob.getHeight() + 0.5, 0), 1);
            }
        }
    }
    
    private void performCallOut() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        Sound callSound = switch (mob.getType()) {
            case COW -> Sound.ENTITY_COW_AMBIENT;
            case SHEEP -> Sound.ENTITY_SHEEP_AMBIENT;
            case PIG -> Sound.ENTITY_PIG_AMBIENT;
            case HORSE -> Sound.ENTITY_HORSE_AMBIENT;
            case WOLF -> Sound.ENTITY_WOLF_AMBIENT;
            default -> Sound.ENTITY_VILLAGER_AMBIENT;
        };
        
        mobLoc.getWorld().playSound(mobLoc, callSound, 1.5f, 0.9f);
        mobLoc.getWorld().spawnParticle(Particle.NOTE, 
            mobLoc.clone().add(0, mob.getHeight() + 1, 0), 5, 0.3, 0.3, 0.3, 0.01);
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
