package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.core.AutoRegisterManager;
import org.shotrush.atom.core.blocks.CustomBlockManager;
import org.shotrush.atom.commands.*;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.storage.DataStorage;
import org.shotrush.atom.core.skin.SkinListener;

public final class Atom extends JavaPlugin {

    @Getter
    private static Atom instance;
    @Getter
    private CustomBlockManager blockManager;
    @Getter
    private CustomItemRegistry itemRegistry;
    @Getter
    private DataStorage dataStorage;
    @Getter
    private AgeManager ageManager;

    @Override
    public void onEnable() {
        instance = this;
        
        
        dataStorage = new DataStorage(this);
        ageManager = new AgeManager(this, dataStorage);
        itemRegistry = new CustomItemRegistry(this);
        blockManager = new CustomBlockManager(this);
        
        AutoRegisterManager.registerItems(this, itemRegistry);
        AutoRegisterManager.registerBlocks(this, blockManager.getRegistry());
        
        getServer().getPluginManager().registerEvents(new SkinListener(), this);
        
        setupCommands();
        
        getLogger().info("Atom plugin has been enabled!");
    }
    
    private void setupCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        
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

}
