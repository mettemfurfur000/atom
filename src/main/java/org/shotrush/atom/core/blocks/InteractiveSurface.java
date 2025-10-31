package org.shotrush.atom.core.blocks;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public abstract class InteractiveSurface extends CustomBlock {
    protected final List<PlacedItem> placedItems = new ArrayList<>();
    
    public InteractiveSurface(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }
    
    public InteractiveSurface(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }
    
    public abstract int getMaxItems();
    public abstract boolean canPlaceItem(ItemStack item);
    public abstract Vector3f calculatePlacement(Player player, int itemCount);
    
    public boolean placeItem(ItemStack item, Vector3f position, float yaw) {
        if (placedItems.size() >= getMaxItems()) return false;
        if (!canPlaceItem(item)) return false;
        
        PlacedItem placedItem = new PlacedItem(item.clone(), position, yaw);
        placedItems.add(placedItem);
        spawnItemDisplay(placedItem);
        return true;
    }
    
    public ItemStack removeLastItem() {
        if (placedItems.isEmpty()) return null;
        PlacedItem item = placedItems.remove(placedItems.size() - 1);
        removeItemDisplay(item);
        return item.getItem();
    }
    
    protected abstract void spawnItemDisplay(PlacedItem item);
    protected abstract void removeItemDisplay(PlacedItem item);
    
    public List<PlacedItem> getPlacedItems() {
        return new ArrayList<>(placedItems);
    }
    
    public static class PlacedItem {
        private final ItemStack item;
        private final Vector3f position;
        private final float yaw;
        private java.util.UUID displayUUID;
        
        public PlacedItem(ItemStack item, Vector3f position, float yaw) {
            this.item = item;
            this.position = position;
            this.yaw = yaw;
        }
        
        public ItemStack getItem() { return item; }
        public Vector3f getPosition() { return position; }
        public float getYaw() { return yaw; }
        public java.util.UUID getDisplayUUID() { return displayUUID; }
        public void setDisplayUUID(java.util.UUID uuid) { this.displayUUID = uuid; }
    }
}
