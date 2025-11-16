package org.shotrush.atom.content.workstation.knapping

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.content.workstation.core.InteractiveSurface
import org.shotrush.atom.content.workstation.core.PlacedItem
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.core.api.item.ItemQualityAPI
import org.shotrush.atom.core.api.player.PlayerDataAPI
import org.shotrush.atom.core.items.ItemQuality
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.matches
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.max
import kotlin.random.Random


class KnappingStationBehavior(
    block: CustomBlock
) : InteractiveSurface(block) {
    
    companion object {
        private val activeKnapping = mutableMapOf<Player, KnappingJob>()
        
        data class KnappingJob(
            val job: Job,
            val isPressureFlaking: Boolean
        )
        
        object Factory : BlockBehaviorFactory {
            override fun create(
                block: CustomBlock,
                arguments: Map<String?, Any?>
            ): BlockBehavior = KnappingStationBehavior(block)
        }
    }
    
    override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T> =
        @Suppress("UNCHECKED_CAST")
        Workstations.KNAPPING_STATION_ENTITY_TYPE as BlockEntityType<T>
    
    override fun createBlockEntity(
        pos: BlockPos,
        state: ImmutableBlockState): BlockEntity {
        
        this.blockPos = pos
        return KnappingStationEntity(pos, state)
    }
    
    override fun getMaxItems(): Int = 1
    
    override fun canPlaceItem(item: ItemStack): Boolean {
        return item.type == Material.FLINT || item.matches("atom:sharpened_rock")
    }
    
    override fun calculatePlacement(player: Player, itemCount: Int): Vector3f {
        
        return Vector3f(-0.2f, 1f, 0.2f)
    }
    
    override fun getFullMessage(): String = "§cKnapping station is full!"
    
    override fun getEmptyMessage(): String = "§cPlace flint first!"
    
    override fun getItemDisplayRotation(item: PlacedItem): AxisAngle4f {
        
        val randomYaw = (Math.random() * Math.PI * 2).toFloat()
        val yawRotation = AxisAngle4f(randomYaw, 0f, 1f, 0f)
        val flatRotation = AxisAngle4f(Math.toRadians(90.0).toFloat(), 1f, 0f, 0f)
        
        
        val q1 = Quaternionf().rotateAxis(yawRotation.angle, yawRotation.x, yawRotation.y, yawRotation.z)
        val q2 = Quaternionf().rotateAxis(flatRotation.angle, flatRotation.x, flatRotation.y, flatRotation.z)
        val combined = q1.mul(q2)
        
        
        val result = AxisAngle4f()
        combined.get(result)
        return result
    }
    
    override fun getItemDisplayScale(item: PlacedItem): Vector3f {
        return Vector3f(1.0f, 1.0f, 1.0f)
    }
    
    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val item = context.item.item as? ItemStack ?: return InteractionResult.PASS
        
        
        val targetBlock = player.getTargetBlockExact(5)
        if (targetBlock == null) {
            Atom.instance.logger.warning("No target block found")
            return InteractionResult.PASS
        }
        
        val pos = BlockPos(targetBlock.x, targetBlock.y, targetBlock.z)
        this.blockPos = pos
        
        
        val workstationData = WorkstationDataManager.getWorkstationData(pos, "knapping_station")
        
        
        placedItems.clear()
        placedItems.addAll(workstationData.placedItems)
        
        
        
        val location = player.getTargetBlockExact(5)?.location?.add(0.5, 0.5, 0.5)
        location?.let { loc ->
            loc.world?.getNearbyEntities(loc, 1.5, 1.5, 1.5)?.forEach { entity ->
                if (entity is org.bukkit.entity.ItemDisplay) {
                    
                    val belongsToUs = placedItems.any { it.displayUUID == entity.uniqueId }
                    if (!belongsToUs) {
                        
                        entity.remove()
                    }
                }
            }
        }
        
        
        placedItems.forEach { placedItem ->
            if (placedItem.displayUUID != null) {
                
                val entity = Bukkit.getEntity(placedItem.displayUUID!!)
                if (entity == null) {
                    
                    placedItem.displayUUID = null
                    spawnItemDisplay(placedItem)
                }
            } else {
                
                spawnItemDisplay(placedItem)
            }
        }
        
        Atom.instance.logger.info("KnappingStation interaction: player=${player.name}, item=${item.type}, pos=$pos, placedItems=${placedItems.size}")
        
        
        if (item.type == Material.CLAY_BALL || item.type == Material.HONEYCOMB || item.matches("atom:pebble")) {
            
            val knappingBehavior = KnappingBlockBehavior(block())
            return knappingBehavior.useOnBlock(context, state)
        }
        
        
        if (isKnappingTool(item)) {
            if (placedItems.isEmpty()) {
                ActionBarManager.send(player, getEmptyMessage())
                return InteractionResult.SUCCESS
            }
            
            
            val isPressureFlaking = item.matches("atom:pressure_flaker")
            val inputFlint = placedItems.firstOrNull()?.item
            
            
            if (!isProcessing(player)) {
                if (isPressureFlaking && inputFlint != null) {
                    startPressureFlaking(player, inputFlint)
                } else {
                    startKnapping(player)
                }
            }
            return InteractionResult.SUCCESS
        }
        
        
        Atom.instance.logger.info("Attempting to place item: canPlace=${canPlaceItem(item)}, currentItems=${placedItems.size}")
        
        
        val result = super.useOnBlock(context, state)
        
        
        if (result == InteractionResult.SUCCESS && blockPos != null) {
            WorkstationDataManager.updatePlacedItems(blockPos!!, placedItems)
        }
        
        return result
    }
    
    private fun isKnappingTool(item: ItemStack): Boolean {
        return item.matches("atom:pebble") || item.matches("atom:pressure_flaker")
    }
    
    private fun isProcessing(player: Player): Boolean {
        return activeKnapping.containsKey(player)
    }
    
    private fun startKnapping(player: Player) {
        
        activeKnapping[player]?.job?.cancel()
        
        val strokeCount = 15 + Random.nextInt(11)
        
        
        val job = GlobalScope.launch {
            var currentStroke = 0
            
            ActionBarManager.sendStatus(player, "§7Knapping... Strike the flint")
            
            while (currentStroke < strokeCount && isActive) {
                delay(250) 
                
                
                if (!player.isHandRaised || !isKnappingTool(player.activeItem)) {
                    ActionBarManager.sendStatus(player, "§cKnapping cancelled - tool lowered")
                    delay(1000) 
                    break
                }
                
                if (!player.isOnline || player.location.distance(getBlockLocation()) > 5.0) {
                    break
                }
                
                
                player.scheduler.run(Atom.instance!!, { _ ->
                    playKnappingEffects(player)
                }, null)
                
                currentStroke++
                
                
                val progress = (currentStroke.toFloat() / strokeCount * 100).toInt()
                ActionBarManager.sendStatus(player, "§7Knapping... §e$progress%")
            }
            
            if (currentStroke >= strokeCount) {
                
                player.scheduler.run(Atom.instance!!, { _ ->
                    finishKnapping(player)
                }, null)
            }
            
            activeKnapping.remove(player)
            ActionBarManager.clearStatus(player)
        }
        
        activeKnapping[player] = KnappingJob(job, false)
    }
    
    private fun startPressureFlaking(player: Player, inputFlint: ItemStack) {
        
        activeKnapping[player]?.job?.cancel()
        
        val strokeCount = 25 + Random.nextInt(11)
        
        
        val job = GlobalScope.launch {
            var currentStroke = 0
            
            ActionBarManager.sendStatus(player, "§7Pressure flaking... Carefully work the flint")
            
            while (currentStroke < strokeCount && isActive) {
                delay(300) 
                
                
                if (!player.isHandRaised || player.activeItem.type == Material.AIR || !player.activeItem.matches("atom:pressure_flaker")) {
                    ActionBarManager.sendStatus(player, "§cPressure flaking cancelled - tool lowered")
                    delay(1000) 
                    break
                }
                
                if (!player.isOnline || player.location.distance(getBlockLocation()) > 5.0) {
                    break
                }
                
                
                player.scheduler.run(Atom.instance!!, { _ ->
                    playPressureFlakingEffects(player)
                }, null)
                
                currentStroke++
                
                
                val progress = (currentStroke.toFloat() / strokeCount * 100).toInt()
                ActionBarManager.sendStatus(player, "§7Pressure flaking... §e$progress%")
            }
            
            if (currentStroke >= strokeCount) {
                
                player.scheduler.run(Atom.instance!!, { _ ->
                    finishPressureFlaking(player, inputFlint)
                }, null)
            }
            
            activeKnapping.remove(player)
            ActionBarManager.clearStatus(player)
        }
        
        activeKnapping[player] = KnappingJob(job, true)
    }
    
    private fun playKnappingEffects(player: Player) {
        val location = getBlockLocation().add(0.5, 1.0, 0.5)
        
        
        location.world?.playSound(location, Sound.BLOCK_STONE_HIT, 1.0f, 1.0f)
        
        
        val dustOptions = Particle.DustOptions(Color.fromRGB(128, 128, 128), 1.0f)
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
    
    private fun playPressureFlakingEffects(player: Player) {
        val location = getBlockLocation().add(0.5, 1.0, 0.5)
        
        
        location.world?.playSound(location, Sound.BLOCK_STONE_HIT, 0.7f, 1.2f)
        
        
        val dustOptions = Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.8f)
        location.world?.spawnParticle(
            Particle.DUST,
            location,
            5,
            0.1,
            0.1,
            0.1,
            0.0,
            dustOptions
        )
    }
    
    private fun finishKnapping(player: Player) {
        val location = getBlockLocation().add(0.5, 0.5, 0.5)
        
        
        val knapCount = PlayerDataAPI.getInt(player, "knapping.count", 0)
        val failChance = max(0.1, 0.5 - (knapCount * 0.02))
        
        if (Math.random() < failChance) {
            
            ActionBarManager.send(player, "§cThe flint broke!")
            player.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f)
            removeLastItem()
            
            blockPos?.let { 
                WorkstationDataManager.updatePlacedItems(it, placedItems)
                WorkstationDataManager.saveData() 
            }
        } else {
            
            ActionBarManager.send(player, "§aSuccessfully sharpened the flint!")
            player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f)
            
            
            removeLastItem()
            
            blockPos?.let { 
                WorkstationDataManager.updatePlacedItems(it, placedItems)
                WorkstationDataManager.saveData() 
            }
            
            
            CraftEngineItems.byId(Key.of("atom:sharpened_rock"))?.let { customItem ->
                val sharpenedFlint = customItem.buildItemStack()
                
                
                val quality = if (knapCount > 20) ItemQuality.MEDIUM else ItemQuality.LOW
                ItemQualityAPI.setQuality(sharpenedFlint, quality)
                
                location.world?.dropItemNaturally(location, sharpenedFlint)
            }
        }
        
        
        PlayerDataAPI.incrementInt(player, "knapping.count", 0)
    }
    
    private fun finishPressureFlaking(player: Player, inputFlint: ItemStack) {
        val location = getBlockLocation().add(0.5, 0.5, 0.5)
        
        
        val knapCount = PlayerDataAPI.getInt(player, "pressure_flaking.count", 0)
        var failChance = max(0.2, 0.7 - (knapCount * 0.03))
        
        
        val itemHeat = org.shotrush.atom.content.systems.ItemHeatSystem.getItemHeat(inputFlint)
        val hasHeatBonus = itemHeat >= 50 && itemHeat <= 150
        if (hasHeatBonus) {
            failChance *= 0.8
        }
        
        if (Math.random() < failChance) {
            
            ActionBarManager.send(player, "§cThe flint shattered during pressure flaking!")
            player.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f)
            removeLastItem()
            
            blockPos?.let { 
                WorkstationDataManager.updatePlacedItems(it, placedItems)
                WorkstationDataManager.saveData() 
            }
        } else {
            
            ActionBarManager.send(player, "§aSuccessfully created high quality sharpened flint!")
            player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.8f)
            
            
            removeLastItem()
            
            blockPos?.let { 
                WorkstationDataManager.updatePlacedItems(it, placedItems)
                WorkstationDataManager.saveData() 
            }
            
            
            CraftEngineItems.byId(Key.of("atom:sharpened_rock"))?.let { customItem ->
                val sharpenedFlint = customItem.buildItemStack()
                ItemQualityAPI.setQuality(sharpenedFlint, ItemQuality.HIGH)
                
                if (hasHeatBonus) {
                    ActionBarManager.send(player, "§6The heat treatment improved the quality!", 4)
                }
                
                location.world?.dropItemNaturally(location, sharpenedFlint)
            }
        }
        
        
        PlayerDataAPI.incrementInt(player, "pressure_flaking.count", 0)
    }
    
    private fun getBlockLocation(): Location {
        
        return blockPos?.let { pos ->
            Location(
                Bukkit.getWorld("world"), 
                pos.x().toDouble(),
                pos.y().toDouble(),
                pos.z().toDouble()
            )
        } ?: Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)
    }
    
    override fun onRemoved() {
        
        activeKnapping.values.forEach { it.job.cancel() }
        activeKnapping.clear()
        
        
        super.onRemoved()
    }
}


class KnappingStationEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState
) : BlockEntity(Workstations.KNAPPING_STATION_ENTITY_TYPE, pos, blockState) {
    
    val placedItems = mutableListOf<PlacedItem>()
    
    init {
        Atom.instance.logger.info("KnappingStationEntity initialized at $pos")
    }
    
    override fun loadCustomData(tag: CompoundTag) {
        super.loadCustomData(tag)
        
        placedItems.clear()
        val itemCount = tag.getInt("placedItemsCount")
        
        for (i in 0 until itemCount) {
            val prefix = "item_$i"
            
            
            val typeName = tag.getString("${prefix}_type") ?: continue
            val material = Material.getMaterial(typeName) ?: continue
            val amount = tag.getInt("${prefix}_amount")
            
            val item = ItemStack(material, amount)
            
            
            val position = Vector3f(
                tag.getFloat("${prefix}_x"),
                tag.getFloat("${prefix}_y"),
                tag.getFloat("${prefix}_z")
            )
            val yaw = tag.getFloat("${prefix}_yaw")
            
            placedItems.add(PlacedItem(item, position, yaw))
        }
    }
    
    override fun saveCustomData(tag: CompoundTag) {
        super.saveCustomData(tag)
        
        tag.putInt("placedItemsCount", placedItems.size)
        
        
        placedItems.forEachIndexed { index, placedItem ->
            val prefix = "item_$index"
            
            
            tag.putString("${prefix}_type", placedItem.item.type.name)
            tag.putInt("${prefix}_amount", placedItem.item.amount)
            
            
            tag.putFloat("${prefix}_x", placedItem.position.x)
            tag.putFloat("${prefix}_y", placedItem.position.y)
            tag.putFloat("${prefix}_z", placedItem.position.z)
            tag.putFloat("${prefix}_yaw", placedItem.yaw)
        }
    }
}
