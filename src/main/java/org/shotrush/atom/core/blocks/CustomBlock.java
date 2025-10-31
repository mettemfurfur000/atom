package org.shotrush.atom.core.blocks;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.*;

import java.util.UUID;


public abstract class CustomBlock {
    protected final Location spawnLocation;
    protected final Location blockLocation;
    protected final BlockFace blockFace;
    protected UUID interactionUUID;
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

    
    public abstract void spawn(Atom plugin);

    
    public abstract void update(float globalAngle);

    
    public abstract void remove();

    
    public abstract boolean isValid();
    
    
    public boolean onWrenchInteract(org.bukkit.entity.Player player, boolean sneaking) {
        return false;
    }
    
    
    public void onPlaced() {
    }
    
    
    public void onRemoved() {
    }

    
    public abstract String getBlockType();

    
    public abstract String serialize();

    
    public Location getLocation() {
        return spawnLocation;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public BlockFace getBlockFace() {
        return blockFace;
    }

    public UUID getInteractionUUID() {
        return interactionUUID;
    }

    public UUID getDisplayUUID() {
        return displayUUID;
    }
}
