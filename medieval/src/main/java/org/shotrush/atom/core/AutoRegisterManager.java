package org.shotrush.atom.core;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.shotrush.atom.core.age.Age;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.age.AgeProvider;
import org.shotrush.atom.core.blocks.BlockType;
import org.shotrush.atom.core.blocks.CustomBlockRegistry;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.CustomItemRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class AutoRegisterManager {
    
    public static void registerItems(Plugin plugin, CustomItemRegistry itemRegistry) {
        Reflections reflections = new Reflections("org.shotrush.atom");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(
            org.shotrush.atom.core.items.annotation.AutoRegister.class
        );
        
        List<Class<?>> sortedClasses = new ArrayList<>(annotatedClasses);
        sortedClasses.sort(Comparator.comparingInt(cls -> 
            cls.getAnnotation(org.shotrush.atom.core.items.annotation.AutoRegister.class).priority()
        ));
        
        for (Class<?> clazz : sortedClasses) {
            if (CustomItem.class.isAssignableFrom(clazz)) {
                try {
                    Constructor<?> constructor = clazz.getConstructor(Plugin.class);
                    CustomItem item = (CustomItem) constructor.newInstance(plugin);
                    itemRegistry.register(item);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to auto-register item: " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void registerBlocks(Plugin plugin, CustomBlockRegistry blockRegistry) {
        Reflections reflections = new Reflections("org.shotrush.atom");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(
            org.shotrush.atom.core.blocks.annotation.AutoRegister.class
        );
        
        List<Class<?>> sortedClasses = new ArrayList<>(annotatedClasses);
        sortedClasses.sort(Comparator.comparingInt(cls -> 
            cls.getAnnotation(org.shotrush.atom.core.blocks.annotation.AutoRegister.class).priority()
        ));
        
        for (Class<?> clazz : sortedClasses) {
            if (BlockType.class.isAssignableFrom(clazz)) {
                try {
                    if (org.shotrush.atom.core.blocks.CustomBlock.class.isAssignableFrom(clazz)) {
                        blockRegistry.register(clazz, plugin);
                    } else {
                        Constructor<?> constructor;
                        BlockType blockType;
                        try {
                            constructor = clazz.getConstructor(org.shotrush.atom.Atom.class);
                            blockType = (BlockType) constructor.newInstance(plugin);
                        } catch (NoSuchMethodException e) {
                            constructor = clazz.getConstructor(Plugin.class);
                            blockType = (BlockType) constructor.newInstance(plugin);
                        }
                        
                        blockRegistry.register(blockType.getIdentifier(), blockType);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to auto-register block type: " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void registerCommands(Plugin plugin, PaperCommandManager commandManager) {
        Reflections reflections = new Reflections("org.shotrush.atom");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(
            org.shotrush.atom.commands.annotation.AutoRegister.class
        );
        
        List<Class<?>> sortedClasses = new ArrayList<>(annotatedClasses);
        sortedClasses.sort(Comparator.comparingInt(cls -> 
            cls.getAnnotation(org.shotrush.atom.commands.annotation.AutoRegister.class).priority()
        ));
        
        for (Class<?> clazz : sortedClasses) {
            if (BaseCommand.class.isAssignableFrom(clazz)) {
                try {
                    Constructor<?> constructor;
                    BaseCommand command;
                    try {
                        constructor = clazz.getConstructor(Plugin.class);
                        command = (BaseCommand) constructor.newInstance(plugin);
                    } catch (NoSuchMethodException e) {
                        constructor = clazz.getConstructor();
                        command = (BaseCommand) constructor.newInstance();
                    }
                    
                    commandManager.registerCommand(command);
                    plugin.getLogger().info("Auto-registered command: " + clazz.getSimpleName());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to auto-register command: " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void registerAges(Plugin plugin, AgeManager ageManager) {
        Reflections reflections = new Reflections("org.shotrush.atom");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(
            org.shotrush.atom.core.age.annotation.AutoRegisterAge.class
        );
        
        List<Class<?>> sortedClasses = new ArrayList<>(annotatedClasses);
        sortedClasses.sort(Comparator.comparingInt(cls -> 
            cls.getAnnotation(org.shotrush.atom.core.age.annotation.AutoRegisterAge.class).order()
        ));
        
        List<Age> ages = new ArrayList<>();
        for (Class<?> clazz : sortedClasses) {
            if (AgeProvider.class.isAssignableFrom(clazz)) {
                try {
                    AgeProvider provider = (AgeProvider) clazz.getConstructor().newInstance();
                    Age age = provider.createAge();
                    ages.add(age);
                    plugin.getLogger().info("Auto-registered age: " + age.getDisplayName() + " (order: " + age.getOrder() + ")");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to auto-register age: " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }
        
        if (!ages.isEmpty()) {
            ageManager.registerAges(ages.toArray(new Age[0]));
            ageManager.setAge(ages.get(0));
        }
    }
    
    
    
    
    public static void registerSystems(Plugin plugin) {
        Reflections reflections = new Reflections("org.shotrush.atom");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(
            org.shotrush.atom.core.systems.annotation.AutoRegisterSystem.class
        );
        
        List<Class<?>> sortedClasses = new ArrayList<>(annotatedClasses);
        sortedClasses.sort(Comparator.comparingInt(cls -> 
            cls.getAnnotation(org.shotrush.atom.core.systems.annotation.AutoRegisterSystem.class).priority()
        ));
        
        for (Class<?> clazz : sortedClasses) {
            try {
                Constructor<?> constructor;
                Object instance;
                
                try {
                    constructor = clazz.getConstructor(Plugin.class);
                    instance = constructor.newInstance(plugin);
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor();
                    instance = constructor.newInstance();
                }
                
                if (instance instanceof org.bukkit.event.Listener listener) {
                    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                }
                
                plugin.getLogger().info("Auto-registered system: " + clazz.getSimpleName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to auto-register system: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }
}
