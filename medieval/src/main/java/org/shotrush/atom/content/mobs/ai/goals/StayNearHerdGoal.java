package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.content.mobs.herd.HerdRole;

import java.util.EnumSet;
import java.util.Optional;

public class StayNearHerdGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final HerdManager herdManager;
    private final SpeciesBehavior behavior;
    private int repathTimer;
    private static final int REPATH_INTERVAL = 40;
    
    public StayNearHerdGoal(Mob mob, Plugin plugin, HerdManager herdManager, SpeciesBehavior behavior) {
        this.mob = mob;
        this.herdManager = herdManager;
        this.behavior = behavior;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "stay_near_herd"));
        this.repathTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (herdManager.getRole(mob.getUniqueId()) == HerdRole.LEADER) {
            return false;
        }
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        Animals leader = (Animals) Bukkit.getEntity(herd.leader());
        
        if (leader == null || !leader.isValid()) return false;
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor((Animals) mob);
        double cohesionRadius = behavior.getCohesionRadius(domesticationFactor);
        
        double distance = mob.getLocation().distance(leader.getLocation());
        return distance > cohesionRadius;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (mob.getTarget() != null) return false;
        if (mob.hasMetadata("fleeing") && mob.getMetadata("fleeing").get(0).asBoolean()) {
            return false;
        }
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        if (herd.isPanicking()) return false;
        
        Animals leader = (Animals) Bukkit.getEntity(herd.leader());
        if (leader == null || !leader.isValid()) return false;
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor((Animals) mob);
        double innerRadius = behavior.getCohesionRadius(domesticationFactor) * 0.6;
        
        double distance = mob.getLocation().distance(leader.getLocation());
        return distance > innerRadius;
    }
    
    @Override
    public void start() {
        repathTimer = 0;
    }
    
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        repathTimer++;
        if (repathTimer < REPATH_INTERVAL) return;
        repathTimer = 0;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd herd = herdOpt.get();
        Animals leader = (Animals) Bukkit.getEntity(herd.leader());
        
        if (leader == null || !leader.isValid()) return;
        
        Location targetLocation = leader.getLocation();
        if (targetLocation == null || targetLocation.getWorld() == null) return;
        
        Location centroid = herdManager.getHerdCentroid(herd);
        if (centroid != null && Math.random() < 0.3) {
            targetLocation = centroid;
        }
        
        if (targetLocation == null || targetLocation.getWorld() == null) return;
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor((Animals) mob);
        double speed = 1.0 + ((1.2 - 1.0) * (1.0 - domesticationFactor));
        
        mob.getPathfinder().moveTo(targetLocation, speed);
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
