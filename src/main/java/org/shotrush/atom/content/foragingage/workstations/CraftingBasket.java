package org.shotrush.atom.content.foragingage.workstations;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
        return 2;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null;
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        float[][] positions = {
            {-0.2f, 0.2f, 0f},
            {0.2f, 0.2f, 0f}
        };
        
        if (itemCount < positions.length) {
            return new Vector3f(positions[itemCount][0], positions[itemCount][1], positions[itemCount][2]);
        }
        return new Vector3f(0, 0.2f, 0);
    }

    @Override
    public void spawn(Atom plugin) {
        Bukkit.getRegionScheduler().run(plugin, spawnLocation, task -> {
            ItemDisplay display = (ItemDisplay) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
            ItemStack basketItem = createItemWithCustomModel(Material.STONE_BUTTON, "crafting_basket");
            
            spawnDisplay(display, plugin, basketItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), false, 1f, 0.2f);
            
            for (PlacedItem item : placedItems) {
                spawnItemDisplay(item);
            }
        });
    }

    @Override
    protected void spawnItemDisplay(PlacedItem item) {
        Location itemLoc = spawnLocation.clone().add(item.getPosition().x, item.getPosition().y, item.getPosition().z);
        Bukkit.getRegionScheduler().run(Atom.getInstance(), itemLoc, task -> {
            ItemDisplay display = (ItemDisplay) itemLoc.getWorld().spawnEntity(itemLoc, EntityType.ITEM_DISPLAY);
            display.setItemStack(item.getItem());
            
            AxisAngle4f flatRotation = new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);
            
            display.setTransformation(new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),
                flatRotation,
                new Vector3f(0.5f, 0.5f, 0.5f),
                new AxisAngle4f()
            ));
            item.setDisplayUUID(display.getUniqueId());
        });
    }

    @Override
    protected void removeItemDisplay(PlacedItem item) {
        if (item.getDisplayUUID() != null) {
            Entity entity = Bukkit.getEntity(item.getDisplayUUID());
            if (entity != null) entity.remove();
        }
    }

    @Override
    public void update(float globalAngle) {}

    @Override
    public void remove() {
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
        Entity interaction = Bukkit.getEntity(interactionUUID);
        Entity display = Bukkit.getEntity(displayUUID);
        return interaction != null && display != null && !interaction.isDead() && !display.isDead();
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
        
        if (placeItem(hand, calculatePlacement(player, placedItems.size()), 0)) {
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
            "§8Place up to 2 items"
        };
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        try {
            String[] parts = data.split(";");
            World world = Bukkit.getWorld(parts[0]);
            Location location = new Location(world, 
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
            );
            BlockFace face = BlockFace.valueOf(parts[4]);
            return new CraftingBasket(location, face);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ItemStack getDropItem() {
        return new ItemStack(Material.DEAD_BUSH);
    }
}
