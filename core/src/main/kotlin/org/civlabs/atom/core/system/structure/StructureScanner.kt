package org.civlabs.atom.core.system.structure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.World
import org.civlabs.atom.core.CoreAtom
import org.joml.Vector3i

object StructureScanner {
    suspend fun scanAt(
        world: World,
        start: Vector3i,
        maxVolume: Int = 256,
//        retries: Int = 1,
    ): Structure? {
        val allDefinitions = StructureDefinitions.getAllMatching(world, start)
        if (allDefinitions.isEmpty()) {
            CoreAtom.instance.logger.info { "DEBUH: no defs found for said block" }
            return null
        }

        // sinc structures not only consist of air, multiple definitions could match the block where scan is beginning from
        // we have to check every single one of them to determine what we are even looking at

        // because of this its not really a good idea to define structures out of blocks that are used in construction.
        // might need to define a special little craftEngine block for every multiblock, but am not sure if i woud be able
        // to use the old block material lookup thing

        // TODO: figur out how to handle craftEngine blocks in a scanner

        allDefinitions.forEach { def ->
            CoreAtom.instance.logger.info { "DEBUH: found ${def.name}, attempting scan" }

            val scanner = StructureScanFace(world, start, def, maxVolume)

            if (withContext(Dispatchers.IO) { scanner.scan() }) {
                // tries again if failed
                val structure = scanner.toStructure() ?: return@forEach
                // tries to register immediately if structure is valid
                return StructureRegistry.registerNonOverlapping(structure)
            }
        }

        CoreAtom.instance.logger.info { "DEBUH: none worked..." }

        return null
    }
}
