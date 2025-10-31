package org.shotrush.atom.core;

import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.shotrush.atom.core.blocks.BlockType;
import org.shotrush.atom.core.blocks.CustomBlockRegistry;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.CustomItemRegistry;

import java.lang.reflect.Constructor;
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
                    Constructor<?> constructor = clazz.getConstructor(Plugin.class);
                    BlockType blockType = (BlockType) constructor.newInstance(plugin);
                    blockRegistry.register(blockType.getIdentifier(), blockType);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to auto-register block type: " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }
    }
}
