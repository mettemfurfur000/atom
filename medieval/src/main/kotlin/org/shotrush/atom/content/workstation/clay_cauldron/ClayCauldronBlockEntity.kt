package org.shotrush.atom.content.workstation.clay_cauldron

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.bukkit.item.BukkitItemManager
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.Vec3d
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.World
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.blocks.AtomBlockEntity
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.getItemStack
import org.shotrush.atom.item.Material
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.matches
import org.shotrush.atom.putItemStack

import org.bukkit.Particle
import org.bukkit.Sound
import org.shotrush.atom.content.systems.ItemHeatSystem
import org.shotrush.atom.core.util.ActionBarManager
import kotlin.math.ceil

class ClayCauldronBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : AtomBlockEntity(Workstations.CLAY_CAULDRON, pos, blockState) {
    val allowedItems = listOf(
        "minecraft:raw_iron",
        "minecraft:raw_copper",
    )
    val fluidAsItem: ItemStack
        get() {
            if (fluid == null) return ItemStack.empty()
            return CraftEngineItems.byId(
                Key.of(
                    when (fluid) {
                        Material.Copper -> "atom:copper_fluid_layer"
                        Material.Iron -> "atom:iron_fluid_layer"
                        else -> "atom:lava_fluid_layer"
                    }
                )
            )?.buildItemStack() ?: ItemStack.empty()
        }
    var currentlyConsuming: ItemStack = ItemStack.empty()
        private set(value) {
            field = value
            markDirty(false)
        }
    var consumingProgress: Int = 0
        private set(value) {
            field = value
            markDirty(false)
        }
    var storedItem: ItemStack = ItemStack.empty()
        private set(value) {
            field = value
            markDirty()
        }
        get() = if (field.isEmpty) ItemStack.empty() else field

    val TICKS_TO_MELT = 200
    val FLUID_PER_RAW = 200
    val FLUID_PER_TICK = FLUID_PER_RAW / TICKS_TO_MELT
    val FLUID_PER_INGOT = FLUID_PER_RAW * 5
    val MAX_FLUID = FLUID_PER_INGOT * 4

    var fluidStored: Int = 0
        private set(value) {
            field = value
            markDirty()
        }
    var fluid: Material? = null
        private set(value) {
            field = value
            markDirty()
        }

    init {
        blockEntityRenderer = ClayCauldronDynamicRenderer(this)
    }

    override fun loadCustomData(tag: CompoundTag) {
        if (tag.containsKey("storedItem")) {
            storedItem = tag.getItemStack("storedItem")
        }
        fluidStored = tag.getInt("fluidStored")
        consumingProgress = tag.getInt("consumingProgress")
        if (tag.containsKey("fluid")) {
            fluid = Material.byId(tag.getString("fluid"))
        }
        if (tag.containsKey("currentlyConsuming")) {
            currentlyConsuming = tag.getItemStack("currentlyConsuming")
        }
    }

    fun isLitCampfireUnderneath(): Boolean {
        val blockState = (world.world.platformWorld() as World).getBlockAt(pos.x(), pos.y() -1, pos.z())
        val data = blockState.blockData
        if(data !is Campfire) return false
        return data.isLit
    }

    override fun saveCustomData(tag: CompoundTag) {
        if (!storedItem.isEmpty) tag.putItemStack("storedItem", storedItem)
        tag.putInt("fluidStored", fluidStored)
        tag.putInt("consumingProgress", consumingProgress)
        if (fluid != null) {
            tag.putString("fluid", fluid!!.id)
        }
        if (!currentlyConsuming.isEmpty) {
            tag.putItemStack("currentlyConsuming", currentlyConsuming)
        }
    }

    override fun preRemove() {
        val pos = Vec3d.atCenterOf(this.pos)
        if (!storedItem.isEmpty) {
            world.world().dropItemNaturally(pos, BukkitItemManager.instance().wrap(storedItem))
        }
    }

    fun canStoreItem(item: ItemStack): Boolean {
        if (item.isEmpty) return false
        if (allowedItems.none { item.matches(it) }) return false
        if (fluid != null) {
            if (fluid == Material.Copper && !item.matches("minecraft:raw_copper")) return false
            if (fluid == Material.Iron && !item.matches("minecraft:raw_iron")) return false
        }
        return storedItem.isEmpty || storedItem.isSimilar(item) && storedItem.amount != storedItem.maxStackSize
    }

    fun amountToStore(item: ItemStack): Int {
        if (item.isEmpty) return 0
        if (storedItem.isEmpty) return item.maxStackSize.coerceAtMost(item.amount)
        if (!storedItem.isSimilar(item)) return 0
        return (storedItem.maxStackSize - storedItem.amount).coerceAtMost(item.amount)
    }

    fun tickConsumption() {
        if (currentlyConsuming.isEmpty) {
            if (storedItem.isEmpty) return
            currentlyConsuming = storedItem.clone().apply {
                amount = 1
            }
            storedItem.amount--
            markDirty()
            return
        }
        if (fluidStored + FLUID_PER_TICK >= MAX_FLUID) {
            return
        }
        if (consumingProgress >= TICKS_TO_MELT) {
            consumingProgress = 0
            currentlyConsuming.amount--

            val bukkitWorld = world.world.platformWorld() as World
            val location = org.bukkit.Location(bukkitWorld, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5)
            bukkitWorld.playSound(location, Sound.BLOCK_LAVA_POP, 0.5f, 1.0f)
            bukkitWorld.spawnParticle(Particle.SMOKE, location, 3, 0.1, 0.2, 0.1, 0.0)
        } else {
            consumingProgress++
            fluid = if (currentlyConsuming.matches("minecraft:raw_copper")) Material.Copper else Material.Iron
            fluidStored += FLUID_PER_TICK
        }
    }

    fun tick() {
        if(isLitCampfireUnderneath()) {
            tickConsumption()
        }
    }

    fun storeItem(clone: ItemStack) {
        if (clone.isEmpty) return
        if (storedItem.isEmpty) {
            storedItem = clone
            return
        } else if (storedItem.isSimilar(clone)) {
            storedItem.amount += clone.amount
            markDirty()
        }
    }

    fun fillMold(player: Player, item: ItemStack, type: MoldType, shape: MoldShape): InteractionResult {
        if (fluid == null) {
            ActionBarManager.send(player, "<red>The cauldron is empty!</red>")
            return InteractionResult.PASS
        }
        if (fluidStored < FLUID_PER_INGOT) {
            val missing = FLUID_PER_INGOT - fluidStored
            val rawNeeded = ceil(missing.toDouble() / FLUID_PER_RAW).toInt()
            val percent = (fluidStored.toDouble() / FLUID_PER_INGOT * 100).toInt()
            val matName = if(fluid == Material.Copper) "Copper" else "Iron"

            ActionBarManager.send(player, "<red>Need more $matName!</red> <gray>Add <white>$rawNeeded</white> more raw ore ($percent% full)</gray>")
            return InteractionResult.PASS
        }
        item.amount--
        fluidStored -= FLUID_PER_INGOT
        val filledMold = Molds.getFilledMold(shape, type, fluid!!)
        
        // Set initial heat to be very hot
        ItemHeatSystem.setItemHeat(filledMold, 300.0)
        
        player.inventory.addItem(filledMold)

        player.playSound(player.location, Sound.ITEM_BUCKET_FILL_LAVA, 1.0f, 1.0f)
        ActionBarManager.send(player, "<green>Filled Mold!</green>")

        if (fluidStored <= 0) fluid = null
        return InteractionResult.SUCCESS
    }
}