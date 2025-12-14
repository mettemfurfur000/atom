package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.util.ActionBarManager;

@AutoRegister(priority = 6)
@CommandAlias("spear|woodspear")
@Description("Get a wooden spear")
public class SpearCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.spear")
    public void onSpear(Player player) {
        CustomItem spearItem = Atom.getInstance().getItemRegistry().getItem("wood_spear");
        if (spearItem != null) {
            ItemStack spear = spearItem.create();
            player.getInventory().addItem(spear);
            ActionBarManager.send(player, "§aYou received a Wooden Spear!");
        } else {
            ActionBarManager.send(player, "§cWooden spear not found!");
        }
    }
}
