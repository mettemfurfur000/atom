# Display Animation System

Quick reference for animating custom ItemDisplay entities.

## Basic Usage

```java
DisplayEntityManager manager = DisplayEntityManager.spawn(plugin, location, model);
DisplayAnimator animator = DisplayAnimator.create(manager, plugin);
```

## Continuous Animations

### Rotate Continuously
```java
animator.rotateY(180);        // 180 degrees/second on Y axis
animator.rotateX(90);          // 90 degrees/second on X axis
animator.rotateZ(45);          // 45 degrees/second on Z axis
animator.rotate(30, 60, 90);   // Multi-axis rotation
```

### Pulse (Scale In/Out)
```java
animator.pulse(0.8f, 1.2f, 40);  // Scale between 0.8x and 1.2x over 40 ticks
```

### Bob (Up/Down Movement)
```java
animator.bob(0.2f, 60);  // Bob 0.2 blocks up/down over 60 tick period
```

## One-Time Animations

### Scale To Target
```java
animator.scale(2.0f, 20);  // Scale to 2x over 20 ticks (1 second)
```

### Rotate To Angle
```java
animator.rotateTo(0, 90, 0, 20);  // Rotate to 90° on Y axis over 20 ticks
```

### Translate (Move)
```java
animator.translate(new Vector3f(0, 1, 0), 20);  // Move up 1 block over 20 ticks
```

## Control

```java
animator.stop();              // Stop current animation
animator.isAnimating();       // Check if animating
animator.updateFromModel();   // Reset base matrix from current model
```

## Examples

### Spinning Cog
```java
DisplayEntityManager manager = DisplayEntityManager.spawn(plugin, location, cogModel);
DisplayAnimator.create(manager, plugin).rotateY(180);
```

### Pulsing Power Source
```java
DisplayEntityManager manager = DisplayEntityManager.spawn(plugin, location, powerModel);
DisplayAnimator animator = DisplayAnimator.create(manager, plugin);
animator.pulse(0.9f, 1.1f, 30).rotateY(90);  // Pulse AND rotate
```

### Smooth State Transition
```java
manager.updateModel(poweredModel);
DisplayAnimator.create(manager, plugin).scale(1.2f, 10);  // Grow when powered
```
