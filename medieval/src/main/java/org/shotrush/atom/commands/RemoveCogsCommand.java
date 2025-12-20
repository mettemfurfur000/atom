package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 100)
@CommandAlias("removeblocks|removecogs")
@Description("Remove all custom blocks")
public class RemoveCogsCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.removeblocks")
    public void onRemoveBlocks(Player sender) {
        Atom.getInstance().getBlockManager().removeAllBlocks();
        sender.sendMessage("§aAll blocks have been removed!");
    }
}
