package org.shotrush.atom.content.cog;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;
import org.shotrush.atom.content.cog.*;;

public class Cog extends CustomBlock {
    private boolean isPowerSource;
    private boolean isPowered;
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
    public String serialize() {
        return String.format("%s;%f;%f;%f;%s;%b",
            spawnLocation.getWorld().getName(),
            spawnLocation.getX(),
            spawnLocation.getY(),
            spawnLocation.getZ(),
            blockFace.name(),
            isPowerSource
        );
    }

    @Override
    public void update(float globalAngle) {
        updateRotation(globalAngle);
    }

    @Override
    public void spawn(Atom plugin) {
        
        Bukkit.getRegionScheduler().run(plugin, spawnLocation, task -> {
            
            cleanupExistingEntities();
            
            
            blockLocation.getBlock().setType(Material.BARRIER);

            
            ItemDisplay display = (ItemDisplay) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
            spawnDisplay(display, plugin);
        });
    }
    
    private void spawnDisplay(ItemDisplay display, Atom plugin) {

        
        ItemStack diamondItem = new ItemStack(Material.DIAMOND);
        ItemMeta diamondMeta = diamondItem.getItemMeta();
        if (diamondMeta != null) {
            org.bukkit.inventory.meta.components.CustomModelDataComponent component = diamondMeta.getCustomModelDataComponent();
            component.setStrings(java.util.List.of(isPowerSource ? "cog_small_powered" : "cog_small"));
            diamondMeta.setCustomModelDataComponent(component);
            diamondItem.setItemMeta(diamondMeta);
        }

        display.setItemStack(diamondItem);
        
        
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(0);

        
        AxisAngle4f initialRotation = getInitialRotationFromFace(blockFace);

        display.setTransformation(new Transformation(
                new Vector3f(0, 0.5f, 0),
                initialRotation,
                new Vector3f(1, 1, 1),
                new AxisAngle4f()
        ));

        
        Interaction interaction = (Interaction) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.INTERACTION);
        interaction.setInteractionWidth(1f);
        interaction.setInteractionHeight(1f);
        interaction.setResponsive(true);
        interaction.setInvulnerable(true);
        interaction.setGravity(false);
        interaction.addPassenger(display);

        
        this.interactionUUID = interaction.getUniqueId();
        this.displayUUID = display.getUniqueId();
    }

    
    public void updateRotation(float globalAngle) {
        if (!isPowered) return;
        if (displayUUID == null) return; 
        
        Entity entity = Bukkit.getEntity(displayUUID);
        if (!(entity instanceof ItemDisplay)) return;
        
        ItemDisplay display = (ItemDisplay) entity;
        if (display.isDead() || !display.isValid()) return;

        
        display.getScheduler().run(Atom.getInstance(), task -> {
            AxisAngle4f baseRotation = getInitialRotationFromFace(blockFace);
            AxisAngle4f spinRotation = new AxisAngle4f(globalAngle * rotationDirection, 0, 1, 0);
            AxisAngle4f combinedRotation = combineRotations(baseRotation, spinRotation);

            Transformation transformation = new Transformation(
                    new Vector3f(0, 0.5f, 0),
                    combinedRotation,
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f()
            );

            display.setTransformation(transformation);
        }, null);
    }

    
    public void togglePowerSource() {
        this.isPowerSource = !this.isPowerSource;
        this.isPowered = this.isPowerSource;
        
        
        Entity entity = Bukkit.getEntity(displayUUID);
        if (entity instanceof ItemDisplay) {
            ItemDisplay display = (ItemDisplay) entity;
        
            
            Transformation currentTransform = display.getTransformation();
        
            
            ItemStack diamondItem = new ItemStack(Material.DIAMOND);
            ItemMeta diamondMeta = diamondItem.getItemMeta();
            if (diamondMeta != null) {
                org.bukkit.inventory.meta.components.CustomModelDataComponent component = diamondMeta.getCustomModelDataComponent();
                component.setStrings(java.util.List.of(isPowerSource ? "cog_small_powered" : "cog_small"));
                diamondMeta.setCustomModelDataComponent(component);
                diamondItem.setItemMeta(diamondMeta);
            }
        
            
            display.setInterpolationDuration(0);
            display.setInterpolationDelay(0);
        
            
            display.setItemStack(diamondItem);
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

    private AxisAngle4f getInitialRotationFromFace(BlockFace face) {
        switch (face) {
            case UP, DOWN:
                return new AxisAngle4f();
            case NORTH, SOUTH:
                return new AxisAngle4f((float) Math.PI / 2, 1, 0, 0);
            case WEST, EAST:
                return new AxisAngle4f((float) Math.PI / 2, 0, 0, 1);
            default:
                return new AxisAngle4f();
        }
    }

    private AxisAngle4f combineRotations(AxisAngle4f first, AxisAngle4f second) {
        Quaternionf q1 = new Quaternionf().rotateAxis(first.angle, first.x, first.y, first.z);
        Quaternionf q2 = new Quaternionf().rotateAxis(second.angle, second.x, second.y, second.z);
        q1.mul(q2);

        AxisAngle4f result = new AxisAngle4f();
        q1.get(result);
        return result;
    }

    
    public boolean isPowerSource() {
        return isPowerSource;
    }

    public void setPowerSource(boolean powerSource) {
        this.isPowerSource = powerSource;
        if (powerSource) {
            this.isPowered = true;
        }
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean powered) {
        if (isPowerSource) {
            this.isPowered = true;
        } else {
            this.isPowered = powered;
        }
    }

    public int getRotationDirection() {
        return rotationDirection;
    }

    public void setRotationDirection(int direction) {
        this.rotationDirection = direction;
    }

    public boolean isSameAxisAs(Cog other) {
        return getAxis(this.blockFace).equals(getAxis(other.blockFace));
    }

    private String getAxis(BlockFace face) {
        switch (face) {
            case UP:
            case DOWN:
                return "Y";
            case NORTH:
            case SOUTH:
                return "Z";
            case EAST:
            case WEST:
                return "X";
            default:
                return "UNKNOWN";
        }
    }

    public boolean isConnectedAlongAxis(Cog other) {
        Location thisLoc = this.getBlockLocation();
        Location otherLoc = other.getBlockLocation();
        
        int dx = Math.abs(thisLoc.getBlockX() - otherLoc.getBlockX());
        int dy = Math.abs(thisLoc.getBlockY() - otherLoc.getBlockY());
        int dz = Math.abs(thisLoc.getBlockZ() - otherLoc.getBlockZ());
        
        String axis = getAxis(this.blockFace);
        
        switch (axis) {
            case "Y":
                return dy > 0 && dx == 0 && dz == 0;
            case "Z":
                return dz > 0 && dx == 0 && dy == 0;
            case "X":
                return dx > 0 && dy == 0 && dz == 0;
            default:
                return false;
        }
    }
    
    @Override
    public boolean onWrenchInteract(org.bukkit.entity.Player player, boolean sneaking) {
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
}
