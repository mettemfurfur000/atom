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

@AutoRegister(priority = 30)
public class GroundStick extends CustomBlock {

    public GroundStick(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public GroundStick(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);

        ItemStack stickItem = createItemWithCustomModel(Material.STONE_BUTTON, "ground_stick");

        float randomYaw = (float) (Math.random() * Math.PI * 2);
        AxisAngle4f randomRotation = new AxisAngle4f(randomYaw, 0, 1, 0);

        spawnDisplay(display, plugin, stickItem, new Vector3f(0, 1.25f, 0), randomRotation, new Vector3f(2.5f, 2.5f, 2.5f), false, 1f, 0.375f);
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
        return "ground_stick";
    }

    @Override
    public String getDisplayName() {
        return "§eGround Stick";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "§7A decorative ground stick",
                "§8[Decorative Item]"
        };
    }

    @Override
    public CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;
        return new GroundStick((Location) parsed[1], (BlockFace) parsed[2]);
    }
}
