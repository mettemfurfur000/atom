package org.shotrush.atom.content.mobs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.shotrush.atom.Atom;

import java.util.*;

public class AnimalBehavior implements Listener {
    
    private final Atom plugin;
    private static final Set<EntityType> COMMON_ANIMALS = new HashSet<>();
    private static final double AGGRESSION_CHANCE = 0.65;
    private static final double FLEE_DISTANCE = 10.0;
    private static final double INJURED_THRESHOLD = 0.4;
    private final Set<UUID> trackedAnimals = new HashSet<>();
    
    static {
        COMMON_ANIMALS.add(EntityType.COW);
        COMMON_ANIMALS.add(EntityType.PIG);
        COMMON_ANIMALS.add(EntityType.SHEEP);
        COMMON_ANIMALS.add(EntityType.CHICKEN);
        COMMON_ANIMALS.add(EntityType.RABBIT);
        COMMON_ANIMALS.add(EntityType.HORSE);
        COMMON_ANIMALS.add(EntityType.DONKEY);
        COMMON_ANIMALS.add(EntityType.MULE);
        COMMON_ANIMALS.add(EntityType.LLAMA);
        COMMON_ANIMALS.add(EntityType.GOAT);
        COMMON_ANIMALS.add(EntityType.CAT);
        COMMON_ANIMALS.add(EntityType.WOLF);
        COMMON_ANIMALS.add(EntityType.FOX);
        COMMON_ANIMALS.add(EntityType.PANDA);
        COMMON_ANIMALS.add(EntityType.POLAR_BEAR);
        COMMON_ANIMALS.add(EntityType.OCELOT);
        COMMON_ANIMALS.add(EntityType.TURTLE);
        COMMON_ANIMALS.add(EntityType.STRIDER);
        COMMON_ANIMALS.add(EntityType.AXOLOTL);
        COMMON_ANIMALS.add(EntityType.FROG);
        COMMON_ANIMALS.add(EntityType.CAMEL);
        COMMON_ANIMALS.add(EntityType.SNIFFER);
        COMMON_ANIMALS.add(EntityType.ARMADILLO);
    }
    
    public AnimalBehavior(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onAnimalSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        
        if (!COMMON_ANIMALS.contains(entity.getType())) return;
        if (!(entity instanceof Animals animal)) return;
        
        if (AnimalDomestication.isFullyDomesticated(animal)) {
            plugin.getLogger().info("Fully domesticated " + animal.getType() + " spawned - skipping enhancement");
            return;
        }
        
        plugin.getLogger().info("Animal spawned: " + animal.getType() + " at " + animal.getLocation() + " (Reason: " + event.getSpawnReason() + ")");
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
        
        enhanceAnimalStats(animal, domesticationFactor);
        
        double adjustedAggressionChance = AGGRESSION_CHANCE * (1.0 - domesticationFactor);
        boolean isAggressive = Math.random() < adjustedAggressionChance;
        animal.setMetadata("aggressive", new FixedMetadataValue(plugin, isAggressive));
        
        plugin.getLogger().info("Animal is aggressive: " + isAggressive);
        
        double maxStamina = 100 + (Math.random() * 100);
        animal.setMetadata("maxStamina", new FixedMetadataValue(plugin, maxStamina));
        animal.setMetadata("stamina", new FixedMetadataValue(plugin, maxStamina));
        animal.setMetadata("fleeing", new FixedMetadataValue(plugin, false));
        
        trackedAnimals.add(animal.getUniqueId());
        
        if (isAggressive && entity instanceof Wolf wolf) {
            wolf.setAngry(true);
        }
        
        
        animal.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (animal.isDead() || !animal.isValid()) {
                trackedAnimals.remove(animal.getUniqueId());
                scheduledTask.cancel();
                return;
            }
            handleAnimalBehavior(animal);
        }, null, 1L, 10L);
    }
    
    private void enhanceAnimalStats(Animals animal, double domesticationFactor) {
        double wildFactor = 1.0 - domesticationFactor;
        
        if (animal.getAttribute(Attribute.MAX_HEALTH) != null) {
            double currentMax = Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
            double healthMultiplier = 1.0 + (0.5 * wildFactor);
            Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(currentMax * healthMultiplier);
            animal.setHealth(Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue());
        }
        
        if (animal.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            Objects.requireNonNull(animal.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(0.5 * wildFactor);
        }
        
        if (animal.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            double currentSpeed = Objects.requireNonNull(animal.getAttribute(Attribute.MOVEMENT_SPEED)).getBaseValue();
            double speedMultiplier = 1.0 + (0.35 * wildFactor);
            Objects.requireNonNull(animal.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(currentSpeed * speedMultiplier);
        }
    }
    
    @EventHandler
    public void onAnimalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Animals animal)) return;
        if (!COMMON_ANIMALS.contains(animal.getType())) return;
        
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        if (attacker == null) return;
        
        double healthPercent = animal.getHealth() / Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue();
        
        if (healthPercent < INJURED_THRESHOLD) {
            switchToFleeing(animal);
        }
    }
    
    private void switchToFleeing(Animals animal) {
        if (animal.hasMetadata("fleeing") && animal.getMetadata("fleeing").getFirst().asBoolean()) {
            return;
        }
        
        animal.setMetadata("fleeing", new FixedMetadataValue(plugin, true));
        
        if (animal instanceof Mob mob) {
            mob.setTarget(null);
        }
        
        makeFlee(animal);
    }
    
    private void makeFlee(Animals animal) {
        plugin.getLogger().info("Making " + animal.getType() + " flee");
    }
    
    
    private void handleAnimalBehavior(Animals animal) {
        if (!animal.hasMetadata("aggressive")) return;
        
        boolean isAggressive = animal.getMetadata("aggressive").getFirst().asBoolean();
        boolean isFleeing = animal.hasMetadata("fleeing") && animal.getMetadata("fleeing").getFirst().asBoolean();
        
        Player nearestPlayer = getNearestPlayer(animal);
        
        if (nearestPlayer == null) {
            
            if (animal instanceof Mob mob) {
                mob.setTarget(null);
            }
            return;
        }
        
        double healthPercent = animal.getHealth() / Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue();
        int nearbyPlayers = animal.getLocation().getNearbyPlayers(15.0).size();
        
        if (healthPercent < INJURED_THRESHOLD || (isFleeing && healthPercent < 0.7)) {
            if (!isFleeing) {
                switchToFleeing(animal);
            }
            handleFleeingBehavior(animal, nearestPlayer, nearbyPlayers);
        } else if (!isAggressive && !isFleeing) {
            if (nearestPlayer.getLocation().distance(animal.getLocation()) < FLEE_DISTANCE) {
                switchToFleeing(animal);
            }
        }
    }
    
    private void handleFleeingBehavior(Animals animal, Player nearestPlayer, int nearbyPlayers) {
        if (!animal.hasMetadata("stamina")) return;
        
        double stamina = animal.getMetadata("stamina").getFirst().asDouble();
        double maxStamina = animal.getMetadata("maxStamina").getFirst().asDouble();
        
        double distance = animal.getLocation().distance(nearestPlayer.getLocation());
        
        if (distance < 20.0 && stamina > 0) {
            org.bukkit.util.Vector direction = animal.getLocation().toVector().subtract(nearestPlayer.getLocation().toVector()).normalize();
            double fleeDistance = 15.0 * (stamina / maxStamina); 
            Location fleeTarget = animal.getLocation().add(direction.multiply(fleeDistance));
            
            if (animal instanceof Mob mob) {
                mob.setTarget(null);
                double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
                double fleeSpeed = 1.4 * (1.0 - (domesticationFactor * 0.5));
                mob.getPathfinder().moveTo(fleeTarget, fleeSpeed);
            }
            
            double staminaDrain = 0.2 + (nearbyPlayers * 0.1);
            double healthPercent = animal.getHealth() / Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue();
            staminaDrain *= (1.0 + (1.0 - healthPercent) * 0.5);
            
            stamina = Math.max(0, stamina - staminaDrain);
            animal.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        } else if (distance >= 60.0 && stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + 2.0);
            animal.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        }
    }
    
    private Player getNearestPlayer(Animals animal) {
        Player nearest = null;
        double nearestDist = AnimalBehavior.FLEE_DISTANCE;
        
        for (Player player : animal.getLocation().getNearbyPlayers(AnimalBehavior.FLEE_DISTANCE)) {
            double dist = player.getLocation().distance(animal.getLocation());
            if (dist < nearestDist) {
                nearest = player;
                nearestDist = dist;
            }
        }
        
        return nearest;
    }
}
