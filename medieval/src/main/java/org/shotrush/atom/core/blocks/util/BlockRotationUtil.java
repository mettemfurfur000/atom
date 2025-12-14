package org.shotrush.atom.core.blocks.util;

import org.bukkit.block.BlockFace;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;


public class BlockRotationUtil {
    
    
    public enum Axis {
        X, Y, Z, UNKNOWN
    }
    
    
    public static AxisAngle4f getInitialRotationFromFace(BlockFace face) {
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

    
    public static AxisAngle4f combineRotations(AxisAngle4f first, AxisAngle4f second) {
        Quaternionf q1 = new Quaternionf().rotateAxis(first.angle, first.x, first.y, first.z);
        Quaternionf q2 = new Quaternionf().rotateAxis(second.angle, second.x, second.y, second.z);
        q1.mul(q2);

        AxisAngle4f result = new AxisAngle4f();
        q1.get(result);
        return result;
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
        return getAxis(face1) == getAxis(face2);
    }
}
