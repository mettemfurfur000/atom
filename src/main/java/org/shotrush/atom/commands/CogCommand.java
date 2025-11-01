package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 10)
@CommandAlias("cog")
@Description("Get a cog block")
public class CogCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.cog")
    public void onCog(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "cog");
    }
}