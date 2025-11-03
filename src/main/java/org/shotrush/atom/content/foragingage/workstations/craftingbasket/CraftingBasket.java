package org.shotrush.atom.content.foragingage.workstations.craftingbasket;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.InteractiveSurface;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;
import org.shotrush.atom.core.recipe.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@AutoRegister(priority = 33)
public class CraftingBasket extends InteractiveSurface {

    public CraftingBasket(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public CraftingBasket(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public int getMaxItems() {
        return 4;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null;
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        float[][] positions = {
                {-0.3f, 0.3f, -0.3f},
                {0.3f, 0.3f, -0.3f},
                {-0.3f, 0.3f, 0.3f},
                {0.3f, 0.3f, 0.3f}
        };

        if (itemCount < positions.length) {
            return new Vector3f(positions[itemCount][0], positions[itemCount][1], positions[itemCount][2]);
        }
        return new Vector3f(0, 0.2f, 0);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack basketItem = createItemWithCustomModel(Material.STONE_BUTTON, "crafting_basket");

        spawnDisplay(display, plugin, basketItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), false, 1f, 0.2f);

        /* // Handled by InteractiveSurface.spawn()
        for (PlacedItem item : placedItems) {
            spawnItemDisplay(item);
        }
         */
    }


    @Override
    public void update(float globalAngle) {
    }

    @Override
    protected void removeEntities() {
        for (PlacedItem item : placedItems) {
            removeItemDisplay(item);
            spawnLocation.getWorld().dropItemNaturally(spawnLocation, item.getItem());
        }
        Entity display = Bukkit.getEntity(displayUUID);
        if (display != null) display.remove();
        Entity interaction = Bukkit.getEntity(interactionUUID);
        if (interaction != null) interaction.remove();
    }

    @Override
    public boolean isValid() {
        if (interactionUUID == null || displayUUID == null) return false;
        Entity interaction = Bukkit.getEntity(interactionUUID);
        Entity display = Bukkit.getEntity(displayUUID);
        return interaction != null && display != null && !interaction.isDead() && !display.isDead();
    }

    @Override
    protected ItemStack checkRecipe() {
        RecipeManager recipeManager = Atom.getInstance().getRecipeManager();
        if (recipeManager == null) return null;

        List<ItemStack> items = new ArrayList<>();
        for (PlacedItem placedItem : placedItems) {
            items.add(placedItem.getItem());
        }

        ItemStack result = recipeManager.findMatch(items);
        if (result != null) {
            applyQualityInheritance(result);
        }
        return result;
    }

    @Override
    public boolean onWrenchInteract(Player player, boolean sneaking) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (sneaking) {
            ItemStack removed = removeLastItem();
            if (removed != null) {
                player.getInventory().addItem(removed);
                return true;
            }
            return false;
        }

        if (hand.getType() == Material.WOODEN_HOE || hand.getType() == Material.AIR) return false;

        if (placeItem(player, hand, calculatePlacement(player, placedItems.size()), 0)) {
            hand.setAmount(hand.getAmount() - 1);
            return true;
        }
        return false;
    }

    @Override
    public String getIdentifier() {
        return "crafting_basket";
    }

    @Override
    public String getDisplayName() {
        return "§eCrafting Basket";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "§7A basket for crafting items",
                "§8Place up to 4 items"
        };
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;

        CraftingBasket basket = new CraftingBasket((Location) parsed[1], (BlockFace) parsed[2]);

        // Deserialize additional data into the new instance
        String[] parts = data.split(";");
        basket.deserializeAdditionalData(parts, 5);

        return basket;
    }
}
