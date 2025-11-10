package org.shotrush.atom.core.workstations;

import lombok.Getter;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@RegisterSystem(
    id = "workstation_manager",
    priority = 3,
    toggleable = false,
    description = "Manages item placement data on CraftEngine workstation blocks"
)
public class WorkstationManager {
    
    @Getter
    public static WorkstationManager instance;
    private final Map<Location, WorkstationData> workstations = new HashMap<>();
    private final Plugin plugin;
    
    public WorkstationManager(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        loadWorkstations();
    }

    private Location toBlockLocation(Block block) {
        return new Location(
            block.getWorld(),
            block.getX(),
            block.getY(),
            block.getZ()
        );
    }
    
    public WorkstationData getOrCreate(Block block, String workstationType) {
        Location loc = toBlockLocation(block);
        return workstations.computeIfAbsent(loc, k -> new WorkstationData(k, workstationType));
    }
    
    public WorkstationData get(Block block) {
        Location lookupLoc = toBlockLocation(block);
        WorkstationData result = workstations.get(lookupLoc);

        if (result == null && !workstations.isEmpty()) {
            plugin.getLogger().info("DEBUG: Looking up " + lookupLoc.getBlockX() + "," + lookupLoc.getBlockY() + "," + lookupLoc.getBlockZ());
            plugin.getLogger().info("DEBUG: Map has " + workstations.size() + " entries:");
            for (Location loc : workstations.keySet()) {
                plugin.getLogger().info("  - " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + 
                    " (equals: " + loc.equals(lookupLoc) + ")");
            }
        }
        
        return result;
    }
    
    public WorkstationData get(Location location) {
        return workstations.get(location);
    }
    
    public int getWorkstationCount() {
        return workstations.size();
    }
    
    public void remove(Block block) {
        WorkstationData data = workstations.remove(toBlockLocation(block));
        if (data != null) {
            data.clearAllItems();
        }
    }
    
    public boolean isWorkstationBlock(Block block, String workstationType) {
        Key key = Key.of("atom:" + workstationType);
        return CraftEngineBlocks.getCustomBlockState(block) != null &&
               Objects.requireNonNull(CraftEngineBlocks.getCustomBlockState(block)).owner().matchesKey(key);
    }
    
    public void saveWorkstations() {
        File file = new File(plugin.getDataFolder(), "workstations.dat");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(workstations.size());
            
            for (Map.Entry<Location, WorkstationData> entry : workstations.entrySet()) {
                Location loc = entry.getKey();
                WorkstationData data = entry.getValue();
                
                plugin.getLogger().info("Saving workstation at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + 
                    " with " + data.getPlacedItems().size() + " items");
                
                
                out.writeUTF(loc.getWorld().getName());
                out.writeInt(loc.getBlockX());
                out.writeInt(loc.getBlockY());
                out.writeInt(loc.getBlockZ());
                
                
                String serialized = data.serialize();
                plugin.getLogger().info("Serialized data: " + serialized);
                out.writeUTF(serialized);
            }
            
            plugin.getLogger().info("Saved " + workstations.size() + " workstation(s)");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save workstations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadWorkstations() {
        File file = new File(plugin.getDataFolder(), "workstations.dat");
        if (!file.exists()) {
            plugin.getLogger().info("No workstations data file found");
            return;
        }
        
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int count = in.readInt();
            plugin.getLogger().info("Loading " + count + " workstation(s)");
            
            for (int i = 0; i < count; i++) {
                
                String worldName = in.readUTF();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World " + worldName + " not found, skipping workstation");
                    continue;
                }
                
                Location loc = new Location(world, x, y, z);
                
                
                String data = in.readUTF();
                plugin.getLogger().info("Loading workstation at " + x + "," + y + "," + z + " - data: " + data);
                WorkstationData workstation = WorkstationData.deserialize(loc, data);
                
                if (workstation != null) {
                    plugin.getLogger().info("Deserialized workstation with " + workstation.getPlacedItems().size() + " items");
                    workstations.put(loc, workstation);
                } else {
                    plugin.getLogger().warning("Failed to deserialize workstation data");
                }
            }
            
            plugin.getLogger().info("Loaded " + workstations.size() + " workstation(s)");
            
            
            for (WorkstationData data : workstations.values()) {
                org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(data.getBlockLocation(), () -> {
                    data.respawnAllDisplays();
                    plugin.getLogger().info("Respawned displays for workstation at " + 
                        data.getBlockLocation().getBlockX() + "," + 
                        data.getBlockLocation().getBlockY() + "," + 
                        data.getBlockLocation().getBlockZ());
                }, 40L);
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load workstations: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
