package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;
import org.shotrush.atom.core.skin.SkinAPI;

@AutoRegister(priority = 60)
@CommandAlias("skin")
@CommandPermission("atom.skin")
public class SkinCommand extends BaseCommand {
    
    @Subcommand("set")
    @CommandPermission("atom.skin.set")
    public void setSkin(Player player, String username) {
        player.sendMessage("§7Fetching skin from §e" + username + "§7...");
        
        SkinAPI.setSkinFromUsername(player, username).thenAccept(success -> {
            if (success) {
                player.sendMessage("§aSkin changed to " + username + "'s skin!");
            } else {
                player.sendMessage("§cFailed to fetch skin from " + username);
            }
        });
    }
    
    @Subcommand("setdefault")
    @CommandPermission("atom.skin.setdefault")
    public void setDefaultSkin(Player player, String username) {
        player.sendMessage("§7Setting default skin to §e" + username + "§7...");
        
        SkinAPI.setSkinFromUsername(player, username).thenAccept(success -> {
            if (success) {
                YamlConfiguration serverData = Atom.getInstance().getDataStorage().getServerData();
                serverData.set("default_skin_username", username);
                Atom.getInstance().getDataStorage().saveServerData(serverData);
                
                player.sendMessage("§aDefault skin set to " + username + "'s skin!");
                player.sendMessage("§7All players will receive this skin on join.");
            } else {
                player.sendMessage("§cFailed to fetch skin from " + username);
            }
        });
    }
    
    @Subcommand("reset")
    @CommandPermission("atom.skin.reset")
    public void resetSkin(Player player) {
        SkinAPI.setDefaultSkin(player);
        player.sendMessage("§aSkin reset to default!");
    }
}
