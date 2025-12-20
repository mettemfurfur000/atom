package org.civlabs.atom.core.system.structure

import com.comphenix.protocol.scheduler.FoliaScheduler
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import io.papermc.paper.threadedregions.scheduler.FoliaGlobalRegionScheduler
import io.papermc.paper.threadedregions.scheduler.FoliaRegionScheduler
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler
import org.bukkit.Location
import org.bukkit.World
import org.civlabs.atom.core.CoreAtom
import org.joml.Vector3i

/**
 * Defines what blocks are allowed in a structure and their roles.
 */
data class StructureDefinition(
    val name: String,
    var blockLimit: Int,
    // Material blocks that form the main body of the structure
    val bodyMaterials: Set<String>,
    // Controller blocks and their constraints
    val controllerRules: List<ControllerRule> = emptyList(),
) {

    fun isMaterialAllowed(material: String): Boolean {
        return material in bodyMaterials
    }

    fun isBodyBlock(world: World, x: Int, y: Int, z: Int): Boolean {
        val block = world.getBlockAt(x, y, z)
        return block.type.key.toString() in bodyMaterials
    }

    fun isControllerBlock(world: World, x: Int, y: Int, z: Int): Boolean {
        val block = world.getBlockAt(x, y, z)
        val blockKey = block.type.key.toString()
        return controllerRules.any { it.matches(blockKey, world, x, y, z, this) }
    }

    fun isPartOfStructure(world: World, x: Int, y: Int, z: Int): Boolean =
        isBodyBlock(world, x, y, z) || isControllerBlock(world, x, y, z)
}

/**
 * Constraint for controller blocks.
 */
data class ControllerRule(
    val blockKey: String,
    // If set, minimum neighbor count (including diagonal/vertical) the block must have
    val minNeighbors: Int = 0,
    val maxNeighbors: Int = Int.MAX_VALUE,
) {
    fun matches(key: String, world: World, x: Int, y: Int, z: Int, def: StructureDefinition): Boolean {
        if (key != blockKey) return false
        if (minNeighbors <= 0) return true

        // Count solid neighbors (including diagonals)
        var neighbors = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    val nz = z + dz
//                    if (!world.getBlockAt(nx, ny, nz).isEmpty) {
                    if (def.isBodyBlock(world, nx, ny, nz)) {
                        neighbors++
                    }
                }
            }
        }
        return neighbors in minNeighbors..maxNeighbors
    }
}

/**
 * Registry for structure definitions.
 * Will be expanded with file-based deserialization later.
 */
object StructureDefinitions {
    private val definitions = mutableMapOf<String, StructureDefinition>()

    fun register(def: StructureDefinition) {
        definitions[def.name] = def
        CoreAtom.instance.logger.info { "DEBUH: registered ${def.name}" }
    }

    fun get(name: String): StructureDefinition? = definitions[name]

    fun getAll(): List<StructureDefinition> = definitions.values.toList()
    fun getAllNames(): List<String> = definitions.keys.toList()

    fun getAllMatching(world: World, position: Vector3i): List<StructureDefinition> {
        val material = world.getBlockAt(position.x, position.y, position.z).type.key.toString()
        CoreAtom.instance.logger.info { "DEBUH: test material $material" }
        return definitions.values.filter { d -> d.isMaterialAllowed(material) }
    }

    fun registerDefaults() {
        // Example: Large Furnace made of stone bricks with furnace controllers at its sides, but not on the corners
        register(
            StructureDefinition(
                name = "large_furnace",
                bodyMaterials = setOf("minecraft:stone_bricks", "minecraft:furnace"),
                blockLimit = 6 * 6 * 6,
                controllerRules = listOf(
                    ControllerRule("minecraft:furnace", minNeighbors = 4, maxNeighbors = 5)
                )
            )
        )
    }
}
