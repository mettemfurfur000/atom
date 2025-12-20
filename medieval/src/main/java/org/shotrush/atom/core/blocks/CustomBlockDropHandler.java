package org.shotrush.atom.core.blocks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.annotation.CustomBlockDrops;

import java.util.*;

import org.shotrush.atom.core.api.annotation.RegisterSystem;


@RegisterSystem(
    id = "custom_block_drop_handler",
    priority = 8,
    toggleable = false,
    description = "Handles custom block drops from @CustomBlockDrops annotation"
)
public class CustomBlockDropHandler implements Listener {
    
    private static final Map<Material, List<DropConfigWithAge>> customDrops = new HashMap<>();
    private static final Random random = new Random();
    
    public CustomBlockDropHandler(Plugin plugin) {
        scanForCustomDrops();
    }
    
    
    private void scanForCustomDrops() {
        try {
            Reflections reflections = new Reflections("org.shotrush.atom");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CustomBlockDrops.class);
            
            for (Class<?> clazz : annotatedClasses) {
                CustomBlockDrops annotation = clazz.getAnnotation(CustomBlockDrops.class);
                if (annotation != null) {
                    registerDrops(annotation);
                }
            }
            
            Atom.getInstance().getLogger().info("Loaded custom drops for " + customDrops.size() + " block types");
        } catch (Exception e) {
            Atom.getInstance().getLogger().warning("Failed to scan for custom block drops: " + e.getMessage());
        }
    }
    
    
    private void registerDrops(CustomBlockDrops annotation) {
        List<Material> targetBlocks = new ArrayList<>();
        
        
        if (!annotation.blockPattern().isEmpty()) {
            String pattern = annotation.blockPattern().toUpperCase();
            
            for (Material material : Material.values()) {
                if (material.isBlock()) {
                    String name = material.name();
                    
                    
                    if (name.contains(pattern)) {
                        targetBlocks.add(material);
                    }
                }
            }
            
            Atom.getInstance().getLogger().info("Pattern '" + pattern + "' matched " + 
                targetBlocks.size() + " blocks");
        } else {
            
            targetBlocks.addAll(Arrays.asList(annotation.blocks()));
        }
        
        
        Set<String> allowedAges = new HashSet<>(Arrays.asList(annotation.ages()));
        
        
        for (Material block : targetBlocks) {
            List<DropConfigWithAge> drops = customDrops.computeIfAbsent(block, k -> new ArrayList<>());
            
            for (CustomBlockDrops.Drop drop : annotation.drops()) {
                drops.add(new DropConfigWithAge(
                    drop.material(),
                    drop.customItemId(),
                    drop.chance(),
                    drop.min(),
                    drop.max(),
                    allowedAges
                ));
            }
            
            String ageInfo = allowedAges.isEmpty() ? "all ages" : "ages: " + String.join(", ", allowedAges);
            Atom.getInstance().getLogger().info("Registered " + annotation.drops().length + 
                " custom drops for " + block.name() + " (" + ageInfo + ")");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Block block = event.getBlock();
        Material blockType = block.getType();
        
        
        if (!customDrops.containsKey(blockType)) {
            return;
        }
        
        Player player = event.getPlayer();
        List<DropConfigWithAge> drops = customDrops.get(blockType);
        
        
        String currentAge = Atom.getInstance().getAgeManager().getCurrentAge().getId();
        
        
        List<DropConfigWithAge> applicableDrops = new ArrayList<>();
        for (DropConfigWithAge drop : drops) {
            if (drop.allowedAges.isEmpty() || drop.allowedAges.contains(currentAge)) {
                applicableDrops.add(drop);
            }
        }
        
        
        if (applicableDrops.isEmpty()) {
            return;
        }
        
        
        event.setDropItems(false);
        
        
        for (DropConfigWithAge drop : applicableDrops) {
            if (random.nextDouble() <= drop.chance) {
                int amount = drop.min;
                if (drop.max > drop.min) {
                    amount = drop.min + random.nextInt(drop.max - drop.min + 1);
                }
                
                ItemStack itemToDrop;
                
                
                if (!drop.customItemId.isEmpty()) {
                    itemToDrop = Atom.getInstance().getItemRegistry().createItem(drop.customItemId);
                    if (itemToDrop == null) {
                        Atom.getInstance().getLogger().warning("Custom item not found: " + drop.customItemId);
                        continue;
                    }
                } else {
                    itemToDrop = new ItemStack(drop.material);
                }
                
                itemToDrop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation(), itemToDrop);
            }
        }
    }
    
    
    private static class DropConfigWithAge {
        final Material material;
        final String customItemId;
        final double chance;
        final int min;
        final int max;
        final Set<String> allowedAges;
        
        DropConfigWithAge(Material material, String customItemId, double chance, int min, int max, Set<String> allowedAges) {
            this.material = material;
            this.customItemId = customItemId;
            this.chance = chance;
            this.min = min;
            this.max = max;
            this.allowedAges = allowedAges;
        }
    }
}
