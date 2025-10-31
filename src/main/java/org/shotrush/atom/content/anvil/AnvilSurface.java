package org.shotrush.atom.content.anvil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.InteractiveSurface;

import java.util.UUID;

public class AnvilSurface extends InteractiveSurface {
    
    public AnvilSurface(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }
    
    public AnvilSurface(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }
    
    @Override
    public int getMaxItems() {
        return 8;
    }
    
    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
    
    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        org.bukkit.util.RayTraceResult result = player.rayTraceBlocks(5);
        if (result != null && result.getHitPosition() != null) {
            org.bukkit.util.Vector hitPos = result.getHitPosition();
            float x = (float) (hitPos.getX() - blockLocation.getX() - 0.5);
            float z = (float) (hitPos.getZ() - blockLocation.getZ() - 0.5);
            return new Vector3f(x, 0.6f, z);
        }
        float offset = itemCount * 0.15f - 0.3f;
        return new Vector3f(offset, 0.6f, 0);
    }
    
    @Override
    public void spawn(Atom plugin) {
        Bukkit.getRegionScheduler().run(plugin, spawnLocation, task -> {
            blockLocation.getBlock().setType(Material.BARRIER);
            
            ItemDisplay base = (ItemDisplay) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
            base.setItemStack(new ItemStack(Material.ANVIL));
            base.setTransformation(new Transformation(
                new Vector3f(0, 0.5f, 0),
                new AxisAngle4f(),
                new Vector3f(1, 1, 1),
                new AxisAngle4f()
            ));
            
            Interaction interaction = (Interaction) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.INTERACTION);
            interaction.setInteractionWidth(1.01f);
            interaction.setInteractionHeight(1.01f);
            interaction.setResponsive(true);
            
            this.displayUUID = base.getUniqueId();
            this.interactionUUID = interaction.getUniqueId();
            
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
            
            float yawRadians = (float) Math.toRadians(item.getYaw());
            AxisAngle4f yawRotation = new AxisAngle4f(yawRadians, 0, 1, 0);
            AxisAngle4f tiltRotation = new AxisAngle4f((float) Math.PI / 2, 1, 0, 0);
            
            display.setTransformation(new Transformation(
                new Vector3f(0, 0.45f, 0),
                combineRotations(yawRotation, tiltRotation),
                new Vector3f(0.3f, 0.3f, 0.3f),
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
    
    private AxisAngle4f combineRotations(AxisAngle4f first, AxisAngle4f second) {
        org.joml.Quaternionf q1 = new org.joml.Quaternionf().rotateAxis(first.angle, first.x, first.y, first.z);
        org.joml.Quaternionf q2 = new org.joml.Quaternionf().rotateAxis(second.angle, second.x, second.y, second.z);
        q1.mul(q2);
        
        AxisAngle4f result = new AxisAngle4f();
        q1.get(result);
        return result;
    }
    
    @Override
    public void update(float globalAngle) {}
    
    @Override
    public void remove() {
        for (PlacedItem item : placedItems) {
            removeItemDisplay(item);
        }
        if (displayUUID != null) {
            Entity entity = Bukkit.getEntity(displayUUID);
            if (entity != null) entity.remove();
        }
        if (interactionUUID != null) {
            Entity entity = Bukkit.getEntity(interactionUUID);
            if (entity != null) entity.remove();
        }
        blockLocation.getBlock().setType(Material.AIR);
    }
    
    @Override
    public boolean isValid() {
        if (interactionUUID == null) return false;
        Entity entity = Bukkit.getEntity(interactionUUID);
        return entity != null && entity.isValid();
    }
    
    @Override
    public String getBlockType() {
        return "anvil_surface";
    }
    
    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(spawnLocation.getWorld().getName()).append(";");
        sb.append(spawnLocation.getX()).append(";");
        sb.append(spawnLocation.getY()).append(";");
        sb.append(spawnLocation.getZ()).append(";");
        sb.append(blockFace.name()).append(";");
        sb.append(placedItems.size());
        for (PlacedItem item : placedItems) {
            sb.append(";").append(item.getItem().getType().name());
            sb.append(",").append(item.getPosition().x);
            sb.append(",").append(item.getPosition().y);
            sb.append(",").append(item.getPosition().z);
            sb.append(",").append(item.getYaw());
        }
        return sb.toString();
    }
    
    @Override
    public boolean onWrenchInteract(Player player, boolean sneaking) {
        if (sneaking) {
            ItemStack removed = removeLastItem();
            if (removed != null) {
                player.getInventory().addItem(removed);
                player.sendMessage("§7Removed item (" + placedItems.size() + "/" + getMaxItems() + ")");
                return true;
            }
            return false;
        }
        
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.WOODEN_HOE) return false;
        if (hand.getType() == Material.AIR) return false;
        
        Vector3f pos = calculatePlacement(player, placedItems.size());
        if (placeItem(hand, pos, player.getLocation().getYaw())) {
            hand.setAmount(hand.getAmount() - 1);
            player.sendMessage("§aPlaced item (" + placedItems.size() + "/" + getMaxItems() + ")");
            return true;
        }
        
        player.sendMessage("§cSurface is full!");
        return false;
    }
}
