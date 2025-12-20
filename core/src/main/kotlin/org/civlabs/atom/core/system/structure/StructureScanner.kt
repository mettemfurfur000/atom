package org.civlabs.atom.core.system.structure

import com.github.shynixn.mccoroutine.folia.ticks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.World
import org.civlabs.atom.core.CoreAtom
import org.joml.Vector3i

object StructureScanner {
    suspend fun scanAt(
        world: World,
        start: Vector3i,
        maxVolume: Int = 256,
        retries: Int = 1,
    ): Structure? {
        repeat(retries + 1) { attempt ->
            val def = StructureDefinitions.getAllMatching(world, start).firstOrNull()
            if (def == null)
            {
                CoreAtom.instance.logger.info { "DEBUH: no defs found for said block" }
                return@repeat
            }
            val scanner = StructureScanFace(world,start, def , maxVolume)

            CoreAtom.instance.logger.info { "DEBUH: found ${def.name}, attempting scan" }

            val ok = withContext(Dispatchers.IO) {
                scanner.scan()
            }
            if (ok) {
                val structure = scanner.toStructure() ?: return null
                return if (StructureRegistry.tryRegisterDedup(structure)) structure else null
            } else if (attempt < retries) {
                delay((4..12).random().ticks)
            }
        }
        return null
    }
}
