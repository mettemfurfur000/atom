package org.shotrush.atom.content.cog;

import org.shotrush.atom.core.blocks.annotation.AutoRegister;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;
import org.shotrush.atom.content.cog.*;
import org.shotrush.atom.core.items.CustomItem;;


@AutoRegister(priority = 10)
public class CogBlockType implements BlockType {
    private final Atom plugin;

    public CogBlockType(Atom plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cog";
    }

    @Override
    public String getDisplayName() {
        return "§6⚙ Mechanical Cog";
    }

    @Override
    public Material getItemMaterial() {
        return Material.IRON_BLOCK;
    }

    @Override
    public String[] getLore() {
        return new String[]{
            "§7Place this item to create a",
            "§7rotating mechanical cog",
            "§8• Use a wrench to power/remove",
            "§8[Mechanical Component]"
        };
    }

    @Override
    public CustomBlock createBlock(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        return new Cog(spawnLocation, blockLocation, blockFace);
    }

    @Override
    public CustomBlock deserialize(String data) {
        try {
            String[] parts = data.split(";");
            if (parts.length >= 5) {
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) return null;
                
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                Location location = new Location(world, x, y, z);
                
                BlockFace face = BlockFace.valueOf(parts[4]);
                boolean isPowerSource = parts.length > 5 && Boolean.parseBoolean(parts[5]);
                
                return new Cog(location, face, isPowerSource);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not deserialize cog: " + data);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean requiresUpdate() {
        return true; 
    }
    
    @Override
    public ItemStack getDropItem() {
        CustomItem item = plugin.getItemRegistry().getItem(getIdentifier());
        if (item != null) {
            return item.create();
        }
        return new ItemStack(getItemMaterial());
    }
}
