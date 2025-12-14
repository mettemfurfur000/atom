package org.shotrush.atom.content.mobs.ai.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.Optional;
import java.util.UUID;

public class MoraleSystem {
    
    private final Plugin plugin;
    private final HerdManager herdManager;
    private static final double MORALE_BREAK_THRESHOLD = 0.5;
    private static final double NEARBY_CHECK_RADIUS = 20.0;
    private static final int REGROUP_DURATION = 100;
    
    public MoraleSystem(Plugin plugin, HerdManager herdManager) {
        this.plugin = plugin;
        this.herdManager = herdManager;
    }
    
    public boolean checkMorale(Mob mob) {
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isEmpty()) {
            return true;
        }
        
        Herd herd = herdOpt.get();
        int totalMembers = 0;
        int deadOrFledMembers = 0;
        
        for (UUID memberId : herd.members()) {
            Mob member = (Mob) Bukkit.getEntity(memberId);
            
            if (member == null || !member.isValid() || member.isDead()) {
                deadOrFledMembers++;
                totalMembers++;
                continue;
            }
            
            if (member.getLocation().distance(mob.getLocation()) > NEARBY_CHECK_RADIUS) {
                continue;
            }
            
            totalMembers++;
            
            if (member.hasMetadata("fleeing") && member.getMetadata("fleeing").get(0).asBoolean()) {
                deadOrFledMembers++;
            }
        }
        
        if (totalMembers == 0) {
            return true;
        }
        
        double lossRatio = (double) deadOrFledMembers / totalMembers;
        
        if (lossRatio > MORALE_BREAK_THRESHOLD) {
            breakMorale(mob);
            return false;
        }
        
        return true;
    }
    
    public boolean isMoraleBroken(Mob mob) {
        if (!mob.hasMetadata("morale_broken")) {
            return false;
        }
        
        boolean broken = mob.getMetadata("morale_broken").get(0).asBoolean();
        
        if (broken) {
            int breakTimer = mob.hasMetadata("morale_break_timer") 
                ? mob.getMetadata("morale_break_timer").get(0).asInt() 
                : 0;
            
            breakTimer++;
            
            if (breakTimer >= REGROUP_DURATION) {
                restoreMorale(mob);
                return false;
            }
            
            mob.setMetadata("morale_break_timer", new FixedMetadataValue(plugin, breakTimer));
        }
        
        return broken;
    }
    
    public void breakMorale(Mob mob) {
        mob.setMetadata("morale_broken", new FixedMetadataValue(plugin, true));
        mob.setMetadata("morale_break_timer", new FixedMetadataValue(plugin, 0));
        mob.setMetadata("fleeing", new FixedMetadataValue(plugin, true));
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent()) {
            Herd herd = herdOpt.get();
            for (UUID memberId : herd.members()) {
                Mob member = (Mob) Bukkit.getEntity(memberId);
                if (member != null && member.isValid() && !member.isDead()) {
                    if (member.getLocation().distance(mob.getLocation()) <= NEARBY_CHECK_RADIUS) {
                        member.setMetadata("morale_broken", new FixedMetadataValue(plugin, true));
                        member.setMetadata("morale_break_timer", new FixedMetadataValue(plugin, 0));
                        member.setMetadata("fleeing", new FixedMetadataValue(plugin, true));
                    }
                }
            }
        }
    }
    
    private void restoreMorale(Mob mob) {
        mob.removeMetadata("morale_broken", plugin);
        mob.removeMetadata("morale_break_timer", plugin);
        
        Optional<Herd> herdOpt = herdManager.getHerd(mob.getUniqueId());
        if (herdOpt.isPresent()) {
            Herd herd = herdOpt.get();
            int nearbyAllies = 0;
            
            for (UUID memberId : herd.members()) {
                Mob member = (Mob) Bukkit.getEntity(memberId);
                if (member != null && member.isValid() && !member.isDead()) {
                    if (member.getLocation().distance(mob.getLocation()) <= NEARBY_CHECK_RADIUS) {
                        nearbyAllies++;
                    }
                }
            }
            
            if (nearbyAllies >= 2) {
                mob.removeMetadata("fleeing", plugin);
            }
        }
    }
}
