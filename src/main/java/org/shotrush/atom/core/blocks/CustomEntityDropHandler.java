package org.shotrush.atom.core.blocks;

import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.AnimalType;
import org.shotrush.atom.content.carcass.CarcassBlock;
import org.shotrush.atom.content.carcass.CarcassConfigs;
import org.shotrush.atom.core.api.annotation.RegisterSystem;
import org.shotrush.atom.core.blocks.annotation.CustomEntityDrops;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;


@RegisterSystem(
        id = "custom_entity_drop_handler",
        priority = 8,
        toggleable = false,
        description = "Handles custom entity drops from @CustomEntityDrops annotation"
)
public class CustomEntityDropHandler implements Listener {

    private static final Map<EntityType, List<DropConfigWithAge>> customDrops = new HashMap<>();
    private static final Random random = new Random();

    public CustomEntityDropHandler(Plugin plugin) {
        scanForCustomDrops();
    }


    private void scanForCustomDrops() {
        try {
            Reflections reflections = new Reflections("org.shotrush.atom");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CustomEntityDrops.class);

            for (Class<?> clazz : annotatedClasses) {
                CustomEntityDrops annotation = clazz.getAnnotation(CustomEntityDrops.class);
                if (annotation != null) {
                    registerDrops(annotation);
                }
            }

            Atom.getInstance().getLogger().info("Loaded custom drops for " + customDrops.size() + " entity types");
        } catch (Exception e) {
            Atom.getInstance().getLogger().warning("Failed to scan for custom entity drops: " + e.getMessage());
        }
    }


    private void registerDrops(CustomEntityDrops annotation) {
        Set<String> allowedAges = new HashSet<>(Arrays.asList(annotation.ages()));
        Set<EntityType> targetEntities = new HashSet<>();


        targetEntities.addAll(Arrays.asList(annotation.entities()));


        for (CustomEntityDrops.EntityCategory category : annotation.categories()) {
            targetEntities.addAll(getEntitiesFromCategory(category));
        }


        for (EntityType entityType : targetEntities) {
            List<DropConfigWithAge> drops = customDrops.computeIfAbsent(entityType, k -> new ArrayList<>());

            for (CustomEntityDrops.Drop drop : annotation.drops()) {
                drops.add(new DropConfigWithAge(
                        drop.customItemId(),
                        drop.minAmount(),
                        drop.maxAmount(),
                        drop.randomAmount(),
                        annotation.replaceVanillaDrops(),
                        allowedAges
                ));
            }

            String ageInfo = allowedAges.isEmpty() ? "all ages" : "ages: " + String.join(", ", allowedAges);
            Atom.getInstance().getLogger().info("Registered " + annotation.drops().length +
                    " custom drops for " + entityType.name() + " (" + ageInfo + ")");
        }
    }

    private Set<EntityType> getEntitiesFromCategory(CustomEntityDrops.EntityCategory category) {
        Set<EntityType> entities = new HashSet<>();

        for (EntityType type : EntityType.values()) {
            if (!type.isSpawnable() || !type.isAlive()) continue;

            Class<? extends org.bukkit.entity.Entity> entityClass = type.getEntityClass();
            if (entityClass == null) continue;

            boolean matches = switch (category) {
                case ANIMALS -> Animals.class.isAssignableFrom(entityClass);
                case MONSTERS -> Monster.class.isAssignableFrom(entityClass);
                case WATER_MOBS -> WaterMob.class.isAssignableFrom(entityClass);
                case AMBIENT -> Ambient.class.isAssignableFrom(entityClass);
            };

            if (matches) {
                entities.add(type);
            }
        }

        Atom.getInstance().getLogger().info("Category " + category + " matched " + entities.size() + " entity types");
        return entities;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        EntityType entityType = entity.getType();

        // Check if this animal should spawn a carcass instead of drops
        String animalId = getAnimalId(entityType);
        AnimalType animalType = AnimalType.byId(animalId);
        
        if (animalType != null && CarcassConfigs.INSTANCE.hasCarcassConfig(animalType)) {
            // Clear all drops and spawn carcass instead
            event.getDrops().clear();
            event.setDroppedExp(random.nextInt(3) + 1);
            
            boolean success = CarcassBlock.INSTANCE.spawnCarcassFor(entity, animalType);
            if (success) {
                Atom.getInstance().getLogger().info("Spawned carcass for " + animalType.getId() + " at " + entity.getLocation());
            }
            return;
        }

        if (!customDrops.containsKey(entityType)) {
            return;
        }

        List<DropConfigWithAge> drops = customDrops.get(entityType);


        String currentAge = Atom.getInstance().getAgeManager().getCurrentAge().getId();


        List<DropConfigWithAge> applicableDrops = new ArrayList<>();
        boolean shouldReplaceVanilla = false;

        for (DropConfigWithAge drop : drops) {
            if (drop.allowedAges.isEmpty() || drop.allowedAges.contains(currentAge)) {
                applicableDrops.add(drop);
                if (drop.replaceVanillaDrops) {
                    shouldReplaceVanilla = true;
                }
            }
        }

        if (applicableDrops.isEmpty()) {
            return;
        }


        if (shouldReplaceVanilla) {
            event.getDrops().clear();
            event.setDroppedExp(random.nextInt(3) + 1);
        }


        String animalName = getAnimalDisplayName(entityType);
        if(animalType == null) {
            return;
        }

        for (DropConfigWithAge drop : applicableDrops) {
            int amount = drop.minAmount;

            if (drop.randomAmount && drop.maxAmount > drop.minAmount) {
                amount = drop.minAmount + random.nextInt(drop.maxAmount - drop.minAmount + 1);
            }

            if (amount > 0) {
                String itemId = drop.customItemId;
                if (drop.customItemId.equals("bone")) {
                    itemId = "atom:animal_bone_" + animalId;
                } else if (drop.customItemId.equals("uncured_leather")) {
                    itemId = "atom:animal_leather_raw_" + animalId;
                }

                ItemStack itemToDrop = null;
                try {
                    net.momirealms.craftengine.core.item.CustomItem<ItemStack> customItem =
                        net.momirealms.craftengine.bukkit.api.CraftEngineItems.byId(net.momirealms.craftengine.core.util.Key.of(itemId));
                    if (customItem != null) {
                        itemToDrop = customItem.buildItemStack();
                    }
                } catch (Exception e) {
                    Atom.getInstance().getLogger().warning("Failed to create CraftEngine item: " + itemId + " - " + e.getMessage());
                }

                if (itemToDrop != null) {
                    itemToDrop.setAmount(amount);
                    event.getDrops().add(itemToDrop);
                } else {
                    Atom.getInstance().getLogger().warning("Custom item not found: " + itemId);
                }
            }
        }

        // Add wool drops for sheep
        if (entityType == EntityType.SHEEP) {
            Sheep sheep = (Sheep) entity;
            if (!sheep.isSheared()) {
                DyeColor color = sheep.getColor();
                Material woolMaterial = switch (color) {
                    case WHITE -> Material.WHITE_WOOL;
                    case ORANGE -> Material.ORANGE_WOOL;
                    case MAGENTA -> Material.MAGENTA_WOOL;
                    case LIGHT_BLUE -> Material.LIGHT_BLUE_WOOL;
                    case YELLOW -> Material.YELLOW_WOOL;
                    case LIME -> Material.LIME_WOOL;
                    case PINK -> Material.PINK_WOOL;
                    case GRAY -> Material.GRAY_WOOL;
                    case LIGHT_GRAY -> Material.LIGHT_GRAY_WOOL;
                    case CYAN -> Material.CYAN_WOOL;
                    case PURPLE -> Material.PURPLE_WOOL;
                    case BLUE -> Material.BLUE_WOOL;
                    case BROWN -> Material.BROWN_WOOL;
                    case GREEN -> Material.GREEN_WOOL;
                    case RED -> Material.RED_WOOL;
                    case BLACK -> Material.BLACK_WOOL;
                };
                ItemStack wool = new ItemStack(woolMaterial, 1);
                event.getDrops().add(wool);
            }
        }
    }

    private String getAnimalDisplayName(EntityType type) {
        return switch (type) {
            case COW -> "Cow";
            case PIG -> "Pig";
            case SHEEP -> "Sheep";
            case CHICKEN -> "Chicken";
            case RABBIT -> "Rabbit";
            case HORSE -> "Horse";
            case DONKEY -> "Donkey";
            case MULE -> "Mule";
            case LLAMA -> "Llama";
            case GOAT -> "Goat";
            case CAT -> "Cat";
            case WOLF -> "Wolf";
            case FOX -> "Fox";
            case PANDA -> "Panda";
            case POLAR_BEAR -> "Polar Bear";
            case OCELOT -> "Ocelot";
            case CAMEL -> "Camel";
            default -> type.name();
        };
    }

    @Nullable
    private String getAnimalId(EntityType type) {
        return switch (type) {
            case COW -> "cow";
            case PIG -> "pig";
            case SHEEP -> "sheep";
            case CHICKEN -> "chicken";
            case RABBIT -> "rabbit";
            case HORSE -> "horse";
            case DONKEY -> "donkey";
            case MULE -> "mule";
            case LLAMA -> "llama";
            case GOAT -> "goat";
            case CAT -> "cat";
            case WOLF -> "wolf";
            case FOX -> "fox";
            case PANDA -> "panda";
            case POLAR_BEAR -> "polar_bear";
            case OCELOT -> "ocelot";
            case CAMEL -> "camel";
            default -> null;
        };
    }


    private static class DropConfigWithAge {
        final String customItemId;
        final int minAmount;
        final int maxAmount;
        final boolean randomAmount;
        final boolean replaceVanillaDrops;
        final Set<String> allowedAges;

        DropConfigWithAge(String customItemId, int minAmount, int maxAmount, boolean randomAmount,
                          boolean replaceVanillaDrops, Set<String> allowedAges) {
            this.customItemId = customItemId;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.randomAmount = randomAmount;
            this.replaceVanillaDrops = replaceVanillaDrops;
            this.allowedAges = allowedAges;
        }
    }
}
