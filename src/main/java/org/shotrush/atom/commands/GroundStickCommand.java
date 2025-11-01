package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 30)
@CommandAlias("groundstick")
@Description("Get a ground stick block")
public class GroundStickCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.groundstick")
    public void onGroundStick(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "ground_stick");
    }
}
