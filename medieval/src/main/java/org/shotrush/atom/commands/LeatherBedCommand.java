package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 34)
@CommandAlias("leatherbed|leatherrack")
@Description("Get a leather drying bed")
public class LeatherBedCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.leatherbed")
    public void onLeatherBed(Player player) {
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of("atom:leather_bed"));
        if (customItem != null) {
            ItemStack item = customItem.buildItemStack();
            player.getInventory().addItem(item);
            player.sendMessage("§aYou received a Leather Drying Bed!");
        }
    }
}
