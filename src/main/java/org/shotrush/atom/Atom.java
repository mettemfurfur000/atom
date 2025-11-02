package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.shotrush.atom.content.systems.ItemHeatSystem;
import org.shotrush.atom.content.systems.PlayerAttributeModifier;
import org.shotrush.atom.content.systems.PlayerTemperatureSystem;
import org.shotrush.atom.content.systems.ThirstSystem;
import org.shotrush.atom.core.AutoRegisterManager;
import org.shotrush.atom.core.blocks.CustomBlockManager;
import org.shotrush.atom.content.mobs.AnimalBehaviorNew;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.commands.HerdCommand;
import org.shotrush.atom.content.mobs.MobScale;
import org.shotrush.atom.content.foragingage.throwing.SpearProjectileListener;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.recipe.RecipeManager;
import org.shotrush.atom.core.storage.DataStorage;
import org.shotrush.atom.core.skin.SkinListener;
import org.shotrush.atom.core.util.RightClickDetector;
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
    @Getter
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;

        dataStorage = new DataStorage(this);
        ageManager = new AgeManager(this, dataStorage);
        itemRegistry = new CustomItemRegistry(this);
        blockManager = new CustomBlockManager(this);
        recipeManager = new RecipeManager();
        
        AutoRegisterManager.registerAges(this, ageManager);
        AutoRegisterManager.registerItems(this, itemRegistry);
        AutoRegisterManager.registerBlocks(this, blockManager.getRegistry());
        AutoRegisterManager.registerRecipes(this, recipeManager);
        AutoRegisterManager.registerSystems(this);
        
        getServer().getPluginManager().registerEvents(new RightClickDetector(), this);
        getServer().getPluginManager().registerEvents(new SkinListener(), this);
        getServer().getPluginManager().registerEvents(new MobScale(this), this);
        
        AnimalBehaviorNew animalBehavior = new AnimalBehaviorNew(this);
        AnimalDomestication animalDomestication = new AnimalDomestication(this, animalBehavior.getHerdManager());
        getServer().getPluginManager().registerEvents(animalBehavior, this);
        getServer().getPluginManager().registerEvents(animalDomestication, this);
        
        getServer().getPluginManager().registerEvents(new SpearProjectileListener(this), this);
        
        setupCommands(animalBehavior);
        getLogger().info("Atom plugin has been enabled!");
    }
    
    private void setupCommands(AnimalBehaviorNew animalBehavior) {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        AutoRegisterManager.registerCommands(this, commandManager);
        commandManager.registerCommand(new HerdCommand(animalBehavior.getHerdManager()));
    }
    public void onDisable() {
        if (blockManager != null) {
            blockManager.stopGlobalUpdate();
            blockManager.cleanupAllDisplays();
            blockManager.saveBlocks();
        }
        
        getLogger().info("Atom plugin has been disabled!");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return RockChunkGenerator.INSTANCE;
    }
}
