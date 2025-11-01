package org.shotrush.atom.content.foragingage.blocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.CustomBlock;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;

@AutoRegister(priority = 31)
public class Pebble extends CustomBlock {

    public Pebble(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public Pebble(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public void spawn(Atom plugin) {
        Bukkit.getRegionScheduler().run(plugin, spawnLocation, task -> {
            ItemDisplay display = (ItemDisplay) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
            
            ItemStack pebbleItem = createItemWithCustomModel(Material.STONE_BUTTON, "pebble");

            float randomYaw = (float) (Math.random() * Math.PI * 2);
            AxisAngle4f randomRotation = new AxisAngle4f(randomYaw, 0, 1, 0);

            float randomX = (float) (Math.random() * 0.4 - 0.2);
            float randomZ = (float) (Math.random() * 0.4 - 0.2);

            spawnDisplay(display, plugin, pebbleItem, new Vector3f(randomX, 0.05f, randomZ), randomRotation, new Vector3f(0.7f, 0.4f, 0.7f), false, 0.5f, 0.2f);
        });
    }

    @Override
    public void update(float globalAngle) {
    }

    @Override
    public void remove() {
        Entity interaction = Bukkit.getEntity(interactionUUID);
        if (interaction != null) {
            interaction.remove();
        }

        Entity display = Bukkit.getEntity(displayUUID);
        if (display != null) {
            display.remove();
        }
    }

    @Override
    public boolean isValid() {
        Entity interaction = Bukkit.getEntity(interactionUUID);
        Entity display = Bukkit.getEntity(displayUUID);
        return interaction != null && display != null && !interaction.isDead() && !display.isDead();
    }


    @Override
    public String getIdentifier() {
        return "pebble";
    }

    @Override
    public String getDisplayName() {
        return "§7Pebble";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
            "§7A small stone pebble",
            "§8[Decorative Item]"
        };
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
                
                return new Pebble(location, face);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ItemStack getDropItem() {
        return new ItemStack(Material.BRUSH, 1);
    }
    
    public String getModelName() {
        return "pebble";
    }
}
