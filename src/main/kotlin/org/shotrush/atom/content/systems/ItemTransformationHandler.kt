package org.shotrush.atom.content.systems

import org.bukkit.*
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.shotrush.atom.Atom
import org.shotrush.atom.content.systems.ItemHeatSystem.Companion.setItemHeat
import org.shotrush.atom.item.Molds.emptyMold

/**
 * Handles item transformation processes like mold cooling and clay drying
 */
object ItemTransformationHandler {
    private val COOLING_START_KEY = NamespacedKey("atom", "cooling_start_time")
    private val CLAY_DRYING_START_KEY = NamespacedKey("atom", "clay_drying_start_time")

    const val MOLD_COOLING_START_TIME_MS: Long = 30000L
    private const val MOLD_COOLING_RATE = 0.005
    private const val MOLD_PARTICLE_HEAT_THRESHOLD = 50.0

    const val CLAY_DRYING_START_TIME_MS: Long = 30000L

    // private static final long CLAY_DRY_TIME_MS = 900000L; // Actual Production Time
    const val CLAY_MIN_LIGHT_LEVEL: Int = 12

    /**
     * Handles the cooling process for filled molds
     */
    fun handleFilledMoldCooling(
        itemDisplay: ItemDisplay, itemStack: ItemStack,
        loc: Location, currentHeat: Double,
        heatDifference: Double
    ) {
        val heatChange = heatDifference * MOLD_COOLING_RATE
        val newHeat = currentHeat + heatChange

        val meta = itemStack.itemMeta
        var startTime: Long? = null
        if (meta != null) {
            meta.persistentDataContainer.get(COOLING_START_KEY, PersistentDataType.LONG)
            if (startTime == null) {
                startTime = System.currentTimeMillis()
                meta.persistentDataContainer.set(COOLING_START_KEY, PersistentDataType.LONG, startTime)
                itemStack.setItemMeta(meta)
                itemDisplay.setItemStack(itemStack)
            }
        }

        setItemHeat(itemStack, newHeat)
        itemDisplay.setItemStack(itemStack)

        if (currentHeat > MOLD_PARTICLE_HEAT_THRESHOLD) {
            spawnCoolingParticles(loc)
            playCoolingSound(loc, startTime)
        }
        val elapsedMillis =
            System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
        if (elapsedMillis >= MOLD_COOLING_START_TIME_MS) {
            completeMoldCooling(itemDisplay, itemStack, loc)
        }
    }

    fun handleClayDrying(
        itemDisplay: ItemDisplay, itemStack: ItemStack,
        loc: Location
    ) {
        val block = loc.block
        val world = loc.world

        val isInSunlight =
            block.lightFromSky >= CLAY_MIN_LIGHT_LEVEL && world.time >= 0 && world.time <= 12000 && !world.hasStorm()

        val meta = itemStack.itemMeta ?: return

        var startTime = meta.persistentDataContainer.get(
            CLAY_DRYING_START_KEY,
            PersistentDataType.LONG
        )

        if (!isInSunlight) {
            spawnWetClayParticles(loc, world)

            if (startTime != null) {
                meta.persistentDataContainer.remove(CLAY_DRYING_START_KEY)
                itemStack.setItemMeta(meta)
                itemDisplay.setItemStack(itemStack)
            }
            return
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis()
            meta.persistentDataContainer.set<Long?, Long?>(
                CLAY_DRYING_START_KEY,
                PersistentDataType.LONG, startTime
            )
            itemStack.setItemMeta(meta)
            itemDisplay.setItemStack(itemStack)
        }

        val elapsedMillis = System.currentTimeMillis() - startTime
        val progress = elapsedMillis.toFloat() / CLAY_DRYING_START_TIME_MS.toFloat()

        spawnDryingParticles(loc, world, progress)
        playDryingSound(loc, world, elapsedMillis)

        if (elapsedMillis >= CLAY_DRYING_START_TIME_MS) {
            convertClayToBrick(itemDisplay, itemStack, loc)
        }
    }


    private fun spawnCoolingParticles(loc: Location) {
        loc.world.spawnParticle(Particle.SMOKE, loc, 1, 0.1, 0.1, 0.1, 0.01)
    }

    private fun playCoolingSound(loc: Location, startTime: Long?) {
        val elapsedTicks = (System.currentTimeMillis() - (startTime ?: 0)) / 50
        if (elapsedTicks % 100 == 0L) {
            loc.world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.1f, 0.5f)
        }
    }

    private fun spawnWetClayParticles(loc: Location, world: World) {
        world.spawnParticle(
            Particle.SMOKE, loc.clone().add(0.0, 0.3, 0.0),
            1, 0.1, 0.1, 0.1, 0.01
        )
    }

    private fun spawnDryingParticles(loc: Location, world: World, progress: Float) {
        world.spawnParticle(
            Particle.DUST_PLUME, loc.clone().add(0.0, 0.3, 0.0),
            1, 0.1, 0.1, 0.1, 0.01
        )

        if (progress > 0.5f) {
            if (world.gameTime % 40L == 0L) {
                world.spawnParticle(
                    Particle.CLOUD, loc.clone().add(0.0, 0.3, 0.0),
                    1, 0.05, 0.05, 0.05, 0.01
                )
            }
        }

        if (progress > 0.7f) {
            world.spawnParticle(
                Particle.FLAME, loc.clone().add(0.0, 0.3, 0.0),
                1, 0.05, 0.05, 0.05, 0.0
            )
        }
    }

    private fun playDryingSound(loc: Location, world: World, elapsedMillis: Long) {
        val elapsedTicks = elapsedMillis / 50
        if (elapsedTicks % 100 == 0L) {
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.1f, 1.2f)
        }
    }

    private fun completeMoldCooling(itemDisplay: ItemDisplay, itemStack: ItemStack, loc: Location) {
        try {
            val result: Pair<ItemStack?, ItemStack?> = emptyMold(itemStack)

            if (result.first != null && result.first!!.type != Material.AIR) {
                itemDisplay.world.dropItem(loc, result.first!!)
            }

            if (result.second != null && result.second!!.type != Material.AIR) {
                itemDisplay.world.dropItem(loc, result.second!!)
            }

            loc.world.playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            loc.world.spawnParticle(Particle.LAVA, loc, 5, 0.1, 0.1, 0.1, 0.0)

            itemDisplay.remove()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun convertClayToBrick(itemDisplay: ItemDisplay, clayItem: ItemStack, loc: Location) {
        val world = loc.world
        val amount = clayItem.amount

        world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.5f)
        world.spawnParticle(Particle.FLAME, loc, 15, 0.2, 0.2, 0.2, 0.02)
        world.spawnParticle(Particle.SMOKE, loc, 10, 0.2, 0.2, 0.2, 0.01)

        val brick = ItemStack(Material.BRICK, amount)
        itemDisplay.setItemStack(brick)

        Atom.instance.logger.info(
            "Converted clay to brick at " +
                    loc.blockX + ", " + loc.blockY + ", " + loc.blockZ
        )
    }
}