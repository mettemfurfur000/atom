package org.shotrush.atom.content.carcass

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.block.entity.BlockEntityTypes
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.shotrush.atom.Atom
import org.shotrush.atom.content.AnimalType
import org.shotrush.atom.content.workstation.WorkstationDef

object CarcassBlock {
    val CARCASS_KEY = Key.of("atom:carcass")
    
    lateinit var CARCASS_DEF: WorkstationDef
        private set

    fun init() {
        BlockBehaviors.register(CARCASS_KEY, CarcassBlockBehavior.Factory)
        val entityType: BlockEntityType<BlockEntity> = BlockEntityTypes.register(CARCASS_KEY)
        CARCASS_DEF = WorkstationDef(CARCASS_KEY, entityType)
        
        Atom.instance.logger.info("Carcass block system initialized")
    }

    fun spawnCarcassFor(entity: LivingEntity, animalType: AnimalType): Boolean {
        val world = entity.world
        val loc = entity.location
        
        val targetLoc = findSuitableLocation(loc) ?: return false
        
        val success = CraftEngineBlocks.place(targetLoc, CARCASS_KEY, false)
        if (!success) {
            Atom.instance.logger.warning("Failed to place carcass block at $targetLoc")
            return false
        }
        
        val pos = BlockPos(targetLoc.blockX, targetLoc.blockY, targetLoc.blockZ)
        val ceWorld = BukkitWorldManager.instance().getWorld(world) ?: return false
        val blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos)
        
        if (blockEntity is CarcassBlockEntity) {
            blockEntity.initialize(animalType, world.fullTime)
            return true
        } else {
            Atom.instance.logger.warning("Block entity at $pos is not CarcassBlockEntity: $blockEntity")
            return false
        }
    }

    private fun findSuitableLocation(loc: Location): Location? {
        val blockLoc = loc.block.location
        
        data class Offset(val dx: Int, val dy: Int, val dz: Int) {
            fun distanceSq() = dx * dx + dy * dy + dz * dz
        }
        
        val offsets = mutableListOf<Offset>()
        for (dy in 0..2) {
            for (dx in -2..2) {
                for (dz in -2..2) {
                    offsets.add(Offset(dx, dy, dz))
                }
            }
        }
        offsets.sortBy { it.distanceSq() }
        
        for (offset in offsets) {
            val check = blockLoc.clone().add(offset.dx.toDouble(), offset.dy.toDouble(), offset.dz.toDouble())
            val block = check.block
            val below = check.clone().add(0.0, -1.0, 0.0).block
            
            if (block.type.isAir && !below.type.isAir && below.isSolid) {
                return check
            }
        }
        
        for (offset in offsets) {
            val check = blockLoc.clone().add(offset.dx.toDouble(), offset.dy.toDouble(), offset.dz.toDouble())
            if (check.block.type.isAir) {
                return check
            }
        }
        
        return null
    }
}
