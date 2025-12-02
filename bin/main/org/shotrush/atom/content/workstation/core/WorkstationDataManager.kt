package org.shotrush.atom.content.workstation.core

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import org.shotrush.atom.Atom
import java.io.File
import java.util.UUID
import net.momirealms.craftengine.core.world.BlockPos


object WorkstationDataManager {
    private val dataFile: File by lazy {
        File(Atom.instance.dataFolder, "workstations.yml").apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
    }
    
    private val config: YamlConfiguration by lazy {
        YamlConfiguration.loadConfiguration(dataFile)
    }
    
    
    private val workstationData = mutableMapOf<String, WorkstationData>()
    
    fun initialize() {
        loadData()
        
        Atom.instance?.let { plugin ->
            
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                saveData()
            }, 5, 5, java.util.concurrent.TimeUnit.MINUTES)
            
            
            Runtime.getRuntime().addShutdownHook(Thread {
                saveData()
                plugin.logger.info("Saved workstation data on shutdown")
            })
        }
    }
    
    
    fun getWorkstationData(pos: BlockPos, type: String): WorkstationData {
        val key = positionKey(pos)
        return workstationData.getOrPut(key) {
            WorkstationData(pos, type, mutableListOf(), null)
        }
    }
    
    
    fun removeWorkstationData(pos: BlockPos) {
        val key = positionKey(pos)
        val data = workstationData.remove(key)
        config.set(key, null)
        
        
        data?.placedItems?.forEach { placedItem ->
            placedItem.displayUUID?.let { uuid ->
                Bukkit.getEntity(uuid)?.remove()
            }
        }
        
        Atom.instance.logger.info("Removed workstation data for $key")
    }
    
    
    fun getPlacedItems(pos: BlockPos): MutableList<PlacedItem> {
        val key = positionKey(pos)
        return workstationData[key]?.placedItems ?: mutableListOf()
    }
    
    
    fun getAllWorkstations(): Map<String, WorkstationData> {
        return workstationData.toMap()
    }
    
    
    fun hasWorkstation(pos: BlockPos): Boolean {
        val key = positionKey(pos)
        return workstationData.containsKey(key)
    }
    
    
    fun updatePlacedItems(pos: BlockPos, items: List<PlacedItem>) {
        val key = positionKey(pos)
        val data = workstationData[key]
        if (data != null) {
            data.placedItems.clear()
            data.placedItems.addAll(items)
        } else {
            
            val type = key.substringAfter("_").substringBefore("_") 
            workstationData[key] = WorkstationData(pos, "unknown", items.toMutableList(), null)
        }
    }
    
    
    private fun loadData() {
        workstationData.clear()
        
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue
            
            val type = section.getString("type") ?: continue
            val worldName = section.getString("world") ?: "world"
            val x = section.getInt("x")
            val y = section.getInt("y")
            val z = section.getInt("z")
            
            val pos = BlockPos(x, y, z)
            val placedItems = mutableListOf<PlacedItem>()
            
            
            val itemsSection = section.getConfigurationSection("items")
            if (itemsSection != null) {
                for (itemKey in itemsSection.getKeys(false)) {
                    val itemSection = itemsSection.getConfigurationSection(itemKey) ?: continue
                    
                    val material = itemSection.getString("material") ?: continue
                    val amount = itemSection.getInt("amount", 1)
                    val customItemId = itemSection.getString("custom_item_id")
                    val posX = itemSection.getDouble("pos_x", 0.0).toFloat()
                    val posY = itemSection.getDouble("pos_y", 0.0).toFloat()
                    val posZ = itemSection.getDouble("pos_z", 0.0).toFloat()
                    val yaw = itemSection.getDouble("yaw", 0.0).toFloat()
                    val displayUuidString = itemSection.getString("display_uuid")
                    
                    
                    val item = if (customItemId != null) {
                        net.momirealms.craftengine.bukkit.api.CraftEngineItems.byId(
                            net.momirealms.craftengine.core.util.Key.of(customItemId)
                        )?.buildItemStack()?.apply {
                            this.amount = amount
                        }
                    } else {
                        null
                    } ?: run {
                        
                        val bukkitMaterial = org.bukkit.Material.getMaterial(material)
                        if (bukkitMaterial != null) {
                            ItemStack(bukkitMaterial, amount)
                        } else {
                            null
                        }
                    }
                    
                    if (item != null) {
                        val position = Vector3f(posX, posY, posZ)
                        val displayUuid = displayUuidString?.let { UUID.fromString(it) }
                        placedItems.add(PlacedItem(item, position, yaw, displayUuid))
                    }
                }
            }
            
            
            val curingStartTime = section.getLong("curing_start_time", 0L).takeIf { it > 0 }
            val fuelQueue = section.getString("fuel_queue", "") ?: ""
            
            workstationData[key] = WorkstationData(pos, type, placedItems, curingStartTime, fuelQueue)
        }
        
        Atom.instance.logger.info("Loaded ${workstationData.size} workstation data entries")
    }
    
    
    fun saveData() {
        
        for (key in config.getKeys(false)) {
            config.set(key, null)
        }
        
        
        for ((key, data) in workstationData) {
            val section = config.createSection(key)
            
            section.set("type", data.type)
            section.set("world", "world") 
            section.set("x", data.position.x())
            section.set("y", data.position.y())
            section.set("z", data.position.z())
            
            
            if (data.placedItems.isNotEmpty()) {
                val itemsSection = section.createSection("items")
                data.placedItems.forEachIndexed { index, placedItem ->
                    val itemSection = itemsSection.createSection("item_$index")
                    itemSection.set("material", placedItem.item.type.name)
                    itemSection.set("amount", placedItem.item.amount)
                    
                    
                    val customItemId = net.momirealms.craftengine.bukkit.api.CraftEngineItems.getCustomItemId(placedItem.item)
                    if (customItemId != null) {
                        itemSection.set("custom_item_id", customItemId.value())
                    }
                    
                    itemSection.set("pos_x", placedItem.position.x.toDouble())
                    itemSection.set("pos_y", placedItem.position.y.toDouble())
                    itemSection.set("pos_z", placedItem.position.z.toDouble())
                    itemSection.set("yaw", placedItem.yaw.toDouble())
                    
                    
                    placedItem.displayUUID?.let {
                        itemSection.set("display_uuid", it.toString())
                    }
                }
            }
            
            
            data.curingStartTime?.let {
                section.set("curing_start_time", it)
            }
            
            if (data.fuelQueue.isNotEmpty()) {
                section.set("fuel_queue", data.fuelQueue)
            }
        }
        
        
        try {
            config.save(dataFile)
        } catch (e: Exception) {
            Atom.instance.logger.severe("Failed to save workstation data: ${e.message}")
        }
    }
    
    
    private fun positionKey(pos: BlockPos): String {
        return "world_${pos.x()}_${pos.y()}_${pos.z()}"
    }
    
    
    fun cleanupOrphanedData() {
        val world = Bukkit.getWorld("world") ?: return
        val toRemove = mutableListOf<BlockPos>()
        
        workstationData.forEach { (key, data) ->
            val block = world.getBlockAt(data.position.x(), data.position.y(), data.position.z())
            
            
            val state = net.momirealms.craftengine.bukkit.api.CraftEngineBlocks.getCustomBlockState(block)
            if (state == null || !state.owner().matchesKey(net.momirealms.craftengine.core.util.Key.of("atom:${data.type}"))) {
                toRemove.add(data.position)
                Atom.instance.logger.info("Found orphaned workstation data at ${data.position}, cleaning up")
            }
        }
        
        toRemove.forEach { pos ->
            removeWorkstationData(pos)
        }
        
        if (toRemove.isNotEmpty()) {
            saveData()
            Atom.instance.logger.info("Cleaned up ${toRemove.size} orphaned workstation entries")
        }
    }
    
    
    data class WorkstationData(
        val position: BlockPos,
        val type: String,
        val placedItems: MutableList<PlacedItem>,
        var curingStartTime: Long? = null,
        var fuelQueue: String = ""
    )
}
