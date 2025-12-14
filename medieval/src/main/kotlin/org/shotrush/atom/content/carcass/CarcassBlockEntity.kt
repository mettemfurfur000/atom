package org.shotrush.atom.content.carcass

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import net.momirealms.craftengine.libraries.nbt.ListTag
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.content.AnimalType
import org.shotrush.atom.blocks.AtomBlockEntity
import org.shotrush.atom.core.util.ActionBarManager
import java.util.Collections
import java.util.concurrent.ThreadLocalRandom

class CarcassBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : AtomBlockEntity(CarcassBlock.CARCASS_DEF, pos, blockState) {

    var animalType: AnimalType? = null
        private set
    var deathTick: Long = 0
        private set
    var decomposed: Boolean = false
        private set
    var opened: Boolean = false
        private set
    val parts: MutableList<CarcassPartState> = Collections.synchronizedList(mutableListOf())

    private var initialized = false

    fun initialize(animalType: AnimalType, currentTick: Long) {
        val cfg = CarcassConfigs.configFor(animalType) ?: return

        this.animalType = animalType
        this.deathTick = currentTick
        this.decomposed = false
        this.opened = false
        this.parts.clear()

        val rnd = ThreadLocalRandom.current()
        cfg.parts.forEach { def ->
            parts.add(CarcassPartState(
                partId = def.id,
                remainingAmount = rnd.nextInt(def.minAmount, def.maxAmount + 1)
            ))
        }

        initialized = true
        markDirty()
    }

    override fun loadCustomData(tag: CompoundTag) {
        val animalId = tag.getString("animalType")
        if (animalId.isEmpty()) return

        val type = AnimalType.byId(animalId) ?: return
        this.animalType = type
        this.deathTick = tag.getLong("deathTick")
        this.decomposed = tag.getBoolean("decomposed")
        this.opened = tag.getBoolean("opened")

        this.parts.clear()
        val partsTag = tag.getList("parts")
        for (element in partsTag) {
            if (element is CompoundTag) {
                val partId = element.getString("id")
                val remaining = element.getInt("remaining")
                parts.add(CarcassPartState(partId, remaining))
            }
        }

        initialized = true
    }

    override fun saveCustomData(tag: CompoundTag) {
        if (!initialized) return
        val type = animalType ?: return

        tag.putString("animalType", type.id)
        tag.putLong("deathTick", deathTick)
        tag.putBoolean("decomposed", decomposed)
        tag.putBoolean("opened", opened)

        val list = ListTag()
        parts.forEach { part ->
            val partTag = CompoundTag()
            partTag.putString("id", part.partId)
            partTag.putInt("remaining", part.remainingAmount)
            list.add(partTag)
        }
        tag.put("parts", list)
    }

    fun tick() {
        if (!initialized || decomposed) return
        val type = animalType ?: return
        val cfg = CarcassConfigs.configFor(type) ?: return

        val currentTick = bukkitWorld.fullTime
        val age = currentTick - deathTick

        if (age >= cfg.decompositionTicks) {
            handleDecomposition()
        }
    }

    private fun handleDecomposition() {
        decomposed = true
        markDirty()

        val loc = location.clone().add(0.5, 0.5, 0.5)
        bukkitWorld.spawnParticle(Particle.SMOKE, loc, 20, 0.3, 0.3, 0.3, 0.02)
        bukkitWorld.playSound(loc, Sound.BLOCK_WET_GRASS_BREAK, 1f, 0.5f)

        CraftEngineBlocks.remove(location.block)
    }

    fun getConfig(): CarcassAnimalConfig? = animalType?.let { CarcassConfigs.configFor(it) }

    fun markOpened() {
        if (!opened) {
            opened = true
            markDirty()
        }
    }

    fun getPartState(partId: String): CarcassPartState? = parts.find { it.partId == partId }

    fun getPartDef(partId: String): CarcassPartDef? = getConfig()?.parts?.find { it.id == partId }

    fun isEmpty(): Boolean = !decomposed && parts.all { it.remainingAmount <= 0 }

    fun getRemainingTime(): Long {
        val cfg = getConfig() ?: return 0
        val currentTick = bukkitWorld.fullTime
        val age = currentTick - deathTick
        return (cfg.decompositionTicks - age).coerceAtLeast(0)
    }

    fun harvestPart(player: Player, partId: String): Boolean {
        if (decomposed) {
            ActionBarManager.send(player, "<red>This carcass has decomposed!</red>")
            return false
        }

        val cfg = getConfig() ?: return false
        val def = cfg.parts.find { it.id == partId } ?: return false
        val partState = parts.find { it.partId == partId } ?: return false

        if (partState.remainingAmount <= 0) {
            ActionBarManager.send(player, "<gray>Already fully harvested.</gray>")
            return false
        }

        val amountToGive = 1.coerceAtMost(partState.remainingAmount)
        val item = buildItem(def.itemId, amountToGive)
        if (item == null) {
            Atom.instance.logger.warning("Failed to build item: ${def.itemId}")
            return false
        }

        val leftover = player.inventory.addItem(item)
        if (leftover.isNotEmpty()) {
            leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
        }

        partState.remainingAmount -= amountToGive
        markDirty()

        val loc = location.clone().add(0.5, 0.5, 0.5)
        bukkitWorld.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)

        if (isEmpty()) {
            bukkitWorld.spawnParticle(Particle.SMOKE, loc, 10, 0.2, 0.2, 0.2, 0.01)
            bukkitWorld.playSound(loc, Sound.BLOCK_WET_GRASS_BREAK, 0.8f, 1f)
            CraftEngineBlocks.remove(location.block)
        }

        return true
    }

    private fun buildItem(itemId: String, amount: Int): ItemStack? {
        return try {
            val customItem = CraftEngineItems.byId(Key.of(itemId))
            if (customItem != null) {
                customItem.buildItemStack().apply { this.amount = amount }
            } else {
                val material = org.bukkit.Material.matchMaterial(itemId.removePrefix("minecraft:"))
                material?.let { ItemStack(it, amount) }
            }
        } catch (e: Exception) {
            Atom.instance.logger.warning("Failed to create item: $itemId - ${e.message}")
            null
        }
    }

    override fun preRemove() {
        // No automatic drops - harvesting handles this
    }
}
