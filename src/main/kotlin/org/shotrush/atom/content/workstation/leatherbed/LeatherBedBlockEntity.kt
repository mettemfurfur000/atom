package org.shotrush.atom.content.workstation.leatherbed

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.bukkit.item.BukkitItemManager
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.properties.Property
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.util.HorizontalDirection
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.ChunkPos
import net.momirealms.craftengine.core.world.Vec3d
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.content.AnimalProduct
import org.shotrush.atom.content.foraging.items.SharpenedFlint
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.core.api.player.PlayerDataAPI
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.getNamespacedKey
import org.shotrush.atom.item.Items
import org.shotrush.atom.matches
import plutoproject.adventurekt.component
import plutoproject.adventurekt.text.style.textDarkGray
import plutoproject.adventurekt.text.style.textGray
import plutoproject.adventurekt.text.style.textGreen
import plutoproject.adventurekt.text.style.textYellow
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with
import kotlin.random.Random

class LeatherBedBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : BlockEntity(Workstations.LEATHER_BED_ENTITY_TYPE, pos, blockState) {
    val rotation: HorizontalDirection
        get() = blockState().get(blockState().properties.first() as Property<HorizontalDirection>)
    var storedItem: ItemStack = ItemStack.empty()
        private set(value) {
            field = value
            markDirty()
        }

    init {
        blockEntityRenderer = LeatherBedBlockDynamicRenderer(this)
    }

    override fun loadCustomData(tag: CompoundTag) {
        val str = tag.get("key")
        if (str != null) {
            storedItem = CraftEngineItems.byId(Key.of(tag.getString("key")))?.buildItemStack() ?: ItemStack.empty()
        }
    }

    override fun saveCustomData(tag: CompoundTag) {
        if (!storedItem.isEmpty)
            tag.putString("key", storedItem.getNamespacedKey())
    }

    override fun preRemove() {
        val pos = Vec3d.atCenterOf(this.pos)
        if (!storedItem.isEmpty) {
            world.world().dropItemNaturally(pos, BukkitItemManager.instance().wrap(storedItem))
        }
    }

    fun updateRender() {
        val render = blockEntityRenderer as? LeatherBedBlockDynamicRenderer ?: return
        if (world == null) return
        val chunk = world.getChunkAtIfLoaded(ChunkPos(pos)) ?: return
        val tracking = chunk.trackedBy
        tracking.forEach(render::update)
    }

    val location: Location
        get() = Location(
            world.world.platformWorld() as World,
            pos.x().toDouble(),
            pos.y().toDouble(),
            pos.z().toDouble()
        )

    val dropLocation: Location
        get() {
            val rot = rotation
            return Location(
                world.world.platformWorld() as World,
                pos.x().toDouble() + rot.stepX(),
                pos.y().toDouble() + 0.5f,
                pos.z().toDouble() + rot.stepZ()
            )
        }

    fun markDirty() {
        world?.getChunkAtIfLoaded(ChunkPos(pos))?.setDirty(true);
        updateRender()
    }

    fun hasItem(): Boolean = !storedItem.isEmpty
    fun startScraping(player: Player, item: ItemStack) {
        val atom = Atom.Companion.instance
        atom.launch(atom.regionDispatcher(location)) {
            val strokeCount = 20 + Random.Default.nextInt(11)
            var currentStroke = 0

            ActionBarManager.sendStatus(player, "§7Scraping leather... Use the tool carefully")

            while (currentStroke < strokeCount && isActive) {
                delay(250)
                if (!player.isHandRaised || !LeatherBedBlockBehavior.isScrapingTool(player.activeItem)) {
                    ActionBarManager.sendStatus(player, "§cScraping cancelled - tool lowered")
                    delay(1000)
                    break
                }

                if (!player.isOnline || player.location.distance(location) > 5.0) break

                withContext(atom.entityDispatcher(player)) {
                    playScrapingEffects(player)
                }

                currentStroke++

                val prog = (currentStroke.toFloat() / strokeCount * 100).toInt()
                ActionBarManager.sendStatus(player, "§7Scraping leather... §e$prog%")
                player.sendActionBar(component {
                    text("$prog% ") with textYellow
                    text("[") with textDarkGray
                    var total = 20
                    var bars = total * (prog / 100.0)
                    repeat(bars.toInt()) {
                        text("|") with textGreen
                    }
                    repeat(total - bars.toInt()) {
                        text("|") with textGray
                    }
                    text("]") with textDarkGray
                })
            }


            if (currentStroke >= strokeCount) {
                finishScraping(player, item)
            }

            ActionBarManager.clearStatus(player)
        }

    }

    private fun finishScraping(player: Player, tool: ItemStack) {
        val center = dropLocation

        val storedProduct = Items.getAnimalProductFromItem(storedItem)

        if (storedProduct == AnimalProduct.RawLeather) {
            val animalType = Items.getAnimalFromProduct(storedItem)

            center.world.dropItemNaturally(
                center,
                Items.getAnimalProduct(animalType, AnimalProduct.RawMeat).buildItemStack()
            )
            storedItem = Items.getAnimalProduct(animalType, AnimalProduct.Leather).buildItemStack()
        } else {
            center.world.dropItemNaturally(center, storedItem)
            storedItem = ItemStack.empty()
        }

        if (SharpenedFlint.isSharpenedFlint(tool)) {
            SharpenedFlint.damageItem(tool, player, 0.3)
        }


        player.playSound(player.location, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f)
        ActionBarManager.send(player, "§aScraped the leather successfully!")

        PlayerDataAPI.incrementInt(player, "leather_scraping.count", 0)
    }

    private fun playScrapingEffects(player: Player) {
        val location = location.add(0.5, 1.0, 0.5)
        location.world?.playSound(location, Sound.ITEM_BRUSH_BRUSHING_GENERIC, 1.0f, 1.0f)
        val dustOptions = Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.0f)
        location.world?.spawnParticle(
            Particle.DUST,
            location,
            10,
            0.2,
            0.2,
            0.2,
            0.0,
            dustOptions
        )
    }

    fun tryPlaceItem(player: Player, item: ItemStack): InteractionResult {
        if (!storedItem.isEmpty) {
            return InteractionResult.FAIL
        }
        val subtract = player.gameMode != GameMode.CREATIVE
        if (!Items.isAnimalProduct(item)) return InteractionResult.FAIL
        val product = Items.getAnimalProductFromItem(item)
        if (product == AnimalProduct.Leather) {
            val clone = item.clone()
            if (subtract) item.subtract(1)
            clone.amount = 1
            storedItem = clone
            return InteractionResult.SUCCESS
        }

        if (product == AnimalProduct.RawLeather) {
            val clone = item.clone()
            if (subtract) item.subtract(1)
            clone.amount = 1
            storedItem = clone
            return InteractionResult.SUCCESS
        }

        return InteractionResult.FAIL
    }

    fun tryEmptyItems(player: Player, item: ItemStack): InteractionResult {
        if (storedItem.isEmpty) return InteractionResult.FAIL
        location.world.dropItemNaturally(dropLocation, storedItem)
        storedItem = ItemStack.empty()
        return InteractionResult.SUCCESS
    }

    fun tick() {

    }
}