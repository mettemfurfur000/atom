package org.shotrush.atom.content.foragingage.workstations.leatherbed;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.foragingage.items.LeatherItem;
import org.shotrush.atom.content.foragingage.items.MeatItem;
import org.shotrush.atom.content.foragingage.items.SharpenedFlint;
import org.shotrush.atom.core.blocks.InteractiveSurface;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.util.ActionBarManager;
import org.shotrush.atom.core.workstations.WorkstationHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

@RegisterSystem(
    id = "leather_bed_handler",
    priority = 5,
    toggleable = true,
    description = "Handles leather curing and brushing mechanics"
)
public class LeatherBedHandler extends WorkstationHandler<LeatherBedHandler.BrushingProgress> {
    
    private static final Map<Location, CuringLeather> curingLeathers = new HashMap<>();
    private static final long CURING_TIME_MS = 10 * 60 * 1000; 
    
    
    private static LeatherBedHandler instance;
    
    public LeatherBedHandler(Plugin plugin) {
        super();
        instance = this;
        startCuringCheckTask();
    }
    
    @Override
    protected boolean isValidTool(ItemStack item) {
        if(SharpenedFlint.isSharpenedFlint(item)) return true;
        CustomItem knife = Atom.getInstance().getItemRegistry().getItem("knife");

        boolean isKnife = knife != null && knife.isCustomItem(item);
        
        return isKnife;
    }
    
    @Override
    protected Sound getStrokeSound() {
        return Sound.ITEM_BRUSH_BRUSHING_GENERIC;
    }
    
    @Override
    protected void spawnStrokeParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location particleLoc = location.clone().add(0, 1, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(
            org.bukkit.Color.fromRGB(139, 69, 19), 
            1.0f
        );
        world.spawnParticle(Particle.DUST, particleLoc, 10, 0.2, 0.2, 0.2, 0, dustOptions);
    }
    
    @Override
    protected String getStatusMessage() {
        return "§7Scraping leather... Use the tool carefully";
    }
    
    static class BrushingProgress extends WorkstationHandler.WorkProgress {
        LeatherBed bed;
        ItemStack tool;
        
        BrushingProgress(long startTime, Location bedLocation, LeatherBed bed, ItemStack tool) {
            super(startTime, 20 + (int)(Math.random() * 11), bedLocation); 
            this.bed = bed;
            this.tool = tool;
        }
    }
    
    public static void startBrushing(Player player, LeatherBed bed, ItemStack tool) {
        if (instance == null) return;
        
        if (instance.isProcessing(player)) {
            return;
        }
        
        if (bed.getPlacedItems().isEmpty()) {
            return;
        }
        
        InteractiveSurface.PlacedItem placedItem = bed.getPlacedItems().get(0);
        ItemStack leatherItem = placedItem.getItem();
        
        CustomItem uncuredLeather = Atom.getInstance().getItemRegistry().getItem("uncured_leather");
        if (uncuredLeather == null || !uncuredLeather.isCustomItem(leatherItem)) {
            return;
        }
        
        Location bedLocation = bed.getSpawnLocation();
        BrushingProgress progress = new BrushingProgress(System.currentTimeMillis(), bedLocation, bed, tool);
        
        instance.startProcessing(player, bedLocation, progress, () -> {
            finishBrushing(player, bed, tool);
        }, "§7Scraping leather... Use the tool carefully");
    }
    
    public static boolean isBrushing(Player player) {
        return instance != null && instance.isProcessing(player);
    }
    
    public static void cancelBrushing(Player player) {
        if (instance != null) {
            instance.cancelProcessing(player);
        }
    }
    
    private static void finishBrushing(Player player, LeatherBed bed, ItemStack tool) {
        if (bed.getPlacedItems().isEmpty()) {
            ActionBarManager.send(player, "§cThe leather is no longer on the bed!");
            return;
        }
        
        InteractiveSurface.PlacedItem placedItem = bed.getPlacedItems().get(0);
        ItemStack leatherItem = placedItem.getItem();
        
        
        String animalSource = null;
        if (leatherItem.hasItemMeta()) {
            animalSource = LeatherItem.getAnimalSource(leatherItem.getItemMeta());
        }
        
        
        ItemStack meat = Atom.getInstance().getItemRegistry().createItem("raw_meat");
        if (meat != null) {
            if (animalSource != null && meat.hasItemMeta()) {
                ItemMeta meatMeta = meat.getItemMeta();
                MeatItem.setAnimalSource(meatMeta, animalSource);
                meat.setItemMeta(meatMeta);
            }
            player.getWorld().dropItemNaturally(bed.getSpawnLocation(), meat);
        }
        
        
        ItemStack regularLeather = new ItemStack(Material.LEATHER);
        ItemMeta leatherMeta = regularLeather.getItemMeta();
        if (leatherMeta != null) {
            
            leatherMeta.setDisplayName("§fLeather");
            List<String> lore = new ArrayList<>();
            lore.add("§7Scraped leather, curing...");
            if (animalSource != null) {
                lore.add("§8From: " + animalSource);
                org.shotrush.atom.core.data.PersistentData.set(leatherMeta, "animal_source", animalSource);
            }
            long currentTime = System.currentTimeMillis();
            org.shotrush.atom.core.data.PersistentData.set(leatherMeta, "curing_start_time", currentTime);
            leatherMeta.setLore(lore);
            regularLeather.setItemMeta(leatherMeta);
            
            
            bed.removeLastItem();
            bed.placeItem(player, regularLeather, bed.calculatePlacement(player, 0), 0);
            
            
            curingLeathers.put(bed.getSpawnLocation(), new CuringLeather(bed, currentTime, animalSource));
        }
        

        if (SharpenedFlint.isSharpenedFlint(tool)) {
            SharpenedFlint.damageItem(tool, player, 0.3);
        }
        
        player.playSound(player.getLocation(), Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f);
        ActionBarManager.send(player, "§aScraped the meat off! Leather will now stabilize over time...");
        ActionBarManager.clearStatus(player);
    }
    
    private void startCuringCheckTask() {
        class CuringCheckTask implements Runnable {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                
                curingLeathers.entrySet().removeIf(entry -> {
                    Location location = entry.getKey();
                    CuringLeather curing = entry.getValue();
                    
                    if (currentTime - curing.startTime >= CURING_TIME_MS) {
                        finishCuring(curing);
                        return true;
                    }
                    return false;
                });
                
                
                org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTaskLater(
                    () -> run(), 
                    20L * 30 
                );
            }
        }
        
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTaskLater(
            () -> new CuringCheckTask().run(), 
            20L * 30
        );
    }
    
    private static void finishCuring(CuringLeather curing) {
        LeatherBed bed = curing.bed;
        
        if (bed.getPlacedItems().isEmpty()) {
            return;
        }
        
        InteractiveSurface.PlacedItem placedItem = bed.getPlacedItems().get(0);
        ItemStack leatherItem = placedItem.getItem();
        
        
        if (leatherItem.getType() != Material.LEATHER) {
            return;
        }
        
        
        if (!leatherItem.hasItemMeta() || 
            !org.shotrush.atom.core.data.PersistentData.has(leatherItem.getItemMeta(), "curing_start_time")) {
            return;
        }
        
        
        ItemStack stabilizedLeather = Atom.getInstance().getItemRegistry().createItem("stabilized_leather");
        if (stabilizedLeather != null && stabilizedLeather.hasItemMeta()) {
            ItemMeta stabilizedMeta = stabilizedLeather.getItemMeta();
            if (curing.animalSource != null) {
                org.shotrush.atom.content.foragingage.items.StabilizedLeatherItem.setAnimalSource(stabilizedMeta, curing.animalSource);
            }
            stabilizedLeather.setItemMeta(stabilizedMeta);
            
            
            bed.removeLastItem();
            
            bed.placeItem(stabilizedLeather, bed.calculatePlacement(null, 0), 0);
            
            
            bed.updateItemDisplayUUIDs();
        }
        
        
        if (bed.getSpawnLocation().getWorld() != null) {
            bed.getSpawnLocation().getWorld().playSound(
                bed.getSpawnLocation(), 
                Sound.BLOCK_LAVA_EXTINGUISH, 
                0.5f, 
                2.0f
            );
        }
    }
    
    public static boolean isCuring(Location location) {
        return curingLeathers.containsKey(location);
    }
    
    public static void cancelCuring(Location location) {
        curingLeathers.remove(location);
    }
    
    
    public static boolean finishCuringNearby(Location location) {
        
        for (Map.Entry<Location, CuringLeather> entry : curingLeathers.entrySet()) {
            Location curingLoc = entry.getKey();
            if (curingLoc.getWorld() != null && location.getWorld() != null &&
                curingLoc.getWorld().equals(location.getWorld()) &&
                curingLoc.distance(location) <= 3.0) {
                
                CuringLeather curing = entry.getValue();
                finishCuring(curing);
                curingLeathers.remove(curingLoc);
                return true;
            }
        }
        return false;
    }
    
    private static class CuringLeather {
        LeatherBed bed;
        long startTime;
        String animalSource;
        
        CuringLeather(LeatherBed bed, long startTime, String animalSource) {
            this.bed = bed;
            this.startTime = startTime;
            this.animalSource = animalSource;
        }
    }
}
