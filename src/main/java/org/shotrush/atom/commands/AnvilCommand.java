package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;

@CommandAlias("anvil")
@Description("Get an anvil surface block")
public class AnvilCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.anvil")
    public void onAnvil(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "anvil_surface");
    }
}
