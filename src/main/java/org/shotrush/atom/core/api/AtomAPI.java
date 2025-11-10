package org.shotrush.atom.core.api;

import lombok.Getter;

import org.bukkit.plugin.Plugin;

import org.shotrush.atom.core.api.registry.SystemRegistry;

import java.util.List;


public final class AtomAPI {

    private static SystemRegistry systemRegistry;
    private static Plugin plugin;


    @Getter
    public static org.shotrush.atom.core.storage.DataStorage dataStorage;

    @Getter
    public static org.shotrush.atom.core.age.AgeManager ageManager;

    @Getter
    public static org.shotrush.atom.core.items.CustomItemRegistry itemRegistry;

    @Getter
    public static org.shotrush.atom.core.blocks.CustomBlockManager blockManager;

    private AtomAPI() {
    }


    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;

        plugin.getLogger().info("=== AtomAPI Initialization ===");


        plugin.getLogger().info("Initializing core APIs...");
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.init(plugin);
        org.shotrush.atom.core.api.BlockBreakSpeedAPI.initialize(plugin);


        plugin.getLogger().info("Initializing core components...");
        dataStorage = new org.shotrush.atom.core.storage.DataStorage(plugin);
        ageManager = new org.shotrush.atom.core.age.AgeManager(plugin, dataStorage);
        itemRegistry = new org.shotrush.atom.core.items.CustomItemRegistry(plugin);
        blockManager = new org.shotrush.atom.core.blocks.CustomBlockManager((org.shotrush.atom.Atom) plugin);


        systemRegistry = new SystemRegistry(plugin);

        plugin.getLogger().info("AtomAPI initialization complete!");
        plugin.getLogger().info("==============================");
    }


    public static void registerSystems() {
        if (systemRegistry == null) {
            throw new IllegalStateException("AtomAPI not initialized! Call initialize() first.");
        }
        systemRegistry.discoverAndRegister();
    }


    public static void registerAges() {
        if (ageManager == null) {
            throw new IllegalStateException("AtomAPI not initialized!");
        }
        plugin.getLogger().info("Registering ages...");
        org.shotrush.atom.core.AutoRegisterManager.registerAges(plugin, ageManager);
    }


    public static void registerItems() {
        if (itemRegistry == null) {
            throw new IllegalStateException("AtomAPI not initialized!");
        }
        plugin.getLogger().info("Registering items...");
        org.shotrush.atom.core.AutoRegisterManager.registerItems(plugin, itemRegistry);
    }


    public static void registerBlocks() {
        if (blockManager == null) {
            throw new IllegalStateException("AtomAPI not initialized!");
        }
        plugin.getLogger().info("Registering blocks...");
        org.shotrush.atom.core.AutoRegisterManager.registerBlocks(plugin, blockManager.getRegistry());
    }


    public static void registerCommands(co.aikar.commands.PaperCommandManager commandManager) {
        if (plugin == null) {
            throw new IllegalStateException("AtomAPI not initialized!");
        }
        plugin.getLogger().info("Registering commands...");
        org.shotrush.atom.core.AutoRegisterManager.registerCommands(plugin, commandManager);
    }


    public static void shutdown() {
        if (plugin == null) {
            return;
        }

        plugin.getLogger().info("=== AtomAPI Shutdown ===");


        if (blockManager != null) {
            plugin.getLogger().info("Cleaning up block manager...");
            blockManager.stopGlobalUpdate();
            blockManager.cleanupAllDisplays();
            blockManager.saveBlocks();
        }

        plugin.getLogger().info("AtomAPI shutdown complete!");
        plugin.getLogger().info("========================");


        systemRegistry = null;
        dataStorage = null;
        ageManager = null;
        itemRegistry = null;
        blockManager = null;
        plugin = null;
    }

    public static final class Player {
        public static org.shotrush.atom.core.api.player.PlayerDataAPI Data = null;
        public static org.shotrush.atom.core.api.player.AttributeModifierAPI Attributes = null;
    }

    public static final class Item {
        public static org.shotrush.atom.core.api.item.ItemQualityAPI Quality = null;
        public static org.shotrush.atom.core.api.item.QualityInheritanceAPI Inheritance = null;
    }

    public static final class World {
        public static org.shotrush.atom.core.api.world.EnvironmentalFactorAPI Environment = null;
    }

    public static final class Combat {
        public static org.shotrush.atom.core.api.combat.ArmorProtectionAPI Armor = null;
        public static org.shotrush.atom.core.api.combat.TemperatureEffectsAPI Temperature = null;
    }


    public static final class Systems {


        public static Object get(String id) {
            return systemRegistry != null ? systemRegistry.getSystem(id) : null;
        }


        public static Object getService(String serviceName) {
            return systemRegistry != null ? systemRegistry.getService(serviceName) : null;
        }


        public static <T> T getService(String serviceName, Class<T> type) {
            return systemRegistry != null ? systemRegistry.getService(serviceName, type) : null;
        }


        public static java.util.Set<String> list() {
            return systemRegistry != null ? systemRegistry.getSystemIds() : java.util.Collections.emptySet();
        }


        public static List<SystemRegistry.SystemStatus> status() {
            return systemRegistry != null ? systemRegistry.getSystemStatuses() : java.util.Collections.emptyList();
        }


        public static boolean enable(String id) {
            return systemRegistry != null && systemRegistry.enableSystem(id);
        }


        public static boolean disable(String id) {
            return systemRegistry != null && systemRegistry.disableSystem(id);
        }


        public static void setEnabled(String id, boolean enabled) {
            if (systemRegistry != null) {
                systemRegistry.setEnabled(id, enabled);
            }
        }
    }

    public static final class Scheduler {

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTask(org.bukkit.entity.Entity entity, Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(entity, task);
        }

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTaskLater(org.bukkit.entity.Entity entity, Runnable task, long delay) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(entity, task, delay);
        }

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTaskTimer(org.bukkit.entity.Entity entity, Runnable task, long delay, long period) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(entity, task, delay, period);
        }

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTask(org.bukkit.Location location, Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(location, task);
        }

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTaskLater(org.bukkit.Location location, Runnable task, long delay) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(location, task, delay);
        }

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runGlobalTask(Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTask(task);
        }

        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runAsync(Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runAsync(task);
        }
    }
}
