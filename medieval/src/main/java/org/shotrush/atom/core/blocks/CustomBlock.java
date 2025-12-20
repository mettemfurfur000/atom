package org.shotrush.atom.core.blocks;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;
import org.shotrush.atom.core.blocks.util.BlockRotationUtil;

import java.util.UUID;


public abstract class CustomBlock implements BlockType {
    @Getter
    protected final Location spawnLocation;
    @Getter
    protected final Location blockLocation;
    @Getter
    protected final BlockFace blockFace;
    @Setter
    @Getter
    protected UUID interactionUUID;
    @Setter
    @Getter
    protected UUID displayUUID;


    public CustomBlock(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        this.spawnLocation = spawnLocation.clone();
        this.blockLocation = blockLocation.clone();
        this.blockFace = blockFace;
    }

    
    public CustomBlock(Location spawnLocation, BlockFace blockFace) {
        this.spawnLocation = spawnLocation.clone();
        this.blockLocation = new Location(
            spawnLocation.getWorld(),
            spawnLocation.getBlockX(),
            spawnLocation.getBlockY(),
            spawnLocation.getBlockZ()
        );
        this.blockFace = blockFace;
    }


    public void spawn(Atom plugin) {
        if (spawnLocation.getWorld() == null) {
            plugin.getLogger().warning("Cannot spawn block at " + spawnLocation + " - world is null");
            return;
        }
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(spawnLocation, () -> {
            spawn(plugin, spawnLocation.getWorld());
        });
    }

    public abstract void spawn(Atom plugin, RegionAccessor accessor);

    
    public abstract void update(float globalAngle);

    
    public void remove() {
        removeEntities();
        blockLocation.getBlock().setType(Material.AIR);
    }
    
    
    protected void removeEntities() {
        if (spawnLocation.getWorld() == null) return;
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(spawnLocation, () -> {
            
            if (displayUUID != null) {
                Entity display = Bukkit.getEntity(displayUUID);
                if (display != null) display.remove();
            }
            if (interactionUUID != null) {
                Entity interaction = Bukkit.getEntity(interactionUUID);
                if (interaction != null) interaction.remove();
            }
            
            
            for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 1.5, 1.5, 1.5)) {
                if (entity instanceof ItemDisplay || entity instanceof Interaction) {
                    double distance = entity.getLocation().distance(spawnLocation);
                    if (distance < 0.5) {
                        entity.remove();
                    }
                }
            }
        });
    }

    protected void cleanupExistingEntities() {
        if (spawnLocation.getWorld() == null) return;
        
        for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 0.5, 0.5, 0.5)) {
            if (entity instanceof ItemDisplay || entity instanceof Interaction) {
                if (entity.getLocation().distance(spawnLocation) < 0.1) {
                    entity.remove();
                }
            }
        }
    }
    
    public boolean isValid() {
        if (spawnLocation.getWorld() == null) return false;
        
        
        if (interactionUUID != null && displayUUID != null) {
            Entity interaction = Bukkit.getEntity(interactionUUID);
            Entity display = Bukkit.getEntity(displayUUID);
            if (interaction != null && display != null && !interaction.isDead() && !display.isDead()) {
                return true;
            }
        }
        
        
        for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 0.5, 0.5, 0.5)) {
            if (entity instanceof Interaction || entity instanceof ItemDisplay) {
                double distance = entity.getLocation().distance(spawnLocation);
                if (distance < 0.1) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    public boolean onWrenchInteract(org.bukkit.entity.Player player, boolean sneaking) {
        return false;
    }

    public boolean onInteract(org.bukkit.entity.Player player, boolean sneaking) {
        return false;
    }
    
    
    public void onPlaced() {
    }
    
    
    public void onRemoved() {
    }
    
    protected String serializeAdditionalData() {
        return "";
    }
    
    protected String deserializeAdditionalData(String[] parts, int startIndex) {
        return null;
    }
    
    @Override
    public abstract String getIdentifier();

    @Override
    public abstract String getDisplayName();

    @Override
    public abstract Material getItemMaterial();

    @Override
    public abstract String[] getLore();

    @Override
    public CustomBlock createBlock(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        try {
            return this.getClass()
                .getConstructor(Location.class, Location.class, BlockFace.class)
                .newInstance(spawnLocation, blockLocation, blockFace);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create block instance", e);
        }
    }

    @Override
    public abstract CustomBlock deserialize(String data);
    

    protected Object[] parseDeserializeData(String data) {
        try {
            String[] parts = data.split(";");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                Atom.getInstance().getLogger().warning("World '" + parts[0] + "' not loaded, skipping " + getIdentifier() + " deserialization");
                return null;
            }
            Location location = new Location(world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
            );
            BlockFace face = BlockFace.valueOf(parts[4]);



            
            return new Object[]{world, location, face};
        } catch (Exception e) {
            Atom.getInstance().getLogger().warning("Failed to deserialize " + getIdentifier() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean requiresUpdate() {
        return false;
    }

    @Override
    public ItemStack getDropItem() {

        Atom plugin = Atom.getInstance();
        CustomBlockManager manager = plugin.getBlockManager();


        ItemStack item = manager.createBlockItem(getIdentifier());

        if (item == null) {
            plugin.getLogger().warning("Failed to create drop item for " + getIdentifier());

        }

        return item;
    }
    public String getBlockType() {
        return getIdentifier();
    }

    
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(spawnLocation.getWorld().getName()).append(";");
        sb.append(spawnLocation.getX()).append(";");
        sb.append(spawnLocation.getY()).append(";");
        sb.append(spawnLocation.getZ()).append(";");
        sb.append(blockFace.name());
        
        String additionalData = serializeAdditionalData();
        if (!additionalData.isEmpty()) {
            sb.append(";").append(additionalData);
        }
        
        return sb.toString();
    }

    
    protected ItemStack createItemWithCustomModel(Material material, String modelName) {
        return org.shotrush.atom.core.util.ItemUtil.createItemWithCustomModel(material, modelName);
    }

    
    protected void spawnDisplay(ItemDisplay display, Atom plugin, ItemStack itemStack,
                                Vector3f translation, AxisAngle4f initialRotation, Vector3f scale, 
                                boolean placeBarrier, float interactionWidth, float interactionHeight) {
        plugin.getLogger().info("[CustomBlock] spawnDisplay called for: " + getIdentifier());
        plugin.getLogger().info("[CustomBlock] ItemStack: " + itemStack.getType() + ", Display UUID: " + display.getUniqueId());
        
        if (placeBarrier) {
            blockLocation.getBlock().setType(Material.BLACK_STAINED_GLASS);
            plugin.getLogger().info("[CustomBlock] Placed black stained glass at: " + blockLocation);
        }
        
        display.setItemStack(itemStack);
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(0);
        display.setTransformation(new Transformation(
                translation,
                initialRotation,
                scale,
                new AxisAngle4f()
        ));
        display.setGravity(false);
        
        plugin.getLogger().info("[CustomBlock] Display configured, spawning interaction...");
        Interaction interaction = (Interaction) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.INTERACTION);
        interaction.setInteractionWidth(interactionWidth);
        interaction.setInteractionHeight(interactionHeight);
        interaction.setResponsive(true);
        interaction.setInvulnerable(true);
        interaction.setGravity(false);

        this.interactionUUID = interaction.getUniqueId();
        this.displayUUID = display.getUniqueId();
    }

    
    protected void updateDisplayRotation(float globalAngle, int rotationDirection, Vector3f translation, Vector3f scale) {
        if (displayUUID == null) return;
        
        Entity entity = Bukkit.getEntity(displayUUID);
        if (!(entity instanceof ItemDisplay)) return;
        
        ItemDisplay display = (ItemDisplay) entity;
        if (display.isDead() || !display.isValid()) return;

        display.getScheduler().run(Atom.getInstance(), task -> {
            AxisAngle4f baseRotation = BlockRotationUtil.getInitialRotationFromFace(blockFace);
            AxisAngle4f spinRotation = new AxisAngle4f(globalAngle * rotationDirection, 0, 1, 0);
            AxisAngle4f combinedRotation = BlockRotationUtil.combineRotations(baseRotation, spinRotation);

            Transformation transformation = new Transformation(
                    translation,
                    combinedRotation,
                    scale,
                    new AxisAngle4f()
            );

            display.setTransformation(transformation);
        }, null);
    }
}
