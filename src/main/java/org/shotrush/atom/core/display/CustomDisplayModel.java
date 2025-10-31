package org.shotrush.atom.core.display;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;


@Getter
@Builder
public class CustomDisplayModel {
    
    
    @NonNull
    @Builder.Default
    private final Material baseMaterial = Material.DIAMOND;
    
    
    @NonNull
    private final List<String> modelStrings;
    
    
    @Builder.Default
    private final String displayName = null;
    
    
    @NonNull
    @Builder.Default
    private final Vector3f translation = new Vector3f(0, 0.5f, 0);
    
    
    @NonNull
    @Builder.Default
    private final AxisAngle4f leftRotation = new AxisAngle4f();
    
    
    @Builder.Default
    private final float scale = 1.0f;
    
    
    @NonNull
    @Builder.Default
    private final AxisAngle4f rightRotation = new AxisAngle4f();
    
    
    @Builder.Default
    private final boolean glowing = false;
    
    
    @Builder.Default
    private final Integer brightness = null;
    
    
    public Transformation createTransformation() {
        Vector3f scaleVec = new Vector3f(scale, scale, scale);
        return new Transformation(translation, leftRotation, scaleVec, rightRotation);
    }
    
    
    public CustomDisplayModel withScale(float newScale) {
        return CustomDisplayModel.builder()
                .baseMaterial(this.baseMaterial)
                .modelStrings(this.modelStrings)
                .displayName(this.displayName)
                .translation(this.translation)
                .leftRotation(this.leftRotation)
                .scale(newScale)
                .rightRotation(this.rightRotation)
                .glowing(this.glowing)
                .brightness(this.brightness)
                .build();
    }
    
    
    public CustomDisplayModel withModelStrings(List<String> newModelStrings) {
        return CustomDisplayModel.builder()
                .baseMaterial(this.baseMaterial)
                .modelStrings(newModelStrings)
                .displayName(this.displayName)
                .translation(this.translation)
                .leftRotation(this.leftRotation)
                .scale(this.scale)
                .rightRotation(this.rightRotation)
                .glowing(this.glowing)
                .brightness(this.brightness)
                .build();
    }
    
    
    public CustomDisplayModel withLeftRotation(AxisAngle4f newRotation) {
        return CustomDisplayModel.builder()
                .baseMaterial(this.baseMaterial)
                .modelStrings(this.modelStrings)
                .displayName(this.displayName)
                .translation(this.translation)
                .leftRotation(newRotation)
                .scale(this.scale)
                .rightRotation(this.rightRotation)
                .glowing(this.glowing)
                .brightness(this.brightness)
                .build();
    }
    public static AxisAngle4f getRotationFromFace(BlockFace face) {
        return switch (face) {
            case NORTH -> new AxisAngle4f(0, 0, 1, 0);
            case SOUTH -> new AxisAngle4f((float) Math.PI, 0, 1, 0);
            case EAST -> new AxisAngle4f((float) Math.PI / 2, 0, 1, 0);
            case WEST -> new AxisAngle4f((float) -Math.PI / 2, 0, 1, 0);
            case UP -> new AxisAngle4f((float) -Math.PI / 2, 1, 0, 0);
            case DOWN -> new AxisAngle4f((float) Math.PI / 2, 1, 0, 0);
            default -> new AxisAngle4f();
        };
    }
}
