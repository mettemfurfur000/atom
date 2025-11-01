package org.shotrush.atom.content.blocks.anvil;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
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
        return 8;
    }
    
    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
    
    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        org.bukkit.util.RayTraceResult result = player.rayTraceBlocks(5);
        if (result != null && result.getHitPosition() != null) {
            org.bukkit.util.Vector hitPos = result.getHitPosition();
            float x = (float) (hitPos.getX() - blockLocation.getX() - 0.5);
            float z = (float) (hitPos.getZ() - blockLocation.getZ() - 0.5);
            return new Vector3f(x, 0.6f, z);
        }
        float offset = itemCount * 0.15f - 0.3f;
        return new Vector3f(offset, 0.6f, 0);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        ItemDisplay base = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack anvilItem = new ItemStack(Material.ANVIL);

        spawnDisplay(base, plugin, anvilItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), true, 1f, 1f);
        for (PlacedItem item : placedItems) {
            spawnItemDisplay(item);
        }
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
            blockLocation.getWorld().dropItemNaturally(blockLocation, item.getItem());
        }
        if (displayUUID != null) {
            Entity entity = Bukkit.getEntity(displayUUID);
            if (entity != null) entity.remove();
        }
        if (interactionUUID != null) {
            Entity entity = Bukkit.getEntity(interactionUUID);
            if (entity != null) entity.remove();
        }
    }
    
    @Override
    public boolean isValid() {
        if (interactionUUID == null) return false;
        Entity entity = Bukkit.getEntity(interactionUUID);
        return entity != null && entity.isValid();
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
                player.sendMessage("§7Removed item (" + placedItems.size() + "/" + getMaxItems() + ")");
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
            player.sendMessage("§aPlaced item (" + placedItems.size() + "/" + getMaxItems() + ")");
            return true;
        }
        
        player.sendMessage("§cSurface is full!");
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
            "§7Place items on this surface",
            "§8• Right-click: Place item",
            "§8• Shift + Right-click: Remove item"
        };
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
    public ItemStack getDropItem() {
        return new ItemStack(Material.ANVIL);
    }
}
