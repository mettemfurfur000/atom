package org.shotrush.atom.core.api.player;

import org.bukkit.entity.Player;
import org.shotrush.atom.core.data.PersistentData;

public class PlayerDataAPI {
    
    public static int getInt(Player player, String key, int defaultValue) {
        return PersistentData.getInt(player, key, defaultValue);
    }
    
    public static void setInt(Player player, String key, int value) {
        PersistentData.set(player, key, value);
    }
    
    public static void incrementInt(Player player, String key, int defaultValue) {
        setInt(player, key, getInt(player, key, defaultValue) + 1);
    }
    
    public static double getDouble(Player player, String key, double defaultValue) {
        return PersistentData.getDouble(player, key, defaultValue);
    }
    
    public static void setDouble(Player player, String key, double value) {
        PersistentData.set(player, key, value);
    }
    
    public static boolean getBoolean(Player player, String key, boolean defaultValue) {
        return PersistentData.getBoolean(player, key, defaultValue);
    }
    
    public static void setBoolean(Player player, String key, boolean value) {
        PersistentData.set(player, key, value);
    }
    
    public static String getString(Player player, String key, String defaultValue) {
        return PersistentData.getString(player, key, defaultValue);
    }
    
    public static void setString(Player player, String key, String value) {
        PersistentData.set(player, key, value);
    }
    
    public static class BatchUpdate {
        private final Player player;
        
        private BatchUpdate(Player player) {
            this.player = player;
        }
        
        public BatchUpdate setInt(String key, int value) {
            PersistentData.set(player, key, value);
            return this;
        }
        
        public BatchUpdate setDouble(String key, double value) {
            PersistentData.set(player, key, value);
            return this;
        }
        
        public BatchUpdate setBoolean(String key, boolean value) {
            PersistentData.set(player, key, value);
            return this;
        }
        
        public BatchUpdate setString(String key, String value) {
            PersistentData.set(player, key, value);
            return this;
        }
        
        public BatchUpdate increment(String key, int defaultValue) {
            int current = PersistentData.getInt(player, key, defaultValue);
            PersistentData.set(player, key, current + 1);
            return this;
        }
        
        public void save() {
            // No-op for PDC as data is saved automatically
        }
    }
    
    public static BatchUpdate batch(Player player) {
        return new BatchUpdate(player);
    }
}
