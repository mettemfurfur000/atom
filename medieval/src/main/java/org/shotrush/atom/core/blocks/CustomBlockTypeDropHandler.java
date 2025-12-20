package org.shotrush.atom.core.blocks;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.annotation.CustomBlockTypeDrops;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.util.*;

@RegisterSystem(
    id = "custom_block_type_drop_handler",
    priority = 9,
    toggleable = false,
    description = "Handles bonus drops from @CustomBlockTypeDrops annotation on custom blocks"
)
public class CustomBlockTypeDropHandler implements Listener {
    
    private static final Map<Class<? extends CustomBlock>, DropConfigWithAge> customDrops = new HashMap<>();
    private static final Random random = new Random();
    private final Atom plugin;
    
    public CustomBlockTypeDropHandler(Plugin plugin) {
        this.plugin = (Atom) plugin;
        scanForCustomDrops();
    }
    
    private void scanForCustomDrops() {
        try {
            Reflections reflections = new Reflections("org.shotrush.atom");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CustomBlockTypeDrops.class);
            
            for (Class<?> clazz : annotatedClasses) {
                if (CustomBlock.class.isAssignableFrom(clazz)) {
                    CustomBlockTypeDrops annotation = clazz.getAnnotation(CustomBlockTypeDrops.class);
                    if (annotation != null) {
                        registerDrops((Class<? extends CustomBlock>) clazz, annotation);
                    }
                }
            }
            
            plugin.getLogger().info("Loaded custom block type drops for " + customDrops.size() + " block types");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to scan for custom block type drops: " + e.getMessage());
        }
    }
    
    private void registerDrops(Class<? extends CustomBlock> blockClass, CustomBlockTypeDrops annotation) {
        Set<String> allowedAges = new HashSet<>(Arrays.asList(annotation.ages()));
        
        List<DropConfig> drops = new ArrayList<>();
        for (CustomBlockTypeDrops.Drop drop : annotation.drops()) {
            drops.add(new DropConfig(
                drop.material(),
                drop.customItemId(),
                drop.chance(),
                drop.min(),
                drop.max()
            ));
        }
        
        customDrops.put(blockClass, new DropConfigWithAge(drops, allowedAges));
        
        String ageInfo = allowedAges.isEmpty() ? "all ages" : "ages: " + String.join(", ", allowedAges);
        plugin.getLogger().info("Registered " + annotation.drops().length + 
            " bonus drops for " + blockClass.getSimpleName() + " (" + ageInfo + ")");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomBlockBreak(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Interaction interaction)) return;

        
        CustomBlockManager manager = plugin.getBlockManager();
        if (manager == null) return;
        
        for (CustomBlock block : manager.getBlocks()) {
            if (block.getInteractionUUID() != null && 
                block.getInteractionUUID().equals(interaction.getUniqueId())) {
                
                
                DropConfigWithAge dropConfig = customDrops.get(block.getClass());
                if (dropConfig == null) return;
                
                
                String currentAge = plugin.getAgeManager().getCurrentAge().getId();
                if (!dropConfig.allowedAges.isEmpty() && !dropConfig.allowedAges.contains(currentAge)) {
                    return;
                }
                
                
                for (DropConfig drop : dropConfig.drops) {
                    if (random.nextDouble() <= drop.chance) {
                        int amount = drop.min;
                        if (drop.max > drop.min) {
                            amount = drop.min + random.nextInt(drop.max - drop.min + 1);
                        }
                        
                        ItemStack itemToDrop;
                        
                        
                        if (!drop.customItemId.isEmpty()) {
                            itemToDrop = plugin.getItemRegistry().createItem(drop.customItemId);
                            if (itemToDrop == null) {
                                plugin.getLogger().warning("Custom item not found: " + drop.customItemId);
                                continue;
                            }
                        } else {
                            itemToDrop = new ItemStack(drop.material);
                        }
                        
                        itemToDrop.setAmount(amount);
                        block.getSpawnLocation().getWorld().dropItemNaturally(
                            block.getSpawnLocation(), itemToDrop);
                    }
                }
                
                return;
            }
        }
    }
    
    private static class DropConfig {
        final Material material;
        final String customItemId;
        final double chance;
        final int min;
        final int max;
        
        DropConfig(Material material, String customItemId, double chance, int min, int max) {
            this.material = material;
            this.customItemId = customItemId;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }
    }
    
    private static class DropConfigWithAge {
        final List<DropConfig> drops;
        final Set<String> allowedAges;
        
        DropConfigWithAge(List<DropConfig> drops, Set<String> allowedAges) {
            this.drops = drops;
            this.allowedAges = allowedAges;
        }
    }
}
