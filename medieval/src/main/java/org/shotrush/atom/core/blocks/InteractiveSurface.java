package org.shotrush.atom.core.blocks;

import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.util.ActionBarManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public abstract class InteractiveSurface extends CustomBlock {
    protected final List<PlacedItem> placedItems = new ArrayList<>();

    public InteractiveSurface(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public InteractiveSurface(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public boolean onInteract(Player player, boolean sneaking) {
        
        if (useGuiMode() && !sneaking) {
            openGui(player);
            return true;
        }
        
        if (sneaking) {
            return onCrouchRightClick(player);
        }
        return false;
    }

    @Override
    public void spawn(Atom plugin) {

        if (spawnLocation.getWorld() == null) {
            plugin.getLogger().warning("Cannot spawn InteractiveSurface at " + spawnLocation + " - world is null");
            return;
        }

        plugin.getLogger().info(String.format(
            "[InteractiveSurface] spawn() called for %s with %d placedItems",
            getIdentifier(), placedItems.size()
        ));


        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(spawnLocation, () -> {

            cleanupExistingEntities();


            spawn(plugin, spawnLocation.getWorld());



            if (!placedItems.isEmpty()) {
                
                updateItemDisplayUUIDs();
            }

        });
    }

    public abstract int getMaxItems();
    public abstract boolean canPlaceItem(ItemStack item);
    public abstract Vector3f calculatePlacement(Player player, int itemCount);
    
    
    protected boolean useGuiMode() {
        return false;
    }
    
    
    protected void openGui(Player player) {
        player.openWorkbench(spawnLocation, true);
    }

    protected boolean onCrouchRightClick(Player player) {
        ItemStack result = checkRecipe();

        if (result != null) {
            clearAllItems();
            player.getWorld().dropItemNaturally(spawnLocation, result);
            ActionBarManager.send(player, "§aCrafted: " + result.getType().name());
            return true;
        } else {
            releaseAllItems(player);
            return true;
        }
    }

    protected ItemStack checkRecipe() {
        return null;
    }

    protected void applyQualityInheritance(ItemStack result) {
        if (result == null || placedItems.isEmpty()) return;

        ItemStack[] ingredients = placedItems.stream()
            .map(PlacedItem::getItem)
            .toArray(ItemStack[]::new);

        org.shotrush.atom.core.api.item.QualityInheritanceAPI.applyInheritedQuality(result, ingredients);
    }

    protected void releaseAllItems(Player player) {
        if (placedItems.isEmpty()) {
            return;
        }

        for (PlacedItem placedItem : new ArrayList<>(placedItems)) {
            removeItemDisplay(placedItem);
            player.getWorld().dropItemNaturally(spawnLocation, placedItem.getItem());
        }
        placedItems.clear();
    }

    protected void clearAllItems() {
        for (PlacedItem placedItem : new ArrayList<>(placedItems)) {
            removeItemDisplay(placedItem);
        }
        placedItems.clear();
    }

    public boolean placeItem(ItemStack item, Vector3f position, float yaw) {
        if (placedItems.size() >= getMaxItems()) return false;
        if (!canPlaceItem(item)) return false;

        ItemStack singleItem = item.clone();
        singleItem.setAmount(1);
        PlacedItem placedItem = new PlacedItem(singleItem, position, yaw);
        placedItems.add(placedItem);
        spawnItemDisplay(placedItem);
        return true;
    }

    public boolean placeItem(Player player, ItemStack item, Vector3f position, float yaw) {
        if (placeItem(item, position, yaw)) {
            player.swingMainHand();
            return true;
        }
        return false;
    }

    public ItemStack removeLastItem() {
        if (placedItems.isEmpty()) return null;
        PlacedItem item = placedItems.remove(placedItems.size() - 1);
        removeItemDisplay(item);
        return item.getItem();
    }

    protected void spawnItemDisplay(PlacedItem item) {
        if (spawnLocation.getWorld() == null) return;

        Location displayLoc = spawnLocation.clone().add(
            item.getPosition().x,
            item.getPosition().y,
            item.getPosition().z
        );

        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(displayLoc, () -> {
            ItemDisplay display = (ItemDisplay) displayLoc.getWorld().spawnEntity(displayLoc, org.bukkit.entity.EntityType.ITEM_DISPLAY);
            display.setItemStack(item.getItem());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);

            
            org.joml.AxisAngle4f rotation = getItemDisplayRotation(item);
            org.joml.Vector3f translation = getItemDisplayTranslation(item);
            org.joml.Vector3f scale = getItemDisplayScale(item);
            
            org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
                translation,
                rotation,
                scale,
                new org.joml.AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transformation);

            display.setViewRange(64.0f);
            display.setShadowRadius(0.0f);
            display.setShadowStrength(0.0f);
            
            
            if (interactionUUID != null) {
                display.getPersistentDataContainer().set(
                        Objects.requireNonNull(NamespacedKey.fromString("parent_workstation", Atom.getInstance())),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    interactionUUID.toString()
                );
            }

            item.setDisplayUUID(display.getUniqueId());

            org.shotrush.atom.content.systems.ItemHeatSystem.startItemDisplayHeatTracking(display);
        });
    }

    protected org.joml.AxisAngle4f getItemDisplayRotation(PlacedItem item) {
        return new org.joml.AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);
    }

    protected org.joml.Vector3f getItemDisplayTranslation(PlacedItem item) {
        return new org.joml.Vector3f(0.05f, 0, 0);
    }

    protected org.joml.Vector3f getItemDisplayScale(PlacedItem item) {
        return new org.joml.Vector3f(0.5f, 0.5f, 0.5f);
    }

    protected void removeItemDisplay(PlacedItem item) {
        Vector3f pos = item.getPosition();
        Location expectedLoc = spawnLocation.clone().add(pos.x, pos.y, pos.z);
        
        if (expectedLoc.getWorld() == null) return;
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(expectedLoc, () -> {
            for (Entity nearby : expectedLoc.getWorld().getNearbyEntities(expectedLoc, 0.5, 0.5, 0.5)) {
                if (nearby instanceof org.bukkit.entity.ItemDisplay itemDisplay && !nearby.getUniqueId().equals(displayUUID)) {
                    String parentUUID = itemDisplay.getPersistentDataContainer().get(
                        Objects.requireNonNull(NamespacedKey.fromString("parent_workstation", Atom.getInstance())),
                        org.bukkit.persistence.PersistentDataType.STRING
                    );
                    if (parentUUID == null || (interactionUUID != null && parentUUID.equals(interactionUUID.toString()))) {
                        nearby.remove();
                    }
                }
            }
        });
    }

    public List<PlacedItem> getPlacedItems() {
        return new ArrayList<>(placedItems);
    }


    public void updateItemDisplayUUIDs() {
        if (spawnLocation.getWorld() == null) return;


        
        for (PlacedItem item : placedItems) {
            item.setDisplayUUID(null);
        }

        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(spawnLocation, () -> {
            
            for (PlacedItem item : placedItems) {
                Vector3f pos = item.getPosition();
                Location itemLoc = spawnLocation.clone().add(pos.x, pos.y, pos.z);
                
                
                for (Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 0.2, 0.2, 0.2)) {
                    if (entity instanceof org.bukkit.entity.ItemDisplay) {
                        entity.remove();
                    }
                }
            }
            
            
            org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(spawnLocation, () -> {
                for (PlacedItem item : placedItems) {
                    spawnItemDisplay(item);
                }
            }, 5L);
        });
    }

    @Override
    protected String serializeAdditionalData() {
        StringBuilder sb = new StringBuilder();
        sb.append(placedItems.size());

        for (PlacedItem item : placedItems) {
            try {
                String base64 = itemToBase64(item.getItem());
                sb.append(";").append(base64)
                        .append(",").append(item.getPosition().x)
                        .append(",").append(item.getPosition().y)
                        .append(",").append(item.getPosition().z)
                        .append(",").append(item.getYaw());
            } catch (IOException e) {
                Atom.getInstance().getLogger().warning("Failed to serialize item: " + e.getMessage());
            }
        }

        return sb.toString();
    }
    @Override
    protected String deserializeAdditionalData(String[] parts, int startIndex) {
        if (startIndex >= parts.length) return null;

        try {
            int itemCount = Integer.parseInt(parts[startIndex]);

            for (int i = 0; i < itemCount; i++) {
                int partIndex = startIndex + 1 + i;
                if (partIndex >= parts.length) break;


                String[] itemData = parts[partIndex].split(",");


                String base64 = itemData[0];
                float x = Float.parseFloat(itemData[1]);
                float y = Float.parseFloat(itemData[2]);
                float z = Float.parseFloat(itemData[3]);
                float yaw = Float.parseFloat(itemData[4]);

                ItemStack item = itemFromBase64(base64);
                Vector3f position = new Vector3f(x, y, z);
                placedItems.add(new PlacedItem(item, position, yaw));
            }
        } catch (Exception e) {
            Atom.getInstance().getLogger().warning("Failed to deserialize placed items: " + e.getMessage());

        }

        return null;
    }
    public static class PlacedItem {
        private final ItemStack item;
        private final Vector3f position;
        private final float yaw;
        @Setter
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
    }

    @Override
    protected void cleanupExistingEntities() {
        for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 0.5, 0.5, 0.5)) {
            if (entity instanceof ItemDisplay || entity instanceof Interaction) {
                if (entity.getLocation().distance(spawnLocation) < 0.1) {
                    entity.remove();
                }
            }
        }


        for (Entity entity : spawnLocation.getWorld().getNearbyEntities(getBlockCenter(), 0.5, 0.6, 0.5)) {
            if (entity instanceof ItemDisplay) {
                if (entity.getLocation().distance(getBlockCenter()) <= 0.6) {
                    entity.remove();
                }
            }
        }
    }


    protected Location getBlockCenter() {
        return blockLocation.clone().add(0.5, 0.5, 0.5);
    }

    private static String itemToBase64(ItemStack item) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private static ItemStack itemFromBase64(String base64) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

}
