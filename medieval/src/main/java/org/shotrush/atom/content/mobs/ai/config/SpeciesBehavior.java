package org.shotrush.atom.content.mobs.ai.config;

import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

public record SpeciesBehavior(
    int minHerdSize,
    int maxHerdSize,
    double baseAggressionChance,
    double panicHealthThreshold,
    double fleeSpeedMultiplier,
    double chaseSpeedMultiplier,
    double cohesionRadiusMin,
    double cohesionRadiusMax,
    long panicDurationMs,
    double aggroRadius,
    SpecialMechanic specialMechanic
) {
    
    public enum SpecialMechanic {
        NONE,
        STAMPEDE,
        COUNTER_CHARGE,
        RAM_CHARGE,
        FLIGHT_BURST,
        ZIGZAG_FLEE,
        BURROW_HIDE,
        KICK_ATTACK,
        BRAY_ALERT,
        SPIT_ATTACK,
        PACK_HUNTING,
        HOWL_CALL,
        POUNCE_ATTACK,
        TREE_CLIMB,
        CUB_PROTECTION,
        ROLL_DEFENSE,
        DASH_ABILITY
    }
    
    public double getAggressionChance(double domesticationFactor) {
        return baseAggressionChance * (1.0 - domesticationFactor);
    }
    
    public double getFleeSpeed(double domesticationFactor) {
        double wildFactor = 1.0 - domesticationFactor;
        return 1.0 + ((fleeSpeedMultiplier - 1.0) * wildFactor);
    }
    
    public double getChaseSpeed(double domesticationFactor) {
        double wildFactor = 1.0 - domesticationFactor;
        return 1.0 + ((chaseSpeedMultiplier - 1.0) * wildFactor);
    }
    
    public double getCohesionRadius(double domesticationFactor) {
        return cohesionRadiusMin + ((cohesionRadiusMax - cohesionRadiusMin) * (1.0 - domesticationFactor));
    }
    
    public int getHerdSize(double domesticationFactor) {
        double range = maxHerdSize - minHerdSize;
        return (int) (minHerdSize + (range * (1.0 - domesticationFactor)));
    }
    
    private static final Map<EntityType, SpeciesBehavior> BEHAVIORS = new EnumMap<>(EntityType.class);
    
    static {
        BEHAVIORS.put(EntityType.COW, new SpeciesBehavior(
            2, 15, 0.05, 0.60, 1.50, 1.0, 6.0, 10.0, 6000, 8.0, SpecialMechanic.STAMPEDE
        ));
        
        BEHAVIORS.put(EntityType.PIG, new SpeciesBehavior(
            2, 8, 0.40, 0.50, 1.40, 1.30, 8.0, 12.0, 5000, 10.0, SpecialMechanic.COUNTER_CHARGE
        ));
        
        BEHAVIORS.put(EntityType.SHEEP, new SpeciesBehavior(
            4, 20, 0.15, 0.70, 1.20, 1.20, 4.0, 8.0, 7000, 9.0, SpecialMechanic.RAM_CHARGE
        ));
        
        BEHAVIORS.put(EntityType.CHICKEN, new SpeciesBehavior(
            2, 10, 0.20, 0.80, 1.30, 1.0, 6.0, 10.0, 4000, 8.0, SpecialMechanic.FLIGHT_BURST
        ));
        
        BEHAVIORS.put(EntityType.RABBIT, new SpeciesBehavior(
            2, 6, 0.05, 0.90, 1.60, 1.0, 12.0, 16.0, 5000, 12.0, SpecialMechanic.ZIGZAG_FLEE
        ));
        
        BEHAVIORS.put(EntityType.HORSE, new SpeciesBehavior(
            3, 12, 0.30, 0.50, 1.80, 1.50, 8.0, 14.0, 6000, 12.0, SpecialMechanic.KICK_ATTACK
        ));
        
        BEHAVIORS.put(EntityType.DONKEY, new SpeciesBehavior(
            2, 8, 0.50, 0.40, 1.30, 1.20, 8.0, 12.0, 7000, 10.0, SpecialMechanic.BRAY_ALERT
        ));
        
        BEHAVIORS.put(EntityType.MULE, new SpeciesBehavior(
            2, 8, 0.50, 0.40, 1.30, 1.20, 8.0, 12.0, 7000, 10.0, SpecialMechanic.BRAY_ALERT
        ));
        
        BEHAVIORS.put(EntityType.LLAMA, new SpeciesBehavior(
            4, 15, 0.60, 0.45, 1.30, 1.20, 8.0, 14.0, 6000, 12.0, SpecialMechanic.SPIT_ATTACK
        ));
        
        BEHAVIORS.put(EntityType.WOLF, new SpeciesBehavior(
            2, 8, 0.90, 0.20, 1.40, 1.60, 10.0, 16.0, 3000, 16.0, SpecialMechanic.PACK_HUNTING
        ));
        
        BEHAVIORS.put(EntityType.FOX, new SpeciesBehavior(
            2, 4, 0.40, 0.70, 1.50, 1.40, 10.0, 14.0, 5000, 10.0, SpecialMechanic.POUNCE_ATTACK
        ));
        
        BEHAVIORS.put(EntityType.CAT, new SpeciesBehavior(
            1, 2, 0.30, 0.80, 1.40, 1.20, 8.0, 12.0, 5000, 8.0, SpecialMechanic.TREE_CLIMB
        ));
        
        BEHAVIORS.put(EntityType.OCELOT, new SpeciesBehavior(
            1, 2, 0.30, 0.80, 1.40, 1.20, 8.0, 12.0, 5000, 8.0, SpecialMechanic.TREE_CLIMB
        ));
        
        BEHAVIORS.put(EntityType.POLAR_BEAR, new SpeciesBehavior(
            1, 2, 0.80, 0.15, 1.30, 1.40, 8.0, 12.0, 8000, 14.0, SpecialMechanic.CUB_PROTECTION
        ));
        
        BEHAVIORS.put(EntityType.PANDA, new SpeciesBehavior(
            2, 4, 0.50, 0.40, 1.20, 1.30, 8.0, 12.0, 6000, 10.0, SpecialMechanic.NONE
        ));
        
        BEHAVIORS.put(EntityType.GOAT, new SpeciesBehavior(
            4, 12, 0.50, 0.50, 1.20, 1.30, 8.0, 14.0, 6000, 12.0, SpecialMechanic.RAM_CHARGE
        ));
        
        BEHAVIORS.put(EntityType.CAMEL, new SpeciesBehavior(
            2, 8, 0.40, 0.35, 1.30, 1.20, 10.0, 16.0, 7000, 12.0, SpecialMechanic.DASH_ABILITY
        ));
        
        BEHAVIORS.put(EntityType.SNIFFER, new SpeciesBehavior(
            1, 2, 0.0, 0.50, 0.90, 0.90, 8.0, 12.0, 5000, 8.0, SpecialMechanic.NONE
        ));
        
        BEHAVIORS.put(EntityType.ARMADILLO, new SpeciesBehavior(
            1, 2, 0.0, 0.90, 1.10, 1.0, 6.0, 10.0, 4000, 6.0, SpecialMechanic.ROLL_DEFENSE
        ));
        
        BEHAVIORS.put(EntityType.TURTLE, new SpeciesBehavior(
            2, 4, 0.0, 0.60, 0.80, 0.80, 8.0, 12.0, 6000, 8.0, SpecialMechanic.NONE
        ));
        
        BEHAVIORS.put(EntityType.AXOLOTL, new SpeciesBehavior(
            1, 2, 0.60, 0.50, 1.10, 1.20, 6.0, 10.0, 4000, 8.0, SpecialMechanic.NONE
        ));
        
        BEHAVIORS.put(EntityType.FROG, new SpeciesBehavior(
            3, 6, 0.0, 0.70, 1.20, 1.0, 8.0, 12.0, 4000, 6.0, SpecialMechanic.NONE
        ));
        
        BEHAVIORS.put(EntityType.STRIDER, new SpeciesBehavior(
            1, 2, 0.0, 0.60, 1.0, 1.0, 8.0, 12.0, 5000, 8.0, SpecialMechanic.NONE
        ));
    }
    
    public static SpeciesBehavior get(EntityType type) {
        return BEHAVIORS.getOrDefault(type, createDefault());
    }
    
    private static SpeciesBehavior createDefault() {
        return new SpeciesBehavior(
            2, 8, 0.30, 0.50, 1.20, 1.10, 8.0, 12.0, 5000, 10.0, SpecialMechanic.NONE
        );
    }
}
