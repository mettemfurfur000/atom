package org.shotrush.atom.core.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataStorage {
    
    private final Plugin plugin;
    private final File playerDataFolder;
    private final File serverDataFile;
    
    public DataStorage(Plugin plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.serverDataFile = new File(plugin.getDataFolder(), "serverdata.yml");
        
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }
    
    public YamlConfiguration getPlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create player data file for " + uuid);
            }
        }
        
        return YamlConfiguration.loadConfiguration(playerFile);
    }
    
    public void savePlayerData(UUID uuid, YamlConfiguration config) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player data for " + uuid);
        }
    }
    
    public YamlConfiguration getServerData() {
        if (!serverDataFile.exists()) {
            try {
                serverDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create server data file");
            }
        }
        
        return YamlConfiguration.loadConfiguration(serverDataFile);
    }
    
    public void saveServerData(YamlConfiguration config) {
        try {
            config.save(serverDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save server data");
        }
    }
    
    public boolean hasPlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        return playerFile.exists();
    }
    
    public void deletePlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
}
