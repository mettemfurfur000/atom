package org.shotrush.atom.core.blocks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;


public class CustomBlockManager implements Listener {

    private final Atom plugin;
    @Getter
    public final CustomBlockRegistry registry;
    private final NamespacedKey wrenchKey;
    @Getter
    public final List<CustomBlock> blocks;
    private final CustomBlockDataManager dataManager;
    private ScheduledTask globalUpdateTask;
    private float globalAngle = 0;

    public CustomBlockManager(Atom plugin) {
        this.plugin = plugin;
        this.registry = new CustomBlockRegistry(plugin);
        this.wrenchKey = new NamespacedKey(plugin, "wrench");
        this.blocks = new ArrayList<>();
        this.dataManager = new CustomBlockDataManager(plugin, registry);

        
        registerBlockTypes();

        
        Bukkit.getPluginManager().registerEvents(this, plugin);

        
        loadBlocks();
        
        
        startGlobalUpdate();
    }

    
    private void registerBlockTypes() {
        plugin.getLogger().info("Auto-registering block types...");
        
        try {
            Reflections reflections = new Reflections("org.shotrush.atom");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(AutoRegister.class);
            
            
            List<Class<?>> sortedClasses = annotatedClasses.stream()
                .sorted(Comparator.comparingInt(cls -> 
                    cls.getAnnotation(AutoRegister.class).priority()))
                .collect(Collectors.toList());
            
            for (Class<?> clazz : sortedClasses) {
                if (BlockType.class.isAssignableFrom(clazz)) {
                    try {
                        if (CustomBlock.class.isAssignableFrom(clazz)) {
                            registry.register(clazz, plugin);
                            plugin.getLogger().info("  ✓ Registered: " + clazz.getSimpleName());
                        } else {
                            Constructor<?> constructor = clazz.getConstructor(Atom.class);
                            BlockType blockType = (BlockType) constructor.newInstance(plugin);
                            registry.register(blockType.getIdentifier(), blockType);
                            plugin.getLogger().info("  ✓ Registered: " + blockType.getIdentifier());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to register block type: " + clazz.getName());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to auto-register block types!");
            e.printStackTrace();
        }
    }

    
    private void startGlobalUpdate() {
        globalUpdateTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            globalAngle += 0.1f;
            if (globalAngle > Math.PI * 2) {
                globalAngle = 0;
            }

            
            
            for (CustomBlock block : new ArrayList<>(blocks)) {
                BlockType type = registry.getBlockType(block.getBlockType());
                if (type != null && type.requiresUpdate()) {
                    block.update(globalAngle);
                }
            }
        }, 1L, 1L);
    }

    public void stopGlobalUpdate() {
        if (globalUpdateTask != null) {
            globalUpdateTask.cancel();
        }
    }

    private void loadBlocks() {
        List<CustomBlock> loadedBlocks = dataManager.loadBlocks();
        for (CustomBlock block : loadedBlocks) {
            block.spawn(plugin);
            blocks.add(block);
        }
        plugin.getLogger().info("Loaded " + loadedBlocks.size() + " block(s)");
    }

    public void saveBlocks() {
        blocks.removeIf(block -> !block.isValid());
        dataManager.saveBlocks(blocks);
        plugin.getLogger().info("Saved " + blocks.size() + " block(s)");
    }

    public void removeAllBlocks() {
        for (CustomBlock block : blocks) {
            block.remove();
        }
        blocks.clear();
        dataManager.saveBlocks(blocks);
        plugin.getLogger().info("Removed all blocks");
    }

    public ItemStack createWrench() {
        ItemStack wrench = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = wrench.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§e🔧 Mechanical Wrench");
            meta.setLore(Arrays.asList(
                "§7A tool for working with blocks",
                "§8• Right-click: Interact",
                "§8• Shift + Right-click: Remove",
                "§8[Engineering Tool]"
            ));

            meta.getPersistentDataContainer().set(wrenchKey, PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
            wrench.setItemMeta(meta);
        }

        return wrench;
    }

    public void giveBlockItem(Player player, String blockTypeId) {
        BlockType blockType = registry.getBlockType(blockTypeId);
        if (blockType == null) {
            player.sendMessage("§cUnknown block type: " + blockTypeId);
            return;
        }

        ItemStack item = new ItemStack(blockType.getItemMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(blockType.getDisplayName());
            meta.setLore(Arrays.asList(blockType.getLore()));
            
            NamespacedKey key = registry.getKey(blockTypeId);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            
            item.setItemMeta(meta);
        }

        player.getInventory().addItem(item);
        player.sendMessage("§aYou received a " + blockType.getDisplayName() + "!");
    }

    public void giveWrench(Player player) {
        ItemStack wrench = plugin.getItemRegistry().createItem("wrench");
        if (wrench != null) {
            player.getInventory().addItem(wrench);
            player.sendMessage("§aYou received a Mechanical Wrench!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        
        for (Map.Entry<String, BlockType> entry : registry.getAllBlockTypes().entrySet()) {
            BlockType blockType = entry.getValue();
            if (item.getType() != blockType.getItemMaterial()) continue;

            NamespacedKey key = registry.getKey(entry.getKey());
            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) continue;

            event.setCancelled(true);

            Location blockLocation = event.getBlock().getLocation();
            Location spawnLocation = blockLocation.clone().add(0.5, 0, 0.5);
            
            for (CustomBlock existingBlock : blocks) {
                if (existingBlock.getBlockLocation().equals(blockLocation)) {
                    event.getPlayer().sendMessage("§cA block is already placed here!");
                    return;
                }
            }
            
            CustomBlock customBlock = blockType.createBlock(
                spawnLocation,
                blockLocation,
                event.getBlockAgainst().getFace(event.getBlock())
            );
            customBlock.spawn(plugin);
            blocks.add(customBlock);
            customBlock.onPlaced();

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                event.getPlayer().getInventory().setItem(event.getHand(), new ItemStack(Material.AIR));
            }
            return;
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (event.getBlock().getType() != Material.BARRIER) return;
        
        Location brokenLoc = event.getBlock().getLocation();
        for (int i = 0; i < blocks.size(); i++) {
            CustomBlock block = blocks.get(i);
            if (block.getBlockLocation().equals(brokenLoc)) {
                event.setCancelled(true);
                
                BlockType blockType = registry.getBlockType(block.getBlockType());
                if (blockType != null) {
                    ItemStack dropItem = blockType.getDropItem();
                    if (dropItem != null) {
                        brokenLoc.getWorld().dropItemNaturally(brokenLoc, dropItem);
                    }
                }
                
                block.remove();
                blocks.remove(i);
                block.onRemoved();
                event.getPlayer().sendMessage("§cCustom block removed");
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Interaction)) return;
        
        Interaction interaction = (Interaction) entity;
        for (int i = 0; i < blocks.size(); i++) {
            CustomBlock block = blocks.get(i);
            if (block.getInteractionUUID().equals(entity.getUniqueId())) {
                Bukkit.getRegionScheduler().run(plugin, interaction.getLocation(), task -> {
                    org.bukkit.entity.Entity ent = Bukkit.getEntity(interaction.getUniqueId());
                    if (ent instanceof Interaction inter) {
                        inter.setResponsive(false);
                        inter.setResponsive(true);
                    }
                });
                
                handleBlockInteraction(block, i, event.getPlayer(), event);
                return;
            }
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Interaction)) return;
        
        Player player = (Player) event.getDamager();
        Interaction interaction = (Interaction) event.getEntity();
        
        for (int i = 0; i < blocks.size(); i++) {
            CustomBlock block = blocks.get(i);
            if (block.getInteractionUUID().equals(interaction.getUniqueId())) {
                event.setCancelled(true);

                if (block.getBlockLocation().getBlock().getType() != Material.BARRIER) {
                    BlockType blockType = registry.getBlockType(block.getBlockType());
                    if (blockType != null) {
                        ItemStack dropItem = blockType.getDropItem();
                        if (dropItem != null) {
                            block.getSpawnLocation().getWorld().dropItemNaturally(block.getSpawnLocation(), dropItem);
                        }
                    }
                    
                    block.remove();
                    blocks.remove(i);
                    block.onRemoved();
                    player.sendMessage("§cCustom block removed");
                }
                return;
            }
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerInteractBlock(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BARRIER) return;
        
        Location clickedLoc = event.getClickedBlock().getLocation();
        for (int i = 0; i < blocks.size(); i++) {
            CustomBlock block = blocks.get(i);
            if (block.getBlockLocation().equals(clickedLoc)) {
                handleBlockInteraction(block, i, event.getPlayer(), event);
                return;
            }
        }
    }
    
    private void handleBlockInteraction(CustomBlock block, int index, Player player, org.bukkit.event.Cancellable event) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        boolean hasWrench = plugin.getItemRegistry().getItem("wrench") != null &&
                           plugin.getItemRegistry().getItem("wrench").isCustomItem(itemInHand);

        if (block instanceof org.shotrush.atom.core.blocks.InteractiveSurface) {
            if (hasWrench) {
                event.setCancelled(true);
                if (player.isSneaking()) {
                    if (block.onWrenchInteract(player, true)) {
                        return;
                    }
                    block.remove();
                    blocks.remove(index);
                    block.onRemoved();
                    player.sendMessage("§cBlock removed!");
                    return;
                } else {
                    block.onWrenchInteract(player, false);
                    return;
                }
            } else {
                boolean shouldCancel = block.onWrenchInteract(player, false);
                if (shouldCancel) {
                    event.setCancelled(true);
                }
                return;
            }
        }
        
        if (hasWrench) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                if (block.onWrenchInteract(player, true)) {
                    return;
                }
                block.remove();
                blocks.remove(index);
                block.onRemoved();
                player.sendMessage("§cBlock removed!");
                return;
            } else {
                block.onWrenchInteract(player, false);
                return;
            }
        }
    }

}
