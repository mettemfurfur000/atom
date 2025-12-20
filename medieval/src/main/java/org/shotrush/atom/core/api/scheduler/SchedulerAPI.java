package org.shotrush.atom.core.api.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;


public class SchedulerAPI {
    
    private static Plugin plugin;
    
    public static void init(Plugin plugin) {
        SchedulerAPI.plugin = plugin;
    }

    
    public static ScheduledTask runTask(Entity entity, Runnable task) {
        return entity.getScheduler().run(plugin, t -> task.run(), null);
    }
    
    public static ScheduledTask runTaskLater(Entity entity, Runnable task, long delay) {
        return entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delay);
    }
    
    public static ScheduledTask runTaskTimer(Entity entity, Runnable task, long delay, long period) {
        return entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, delay, period);
    }
    
    public static ScheduledTask runTaskTimer(Entity entity, Consumer<ScheduledTask> task, long delay, long period) {
        return entity.getScheduler().runAtFixedRate(plugin, task, null, delay, period);
    }

    
    public static ScheduledTask runTask(Location location, Runnable task) {
        return plugin.getServer().getRegionScheduler().run(plugin, location, t -> task.run());
    }
    
    public static ScheduledTask runTaskLater(Location location, Runnable task, long delay) {
        return plugin.getServer().getRegionScheduler().runDelayed(plugin, location, t -> task.run(), delay);
    }
    
    public static ScheduledTask runTaskTimer(Location location, Runnable task, long delay, long period) {
        return plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, t -> task.run(), delay, period);
    }

    
    public static ScheduledTask runGlobalTask(Runnable task) {
        return plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> task.run());
    }
    
    public static ScheduledTask runGlobalTaskLater(Runnable task, long delay) {
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delay);
    }
    
    public static ScheduledTask runGlobalTaskTimer(Runnable task, long delay, long period) {
        return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delay, period);
    }
    

    
    public static ScheduledTask runAsync(Runnable task) {
        return plugin.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
    }
    
    public static ScheduledTask runAsyncLater(Runnable task, long delay) {
        return plugin.getServer().getAsyncScheduler().runDelayed(plugin, t -> task.run(), delay * 50, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    public static ScheduledTask runAsyncTimer(Runnable task, long delay, long period) {
        return plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), 
            delay * 50, period * 50, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
