package org.shotrush.atom.content.anvil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.BlockType;
import org.shotrush.atom.core.blocks.CustomBlock;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;

@AutoRegister(priority = 20)
public class AnvilSurfaceType implements BlockType {
    private final Atom plugin;
    
    public AnvilSurfaceType(Atom plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "anvil_surface";
    }
    
    @Override
    public String getDisplayName() {
        return "§7⚒ Anvil Surface";
    }
    
    @Override
    public Material getItemMaterial() {
        return Material.ANVIL;
    }
    
    @Override
    public String[] getLore() {
        return new String[]{
            "§7Place items on this surface",
            "§8• Right-click: Place item",
            "§8• Shift + Right-click: Remove item"
        };
    }
    
    @Override
    public CustomBlock createBlock(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        return new AnvilSurface(spawnLocation, blockLocation, blockFace);
    }
    
    @Override
    public CustomBlock deserialize(String data) {
        String[] parts = data.split(";");
        World world = Bukkit.getWorld(parts[0]);
        Location spawnLocation = new Location(world, 
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3])
        );
        BlockFace face = BlockFace.valueOf(parts[4]);
        AnvilSurface surface = new AnvilSurface(spawnLocation, face);
        
        int itemCount = Integer.parseInt(parts[5]);
        for (int i = 0; i < itemCount; i++) {
            String[] itemData = parts[6 + i].split(",");
            Material mat = Material.valueOf(itemData[0]);
            Vector3f pos = new Vector3f(
                Float.parseFloat(itemData[1]),
                Float.parseFloat(itemData[2]),
                Float.parseFloat(itemData[3])
            );
            float yaw = itemData.length > 4 ? Float.parseFloat(itemData[4]) : 0f;
            surface.placeItem(new ItemStack(mat), pos, yaw);
        }
        
        return surface;
    }
    
    @Override
    public boolean requiresUpdate() {
        return false;
    }
    
    @Override
    public ItemStack getDropItem() {
        return new ItemStack(Material.ANVIL);
    }
}
