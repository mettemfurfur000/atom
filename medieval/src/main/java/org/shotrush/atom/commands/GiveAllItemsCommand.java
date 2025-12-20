package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;
import org.shotrush.atom.core.items.CustomItem;

import java.util.Collection;

@AutoRegister(priority = 100)
@CommandAlias("giveallitems|allitems")
@Description("Give all auto-registered custom items")
public class GiveAllItemsCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.giveallitems")
    public void onGiveAllItems(Player player) {
        Collection<CustomItem> items = Atom.getInstance().getItemRegistry().getAllItems();
        
        if (items.isEmpty()) {
            player.sendMessage("§cNo custom items registered!");
            return;
        }
        
        int given = 0;
        for (CustomItem item : items) {
            try {
                ItemStack itemStack = item.create();
                if (itemStack != null) {
                    player.getInventory().addItem(itemStack);
                    given++;
                }
            } catch (Exception e) {
                player.sendMessage("§cFailed to create item: " + item.getIdentifier());
                e.printStackTrace();
            }
        }
        
        player.sendMessage("§aGave you " + given + " custom items!");
    }
}
