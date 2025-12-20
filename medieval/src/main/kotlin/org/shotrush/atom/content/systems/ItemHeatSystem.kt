package org.shotrush.atom.content.systems

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import lombok.Getter
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.api.combat.ArmorProtectionAPI
import org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyColdDamage
import org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyHeatDamage
import org.shotrush.atom.core.api.player.AttributeModifierAPI
import org.shotrush.atom.core.api.scheduler.SchedulerAPI
import org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getAmbientTemperature
import org.shotrush.atom.core.api.world.EnvironmentalFactorAPI.getNearbyHeatSources
import org.shotrush.atom.core.data.PersistentData
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.item.Molds.emptyMold
import org.shotrush.atom.item.Molds.isFilledMold
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ItemHeatSystem(private val plugin: Plugin?) : Listener {
    init {
        instance = this
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.getPlayer()
        startHeatTickForPlayer(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.getPlayer()
        val playerId = player.uniqueId

        val slotHeatMap: MutableMap<Int?, Double?>? = playerItemHeatCache[playerId]
        if (slotHeatMap != null) {
            for (slot in 0..8) {
                val item = player.inventory.getItem(slot)
                if (item != null && item.type != Material.AIR) {
                    val cachedHeat = slotHeatMap[slot]
                    if (cachedHeat != null) {
                        setItemHeat(item, cachedHeat)
                    }
                }
            }
        }

        playerItemHeatCache.remove(playerId)
        lastKnownItems.remove(playerId)
    }

    @EventHandler
    fun onItemHeld(event: PlayerItemHeldEvent) {
        val player = event.getPlayer()

        val previousSlot = event.previousSlot
        val previousItem = player.inventory.getItem(previousSlot)
        if (previousItem != null && previousItem.type != Material.AIR) {
            val playerId = player.uniqueId
            val slotHeatMap: MutableMap<Int?, Double?>? = playerItemHeatCache[playerId]
            if (slotHeatMap != null) {
                val cachedHeat = slotHeatMap[previousSlot]
                if (cachedHeat != null) {
                    setItemHeat(previousItem, cachedHeat)
                }
            }
        }

        val item = player.inventory.getItem(event.newSlot)

        if (item != null && item.type != Material.AIR) {
            applyHeatEffect(player, item)
        } else {
            removeHeatEffect(player)
        }
    }

    private fun startHeatTickForPlayer(player: Player) {
        SchedulerAPI.runTaskTimer(player, { task: ScheduledTask? ->
            if (!player.isOnline) {
                task?.cancel()
                playerItemHeatCache.remove(player.uniqueId)
                lastKnownItems.remove(player.uniqueId)
                return@runTaskTimer
            }
            val heldItem = player.inventory.itemInMainHand
            if (heldItem.type != Material.AIR) {
                updateItemHeatInCache(player, heldItem)
                applyHeatEffectFromCache(player, heldItem)
                displayHeatActionBarFromCache(player, heldItem)
            }
        }, 1L, 20L)
    }

    private fun updateItemHeatInCache(player: Player, item: ItemStack) {
        val playerId = player.uniqueId
        val slot = player.inventory.heldItemSlot

        val lastItem: ItemStack? = lastKnownItems[playerId]
        if (lastItem == null || !item.isSimilar(lastItem)) {
            val heat: Double = getItemHeat(item)
            playerItemHeatCache.computeIfAbsent(playerId) { k: UUID? -> HashMap<Int?, Double?>() }!![slot] = heat
            lastKnownItems[playerId] = item.clone()
        }

        val currentHeat: Double =
            playerItemHeatCache.computeIfAbsent(playerId) { k: UUID? -> HashMap<Int?, Double?>() }
                ?.computeIfAbsent(slot) { k: Int? -> getItemHeat(item) }!!

        val loc = player.location

        var bodyTemp = 37.0
        val tempSystem = PlayerTemperatureSystem.instance
        bodyTemp = tempSystem.getPlayerTemperature(player)

        var targetTemp = bodyTemp - 17.0

        val heatFromSources = getNearbyHeatSources(loc, 6)
        targetTemp += heatFromSources * 10

        val heatDifference = targetTemp - currentHeat
        val heatChange = heatDifference * 0.08
        var newHeat = currentHeat + heatChange

        newHeat = max(-100.0, min(500.0, newHeat))

        playerItemHeatCache[playerId]?.set(slot, newHeat)
    }

    private fun applyHeatEffectFromCache(player: Player, item: ItemStack?) {
        val playerId = player.uniqueId
        val slot = player.inventory.heldItemSlot

        var heat: Double? =
            playerItemHeatCache.computeIfAbsent(playerId) { k: UUID? -> HashMap<Int?, Double?>() }!!
                .get(slot)
        if (heat == null) {
            heat = getItemHeat(item)
        }

        val hasProtection = ArmorProtectionAPI.hasLeatherChestplate(player)

        if (heat != 0.0) {
            val speedModifier = -abs(heat) * 0.001
            AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        } else {
            AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            )
        }

        applyHeatDamage(player, heat, hasProtection)
        applyColdDamage(player, heat, hasProtection)
    }

    private fun displayHeatActionBarFromCache(player: Player, item: ItemStack?) {
        val playerId = player.uniqueId
        val slot = player.inventory.heldItemSlot

        var heat: Double? =
            playerItemHeatCache.computeIfAbsent(playerId) { k: UUID? -> HashMap<Int?, Double?>() }!!
                .get(slot)
        if (heat == null) {
            heat = getItemHeat(item)
        }

        val manager = ActionBarManager.getInstance() ?: return

        if (abs(heat) < 5.0) {
            manager.removeMessage(player, "item_heat")
            return
        }

        val color: String?
        val descriptor: String?
        if (heat > 200) {
            color = "§4"
            descriptor = "Burning"
        } else if (heat > 100) {
            color = "§c"
            descriptor = "Very Hot"
        } else if (heat > 50) {
            color = "§6"
            descriptor = "Hot"
        } else if (heat > 20) {
            color = "§e"
            descriptor = "Warm"
        } else if (heat < -50) {
            color = "§b"
            descriptor = "Freezing"
        } else if (heat < -20) {
            color = "§3"
            descriptor = "Cold"
        } else {
            color = "§7"
            descriptor = "Cool"
        }

        val message = String.format("§7Item: %s%s §7(%.0f°C)", color, descriptor, heat)
        manager.setMessage(player, "item_heat", message)
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        val player = event.getPlayer()
        val droppedItem = event.itemDrop
        val item = droppedItem.itemStack

        saveCachedHeatToItem(player, item)
        droppedItem.itemStack = item

        val heat: Double = getItemHeat(item)

        if (heat > 200.0) {
            startDroppedItemFireTracking(droppedItem)
        }

        if (heat >= 50) {
            val chestplate = player.inventory.chestplate
            val hasProtection = chestplate != null && chestplate.type == Material.LEATHER_CHESTPLATE

            if (!hasProtection) {
                player.fireTicks = 40
            }
        }

        startDroppedItemHeatTracking(droppedItem)
    }

    private fun startDroppedItemHeatTracking(droppedItem: Item) {
         SchedulerAPI.runTaskTimer(droppedItem, { task: ScheduledTask? ->
            if (droppedItem.isDead || !droppedItem.isValid) {
                task?.cancel()
                return@runTaskTimer
            }
            val itemStack = droppedItem.itemStack
            val currentHeat: Double = getItemHeat(itemStack)
            val loc = droppedItem.location

            val ambientTemp = getAmbientTemperature(loc)
            val heatDifference = ambientTemp - currentHeat

            if (isFilledMold(itemStack)) {
                val heatChange = heatDifference * 0.005
                val newHeat = currentHeat + heatChange
                val meta = itemStack.itemMeta
                var startTime: Long? = null
                if (meta != null) {
                    startTime =
                        meta.persistentDataContainer.get(COOLING_START_KEY, PersistentDataType.LONG)
                    if (startTime == null) {
                        startTime = System.currentTimeMillis()
                        meta.persistentDataContainer.set(COOLING_START_KEY, PersistentDataType.LONG, startTime)
                        itemStack.setItemMeta(meta)
                        droppedItem.itemStack = itemStack
                    }
                }

                setItemHeat(itemStack, newHeat)
                droppedItem.itemStack = itemStack

                // Visual effects
                if (currentHeat > 50) {
                    loc.world.spawnParticle(Particle.SMOKE, loc, 1, 0.1, 0.1, 0.1, 0.01)
                    val elapsedTicks = (System.currentTimeMillis() - (startTime ?: 0)) / 50
                    if (elapsedTicks % 100 == 0L) {
                        loc.world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.1f, 0.5f)
                    }
                }

                val elapsedMillis =
                    System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())

                if (elapsedMillis >= 30000) {
                    try {
                        val result: Pair<ItemStack?, ItemStack?> = emptyMold(itemStack)

                        if (result.first != null && result.first!!.type != Material.AIR) {
                            droppedItem.world.dropItem(loc, result.first!!)
                        }

                        if (result.second != null && result.second!!.type != Material.AIR) {
                            droppedItem.world.dropItem(loc, result.second!!)
                        }

                        loc.world.playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                        loc.world.spawnParticle(Particle.LAVA, loc, 5, 0.1, 0.1, 0.1, 0.0)

                        droppedItem.remove()
                        task?.cancel()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        task?.cancel()
                    }
                }
                return@runTaskTimer
            }

            // Standard Item Logic
            val heatChange = heatDifference * 0.1
            val newHeat = currentHeat + heatChange

            setItemHeat(itemStack, newHeat)
            droppedItem.itemStack = itemStack
            if (newHeat >= 200) {
                val fireChance = min(0.5, (newHeat - 200) / 600)

                if (Math.random() < fireChance) {
                    val below = loc.block.getRelative(BlockFace.DOWN)
                    if (below.type.isBurnable || below.type == Material.AIR) {
                        loc.block.type = Material.FIRE
                    }
                }
            }
        }, 20L, 20L)
    }

    private fun applyHeatEffect(player: Player, item: ItemStack?) {
        val heat: Double = getItemHeat(item)
        val hasProtection = ArmorProtectionAPI.hasLeatherChestplate(player)

        if (heat != 0.0) {
            val speedModifier = -abs(heat) * 0.001
            AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        } else {
            AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            )
        }

        applyHeatDamage(player, heat, hasProtection)
        applyColdDamage(player, heat, hasProtection)
    }

    private fun removeHeatEffect(player: Player) {
        AttributeModifierAPI.removeModifier(
            player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
        )
    }

    private fun saveCachedHeatToItem(player: Player, item: ItemStack?) {
        val playerId = player.uniqueId
        val slot = player.inventory.heldItemSlot

        val slotHeatMap: MutableMap<Int?, Double?>? = playerItemHeatCache[playerId]
        if (slotHeatMap != null) {
            val cachedHeat = slotHeatMap[slot]
            if (cachedHeat != null) {
                setItemHeat(item, cachedHeat)
            }
        }
    }

    fun saveHeatForSlot(player: Player, slot: Int, item: ItemStack?) {
        val playerId = player.uniqueId
        val slotHeatMap: MutableMap<Int?, Double?>? = playerItemHeatCache[playerId]
        if (slotHeatMap != null) {
            val cachedHeat = slotHeatMap[slot]
            if (cachedHeat != null) {
                setItemHeat(item, cachedHeat)
            }
        }
    }

    companion object {
        @Getter
        var instance: ItemHeatSystem? = null
        private val HEAT_MODIFIER_KEY = NamespacedKey("atom", "heat_modifier")
        private val COOLING_START_KEY = NamespacedKey("atom", "cooling_start_time")

        private val playerItemHeatCache: MutableMap<UUID?, MutableMap<Int?, Double?>?> =
            HashMap<UUID?, MutableMap<Int?, Double?>?>()
        private val lastKnownItems: MutableMap<UUID?, ItemStack?> = HashMap<UUID?, ItemStack?>()

        @JvmStatic
        fun getItemHeat(item: ItemStack?): Double {
            if (item == null || !item.hasItemMeta()) return 0.0

            return PersistentData.getDouble(item.itemMeta, "item_heat", 0.0)
        }

        @JvmStatic
        fun setItemHeat(item: ItemStack?, heat: Double) {
            if (item == null || item.type == Material.AIR) return

            val meta = item.itemMeta ?: return

            PersistentData.set(meta, "item_heat", heat)
            item.setItemMeta(meta)
        }

        private val trackedDisplays = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<UUID, Boolean>())

        @JvmStatic
        fun startItemDisplayHeatTracking(itemDisplay: ItemDisplay) {
            // Prevent duplicate schedulers
            if (!trackedDisplays.add(itemDisplay.uniqueId)) {
                return
            }
            
            SchedulerAPI.runTaskTimer(itemDisplay, { task: ScheduledTask? ->
                if (itemDisplay.isDead || !itemDisplay.isValid) {
                    trackedDisplays.remove(itemDisplay.uniqueId)
                    task?.cancel()
                    return@runTaskTimer
                }
                val itemStack = itemDisplay.itemStack
                if (itemStack.type == Material.AIR) {
                    task?.cancel()
                    return@runTaskTimer
                }

                val currentHeat: Double = getItemHeat(itemStack)
                val loc = itemDisplay.location

                val ambientTemp = getAmbientTemperature(loc)
                val heatDifference = ambientTemp - currentHeat

                // Delegate to ItemTransformationHandler for special items
                if (isFilledMold(itemStack)) {
                    ItemTransformationHandler.handleFilledMoldCooling(
                        itemDisplay,
                        itemStack,
                        loc,
                        currentHeat,
                        heatDifference
                    )
                    return@runTaskTimer
                }
                if (itemStack.type == Material.CLAY_BALL) {
                    ItemTransformationHandler.handleClayDrying(itemDisplay, itemStack, loc)
                    return@runTaskTimer
                }

                // Standard item heat changes
                val heatChange = heatDifference * 0.05
                var newHeat = currentHeat + heatChange

                newHeat = max(-100.0, min(500.0, newHeat))
                if (abs(newHeat - currentHeat) > 0.5) {
                    setItemHeat(itemStack, newHeat)
                    itemDisplay.setItemStack(itemStack)
                }
            }, 20L, 20L)
        }

        fun startDroppedItemFireTracking(item: Item) {
            SchedulerAPI.runTaskTimer(item,  { task: ScheduledTask? ->
                if (item.isDead || !item.isValid) {
                    task?.cancel()
                    return@runTaskTimer
                }
                val itemStack = item.itemStack
                if (itemStack.type == Material.AIR) {
                    task?.cancel()
                    return@runTaskTimer
                }

                val currentHeat: Double = getItemHeat(itemStack)

                if (currentHeat > 200.0) {
                    igniteNearbyBlocks(item.location, currentHeat)
                }

                val ambientTemp = getAmbientTemperature(item.location)
                val heatDifference = ambientTemp - currentHeat
                val heatChange = heatDifference * 0.02
                var newHeat = currentHeat + heatChange

                newHeat = max(-100.0, min(500.0, newHeat))
                if (abs(newHeat - currentHeat) > 0.5) {
                    setItemHeat(itemStack, newHeat)
                    item.itemStack = itemStack
                }
            }, 10L, 10L)
        }

        private fun igniteNearbyBlocks(location: Location, temperature: Double) {
            if (location.world == null) return

            val radius = min(3.0, (temperature - 200) / 100).toInt()

            for (x in -radius..radius) {
                for (y in -1..1) {
                    for (z in -radius..radius) {
                        val checkLoc = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                        val block = checkLoc.block

                        if (isFlammable(block) && block.getRelative(BlockFace.UP).type == Material.AIR) {
                            val igniteChance = min(0.3, (temperature - 200) / 1000)
                            if (Math.random() < igniteChance) {
                                block.getRelative(BlockFace.UP).type = Material.FIRE
                            }
                        }
                    }
                }
            }
        }

        private fun isFlammable(block: Block): Boolean {
            val type = block.type
            return type == Material.OAK_PLANKS || type == Material.SPRUCE_PLANKS || type == Material.BIRCH_PLANKS || type == Material.JUNGLE_PLANKS || type == Material.ACACIA_PLANKS || type == Material.DARK_OAK_PLANKS || type == Material.MANGROVE_PLANKS || type == Material.CHERRY_PLANKS || type == Material.BAMBOO_PLANKS || type == Material.CRIMSON_PLANKS || type == Material.WARPED_PLANKS || type == Material.OAK_LOG || type == Material.SPRUCE_LOG || type == Material.BIRCH_LOG || type == Material.JUNGLE_LOG || type == Material.ACACIA_LOG || type == Material.DARK_OAK_LOG || type == Material.MANGROVE_LOG || type == Material.CHERRY_LOG || type == Material.OAK_LEAVES || type == Material.SPRUCE_LEAVES || type == Material.BIRCH_LEAVES || type == Material.JUNGLE_LEAVES || type == Material.ACACIA_LEAVES || type == Material.DARK_OAK_LEAVES || type == Material.MANGROVE_LEAVES || type == Material.CHERRY_LEAVES || type == Material.AZALEA_LEAVES || type == Material.FLOWERING_AZALEA_LEAVES || type == Material.GRASS_BLOCK || type == Material.TALL_GRASS || type == Material.FERN || type == Material.LARGE_FERN || type == Material.DEAD_BUSH || type == Material.DANDELION || type == Material.POPPY || type == Material.BLUE_ORCHID ||
                    type.name.endsWith("_WOOL") || type.name.endsWith("_CARPET") || type == Material.HAY_BLOCK || type == Material.DRIED_KELP_BLOCK || type == Material.TNT || type == Material.BOOKSHELF
        }
    }
}