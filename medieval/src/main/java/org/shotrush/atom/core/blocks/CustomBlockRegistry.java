package org.shotrush.atom.core.blocks;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class CustomBlockRegistry {
    private final Atom plugin;
    private final Map<String, BlockType> blockTypes;
    private final Map<NamespacedKey, String> keyToIdentifier;

    public CustomBlockRegistry(Atom plugin) {
        this.plugin = plugin;
        this.blockTypes = new HashMap<>();
        this.keyToIdentifier = new HashMap<>();
    }

    
    public void register(Class<?> blockClass, Plugin plugin) throws Exception {
        CustomBlock prototypeBlock = null;
        Location dummyLoc = new Location(null, 0, 0, 0);
        try {
            prototypeBlock = (CustomBlock) blockClass
                .getConstructor(Location.class, BlockFace.class)
                .newInstance(dummyLoc, BlockFace.UP);
        } catch (NoSuchMethodException e) {
            try {
                prototypeBlock = (CustomBlock) blockClass
                    .getConstructor(Location.class, Location.class, BlockFace.class)
                    .newInstance(dummyLoc, dummyLoc, BlockFace.UP);
            } catch (NoSuchMethodException e2) {
                prototypeBlock = (CustomBlock) blockClass
                    .getConstructor(Location.class, BlockFace.class, boolean.class)
                    .newInstance(dummyLoc, BlockFace.UP, false);
            }
        }
        
        String identifier = prototypeBlock.getIdentifier();
        blockTypes.put(identifier, prototypeBlock);
        
        NamespacedKey key = new NamespacedKey(this.plugin, identifier + "_item");
        keyToIdentifier.put(key, identifier);
        
        this.plugin.getLogger().info("Registered block type: " + identifier + " (" + blockClass.getSimpleName() + ")");
    }
    
    public void register(String identifier, BlockType blockType) {
        blockTypes.put(identifier, blockType);
        
        NamespacedKey key = new NamespacedKey(plugin, identifier + "_item");
        keyToIdentifier.put(key, identifier);
        
        plugin.getLogger().info("Registered block type: " + identifier);
    }

    
    public BlockType getBlockType(String identifier) {
        return blockTypes.get(identifier);
    }

    
    public String getIdentifier(NamespacedKey key) {
        return keyToIdentifier.get(key);
    }

    
    public NamespacedKey getKey(String identifier) {
        return new NamespacedKey(plugin, identifier + "_item");
    }

    
    public Map<String, BlockType> getAllBlockTypes() {
        return new HashMap<>(blockTypes);
    }
}
