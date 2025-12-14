package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 33)
@CommandAlias("craftingbasket|basket")
@Description("Get a crafting basket")
public class CraftingBasketCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.craftingbasket")
    public void onCraftingBasket(Player player) {
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of("atom:crafting_basket"));
        if (customItem != null) {
            ItemStack item = customItem.buildItemStack();
            player.getInventory().addItem(item);
            player.sendMessage("§aYou received a Crafting Basket!");
        }
    }
}
