package org.shotrush.atom.content.mobs.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.AnimalBehaviorNew;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.Optional;
import java.util.UUID;

@CommandAlias("herd")
@Description("Debug commands for animal herds")
public class HerdCommand extends BaseCommand {
    
    private final HerdManager herdManager;
    
    public HerdCommand(HerdManager herdManager) {
        this.herdManager = herdManager;
    }
    
    @Default
    @Subcommand("info")
    @Description("Get information about the animal you're looking at")
    public void onInfo(Player player) {
        Entity target = getTargetAnimal(player, 10.0);
        
        if (!(target instanceof Animals animal)) {
            player.sendMessage(Component.text("You must be looking at an animal!", NamedTextColor.RED));
            return;
        }
        
        Optional<Herd> herdOpt = herdManager.getHerd(animal.getUniqueId());
        
        if (herdOpt.isEmpty()) {
            player.sendMessage(Component.text("This animal is not in a herd.", NamedTextColor.YELLOW));
            return;
        }
        
        Herd herd = herdOpt.get();
        boolean isLeader = herd.leader().equals(animal.getUniqueId());
        boolean isAggressive = animal.hasMetadata("aggressive") && animal.getMetadata("aggressive").get(0).asBoolean();
        double stamina = animal.hasMetadata("stamina") ? animal.getMetadata("stamina").get(0).asDouble() : 0;
        double maxStamina = animal.hasMetadata("maxStamina") ? animal.getMetadata("maxStamina").get(0).asDouble() : 100;
        
        player.sendMessage(Component.text("=== Animal Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Species: ", NamedTextColor.GRAY)
            .append(Component.text(animal.getType().name(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY)
            .append(Component.text(animal.getUniqueId().toString(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Role: ", NamedTextColor.GRAY)
            .append(Component.text(isLeader ? "LEADER" : "FOLLOWER", isLeader ? NamedTextColor.YELLOW : NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Aggressive: ", NamedTextColor.GRAY)
            .append(Component.text(isAggressive ? "Yes" : "No", isAggressive ? NamedTextColor.RED : NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Stamina: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f / %.1f", stamina, maxStamina), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Health: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f / %.1f", animal.getHealth(), 
                animal.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()), NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("=== Herd Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Herd ID: ", NamedTextColor.GRAY)
            .append(Component.text(herd.id().toString(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Members: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(herd.size()), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Panicking: ", NamedTextColor.GRAY)
            .append(Component.text(herd.isPanicking() ? "Yes" : "No", herd.isPanicking() ? NamedTextColor.RED : NamedTextColor.GREEN)));
    }
    
    @Subcommand("list")
    @Description("List all nearby herds")
    public void onList(Player player) {
        int count = 0;
        
        for (Entity entity : player.getLocation().getNearbyEntities(50, 50, 50)) {
            if (!(entity instanceof Animals animal)) continue;
            
            Optional<Herd> herdOpt = herdManager.getHerd(animal.getUniqueId());
            if (herdOpt.isEmpty()) continue;
            
            Herd herd = herdOpt.get();
            if (herd.leader().equals(animal.getUniqueId())) {
                player.sendMessage(Component.text("Herd: ", NamedTextColor.GRAY)
                    .append(Component.text(animal.getType().name(), NamedTextColor.YELLOW))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(herd.size() + " members", NamedTextColor.WHITE))
                    .append(Component.text(")", NamedTextColor.GRAY)));
                count++;
            }
        }
        
        if (count == 0) {
            player.sendMessage(Component.text("No herds found nearby.", NamedTextColor.YELLOW));
        }
    }
    
    private Entity getTargetAnimal(Player player, double range) {
        return player.getTargetEntity((int) range, false);
    }
}
