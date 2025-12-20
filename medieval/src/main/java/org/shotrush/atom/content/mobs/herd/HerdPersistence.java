package org.shotrush.atom.content.mobs.herd;

import org.bukkit.entity.Animals;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.data.PersistentData;

import java.util.UUID;

public final class HerdPersistence {
    
    public HerdPersistence(Plugin plugin) {
    }
    
    public void saveHerdData(Animals animal, UUID herdId, boolean isLeader, boolean isAggressive, double maxStamina, double stamina) {
        PersistentData.set(animal, "herd_id", herdId.toString());
        PersistentData.set(animal, "is_leader", isLeader);
        PersistentData.set(animal, "is_aggressive", isAggressive);
        PersistentData.set(animal, "max_stamina", maxStamina);
        PersistentData.set(animal, "stamina", stamina);
    }
    
    public boolean hasHerdData(Animals animal) {
        return PersistentData.has(animal, "herd_id");
    }
    
    public UUID getHerdId(Animals animal) {
        String idString = PersistentData.getString(animal, "herd_id", null);
        if (idString == null) return null;
        
        try {
            return UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public boolean isLeader(Animals animal) {
        return PersistentData.getBoolean(animal, "is_leader", false);
    }
    
    public boolean isAggressive(Animals animal) {
        return PersistentData.getBoolean(animal, "is_aggressive", false);
    }
    
    public double getMaxStamina(Animals animal, double defaultValue) {
        return PersistentData.getDouble(animal, "max_stamina", defaultValue);
    }
    
    public double getStamina(Animals animal, double defaultValue) {
        return PersistentData.getDouble(animal, "stamina", defaultValue);
    }
    
    public void updateStamina(Animals animal, double stamina) {
        PersistentData.set(animal, "stamina", stamina);
    }
    
    public void clearHerdData(Animals animal) {
        PersistentData.remove(animal, "herd_id");
        PersistentData.remove(animal, "is_leader");
        PersistentData.remove(animal, "is_aggressive");
        PersistentData.remove(animal, "max_stamina");
        PersistentData.remove(animal, "stamina");
    }
}
