package org.shotrush.atom.content.workstation.leatherbed

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.momirealms.craftengine.bukkit.item.BukkitItemManager
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.properties.Property
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.util.HorizontalDirection
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.Vec3d
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.content.AnimalProduct
import org.shotrush.atom.blocks.AtomBlockEntity
import org.shotrush.atom.content.foraging.items.SharpenedFlint
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.getItemStack
import org.shotrush.atom.inWholeTicks
import org.shotrush.atom.item.Items
import org.shotrush.atom.putItemStack
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class LeatherBedBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : AtomBlockEntity(Workstations.LEATHER_BED, pos, blockState) {
    companion object {
        val CURING_TIME = 10.minutes.inWholeTicks
        private const val PARTICLE_INTERVAL: Long = 15L
    }

    val rotation: HorizontalDirection
        get() = blockState().get(blockState().properties.first() as Property<HorizontalDirection>)
    var storedItem: ItemStack = ItemStack.empty()
        private set(value) {
            field = value
            markDirty()
        }

    var timeCuring: Long = 0L

    init {
        blockEntityRenderer = LeatherBedBlockDynamicRenderer(this)
    }

    override fun loadCustomData(tag: CompoundTag) {
        storedItem = tag.getItemStack("storedItem")
        timeCuring = tag.getLong("timeCuring")
    }

    override fun saveCustomData(tag: CompoundTag) {
        tag.putItemStack("storedItem", storedItem)
        tag.putLong("timeCuring", timeCuring)
    }

    override fun preRemove() {
        val pos = Vec3d.atCenterOf(this.pos)
        if (!storedItem.isEmpty) {
            world.world().dropItemNaturally(pos, BukkitItemManager.instance().wrap(storedItem))
        }
    }

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

    fun hasItem(): Boolean = !storedItem.isEmpty

    fun startScraping(player: Player, item: ItemStack) {
        val atom = Atom.Companion.instance
        atom.launch(atom.regionDispatcher(location)) {
            val strokeCount = 20 + Random.Default.nextInt(11)
            var currentStroke = 0

            ActionBarManager.send(player, "leather_bed", "<gray>Scraping leather...</gray>")

            while (currentStroke < strokeCount && isActive) {
                delay(250)
                if (!player.isHandRaised || !LeatherBedBlockBehavior.isScrapingTool(player.activeItem)) {
                    ActionBarManager.send(player, "leather_bed", "<red>Scraping cancelled</red>")
                    delay(1000)
                    break
                }

                if (!player.isOnline || player.location.distance(location) > 5.0) break

                withContext(atom.entityDispatcher(player)) {
                    playScrapingEffects(player)
                }

                currentStroke++

                val prog = (currentStroke.toFloat() / strokeCount * 100).toInt()
                val str = buildString {
                    append("<yellow>$prog%</yellow> ")
                    append("<dark_gray>[")
                    append("<green>")
                    val total = 20
                    val bars = (total * (prog / 100.0)).toInt()
                    repeat(bars) { append("|") }
                    append("</green><gray>")
                    repeat(total - bars) { append("|") }
                    append("</gray>]<dark_gray>")
                }
                ActionBarManager.sendStatus(player, str)
            }

            // Complete if all strokes done
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

            // Drop meat
            center.world.dropItemNaturally(
                center,
                Items.getAnimalProduct(animalType, AnimalProduct.RawMeat).buildItemStack()
            )

            // Convert to processed leather and start curing
            storedItem = Items.getAnimalProduct(animalType, AnimalProduct.Leather).buildItemStack()

            ActionBarManager.send(
                player,
                "<green>Scraped the meat off! Leather will now stabilize over time...</green>"
            )
        } else {
            // Just drop the item if it's not raw leather
            center.world.dropItemNaturally(center, storedItem)
            storedItem = ItemStack.empty()
            ActionBarManager.send(player, "<green>Removed the leather from the bed</green>")
        }

        // Damage the tool
        if (SharpenedFlint.isSharpenedFlint(tool)) {
            SharpenedFlint.damageItem(tool, player, 0.3)
        }

        player.playSound(player.location, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f)
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
        if (Items.isAnimalProduct(storedItem)) {
            val type = Items.getAnimalProductFromItem(storedItem)
            if (type == AnimalProduct.Leather) {
                if (timeCuring >= CURING_TIME) {
                    val animal = Items.getAnimalFromProduct(storedItem)
                    storedItem = Items.getAnimalProduct(animal, AnimalProduct.CuredLeather).buildItemStack()
                    timeCuring = 0L
                    // Optional: completion burst
                    spawnCompletionParticles()
                } else {
                    timeCuring++

                    // Spawn curing-in-progress particles at intervals
                    if (timeCuring % PARTICLE_INTERVAL == 0L) {
                        spawnCuringParticles(timeCuring.toFloat() / CURING_TIME.toFloat())
                    }
                }
            }
        } else {
            timeCuring = 0L
        }
    }

    private fun spawnCuringParticles(progress: Float) {
        val baseCount = 2
        val extraByProgress = (progress * 4f).toInt() // ramps up to +4
        val count = baseCount + extraByProgress

        val particle = Particle.SMOKE
        val secondary = Particle.DRIPPING_WATER

        repeat(count) {
            val x = pos.x() + 0.5 + (Random.nextDouble() - 0.5) * 0.6
            val y = pos.y() + 0.9 + Random.nextDouble() * 0.2
            val z = pos.z() + 0.5 + (Random.nextDouble() - 0.5) * 0.6
            val vx = (Random.nextDouble() - 0.5) * 0.01
            val vy = 0.02 + Random.nextDouble() * 0.02
            val vz = (Random.nextDouble() - 0.5) * 0.01
            bukkitWorld.spawnParticle(particle, x, y, z, 1, vx, vy, vz, 0.0)
        }

        // Occasional secondary particle for variety (e.g., every ~2 seconds)
        if (bukkitWorld.gameTime % 40L == 0L) {
            val x = pos.x() + 0.5 + (Random.nextDouble() - 0.5) * 0.3
            val y = pos.y() + 0.8
            val z = pos.z() + 0.5 + (Random.nextDouble() - 0.5) * 0.3
            bukkitWorld.spawnParticle(secondary, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun spawnCompletionParticles() {
        // A small burst to signal completion
        repeat(8) {
            val x = pos.x() + 0.5 + (Random.nextDouble() - 0.5) * 0.8
            val y = pos.y() + 1.0 + Random.nextDouble() * 0.3
            val z = pos.z() + 0.5 + (Random.nextDouble() - 0.5) * 0.8
            val vx = (Random.nextDouble() - 0.5) * 0.03
            val vy = 0.05 + Random.nextDouble() * 0.05
            val vz = (Random.nextDouble() - 0.5) * 0.03
            bukkitWorld.spawnParticle(Particle.HAPPY_VILLAGER, x, y, z, 1, vx, vy, vz, 0.0)
        }
    }
}