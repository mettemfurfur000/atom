package org.shotrush.atom.content.mobs;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;
import org.shotrush.atom.content.mobs.ai.goals.*;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.content.mobs.herd.HerdRole;

import java.util.*;

public class AnimalBehaviorNew implements Listener {
    
    private final Atom plugin;
    private final HerdManager herdManager;
    private static final Set<EntityType> COMMON_ANIMALS = new HashSet<>();
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
    
    public AnimalBehaviorNew(Atom plugin) {
        this.plugin = plugin;
        this.herdManager = new HerdManager(plugin);
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAnimalSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        
        if (!COMMON_ANIMALS.contains(entity.getType())) return;
        if (!(entity instanceof Animals animal)) return;
        if (!(entity instanceof Mob mob)) return;
        
        plugin.getLogger().info(">>> Animal spawn detected: " + animal.getType() + " (Reason: " + event.getSpawnReason() + ")");
        
        if (AnimalDomestication.isFullyDomesticated(animal)) {
            plugin.getLogger().info("Fully domesticated - skipping");
            return;
        }
        
        double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
        SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
        
        enhanceAnimalStats(animal, domesticationFactor, behavior);
        
        Herd herd = herdManager.getOrCreateHerd(animal);
        HerdRole role = herdManager.getRole(animal.getUniqueId());
        
        double aggressionChance = behavior.getAggressionChance(domesticationFactor);
        boolean isAggressive = Math.random() < aggressionChance;
        animal.setMetadata("aggressive", new FixedMetadataValue(plugin, isAggressive));
        
        plugin.getLogger().info(">>> Aggressive: " + isAggressive + " (chance: " + String.format("%.1f%%", aggressionChance * 100) + ", role: " + role + ")");
        
        double maxStamina = 100 + (Math.random() * 100);
        animal.setMetadata("maxStamina", new FixedMetadataValue(plugin, maxStamina));
        animal.setMetadata("stamina", new FixedMetadataValue(plugin, maxStamina));
        animal.setMetadata("fleeing", new FixedMetadataValue(plugin, false));
        
        trackedAnimals.add(animal.getUniqueId());
        
        registerGoals(mob, behavior, isAggressive, role);
        
        if (isAggressive && entity instanceof Wolf wolf) {
            wolf.setAngry(true);
        }
        
        plugin.getLogger().info(">>> Goals registered successfully!");
        
        startStaminaRegeneration(animal);
    }
    
    private void enhanceAnimalStats(Animals animal, double domesticationFactor, SpeciesBehavior behavior) {
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
    
    private void registerGoals(Mob mob, SpeciesBehavior behavior, boolean isAggressive, HerdRole role) {
        com.destroystokyo.paper.entity.ai.MobGoals goalSelector = Bukkit.getMobGoals();
        com.destroystokyo.paper.entity.ai.MobGoals targetSelector = Bukkit.getMobGoals();
        
        goalSelector.addGoal(mob, 0, new HerdPanicGoal(mob, plugin, herdManager, behavior));
        
        goalSelector.addGoal(mob, 1, new AvoidPlayerWhenInjuredGoal(mob, plugin, behavior));
        
        if (isAggressive) {
            targetSelector.addGoal(mob, 2, new AcquireNearestPlayerTargetGoal(mob, plugin, behavior));
            goalSelector.addGoal(mob, 3, new ChaseAndMeleeAttackGoal(mob, plugin, behavior));
        }
        
        if (role == HerdRole.FOLLOWER) {
            goalSelector.addGoal(mob, 4, new StayNearHerdGoal(mob, plugin, herdManager, behavior));
        } else {
            goalSelector.addGoal(mob, 6, new HerdLeaderWanderGoal(mob, plugin, herdManager));
        }
        
        plugin.getLogger().info("Registered goals for " + mob.getType() + " (aggressive: " + isAggressive + ", role: " + role + ")");
    }
    
    private void startStaminaRegeneration(Animals animal) {
        animal.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (animal.isDead() || !animal.isValid()) {
                trackedAnimals.remove(animal.getUniqueId());
                scheduledTask.cancel();
                return;
            }
            
            regenerateStamina(animal);
        }, null, 1L, 40L);
    }
    
    private void regenerateStamina(Animals animal) {
        if (!animal.hasMetadata("stamina")) return;
        
        boolean isFleeing = animal.hasMetadata("fleeing") && animal.getMetadata("fleeing").get(0).asBoolean();
        if (isFleeing) return;
        
        double stamina = animal.getMetadata("stamina").get(0).asDouble();
        double maxStamina = animal.getMetadata("maxStamina").get(0).asDouble();
        
        if (stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + 2.0);
            animal.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        }
    }
    
    @EventHandler
    public void onAnimalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Animals animal)) return;
        if (!COMMON_ANIMALS.contains(animal.getType())) return;
        
        final Player attacker;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            } else {
                return;
            }
        } else {
            return;
        }
        
        herdManager.getHerd(animal.getUniqueId()).ifPresent(herd -> {
            SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
            herdManager.broadcastPanic(herd, attacker.getLocation(), behavior.panicDurationMs());
        });
        
        double healthPercent = animal.getHealth() / Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue();
        SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
        
        if (healthPercent < behavior.panicHealthThreshold()) {
            animal.setMetadata("fleeing", new FixedMetadataValue(plugin, true));
        }
    }
    
    @EventHandler
    public void onAnimalDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals animal)) return;
        if (!COMMON_ANIMALS.contains(animal.getType())) return;
        
        herdManager.leaveHerd(animal.getUniqueId());
        trackedAnimals.remove(animal.getUniqueId());
    }
    
    public HerdManager getHerdManager() {
        return herdManager;
    }
}
