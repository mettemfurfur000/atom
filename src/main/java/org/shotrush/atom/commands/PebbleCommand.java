package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 31)
@CommandAlias("pebble")
@Description("Get a pebble block")
public class PebbleCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.pebble")
    public void onPebble(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "pebble");
    }
}
