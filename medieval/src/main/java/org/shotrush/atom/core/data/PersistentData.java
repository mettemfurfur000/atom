package org.shotrush.atom.core.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class PersistentData {
    
    private static final String NAMESPACE = "atom";
    
    public static NamespacedKey key(String key) {
        return new NamespacedKey(NAMESPACE, key);
    }
    
    public static void set(PersistentDataHolder holder, String key, byte value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.BYTE, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, short value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.SHORT, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, int value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.INTEGER, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, long value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.LONG, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, float value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.FLOAT, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, double value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.DOUBLE, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, String value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value);
    }
    
    public static void set(PersistentDataHolder holder, String key, boolean value) {
        holder.getPersistentDataContainer().set(key(key), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }
    
    public static byte getByte(PersistentDataHolder holder, String key, byte defaultValue) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.BYTE, defaultValue);
    }
    
    public static short getShort(PersistentDataHolder holder, String key, short defaultValue) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.SHORT, defaultValue);
    }
    
    public static int getInt(PersistentDataHolder holder, String key, int defaultValue) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.INTEGER, defaultValue);
    }
    
    public static long getLong(PersistentDataHolder holder, String key, long defaultValue) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.LONG, defaultValue);
    }
    
    public static float getFloat(PersistentDataHolder holder, String key, float defaultValue) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.FLOAT, defaultValue);
    }
    
    public static double getDouble(PersistentDataHolder holder, String key, double defaultValue) {
        PersistentDataContainer container = holder.getPersistentDataContainer();
        NamespacedKey namespacedKey = key(key);
        if (container.has(namespacedKey, PersistentDataType.INTEGER)) {
            int value = container.getOrDefault(namespacedKey, PersistentDataType.INTEGER, (int) defaultValue);
            container.set(namespacedKey, PersistentDataType.DOUBLE, (double) value);
            return (double) value;
        }

        return container.getOrDefault(namespacedKey, PersistentDataType.DOUBLE, defaultValue);
    }
    
    public static String getString(PersistentDataHolder holder, String key, String defaultValue) {
        return holder.getPersistentDataContainer().getOrDefault(key(key), PersistentDataType.STRING, defaultValue);
    }
    
    public static boolean getBoolean(PersistentDataHolder holder, String key, boolean defaultValue) {
        return getByte(holder, key, (byte) (defaultValue ? 1 : 0)) == 1;
    }
    
    public static boolean has(PersistentDataHolder holder, String key) {
        return holder.getPersistentDataContainer().has(key(key));
    }
    
    public static void remove(PersistentDataHolder holder, String key) {
        holder.getPersistentDataContainer().remove(key(key));
    }
    
    public static void flag(PersistentDataHolder holder, String key) {
        set(holder, key, true);
    }
    
    public static boolean isFlagged(PersistentDataHolder holder, String key) {
        return has(holder, key);
    }
}
