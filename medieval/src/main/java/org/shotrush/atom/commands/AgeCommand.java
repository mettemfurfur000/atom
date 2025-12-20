package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;
import org.shotrush.atom.core.blocks.*;
import org.shotrush.atom.core.age.Age;
import org.shotrush.atom.core.age.AgeManager;

@AutoRegister(priority = 50)
@CommandAlias("age")
@Description("Manage server ages and progression")
public class AgeCommand extends BaseCommand {
    
    private final AgeManager ageManager;
    
    public AgeCommand(Plugin plugin) {
        this.ageManager = ((Atom) plugin).getAgeManager();
    }
    
    @Default
    @Subcommand("info")
    @Description("View current age information")
    public void onInfo(Player player) {
        Age current = ageManager.getCurrentAge();
        
        if (current == null) {
            player.sendMessage("§cNo age is currently set!");
            return;
        }
        
        player.sendMessage("§6=== The Current Age ===");
        player.sendMessage("§eAge: §f" + current.getDisplayName());
        player.sendMessage("§eYear: §f" + current.getYearDisplay());
        player.sendMessage("§eProgress: §f" + (ageManager.getCurrentAgeIndex() + 1) + "/" + ageManager.getTotalAges());
        
        if (current.getDescription() != null) {
            player.sendMessage("§eDescription: §7" + current.getDescription());
        }
        
        ageManager.getNextAge().ifPresentOrElse(
            next -> player.sendMessage("§eNext Age: §f" + next.getDisplayName() + " §7(" + next.getYearDisplay() + ")"),
            () -> player.sendMessage("§7This is the final age!")
        );
    }
    
    @Subcommand("next")
    @Description("Progress to the next age")
    @CommandPermission("atom.age.progress")
    public void onNext(Player player) {
        if (ageManager.progressToNextAge(player)) {
            player.sendMessage("§aYou have advanced the server to the next age!");
        } else {
            player.sendMessage("§cCannot progress - already at the final age or event was cancelled!");
        }
    }
    
    @Subcommand("set")
    @Description("Set the current age")
    @CommandPermission("atom.age.set")
    @CommandCompletion("@ages")
    public void onSet(Player player, String ageId) {
        if (ageManager.progressToAge(ageId, player)) {
            player.sendMessage("§aAge changed successfully!");
        } else {
            player.sendMessage("§cFailed to change age - age not found or event was cancelled!");
        }
    }
    
    @Subcommand("list")
    @Description("List all registered ages")
    public void onList(Player player) {
        if (ageManager.getTotalAges() == 0) {
            player.sendMessage("§cNo ages registered!");
            return;
        }
        
        player.sendMessage("§6=== Registered Ages ===");
        Age current = ageManager.getCurrentAge();
        
        for (Age age : ageManager.getAllAges()) {
            String prefix = age.equals(current) ? "§a➤ " : "§7  ";
            player.sendMessage(prefix + "§f" + age.getDisplayName() + " §7(" + age.getYearDisplay() + ")");
        }
    }
    
    @Subcommand("broadcast")
    @Description("Broadcast the current age to all players")
    @CommandPermission("atom.age.broadcast")
    public void onBroadcast(Player player) {
        if (ageManager.getCurrentAge() == null) {
            player.sendMessage("§cNo age is currently set!");
            return;
        }
        
        ageManager.broadcastCurrentAge();
        player.sendMessage("§aAge broadcasted to all players!");
    }
}
