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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.EnumSet;
import java.util.Optional;

public class StampedeGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final HerdManager herdManager;
    private Location stampedeThreat;
    private Vector stampedeDirection;
    private static final double STAMPEDE_SPEED = 1.6;
    private static final double GROUND_SHAKE_RADIUS = 8.0;
    private static final double TRAMPLE_RADIUS = 2.0;
    private static final double TRAMPLE_DAMAGE = 2.0;
    private int soundCooldown;
    
    public StampedeGoal(Mob mob, Plugin plugin, HerdManager herdManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "stampede"));
    }
    
    @Override
    public boolean shouldActivate() {
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        Herd herd = herdOpt.get();
        if (!herd.isPanicking()) return false;
        
        if (herd.size() < 4) return false;
        
        Location threatLoc = herd.lastThreatLocation();
        if (threatLoc == null) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        stampedeThreat = threatLoc;
        stampedeDirection = mobLoc.toVector().subtract(threatLoc.toVector()).normalize();
        stampedeDirection.setY(0);
        
        return true;
    }
    
    @Override
    public boolean shouldStayActive() {
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return false;
        
        return herdOpt.get().isPanicking();
    }
    
    @Override
    public void start() {
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_RAVAGER_STEP, 0.8f, 0.9f);
        }
        soundCooldown = 0;
    }
    
    @Override
    public void stop() {
        stampedeThreat = null;
        stampedeDirection = null;
    }
    
    @Override
    public void tick() {
        if (stampedeDirection == null) return;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        Location target = mobLoc.clone().add(stampedeDirection.clone().multiply(30));
        if (target.getWorld() == null) return;
        
        mob.getPathfinder().moveTo(target, STAMPEDE_SPEED);
        
        if (Math.random() < 0.3) {
            mobLoc.getWorld().spawnParticle(Particle.DUST_PLUME, mobLoc.clone().add(0, 0.1, 0), 8, 0.5, 0.2, 0.5, 0.02);
        }
        
        applyGroundShakeEffects(mobLoc);
        trampleEntitiesInPath(mobLoc);
        
        if (soundCooldown <= 0) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.5f);
            soundCooldown = 15;
        } else {
            soundCooldown--;
        }
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd herd = herdOpt.get();
        for (java.util.UUID memberId : herd.members()) {
            Animals member = (Animals) Bukkit.getEntity(memberId);
            if (member == null || !member.isValid()) continue;
            if (member.getUniqueId().equals(mob.getUniqueId())) continue;
            
            if (member instanceof Mob memberMob) {
                Location memberLoc = member.getLocation();
                if (memberLoc != null && mobLoc.distance(memberLoc) < 5.0) {
                    Location syncTarget = memberLoc.clone().add(stampedeDirection.clone().multiply(25));
                    if (syncTarget.getWorld() != null) {
                        memberMob.getPathfinder().moveTo(syncTarget, STAMPEDE_SPEED);
                    }
                }
            }
        }
    }
    
    private void applyGroundShakeEffects(Location mobLoc) {
        for (Entity nearby : mobLoc.getWorld().getNearbyEntities(mobLoc, GROUND_SHAKE_RADIUS, GROUND_SHAKE_RADIUS, GROUND_SHAKE_RADIUS)) {
            if (nearby instanceof Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false));
                
                Vector shakeVelocity = new Vector(
                    (Math.random() - 0.5) * 0.15,
                    0.1,
                    (Math.random() - 0.5) * 0.15
                );
                player.setVelocity(player.getVelocity().add(shakeVelocity));
            }
        }
    }
    
    private void trampleEntitiesInPath(Location mobLoc) {
        for (Entity nearby : mobLoc.getWorld().getNearbyEntities(mobLoc, TRAMPLE_RADIUS, TRAMPLE_RADIUS, TRAMPLE_RADIUS)) {
            if (nearby instanceof LivingEntity target && !nearby.equals(mob)) {
                if (nearby instanceof Animals) {
                    Optional<Herd> targetHerd = herdManager.getHerd(nearby.getUniqueId());
                    Optional<Herd> mobHerd = herdManager.getHerd(mob.getUniqueId());
                    if (targetHerd.isPresent() && mobHerd.isPresent() && targetHerd.get().equals(mobHerd.get())) {
                        continue;
                    }
                }
                
                target.damage(TRAMPLE_DAMAGE, mob);
                mobLoc.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 10, 0.3, 0.3, 0.3, 
                    org.bukkit.Material.DIRT.createBlockData());
            }
        }
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
