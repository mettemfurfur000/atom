package org.shotrush.atom.content.blocks.anvil;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.util.ActionBarManager;
import org.shotrush.atom.core.blocks.CustomBlock;
import org.shotrush.atom.core.blocks.InteractiveSurface;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;
import org.shotrush.atom.core.blocks.util.BlockRotationUtil;

@AutoRegister(priority = 20)
public class AnvilSurface extends InteractiveSurface {
    
    public AnvilSurface(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }
    
    public AnvilSurface(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }
    
    @Override
    public int getMaxItems() {
        return 2;
    }
    
    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
    
    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        float[][] positions = {
                {-0.25f, 0.6f, 0f},
                {0.25f, 0.6f, 0f}
        };
        
        if (itemCount < positions.length) {
            return new Vector3f(positions[itemCount][0], positions[itemCount][1], positions[itemCount][2]);
        }
        return new Vector3f(0, 0.6f, 0);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay base = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack anvilItem = new ItemStack(Material.ANVIL);

        spawnDisplay(base, plugin, anvilItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), true, 1f, 1f);
        
    }

    @Override
    protected AxisAngle4f getItemDisplayRotation(PlacedItem item) {
        float yawRadians = (float) Math.toRadians(item.getYaw());
        AxisAngle4f yawRotation = new AxisAngle4f(yawRadians, 0, 1, 0);
        AxisAngle4f tiltRotation = new AxisAngle4f((float) Math.PI / 2, 1, 0, 0);
        return BlockRotationUtil.combineRotations(yawRotation, tiltRotation);
    }
    
    @Override
    protected Vector3f getItemDisplayTranslation(PlacedItem item) {
        return new Vector3f(0, 0.45f, 0);
    }
    
    @Override
    protected Vector3f getItemDisplayScale(PlacedItem item) {
        return new Vector3f(0.3f, 0.3f, 0.3f);
    }
    
    @Override
    public void update(float globalAngle) {}
    
    @Override
    protected void removeEntities() {
        
        for (PlacedItem item : placedItems) {
            removeItemDisplay(item);
            if (blockLocation.getWorld() != null) {
                blockLocation.getWorld().dropItemNaturally(blockLocation, item.getItem());
            }
        }
        
        
        super.removeEntities();
    }
    
    
    
    @Override
    public String getBlockType() {
        return "anvil_surface";
    }
    
    @Override
    public boolean onWrenchInteract(Player player, boolean sneaking) {
        if (sneaking) {
            ItemStack removed = removeLastItem();
            if (removed != null) {
                player.getInventory().addItem(removed);
                ActionBarManager.send(player, "§7Removed item (" + placedItems.size() + "/" + getMaxItems() + ")");
                return true;
            }
            return false;
        }
        
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.WOODEN_HOE) return false;
        if (hand.getType() == Material.AIR) return false;
        
        Vector3f pos = calculatePlacement(player, placedItems.size());
        if (placeItem(player, hand, pos, player.getLocation().getYaw())) {
            hand.setAmount(hand.getAmount() - 1);
            ActionBarManager.send(player, "§aPlaced item (" + placedItems.size() + "/" + getMaxItems() + ")");
            return true;
        }
        
        ActionBarManager.send(player, "§cSurface is full!");
        return false;
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
            "§7Place up to 2 items on this surface",
            "§8• Right-click: Place item",
            "§8• Shift + Right-click: Remove item"
        };
    }

    @Override
    public CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;

        AnvilSurface surface = new AnvilSurface((Location) parsed[1], (BlockFace) parsed[2]);
        String[] parts = data.split(";");
        surface.deserializeAdditionalData(parts, 5);

        return surface;
    }

    
}
