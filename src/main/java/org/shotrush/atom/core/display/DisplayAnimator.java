package org.shotrush.atom.core.display;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Getter
public class DisplayAnimator {
    
    private final DisplayEntityManager manager;
    private final Plugin plugin;
    private BukkitTask animationTask;
    private Matrix4f baseMatrix;
    
    public DisplayAnimator(DisplayEntityManager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
        this.baseMatrix = new Matrix4f();
        
        CustomDisplayModel model = manager.getCurrentModel();
        this.baseMatrix.translate(model.getTranslation())
                .rotate(model.getLeftRotation())
                .scale(model.getScale())
                .rotate(model.getRightRotation());
    }
    
    public static DisplayAnimator create(DisplayEntityManager manager, Plugin plugin) {
        return new DisplayAnimator(manager, plugin);
    }
    
    public DisplayAnimator rotateY(float degreesPerSecond) {
        return rotate(0, degreesPerSecond, 0);
    }
    
    public DisplayAnimator rotateX(float degreesPerSecond) {
        return rotate(degreesPerSecond, 0, 0);
    }
    
    public DisplayAnimator rotateZ(float degreesPerSecond) {
        return rotate(0, 0, degreesPerSecond);
    }
    
    public DisplayAnimator rotate(float xDegPerSec, float yDegPerSec, float zDegPerSec) {
        stop();
        
        int updateInterval = 5;
        float radiansPerTick = (float) Math.toRadians(yDegPerSec / 20.0);
        float xRadiansPerTick = (float) Math.toRadians(xDegPerSec / 20.0);
        float zRadiansPerTick = (float) Math.toRadians(zDegPerSec / 20.0);
        
        Matrix4f workingMatrix = new Matrix4f(baseMatrix);
        
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ItemDisplay display = manager.getDisplay();
            if (display == null || !display.isValid()) {
                stop();
                return;
            }
            
            if (xRadiansPerTick != 0) workingMatrix.rotateX(xRadiansPerTick * updateInterval);
            if (radiansPerTick != 0) workingMatrix.rotateY(radiansPerTick * updateInterval);
            if (zRadiansPerTick != 0) workingMatrix.rotateZ(zRadiansPerTick * updateInterval);
            
            display.setTransformationMatrix(workingMatrix);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(updateInterval);
        }, 1L, updateInterval);
        
        return this;
    }
    
    public DisplayAnimator pulse(float minScale, float maxScale, int durationTicks) {
        stop();
        
        final float[] currentScale = {minScale};
        final boolean[] growing = {true};
        final float scaleStep = (maxScale - minScale) / (durationTicks / 2f);
        
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ItemDisplay display = manager.getDisplay();
            if (display == null || !display.isValid()) {
                stop();
                return;
            }
            
            if (growing[0]) {
                currentScale[0] += scaleStep;
                if (currentScale[0] >= maxScale) {
                    currentScale[0] = maxScale;
                    growing[0] = false;
                }
            } else {
                currentScale[0] -= scaleStep;
                if (currentScale[0] <= minScale) {
                    currentScale[0] = minScale;
                    growing[0] = true;
                }
            }
            
            Matrix4f matrix = new Matrix4f(baseMatrix);
            matrix.scale(currentScale[0]);
            
            display.setTransformationMatrix(matrix);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);
        }, 1L, 1L);
        
        return this;
    }
    
    public DisplayAnimator bob(float amplitude, int periodTicks) {
        stop();
        
        final float[] tick = {0};
        Vector3f baseTranslation = manager.getCurrentModel().getTranslation();
        
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ItemDisplay display = manager.getDisplay();
            if (display == null || !display.isValid()) {
                stop();
                return;
            }
            
            tick[0]++;
            float yOffset = (float) (Math.sin(tick[0] * 2 * Math.PI / periodTicks) * amplitude);
            
            Matrix4f matrix = new Matrix4f();
            matrix.translate(baseTranslation.x, baseTranslation.y + yOffset, baseTranslation.z);
            matrix.rotate(manager.getCurrentModel().getLeftRotation());
            matrix.scale(manager.getCurrentModel().getScale());
            matrix.rotate(manager.getCurrentModel().getRightRotation());
            
            display.setTransformationMatrix(matrix);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);
        }, 1L, 1L);
        
        return this;
    }
    
    public DisplayAnimator scale(float targetScale, int durationTicks) {
        ItemDisplay display = manager.getDisplay();
        if (display == null || !display.isValid()) {
            return this;
        }
        
        Matrix4f targetMatrix = new Matrix4f(baseMatrix);
        targetMatrix.scale(targetScale);
        
        display.setTransformationMatrix(targetMatrix);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(durationTicks);
        
        return this;
    }
    
    public DisplayAnimator translate(Vector3f targetTranslation, int durationTicks) {
        ItemDisplay display = manager.getDisplay();
        if (display == null || !display.isValid()) {
            return this;
        }
        
        Matrix4f targetMatrix = new Matrix4f();
        targetMatrix.translate(targetTranslation);
        targetMatrix.rotate(manager.getCurrentModel().getLeftRotation());
        targetMatrix.scale(manager.getCurrentModel().getScale());
        targetMatrix.rotate(manager.getCurrentModel().getRightRotation());
        
        display.setTransformationMatrix(targetMatrix);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(durationTicks);
        
        return this;
    }
    
    public DisplayAnimator rotateTo(float xDeg, float yDeg, float zDeg, int durationTicks) {
        ItemDisplay display = manager.getDisplay();
        if (display == null || !display.isValid()) {
            return this;
        }
        
        Matrix4f targetMatrix = new Matrix4f(baseMatrix);
        targetMatrix.rotateXYZ(
            (float) Math.toRadians(xDeg),
            (float) Math.toRadians(yDeg),
            (float) Math.toRadians(zDeg)
        );
        
        display.setTransformationMatrix(targetMatrix);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(durationTicks);
        
        return this;
    }
    
    public DisplayAnimator updateBaseMatrix(Matrix4f newBase) {
        this.baseMatrix = new Matrix4f(newBase);
        return this;
    }
    
    public DisplayAnimator updateFromModel() {
        CustomDisplayModel model = manager.getCurrentModel();
        this.baseMatrix = new Matrix4f();
        this.baseMatrix.translate(model.getTranslation())
                .rotate(model.getLeftRotation())
                .scale(model.getScale())
                .rotate(model.getRightRotation());
        return this;
    }
    
    public void stop() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
            animationTask = null;
        }
    }
    
    public boolean isAnimating() {
        return animationTask != null && !animationTask.isCancelled();
    }
}
