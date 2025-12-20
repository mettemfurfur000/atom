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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.shotrush.atom.content.mobs.herd.DominanceRank;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class TerritoryDefenseGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private Animals rivalLeader;
    private int chargeTimer;
    private static final double TERRITORY_RADIUS = 20.0;
    private static final double CHARGE_SPEED = 1.5;
    private static final int MAX_CHARGE_DURATION = 200;
    
    public TerritoryDefenseGoal(Mob mob, Plugin plugin, HerdManager herdManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "territory_defense"));
        this.chargeTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        DominanceRank rank = herd.getDominanceHierarchy().getRank(mob.getUniqueId());
        
        if (rank != DominanceRank.ALPHA) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        for (Entity nearby : mobLoc.getWorld().getNearbyEntities(mobLoc, 
                TERRITORY_RADIUS, TERRITORY_RADIUS, TERRITORY_RADIUS)) {
            
            if (nearby instanceof Animals rival && 
                rival.getType() == mob.getType() && 
                rival.getUniqueId() != mob.getUniqueId()) {
                
                Optional<Herd> rivalHerdOpt = herdManager.getHerd(rival.getUniqueId());
                if (rivalHerdOpt.isEmpty()) continue;
                
                Herd rivalHerd = rivalHerdOpt.get();
                if (rivalHerd.id().equals(herd.id())) continue;
                
                if (rivalHerd.leader().equals(rival.getUniqueId())) {
                    rivalLeader = rival;
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!mob.isValid() || mob.isDead()) return false;
        if (rivalLeader == null || !rivalLeader.isValid()) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        double distance = mob.getLocation().distance(rivalLeader.getLocation());
        if (distance > TERRITORY_RADIUS * 1.5) return false;
        
        return chargeTimer < MAX_CHARGE_DURATION;
    }
    
    @Override
    public void start() {
        chargeTimer = 0;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.2f);
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, 
                mobLoc.clone().add(0, mob.getHeight(), 0), 5, 0.5, 0.5, 0.5);
        }
    }
    
    @Override
    public void stop() {
        rivalLeader = null;
        chargeTimer = 0;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        chargeTimer++;
        
        if (rivalLeader == null || !rivalLeader.isValid()) {
            stop();
            return;
        }
        
        Location rivalLocation = rivalLeader.getLocation();
        if (rivalLocation == null) return;
        
        mob.getPathfinder().moveTo(rivalLocation, CHARGE_SPEED);
        
        double distance = mob.getLocation().distance(rivalLocation);
        
        if (distance < 2.5) {
            pushRivalAway();
        }
        
        if (chargeTimer % 20 == 0) {
            Location loc = mob.getLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                    loc.clone().add(0, mob.getHeight() / 2, 0), 1);
            }
        }
    }
    
    private void pushRivalAway() {
        if (rivalLeader == null) return;
        
        Vector direction = rivalLeader.getLocation().toVector()
            .subtract(mob.getLocation().toVector())
            .normalize();
        
        Vector pushVelocity = direction.multiply(0.8).setY(0.3);
        rivalLeader.setVelocity(pushVelocity);
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().spawnParticle(Particle.CLOUD, 
                rivalLeader.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.1);
            mobLoc.getWorld().playSound(rivalLeader.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.5f);
        }
        
        Optional<Herd> myHerdOpt = herdManager.getHerd(mob.getUniqueId());
        if (myHerdOpt.isPresent()) {
            Herd myHerd = myHerdOpt.get();
            myHerd.getDominanceHierarchy().recordConfrontationWin(mob.getUniqueId());
        }
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.TARGET);
    }
}
