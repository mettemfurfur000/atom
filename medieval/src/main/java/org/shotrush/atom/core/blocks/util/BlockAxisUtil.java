package org.shotrush.atom.core.blocks.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;


public class BlockAxisUtil {
    
    public enum Axis {
        X, Y, Z, UNKNOWN
    }
    
    
    public static Axis getAxis(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
            case EAST, WEST -> Axis.X;
            default -> Axis.UNKNOWN;
        };
    }
    
    
    public static boolean isSameAxis(BlockFace face1, BlockFace face2) {
        return getAxis(face1) == getAxis(face2) && getAxis(face1) != Axis.UNKNOWN;
    }
    
    
    public static boolean isConnectedAlongAxis(Location loc1, Location loc2, Axis axis) {
        int dx = Math.abs(loc1.getBlockX() - loc2.getBlockX());
        int dy = Math.abs(loc1.getBlockY() - loc2.getBlockY());
        int dz = Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
        
        return switch (axis) {
            case Y -> dy > 0 && dx == 0 && dz == 0;
            case Z -> dz > 0 && dx == 0 && dy == 0;
            case X -> dx > 0 && dy == 0 && dz == 0;
            default -> false;
        };
    }
    
    
    public static Axis getPrimaryAxis(Location from, Location to) {
        int dx = Math.abs(from.getBlockX() - to.getBlockX());
        int dy = Math.abs(from.getBlockY() - to.getBlockY());
        int dz = Math.abs(from.getBlockZ() - to.getBlockZ());
        
        if (dx > dy && dx > dz) return Axis.X;
        if (dy > dx && dy > dz) return Axis.Y;
        if (dz > dx && dz > dy) return Axis.Z;
        
        return Axis.UNKNOWN;
    }
    
    
    public static int getAxisOffset(Location from, Location to, Axis axis) {
        return switch (axis) {
            case X -> to.getBlockX() - from.getBlockX();
            case Y -> to.getBlockY() - from.getBlockY();
            case Z -> to.getBlockZ() - from.getBlockZ();
            default -> 0;
        };
    }
}
