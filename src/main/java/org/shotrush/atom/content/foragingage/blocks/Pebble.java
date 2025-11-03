package org.shotrush.atom.content.foragingage.blocks;

import org.bukkit.*;
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
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);

        ItemStack pebbleItem = createItemWithCustomModel(Material.STONE_BUTTON, "pebble");

        float randomYaw = (float) (Math.random() * Math.PI * 2);
        AxisAngle4f randomRotation = new AxisAngle4f(randomYaw, 0, 1, 0);

        float randomX = (float) (Math.random() * 0.4 - 0.2);
        float randomZ = (float) (Math.random() * 0.4 - 0.2);

        spawnDisplay(display, plugin, pebbleItem, new Vector3f(randomX, 0.05f, randomZ), randomRotation, new Vector3f(0.7f, 0.4f, 0.7f), false, 0.5f, 0.2f);

    }

    @Override
    public void update(float globalAngle) {
    }

    @Override
    protected void removeEntities() {
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
        if (interactionUUID == null || displayUUID == null) return false;
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
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;
        return new Pebble((Location) parsed[1], (BlockFace) parsed[2]);
    }
    
    @Override
    public ItemStack getDropItem() {
        ItemStack pebble = Atom.getInstance().getItemRegistry().createItem("pebble");
        if (pebble != null) {
            return pebble;
        }
        return org.shotrush.atom.core.util.ItemUtil.createItemWithCustomModel(Material.BRUSH, "pebble");
    }

    public String getModelName() {
        return "pebble";
    }
}
