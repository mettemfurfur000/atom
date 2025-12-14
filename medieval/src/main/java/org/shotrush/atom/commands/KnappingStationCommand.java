package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 32)
@CommandAlias("knapping|knappingstation")
@Description("Get a knapping station")
public class KnappingStationCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.knapping")
    public void onKnapping(Player player) {
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of("atom:knapping_station"));
        if (customItem != null) {
            ItemStack item = customItem.buildItemStack();
            player.getInventory().addItem(item);
            player.sendMessage("§aYou received a Knapping Station!");
        }
    }
}
