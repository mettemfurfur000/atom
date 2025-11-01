package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.shotrush.atom.core.AutoRegisterManager;
import org.shotrush.atom.core.blocks.CustomBlockManager;
import org.shotrush.atom.content.mobs.AnimalBehavior;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.MobScale;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.storage.DataStorage;
import org.shotrush.atom.core.skin.SkinListener;
import org.shotrush.atom.world.RockChunkGenerator;

public final class Atom extends JavaPlugin {

    @Getter
    public static Atom instance;
    @Getter
    public CustomBlockManager blockManager;
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
        
        AutoRegisterManager.registerAges(this, ageManager);
        AutoRegisterManager.registerItems(this, itemRegistry);
        AutoRegisterManager.registerBlocks(this, blockManager.getRegistry());
        
        getServer().getPluginManager().registerEvents(new SkinListener(), this);
        getServer().getPluginManager().registerEvents(new MobScale(this), this);
        getServer().getPluginManager().registerEvents(new AnimalBehavior(this), this);
        getServer().getPluginManager().registerEvents(new AnimalDomestication(this), this);
        
        setupCommands();
        getLogger().info("Atom plugin has been enabled!");
    }
    
    private void setupCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        AutoRegisterManager.registerCommands(this, commandManager);
    }
    public void onDisable() {
        if (blockManager != null) {
            blockManager.stopGlobalUpdate();
            blockManager.saveBlocks();
        }
        
        getLogger().info("Atom plugin has been disabled!");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return RockChunkGenerator.INSTANCE;
    }
}
