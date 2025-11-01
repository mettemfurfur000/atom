package org.shotrush.atom.world

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.shotrush.atom.Atom
import org.shotrush.atom.core.blocks.CustomBlockRegistry
import java.util.*

object RockChunkGenerator : ChunkGenerator() {
    override fun getDefaultPopulators(world: World): List<BlockPopulator> = listOf(RockBlockPopulator)

    override fun shouldGenerateNoise(): Boolean = true
    override fun shouldGenerateSurface(): Boolean = true
    override fun shouldGenerateCaves(): Boolean = true
    override fun shouldGenerateMobs(): Boolean = true
    override fun shouldGenerateDecorations(): Boolean = true
    override fun shouldGenerateStructures(): Boolean = true
}

object RockBlockPopulator : BlockPopulator() {
    private val ALLOWED_BELOW = setOf(
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.STONE,
        Material.SNOW_BLOCK
    )
    private val LEAVES = setOf(
        Material.OAK_LEAVES,
        Material.SPRUCE_LEAVES,
        Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES,
        Material.ACACIA_LEAVES,
        Material.DARK_OAK_LEAVES,
        Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES,
    )
    val rock by lazy { Atom.instance.blockManager.registry.getBlockType("pebble") }
    val stick by lazy { Atom.instance.blockManager.registry.getBlockType("ground_stick") }

    private val CAN_REPLACE = setOf(
        Material.LEAF_LITTER,
        Material.SNOW
    )
    private val BLOCK_UNDER_LEAVES = Material.DARK_OAK_PRESSURE_PLATE
    private val BLOCK_ADJ_TO_LEAVES = Material.OAK_PRESSURE_PLATE
    private val BLOCK_OPEN = Material.PALE_OAK_PRESSURE_PLATE
    private val BLOCK_ADJ_TO_STONE = Material.LIGHT_WEIGHTED_PRESSURE_PLATE
    private val OFFSETS: Array<IntArray> = arrayOf(
        intArrayOf(0, -1),
        intArrayOf(-1, 0), intArrayOf(1, 0),
        intArrayOf(0, 1)
    )

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion,
    ) {
        val startX = chunkX shl 4
        val startZ = chunkZ shl 4

        for (x in startX..startX + 15) {
            for (z in startZ..startZ + 15) {
                val y = limitedRegion.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
                val below = limitedRegion.getType(x, y - 1, z)
                if (below !in ALLOWED_BELOW) continue

                val current = limitedRegion.getType(x, y, z)
                if (!current.isAir && current !in CAN_REPLACE) continue

                val underLeaves = isUnderLeaves(limitedRegion, x, y, z)
                val adjToLeaves = !underLeaves && isAdjacentToLeaves(limitedRegion, x, y, z)
                val adjToStone = isAdjacentToStone(limitedRegion, x, y, z)

                val mat = when {
                    underLeaves -> stick
                    adjToStone -> rock
                    adjToLeaves -> stick
                    else -> rock
                }
                val location = Location(limitedRegion.world, x.toDouble(), y.toDouble(), z.toDouble())
                val spawn = location.clone().add(0.5, 0.0, 0.5)

//                limitedRegion.setBlockData(x, y, z, mat.createBlockData())
                val block = mat.createBlock(spawn, location, BlockFace.NORTH)
                block.spawn(Atom.instance, limitedRegion)
                Atom.instance.blockManager.blocks.add(block)
            }
        }
    }

    private fun isUnderLeaves(region: LimitedRegion, x: Int, y: Int, z: Int): Boolean {
        val mb = region.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING)
        for (yy in y + 1..mb) {
            if (region.getType(x, yy, z) in LEAVES) return true
        }
        return false
    }

    private fun isAdjacentToLeaves(region: LimitedRegion, x: Int, y: Int, z: Int): Boolean {
        return OFFSETS.any { (dx, dz) -> isUnderLeaves(region, x + dx, y - 1, z + dz) }
    }

    private fun isAdjacentToStone(region: LimitedRegion, x: Int, y: Int, z: Int): Boolean {
        return OFFSETS.any { (dx, dz) -> region.getType(x + dx, y, z + dz) == Material.STONE }
    }
}