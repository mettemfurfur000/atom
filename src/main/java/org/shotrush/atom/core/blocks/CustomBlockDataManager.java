package org.shotrush.atom.core.blocks;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CustomBlockDataManager {
    private final Atom plugin;
    private final CustomBlockRegistry registry;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public CustomBlockDataManager(Atom plugin, CustomBlockRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.dataFile = new File(plugin.getDataFolder(), "blocks.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create blocks.yml!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    
    public void saveBlocks(List<CustomBlock> blocks) {
        dataConfig.set("blocks", null);

        
        Map<String, List<String>> blocksByType = new HashMap<>();
        
        for (CustomBlock block : blocks) {
            if (block.isValid()) {
                String type = block.getBlockType();
                String data = block.serialize();
                blocksByType.computeIfAbsent(type, k -> new ArrayList<>()).add(data);
            }
        }
        
        
        for (Map.Entry<String, List<String>> entry : blocksByType.entrySet()) {
            dataConfig.set("blocks." + entry.getKey(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blocks.yml!");
            e.printStackTrace();
        }
    }

    
    public List<CustomBlock> loadBlocks() {
        List<CustomBlock> blocks = new ArrayList<>();
        
        if (!dataConfig.contains("blocks")) {
            return blocks;
        }
        
        for (String typeId : dataConfig.getConfigurationSection("blocks").getKeys(false)) {
            BlockType blockType = registry.getBlockType(typeId);
            if (blockType == null) {
                plugin.getLogger().warning("Unknown block type: " + typeId);
                continue;
            }
            
            List<String> blockData = dataConfig.getStringList("blocks." + typeId);
            for (String data : blockData) {
                try {
                    CustomBlock block = blockType.deserialize(data);
                    if (block != null) {
                        blocks.add(block);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load block: " + data);
                    e.printStackTrace();
                }
            }
        }
        
        return blocks;
    }
}
