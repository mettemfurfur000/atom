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
import org.shotrush.atom.core.util.ActionBarManager;

@AutoRegister(priority = 60)
@CommandAlias("skin")
@CommandPermission("atom.skin")
public class SkinCommand extends BaseCommand {
    
    @Subcommand("set")
    @CommandPermission("atom.skin.set")
    public void setSkin(Player player, String username) {
        ActionBarManager.send(player, "§7Fetching skin from §e" + username + "§7...");
        
        SkinAPI.setSkinFromUsername(player, username).thenAccept(success -> {
            if (success) {
                ActionBarManager.send(player, "§aSkin changed to " + username + "'s skin!");
            } else {
                ActionBarManager.send(player, "§cFailed to fetch skin from " + username);
            }
        });
    }
    
    @Subcommand("setdefault")
    @CommandPermission("atom.skin.setdefault")
    public void setDefaultSkin(Player player, String username) {
        ActionBarManager.send(player, "§7Setting default skin to §e" + username + "§7...");
        
        SkinAPI.setSkinFromUsername(player, username).thenAccept(success -> {
            if (success) {
                YamlConfiguration serverData = Atom.getInstance().getDataStorage().getServerData();
                serverData.set("default_skin_username", username);
                Atom.getInstance().getDataStorage().saveServerData(serverData);
                
                ActionBarManager.send(player, "§aDefault skin set to " + username + "'s skin!");
                ActionBarManager.send(player, "§7All players will receive this skin on join.");
            } else {
                ActionBarManager.send(player, "§cFailed to fetch skin from " + username);
            }
        });
    }
    
    @Subcommand("reset")
    @CommandPermission("atom.skin.reset")
    public void resetSkin(Player player) {
        SkinAPI.setDefaultSkin(player);
        ActionBarManager.send(player, "§aSkin reset to default!");
    }
}
