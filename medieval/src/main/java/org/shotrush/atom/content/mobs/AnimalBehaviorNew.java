package org.shotrush.atom.content.mobs;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.ai.combat.FatigueSystem;
import org.shotrush.atom.content.mobs.ai.combat.InjurySystem;
import org.shotrush.atom.content.mobs.ai.combat.MoraleSystem;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;
import org.shotrush.atom.content.mobs.ai.goals.*;
import org.bukkit.entity.EntityType;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.content.mobs.herd.HerdRole;
import org.shotrush.atom.content.mobs.ai.debug.DebugCategory;
import org.shotrush.atom.content.mobs.ai.debug.DebugLevel;
import org.shotrush.atom.content.mobs.ai.debug.DebugManager;

import java.util.*;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;








public class AnimalBehaviorNew implements Listener {
    
    private final Atom plugin;
    private static HerdManager herdManager;
    private final InjurySystem injurySystem;
    private final FatigueSystem fatigueSystem;
    private final MoraleSystem moraleSystem;
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
    
    public AnimalBehaviorNew(Plugin plugin) {
        this.plugin = (Atom) plugin;
        herdManager = new HerdManager((Atom) plugin);
        this.injurySystem = new InjurySystem(plugin);
        this.fatigueSystem = new FatigueSystem(plugin);
        this.moraleSystem = new MoraleSystem(plugin, herdManager);
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAnimalSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        
        if (!COMMON_ANIMALS.contains(entity.getType())) return;
        if (!(entity instanceof Animals animal)) return;
        if (!(entity instanceof Mob mob)) return;
        
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        
        if (animal.getAttribute(Attribute.STEP_HEIGHT) != null) {
            Objects.requireNonNull(animal.getAttribute(Attribute.STEP_HEIGHT)).setBaseValue(1.0);
        }
        
        if (animal.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
            animal.registerAttribute(Attribute.ATTACK_DAMAGE);
            Objects.requireNonNull(animal.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(2.0);
        }
        

        
        if (AnimalDomestication.isFullyDomesticated(animal)) {

            return;
        }
        
        initializeAnimal(animal, mob);
    }
    
    private void enhanceAnimalStats(Animals animal, double domesticationFactor, SpeciesBehavior behavior) {
        double wildFactor = 1.0 - domesticationFactor;
        
        org.bukkit.NamespacedKey wildnessKey = new org.bukkit.NamespacedKey(plugin, "wildness_modifier");
        
        if (animal.getAttribute(Attribute.MAX_HEALTH) != null) {
            org.bukkit.attribute.AttributeInstance healthAttr = animal.getAttribute(Attribute.MAX_HEALTH);
            Objects.requireNonNull(healthAttr).getModifiers().forEach(mod -> {
                if (mod.getKey().equals(wildnessKey)) {
                    healthAttr.removeModifier(mod);
                }
            });
            
            double healthBonus = 0.5 * wildFactor;
            if (healthBonus > 0) {
                org.bukkit.attribute.AttributeModifier healthMod = new org.bukkit.attribute.AttributeModifier(
                    wildnessKey, healthBonus, org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                healthAttr.addModifier(healthMod);
            }
            animal.setHealth(healthAttr.getValue());
        }
        
        if (animal.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            Objects.requireNonNull(animal.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(0.5 * wildFactor);
        }
        
        if (animal.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            org.bukkit.attribute.AttributeInstance speedAttr = animal.getAttribute(Attribute.MOVEMENT_SPEED);
            speedAttr.getModifiers().forEach(mod -> {
                if (mod.getKey().equals(wildnessKey)) {
                    speedAttr.removeModifier(mod);
                }
            });
            
            double speedBonus = 0.35 * wildFactor;
            if (speedBonus > 0) {
                org.bukkit.attribute.AttributeModifier speedMod = new org.bukkit.attribute.AttributeModifier(
                    wildnessKey, speedBonus, org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                speedAttr.addModifier(speedMod);
            }
        }
    }
    
    private void registerGoals(Mob mob, SpeciesBehavior behavior, boolean isAggressive, HerdRole role) {
        com.destroystokyo.paper.entity.ai.MobGoals goalSelector = Bukkit.getMobGoals();
        com.destroystokyo.paper.entity.ai.MobGoals targetSelector = Bukkit.getMobGoals();

        goalSelector.addGoal(mob, 0, new DeathEffectsGoal(mob, plugin, herdManager, moraleSystem));
        goalSelector.addGoal(mob, 0, new HerdPanicGoal(mob, plugin, herdManager, behavior, moraleSystem));

        goalSelector.addGoal(mob, 1, new AvoidPlayerWhenInjuredGoal(mob, plugin, behavior));
        
        if (isCarnivore(mob.getType())) {
            goalSelector.addGoal(mob, 2, new HuntPreyGoal(mob, plugin, behavior));
            goalSelector.addGoal(mob, 2, new TrackWoundedPreyGoal(mob, plugin, injurySystem));
        }
        
        if (isPackHunter(mob.getType())) {
            goalSelector.addGoal(mob, 2, new FlankAndSurroundGoal(mob, plugin, herdManager));
        }
        
        if (isAggressive) {
            targetSelector.addGoal(mob, 2, new AcquireNearestPlayerTargetGoal(mob, plugin, behavior));
            goalSelector.addGoal(mob, 3, new ChaseAndMeleeAttackGoal(mob, plugin, behavior, fatigueSystem, injurySystem, moraleSystem));
        }
        
        if (isCarnivore(mob.getType())) {
            goalSelector.addGoal(mob, 3, new StalkPreyGoal(mob, plugin));
        }
        
        goalSelector.addGoal(mob, 3, new ReunionGoal(mob, plugin, herdManager, behavior));
        
        if (role == HerdRole.FOLLOWER) {
            goalSelector.addGoal(mob, 4, new StayNearHerdGoal(mob, plugin, herdManager, behavior));
        } else {
            goalSelector.addGoal(mob, 4, new SentryBehaviorGoal(mob, plugin, herdManager));
            goalSelector.addGoal(mob, 4, new TerritoryDefenseGoal(mob, plugin, herdManager));
            goalSelector.addGoal(mob, 6, new HerdLeaderWanderGoal(mob, plugin, herdManager));
        }
        
        registerSpecialGoals(mob, behavior, goalSelector, isAggressive);
        
        plugin.getLogger().info("Registered goals for " + mob.getType() + " (aggressive: " + isAggressive + ", role: " + role + ")");
    }
    
    private void registerSpecialGoals(Mob mob, SpeciesBehavior behavior, com.destroystokyo.paper.entity.ai.MobGoals goalSelector, boolean isAggressive) {
        switch (behavior.specialMechanic()) {
            case RAM_CHARGE:
                if (isAggressive && (mob.getType() == EntityType.SHEEP || mob.getType() == EntityType.GOAT)) {
                    goalSelector.addGoal(mob, 2, new RamChargeGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Ram Charge");
                }
                break;
                
            case KICK_ATTACK:
                if (isAggressive && (mob.getType() == EntityType.HORSE || mob.getType() == EntityType.DONKEY || mob.getType() == EntityType.MULE)) {
                    goalSelector.addGoal(mob, 2, new KickAttackGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Kick Attack");
                }
                break;
                
            case SPIT_ATTACK:
                if (isAggressive && mob.getType() == EntityType.LLAMA) {
                    goalSelector.addGoal(mob, 2, new SpitAttackGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Spit Attack");
                }
                break;
                
            case COUNTER_CHARGE:
                if (isAggressive && mob.getType() == EntityType.PIG) {
                    goalSelector.addGoal(mob, 2, new CounterChargeGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Counter Charge");
                }
                break;
                
            case PACK_HUNTING:
                if (isAggressive && mob.getType() == EntityType.WOLF) {
                    goalSelector.addGoal(mob, 2, new PackHuntingGoal(mob, plugin, herdManager));
                    plugin.getLogger().info("  + Added Pack Hunting");
                }
                break;
                
            case POUNCE_ATTACK:
                if (isAggressive && mob.getType() == EntityType.FOX) {
                    goalSelector.addGoal(mob, 2, new PounceAttackGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Pounce Attack");
                }
                break;
                
            case STAMPEDE:
                if (mob.getType() == EntityType.COW) {
                    goalSelector.addGoal(mob, 1, new StampedeGoal(mob, plugin, herdManager));
                    plugin.getLogger().info("  + Added Stampede");
                }
                break;
                
            case FLIGHT_BURST:
                if (mob.getType() == EntityType.CHICKEN) {
                    goalSelector.addGoal(mob, 1, new FlightBurstGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Flight Burst");
                }
                break;
                
            case CUB_PROTECTION:
                if (mob.getType() == EntityType.POLAR_BEAR) {
                    goalSelector.addGoal(mob, 1, new CubProtectionGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Cub Protection");
                }
                break;
                
            case ROLL_DEFENSE:
                if (mob.getType() == EntityType.ARMADILLO) {
                    goalSelector.addGoal(mob, 0, new RollDefenseGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Roll Defense");
                }
                break;
                
            default:
                break;
        }
    }
    
    private void startStaminaRegeneration(Animals animal) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(animal, scheduledTask -> {
            if (animal.isDead() || !animal.isValid()) {
                trackedAnimals.remove(animal.getUniqueId());
                scheduledTask.cancel();
                return;
            }
            
            regenerateStamina(animal);
        }, 1L, 40L);
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
        if (!(animal instanceof Mob mob)) return;
        
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
        
        injurySystem.applyInjuryEffects(mob);
        
        moraleSystem.checkMorale(mob);
        
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
        
        UUID animalId = animal.getUniqueId();
        
        herdManager.leaveHerd(animalId);
        trackedAnimals.remove(animalId);
        
        plugin.getLogger().info(">>> Animal died: " + animal.getType() + " - cleaned up all systems");
    }
    
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!COMMON_ANIMALS.contains(entity.getType())) continue;
            if (!(entity instanceof Animals animal)) continue;
            if (!(entity instanceof Mob mob)) continue;
            
            if (trackedAnimals.contains(animal.getUniqueId())) {
                continue;
            }
            

            
            if (AnimalDomestication.isFullyDomesticated(animal)) {

                continue;
            }
            
            initializeAnimal(animal, mob);
        }
    }
    
    private void initializeAnimal(Animals animal, Mob mob) {
        double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
        SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
        



        
        if (!animal.hasMetadata("stats_enhanced")) {
            enhanceAnimalStats(animal, domesticationFactor, behavior);
            animal.setMetadata("stats_enhanced", new FixedMetadataValue(plugin, true));
        }
        
        Herd herd = herdManager.getOrCreateHerd(animal);
        HerdRole role = herdManager.getRole(animal.getUniqueId());
        
        boolean isAggressive;
        if (animal.hasMetadata("aggressive")) {
            isAggressive = animal.getMetadata("aggressive").get(0).asBoolean();
        } else {
            isAggressive = herdManager.getPersistence().isAggressive(animal);
            if (!isAggressive) {
                double aggressionChance = behavior.getAggressionChance(domesticationFactor);
                isAggressive = Math.random() < aggressionChance;
            }
            animal.setMetadata("aggressive", new FixedMetadataValue(plugin, isAggressive));
        }
        

        



        
        double maxStamina = herdManager.getPersistence().getMaxStamina(animal, 100 + (Math.random() * 100));
        double stamina = herdManager.getPersistence().getStamina(animal, maxStamina);
        
        animal.setMetadata("maxStamina", new FixedMetadataValue(plugin, maxStamina));
        animal.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        animal.setMetadata("fleeing", new FixedMetadataValue(plugin, false));
        
        herdManager.getPersistence().saveHerdData(animal, herd.id(), role == HerdRole.LEADER, isAggressive, maxStamina, stamina);
        
        trackedAnimals.add(animal.getUniqueId());
        
        registerGoals(mob, behavior, isAggressive, role);
        
        if (isAggressive && animal instanceof Wolf wolf) {
            wolf.setAngry(true);
        }
        
        plugin.getLogger().info(">>> Initialization complete!");
        
        startStaminaRegeneration(animal);
    }
    
    public HerdManager getHerdManager() {
        return herdManager;
    }
    
    private boolean isHerbivore(EntityType type) {
        return type == EntityType.COW || type == EntityType.SHEEP || 
               type == EntityType.HORSE || type == EntityType.DONKEY || 
               type == EntityType.MULE || type == EntityType.LLAMA || 
               type == EntityType.GOAT || type == EntityType.RABBIT || 
               type == EntityType.CHICKEN || type == EntityType.PIG ||
               type == EntityType.CAMEL || type == EntityType.SNIFFER;
    }
    
    private boolean isCarnivore(EntityType type) {
        return type == EntityType.WOLF || type == EntityType.FOX || 
               type == EntityType.CAT || type == EntityType.OCELOT ||
               type == EntityType.POLAR_BEAR;
    }
    
    private boolean isPackHunter(EntityType type) {
        return type == EntityType.WOLF;
    }
}
