package org.shotrush.atom.content.mobs;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

@CommandAlias("domestication|dom")
@CommandPermission("atom.domestication")
public class DomesticationCommand extends BaseCommand {
    
    @Default
    @Subcommand("check")
    public void checkDomestication(Player player) {
        RayTraceResult result = player.rayTraceEntities(5);
        
        if (result == null || result.getHitEntity() == null) {
            player.sendMessage("§cNo animal found. Look at an animal within 5 blocks.");
            return;
        }
        
        Entity entity = result.getHitEntity();
        
        if (!(entity instanceof Animals animal)) {
            player.sendMessage("§cThat's not an animal!");
            return;
        }
        
        double factor = AnimalDomestication.getDomesticationFactor(animal);
        int level = 0;
        
        if (animal.hasMetadata("domesticationLevel")) {
            level = animal.getMetadata("domesticationLevel").getFirst().asInt();
        }
        
        boolean fullyDomesticated = AnimalDomestication.isFullyDomesticated(animal);
        boolean isAggressive = animal.hasMetadata("aggressive") && animal.getMetadata("aggressive").getFirst().asBoolean();
        
        player.sendMessage("§6=== " + animal.getType() + " Domestication ===");
        player.sendMessage("§eDomestication Level: §f" + level + "/5");
        player.sendMessage("§eDomestication Factor: §f" + String.format("%.1f%%", factor * 100));
        player.sendMessage("§eStatus: §f" + (fullyDomesticated ? "§aFully Domesticated" : "§cWild/Partial"));
        player.sendMessage("§eAggressive: §f" + (isAggressive ? "§cYes" : "§aNo"));
    }
    
    @Subcommand("set")
    @CommandPermission("atom.domestication.set")
    public void setDomestication(Player player, int level) {
        if (level < 0 || level > 5) {
            player.sendMessage("§cLevel must be between 0 and 5!");
            return;
        }
        
        RayTraceResult result = player.rayTraceEntities(5);
        
        if (result == null || result.getHitEntity() == null) {
            player.sendMessage("§cNo animal found. Look at an animal within 5 blocks.");
            return;
        }
        
        Entity entity = result.getHitEntity();
        
        if (!(entity instanceof Animals animal)) {
            player.sendMessage("§cThat's not an animal!");
            return;
        }
        
        animal.setMetadata("domesticationLevel", new org.bukkit.metadata.FixedMetadataValue(
            org.shotrush.atom.Atom.getInstance(), level));
        
        if (level >= 5) {
            animal.setMetadata("fullyDomesticated", new org.bukkit.metadata.FixedMetadataValue(
                org.shotrush.atom.Atom.getInstance(), true));
        } else {
            animal.removeMetadata("fullyDomesticated", org.shotrush.atom.Atom.getInstance());
        }
        
        player.sendMessage("§aSet " + animal.getType() + " domestication level to " + level);
        player.sendMessage("§eRespawn the animal to see stat changes take effect.");
    }
}
