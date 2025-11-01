package org.shotrush.atom.content.blocks.cog;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;
import org.shotrush.atom.core.blocks.util.BlockRotationUtil;
;

@AutoRegister(priority = 10)
public class Cog extends CustomBlock {
    @Getter
    private boolean isPowerSource;
    private boolean isPowered;
    @Getter @Setter
    private int rotationDirection;

    
    public Cog(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
        this.isPowerSource = false;
        this.isPowered = false;
        this.rotationDirection = 1;
    }
    
    
    public Cog(Location spawnLocation, BlockFace blockFace, boolean isPowerSource) {
        super(spawnLocation, blockFace);
        this.isPowerSource = isPowerSource;
        this.isPowered = isPowerSource;
        this.rotationDirection = 1;
    }

    @Override
    public String getBlockType() {
        return "cog";
    }

    @Override
    protected String serializeAdditionalData() {
        return String.valueOf(isPowerSource);
    }

    @Override
    public void update(float globalAngle) {
        updateRotation(globalAngle);
    }
    @Override
    public void spawn(Atom plugin) {
        
        Bukkit.getRegionScheduler().run(plugin, spawnLocation, task -> {
            cleanupExistingEntities();
            ItemDisplay display = (ItemDisplay) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);

            String modelName = isPowerSource ? "cog_small_powered" : "cog_small";
            ItemStack buttonItem = createItemWithCustomModel(Material.STONE_BUTTON, modelName);

            AxisAngle4f initialRotation = BlockRotationUtil.getInitialRotationFromFace(blockFace);

            spawnDisplay(display, plugin, buttonItem, new Vector3f(0, 0.5f, 0), initialRotation, new Vector3f(1, 1, 1), true, 1f, 1f);
        });
    }
    
    public void updateRotation(float globalAngle) {
        if (!isPowered) return;
        updateDisplayRotation(globalAngle, rotationDirection, new Vector3f(0, 0.5f, 0), new Vector3f(1, 1, 1));
    }

    
    public void togglePowerSource() {
        this.isPowerSource = !this.isPowerSource;
        this.isPowered = this.isPowerSource;
        
        
        Entity entity = Bukkit.getEntity(displayUUID);
        if (entity instanceof ItemDisplay) {
            ItemDisplay display = (ItemDisplay) entity;
        
            
            Transformation currentTransform = display.getTransformation();
            String modelName = isPowerSource ? "cog_small_powered" : "cog_small";
            ItemStack buttonItem = createItemWithCustomModel(Material.STONE_BUTTON, modelName);
        
            
            display.setInterpolationDuration(0);
            display.setInterpolationDelay(0);
        
            
            display.setItemStack(buttonItem);
            display.setTransformation(currentTransform);
        
            
            display.teleportAsync(display.getLocation());
        }
    }

    private void cleanupExistingEntities() {
        for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 0.5, 0.5, 0.5)) {
            if (entity instanceof ItemDisplay || entity instanceof Interaction) {
                if (entity.getLocation().distance(spawnLocation) < 0.1) {
                    entity.remove();
                }
            }
        }
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

        if (blockLocation.getBlock().getType() == Material.BARRIER) {
            blockLocation.getBlock().setType(Material.AIR);
        }
    }

    @Override
    public boolean isValid() {
        Entity interaction = Bukkit.getEntity(interactionUUID);
        Entity display = Bukkit.getEntity(displayUUID);
        return interaction != null && display != null && !interaction.isDead() && !display.isDead();
    }


    public void setPowerSource(boolean powerSource) {
        this.isPowerSource = powerSource;
        if (powerSource) {
            this.isPowered = true;
        }
    }

    public void setPowered(boolean powered) {
        if (isPowerSource) {
            this.isPowered = true;
        } else {
            this.isPowered = powered;
        }
    }

    public boolean isSameAxisAs(Cog other) {
        return BlockRotationUtil.isSameAxis(this.blockFace, other.blockFace);
    }

    public boolean isConnectedAlongAxis(Cog other) {
        Location thisLoc = this.getBlockLocation();
        Location otherLoc = other.getBlockLocation();
        
        int dx = Math.abs(thisLoc.getBlockX() - otherLoc.getBlockX());
        int dy = Math.abs(thisLoc.getBlockY() - otherLoc.getBlockY());
        int dz = Math.abs(thisLoc.getBlockZ() - otherLoc.getBlockZ());
        
        BlockRotationUtil.Axis axis = BlockRotationUtil.getAxis(this.blockFace);
        
        return switch (axis) {
            case Y -> dy > 0 && dx == 0 && dz == 0;
            case Z -> dz > 0 && dx == 0 && dy == 0;
            case X -> dx > 0 && dy == 0 && dz == 0;
            default -> false;
        };
    }
    
    @Override
    public boolean onWrenchInteract(org.bukkit.entity.Player player, boolean sneaking) {
        if (sneaking) {
            return false;
        }
        
        togglePowerSource();
        player.sendMessage(isPowerSource ? 
            "§aCog is now a power source!" : 
            "§7Cog is no longer a power source");
        
        CogManager cogManager = new CogManager(Atom.getInstance());
        cogManager.recalculatePower(Atom.getInstance().getBlockManager().getBlocks());
        return true;
    }
    
    @Override
    public void onPlaced() {
        CogManager cogManager = new CogManager(Atom.getInstance());
        cogManager.recalculatePower(Atom.getInstance().getBlockManager().getBlocks());
    }
    
    @Override
    public void onRemoved() {
        CogManager cogManager = new CogManager(Atom.getInstance());
        cogManager.recalculatePower(Atom.getInstance().getBlockManager().getBlocks());
    }

    @Override
    public String getIdentifier() {
        return "cog_small";
    }

    @Override
    public String getDisplayName() {
        return "§6⚙ Mechanical Cog";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
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
    public CustomBlock deserialize(String data) {
        try {
            String[] parts = data.split(";");
            if (parts.length >= 5) {
                org.bukkit.World world = Bukkit.getWorld(parts[0]);
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
        org.shotrush.atom.core.items.CustomItem item = Atom.getInstance().getItemRegistry().getItem(getIdentifier());
        if (item != null) {
            return item.create();
        }
        return new ItemStack(getItemMaterial());
    }
}
