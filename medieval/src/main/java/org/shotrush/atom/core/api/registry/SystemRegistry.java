package org.shotrush.atom.core.api.registry;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;


public class SystemRegistry {
    
    private final Plugin plugin;
    private final Map<String, RegisteredSystem> systems = new LinkedHashMap<>();
    private final Map<String, Boolean> featureFlags = new HashMap<>();
    private final Map<String, Object> services = new HashMap<>();  
    
    public SystemRegistry(Plugin plugin) {
        this.plugin = plugin;
    }
    
    
    public void discoverAndRegister() {
        plugin.getLogger().info("=== AtomAPI System Registration ===");
        
        try {
            Reflections reflections = new Reflections("org.shotrush.atom");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(RegisterSystem.class);
            
            
            List<SystemInfo> systemInfos = new ArrayList<>();
            for (Class<?> clazz : annotatedClasses) {
                RegisterSystem annotation = clazz.getAnnotation(RegisterSystem.class);
                String id = annotation.id().isEmpty() ? 
                    toSnakeCase(clazz.getSimpleName()) : annotation.id();
                
                systemInfos.add(new SystemInfo(id, clazz, annotation));
            }
            
            
            systemInfos.sort(Comparator.comparingInt(s -> s.annotation.priority()));
            
            
            for (SystemInfo info : systemInfos) {
                registerSystem(info);
            }
            
            plugin.getLogger().info("Registered " + systems.size() + " systems through AtomAPI");
            plugin.getLogger().info("===================================");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to discover systems: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private void registerSystem(SystemInfo info) {
        String id = info.id;
        RegisterSystem annotation = info.annotation;
        
        
        if (annotation.toggleable() && !isEnabled(id, annotation.enabledByDefault())) {
            plugin.getLogger().info("  [SKIP] " + id + " (disabled in config)");
            return;
        }
        
        
        for (String dep : annotation.dependencies()) {
            if (!systems.containsKey(dep)) {
                plugin.getLogger().warning("  [SKIP] " + id + " (missing dependency: " + dep + ")");
                return;
            }
            if (!systems.get(dep).enabled) {
                plugin.getLogger().warning("  [SKIP] " + id + " (dependency disabled: " + dep + ")");
                return;
            }
        }
        
        
        for (String service : annotation.requires()) {
            if (!services.containsKey(service)) {
                plugin.getLogger().warning("  [SKIP] " + id + " (missing required service: " + service + ")");
                return;
            }
        }
        
        
        try {
            Constructor<?> constructor = info.clazz.getConstructor(Plugin.class);
            Object instance = constructor.newInstance(plugin);
            
            
            if (instance instanceof Listener) {
                plugin.getServer().getPluginManager().registerEvents((Listener) instance, plugin);
            }
            
            RegisteredSystem system = new RegisteredSystem(
                id, instance, annotation, true
            );
            systems.put(id, system);
            
            
            for (String service : annotation.provides()) {
                services.put(service, instance);
                plugin.getLogger().info("    → Provides service: " + service);
            }
            
            String desc = annotation.description().isEmpty() ? "" : " - " + annotation.description();
            String toggle = annotation.toggleable() ? " [toggleable]" : " [core]";
            String serviceInfo = annotation.provides().length > 0 ? 
                " [provides: " + String.join(", ", annotation.provides()) + "]" : "";
            plugin.getLogger().info("  [OK] " + id + " (priority: " + annotation.priority() + ")" + toggle + serviceInfo + desc);
            
        } catch (Exception e) {
            plugin.getLogger().severe("  [FAIL] " + id + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private boolean isEnabled(String id, boolean defaultValue) {
        return featureFlags.getOrDefault(id, defaultValue);
    }
    
    
    public void setEnabled(String id, boolean enabled) {
        featureFlags.put(id, enabled);
    }
    
    
    public Object getSystem(String id) {
        RegisteredSystem system = systems.get(id);
        return system != null ? system.instance : null;
    }
    
    
    public Object getService(String serviceName) {
        return services.get(serviceName);
    }
    
    
    @SuppressWarnings("unchecked")
    public <T> T getService(String serviceName, Class<T> type) {
        Object service = services.get(serviceName);
        if (service != null && type.isInstance(service)) {
            return (T) service;
        }
        return null;
    }
    
    
    public Set<String> getSystemIds() {
        return new HashSet<>(systems.keySet());
    }
    
    
    public boolean disableSystem(String id) {
        RegisteredSystem system = systems.get(id);
        if (system == null || !system.annotation.toggleable()) {
            return false;
        }
        
        
        if (system.instance instanceof Listener) {
            HandlerList.unregisterAll((Listener) system.instance);
        }
        
        system.enabled = false;
        plugin.getLogger().info("Disabled system: " + id);
        return true;
    }
    
    
    public boolean enableSystem(String id) {
        RegisteredSystem system = systems.get(id);
        if (system == null || !system.annotation.toggleable()) {
            return false;
        }
        
        
        if (system.instance instanceof Listener) {
            plugin.getServer().getPluginManager().registerEvents((Listener) system.instance, plugin);
        }
        
        system.enabled = true;
        plugin.getLogger().info("Enabled system: " + id);
        return true;
    }
    
    
    public List<SystemStatus> getSystemStatuses() {
        return systems.entrySet().stream()
            .map(e -> new SystemStatus(
                e.getKey(),
                e.getValue().instance.getClass().getSimpleName(),
                e.getValue().enabled,
                e.getValue().annotation.toggleable(),
                e.getValue().annotation.description()
            ))
            .collect(Collectors.toList());
    }
    
    
    private String toSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    
    private static class SystemInfo {
        final String id;
        final Class<?> clazz;
        final RegisterSystem annotation;
        
        SystemInfo(String id, Class<?> clazz, RegisterSystem annotation) {
            this.id = id;
            this.clazz = clazz;
            this.annotation = annotation;
        }
    }
    
    
    private static class RegisteredSystem {
        final String id;
        final Object instance;
        final RegisterSystem annotation;
        boolean enabled;
        
        RegisteredSystem(String id, Object instance, RegisterSystem annotation, boolean enabled) {
            this.id = id;
            this.instance = instance;
            this.annotation = annotation;
            this.enabled = enabled;
        }
    }
    
    
    public static class SystemStatus {
        public final String id;
        public final String className;
        public final boolean enabled;
        public final boolean toggleable;
        public final String description;
        
        public SystemStatus(String id, String className, boolean enabled, boolean toggleable, String description) {
            this.id = id;
            this.className = className;
            this.enabled = enabled;
            this.toggleable = toggleable;
            this.description = description;
        }
    }
}
