package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class PackHuntingGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private int coordinationTimer;
    private static final int COORDINATION_INTERVAL = 60;
    private static final double PACK_BONUS_DAMAGE = 2.0;
    private static final double SURROUND_RANGE = 15.0;
    
    public PackHuntingGoal(Mob mob, Plugin plugin, HerdManager herdManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pack_hunting"));
        this.coordinationTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Wolf)) return false;
        if (mob.getTarget() == null) return false;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd pack = herdOpt.get();
        return pack.size() >= 2;
    }
    
    @Override
    public boolean shouldStayActive() {
        return mob.getTarget() != null && mob.getTarget().isValid();
    }
    
    @Override
    public void start() {
        coordinationTimer = 0;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void tick() {
        coordinationTimer++;
        
        if (coordinationTimer >= COORDINATION_INTERVAL) {
            coordinationTimer = 0;
            coordinatePackAttack();
        }
    }
    
    private void coordinatePackAttack() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return;
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd pack = herdOpt.get();
        List<Wolf> nearbyPackMembers = new ArrayList<>();
        
        for (java.util.UUID memberId : pack.members()) {
            Wolf packMember = (Wolf) Bukkit.getEntity(memberId);
            if (packMember == null || !packMember.isValid()) continue;
            if (packMember.getUniqueId().equals(mob.getUniqueId())) continue;
            
            Location memberLoc = packMember.getLocation();
            if (memberLoc == null) continue;
            
            if (memberLoc.distance(target.getLocation()) < SURROUND_RANGE) {
                nearbyPackMembers.add(packMember);
            }
        }
        
        if (nearbyPackMembers.isEmpty()) return;
        
        for (Wolf packMember : nearbyPackMembers) {
            if (packMember.getTarget() == null) {
                packMember.setTarget(target);
            }
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_WOLF_AMBIENT, 0.5f, 0.8f);
            mobLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mobLoc.clone().add(0, 1, 0), 2);
        }
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
