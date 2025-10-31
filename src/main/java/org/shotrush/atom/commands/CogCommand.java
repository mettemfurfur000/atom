package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.cog.Cog;

@CommandAlias("cog")
@Description("Get cog items")
public class CogCommand extends BaseCommand {
    
    @Default
    @Subcommand("get")
    @Description("Get a small cog")
    @CommandPermission("atom.cog.get")
    public void onGet(Player player, @Default("1") int amount) {
        player.getInventory().addItem(Cog.getCogItem(false).asQuantity(amount));
        player.sendMessage("§aGave you " + amount + " small cog(s)!");
    }
    
    @Subcommand("get powered")
    @Description("Get a powered cog")
    @CommandPermission("atom.cog.get")
    public void onGetPowered(Player player, @Default("1") int amount) {
        player.getInventory().addItem(Cog.getCogItem(true).asQuantity(amount));
        player.sendMessage("§aGave you " + amount + " powered cog(s)!");
    }
}
