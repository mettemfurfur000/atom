package org.shotrush.atom.core.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;

import java.util.UUID;

public class StorageExample {
    
    public static void savePlayerData(Player player, String key, Object value) {
        DataStorage storage = Atom.getInstance().getDataStorage();
        UUID uuid = player.getUniqueId();
        
        YamlConfiguration config = storage.getPlayerData(uuid);
        config.set(key, value);
        storage.savePlayerData(uuid, config);
    }
    
    public static Object getPlayerData(Player player, String key) {
        DataStorage storage = Atom.getInstance().getDataStorage();
        UUID uuid = player.getUniqueId();
        
        YamlConfiguration config = storage.getPlayerData(uuid);
        return config.get(key);
    }
    
    public static void saveServerData(String key, Object value) {
        DataStorage storage = Atom.getInstance().getDataStorage();
        
        YamlConfiguration config = storage.getServerData();
        config.set(key, value);
        storage.saveServerData(config);
    }
    
    public static Object getServerData(String key) {
        DataStorage storage = Atom.getInstance().getDataStorage();
        
        YamlConfiguration config = storage.getServerData();
        return config.get(key);
    }
    
    public static void examplePlayerStats(Player player) {
        DataStorage storage = Atom.getInstance().getDataStorage();
        UUID uuid = player.getUniqueId();
        
        YamlConfiguration config = storage.getPlayerData(uuid);
        
        int kills = config.getInt("stats.kills", 0);
        int deaths = config.getInt("stats.deaths", 0);
        long playtime = config.getLong("stats.playtime", 0L);
        
        config.set("stats.kills", kills + 1);
        config.set("stats.last_login", System.currentTimeMillis());
        
        storage.savePlayerData(uuid, config);
    }
    
    public static void exampleServerConfig() {
        DataStorage storage = Atom.getInstance().getDataStorage();
        
        YamlConfiguration config = storage.getServerData();
        
        config.set("settings.pvp_enabled", true);
        config.set("settings.difficulty", "hard");
        config.set("stats.total_players", config.getInt("stats.total_players", 0) + 1);
        
        storage.saveServerData(config);
    }
}
