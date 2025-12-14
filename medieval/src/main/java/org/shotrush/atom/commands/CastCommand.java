package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 31)
@CommandAlias("cast")
@Description("Get clay mold (cast) blocks")
public class CastCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.cast")
    public void onCast(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "clay_mold");
        player.sendMessage("§aGiven empty clay mold!");
    }
    
    @Subcommand("empty")
    @CommandPermission("atom.cast")
    @Description("Get an empty clay mold")
    public void onCastEmpty(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "clay_mold");
        player.sendMessage("§aGiven empty clay mold!");
    }
    
    @Subcommand("filled")
    @CommandPermission("atom.cast")
    @Description("Get a filled clay mold")
    public void onCastFilled(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "clay_mold_filled");
        player.sendMessage("§aGiven filled clay mold!");
    }
}
