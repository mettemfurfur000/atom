package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HuntPreyGoal implements Goal<Mob>, Listener {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final SpeciesBehavior behavior;
    private LivingEntity preyTarget;
    private static final Set<EntityType> PREY_TYPES = Set.of(
        EntityType.RABBIT, EntityType.CHICKEN, EntityType.SHEEP
    );
    private static final Set<UUID> HUNTING_MOBS = ConcurrentHashMap.newKeySet();
    
    public HuntPreyGoal(Mob mob, Plugin plugin, SpeciesBehavior behavior) {
        this.mob = mob;
        this.plugin = plugin;
        this.behavior = behavior;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "hunt_prey"));
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (mob.getType() != EntityType.WOLF && mob.getType() != EntityType.FOX) {
            return false;
        }
        
        if (mob.getTarget() instanceof Player) {
            return false;
        }
        
        preyTarget = findNearestPrey();
        return preyTarget != null;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!(mob instanceof Animals)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (preyTarget == null || !preyTarget.isValid() || preyTarget.isDead()) {
            return false;
        }
        
        if (mob.getTarget() instanceof Player) {
            return false;
        }
        
        Location mobLoc = mob.getLocation();
        Location preyLoc = preyTarget.getLocation();
        if (mobLoc == null || preyLoc == null) return false;
        
        return mobLoc.distanceSquared(preyLoc) < behavior.aggroRadius() * behavior.aggroRadius() * 4;
    }
    
    @Override
    public void start() {
        if (preyTarget != null && preyTarget instanceof Mob preyMob) {
            mob.setTarget(preyTarget);
            mob.setMetadata("hunting", new FixedMetadataValue(plugin, preyTarget.getUniqueId()));
            HUNTING_MOBS.add(mob.getUniqueId());
        }
    }
    
    @Override
    public void stop() {
        mob.setTarget(null);
        mob.removeMetadata("hunting", plugin);
        HUNTING_MOBS.remove(mob.getUniqueId());
        preyTarget = null;
    }
    
    @Override
    public void tick() {
        if (!(mob instanceof Animals animal)) return;
        if (preyTarget == null || !preyTarget.isValid() || preyTarget.isDead()) {
            stop();
            return;
        }
        
        mob.setTarget(preyTarget);
        
        Location preyLoc = preyTarget.getLocation();
        if (preyLoc != null && preyLoc.getWorld() != null) {
            double chaseSpeed = behavior.getChaseSpeed(0.0);
            mob.getPathfinder().moveTo(preyLoc, chaseSpeed);
        }
    }
    
    private LivingEntity findNearestPrey() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return null;
        
        return mobLoc.getWorld().getNearbyEntities(mobLoc, 
            behavior.aggroRadius(), behavior.aggroRadius(), behavior.aggroRadius())
            .stream()
            .filter(entity -> entity instanceof LivingEntity)
            .filter(entity -> PREY_TYPES.contains(entity.getType()))
            .filter(entity -> entity.isValid() && !((LivingEntity) entity).isDead())
            .map(entity -> (LivingEntity) entity)
            .min((a, b) -> Double.compare(
                mobLoc.distanceSquared(a.getLocation()),
                mobLoc.distanceSquared(b.getLocation())
            ))
            .orElse(null);
    }
    
    @EventHandler
    public void onPreyDeath(EntityDeathEvent event) {
        UUID killerId = event.getEntity().getKiller() != null 
            ? event.getEntity().getKiller().getUniqueId() 
            : null;
        
        if (killerId == null) return;
        if (!HUNTING_MOBS.contains(killerId)) return;
        
        Entity killerEntity = plugin.getServer().getEntity(killerId);
        if (!(killerEntity instanceof Animals animal)) return;
        
        HUNTING_MOBS.remove(killerId);
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
