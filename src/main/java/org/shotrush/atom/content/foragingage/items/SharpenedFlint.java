package org.shotrush.atom.content.foragingage.items;

import org.shotrush.atom.core.util.ActionBarManager;
import org.shotrush.atom.item.Items;
import org.shotrush.atom.item.ItemsKt;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SharpenedFlint {
    public static boolean isSharpenedFlint(ItemStack stack) {
        return ItemsKt.isItem(Items.INSTANCE.getSharpenedFlint(), stack);
    }
    public static void damageItem(ItemStack item, Player player) {
        damageItem(item, player, 0.4);
    }

    public static void damageItem(ItemStack item, Player player, double breakChance) {
        if (item == null || item.getAmount() <= 0) return;

        int currentAmount = item.getAmount();

        if (Math.random() < breakChance) {
            item.setAmount(currentAmount - 1);

            if (currentAmount - 1 <= 0) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                ActionBarManager.send(player, "§cYour Sharpened Flint broke!");
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1.2f);
            }
        }
    }
}
