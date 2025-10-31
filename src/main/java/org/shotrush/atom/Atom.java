package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.core.blocks.CustomBlockManager;
import org.shotrush.atom.commands.*;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.storage.DataStorage;
import org.shotrush.atom.content.tools.WrenchItem;

public final class Atom extends JavaPlugin {

    private static Atom instance;
    private CustomBlockManager blockManager;
    private CustomItemRegistry itemRegistry;
    private DataStorage dataStorage;
    private AgeManager ageManager;
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize core systems
        dataStorage = new DataStorage(this);
        ageManager = new AgeManager(this, dataStorage);
        itemRegistry = new CustomItemRegistry(this);
        
        // Register custom items
        itemRegistry.register(new WrenchItem(this));
        
        // Initialize block system
        blockManager = new CustomBlockManager(this);
        
        // Setup commands
        setupCommands();
        
        getLogger().info("Atom plugin has been enabled!");
    }
    
    private void setupCommands() {
        commandManager = new PaperCommandManager(this);
        
        commandManager.registerCommand(new CogCommand());
        commandManager.registerCommand(new WrenchCommand());
        commandManager.registerCommand(new RemoveCogsCommand());
        commandManager.registerCommand(new AnvilCommand());
        commandManager.registerCommand(new AgeCommand(this));
    }

    @Override
    public void onDisable() {
        if (blockManager != null) {
            blockManager.stopGlobalUpdate();
            blockManager.saveBlocks();
        }
        
        getLogger().info("Atom plugin has been disabled!");
    }
    
    public static Atom getInstance() {
        return instance;
    }
    
    public CustomBlockManager getBlockManager() {
        return blockManager;
    }
    
    public DataStorage getDataStorage() {
        return dataStorage;
    }
    
    public AgeManager getAgeManager() {
        return ageManager;
    }
    
    public CustomItemRegistry getItemRegistry() {
        return itemRegistry;
    }
}
