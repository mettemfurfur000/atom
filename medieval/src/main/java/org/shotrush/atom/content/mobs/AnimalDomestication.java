package org.shotrush.atom.content.mobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.util.Optional;

@RegisterSystem(
    id = "animal_domestication",
    priority = 5,
    toggleable = true,
    dependencies = {"animal_behavior"},
    requires = {"herd_manager"},
    description = "Handles animal breeding and domestication"
)
public class AnimalDomestication implements Listener {
    
    private final Atom plugin;
    private final HerdManager herdManager;
    private static final int MAX_DOMESTICATION_LEVEL = 5;
    
    public AnimalDomestication(Plugin plugin) {
        this.plugin = (Atom) plugin;
        
        this.herdManager = org.shotrush.atom.core.api.AtomAPI.Systems.getService("herd_manager", HerdManager.class);
    }
    
    @EventHandler
    public void onAnimalBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Animals baby)) return;
        if (!(event.getMother() instanceof Animals mother)) return;
        if (!(event.getFather() instanceof Animals father)) return;
        
        int motherLevel = getDomesticationLevel(mother);
        int fatherLevel = getDomesticationLevel(father);
        
        int babyLevel = Math.min(MAX_DOMESTICATION_LEVEL, Math.max(motherLevel, fatherLevel) + 1);
        
        baby.setMetadata("domesticationLevel", new FixedMetadataValue(plugin, babyLevel));
        
        plugin.getLogger().info("Baby " + baby.getType() + " born with domestication level: " + babyLevel);
        
        if (babyLevel >= MAX_DOMESTICATION_LEVEL) {
            baby.setMetadata("fullyDomesticated", new FixedMetadataValue(plugin, true));
            plugin.getLogger().info("Baby is fully domesticated!");
        }
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(baby.getLocation(), () -> {
            if (baby.isValid() && !baby.isDead()) {
                Optional<Herd> motherHerd = herdManager.getHerd(mother.getUniqueId());
                Optional<Herd> fatherHerd = herdManager.getHerd(father.getUniqueId());
                
                Herd targetHerd = motherHerd.orElse(fatherHerd.orElse(null));
                if (targetHerd != null) {
                    targetHerd.addMember(baby.getUniqueId());
                    plugin.getLogger().info("Baby " + baby.getType() + " joined parent's herd: " + targetHerd.id());
                }
            }
        }, 20L);
    }
    
    private int getDomesticationLevel(Animals animal) {
        if (animal.hasMetadata("domesticationLevel")) {
            return animal.getMetadata("domesticationLevel").getFirst().asInt();
        }
        return 0;
    }
    
    public static double getDomesticationFactor(Animals animal) {
        if (animal.hasMetadata("fullyDomesticated")) {
            return 1.0;
        }
        
        if (animal.hasMetadata("domesticationLevel")) {
            int level = animal.getMetadata("domesticationLevel").getFirst().asInt();
            return level / (double) MAX_DOMESTICATION_LEVEL;
        }
        
        return 0.0;
    }
    
    public static boolean isFullyDomesticated(Animals animal) {
        return animal.hasMetadata("fullyDomesticated");
    }
}
