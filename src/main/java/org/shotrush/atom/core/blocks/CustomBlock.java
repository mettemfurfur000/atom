package org.shotrush.atom.core.blocks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;
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
    @Getter
    protected UUID interactionUUID;
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
        Bukkit.getRegionScheduler().run(plugin, spawnLocation, task -> {
            spawn(plugin, spawnLocation.getWorld());
        });
    }

    public abstract void spawn(Atom plugin, RegionAccessor accessor);

    
    public abstract void update(float globalAngle);

    
    public final void remove() {
        removeEntities();
        blockLocation.getBlock().setType(Material.AIR);
    }
    
    
    protected abstract void removeEntities();

    
    public abstract boolean isValid();
    
    
    public boolean onWrenchInteract(org.bukkit.entity.Player player, boolean sneaking) {
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

    @Override
    public boolean requiresUpdate() {
        return false;
    }

    @Override
    public ItemStack getDropItem() {
        return new ItemStack(getItemMaterial());
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
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            org.bukkit.inventory.meta.components.CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setStrings(java.util.List.of(modelName));
            meta.setCustomModelDataComponent(component);
            item.setItemMeta(meta);
        }
        return item;
    }

    
    protected void spawnDisplay(ItemDisplay display, Atom plugin, ItemStack itemStack,
                                Vector3f translation, AxisAngle4f initialRotation, Vector3f scale, 
                                boolean placeBarrier, float interactionWidth, float interactionHeight) {
        plugin.getLogger().info("[CustomBlock] spawnDisplay called for: " + getIdentifier());
        plugin.getLogger().info("[CustomBlock] ItemStack: " + itemStack.getType() + ", Display UUID: " + display.getUniqueId());
        
        if (placeBarrier) {
            blockLocation.getBlock().setType(Material.BARRIER);
            plugin.getLogger().info("[CustomBlock] Placed barrier at: " + blockLocation);
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
