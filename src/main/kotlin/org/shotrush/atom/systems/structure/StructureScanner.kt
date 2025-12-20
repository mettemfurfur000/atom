package org.shotrush.atom.systems.structure

import com.github.shynixn.mccoroutine.folia.ticks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.World
import org.joml.Vector3i
import org.shotrush.atom.Atom
import org.shotrush.atom.core.api.AtomAPI
import kotlin.random.Random

object StructureScanner {
    suspend fun scanAt(
        world: World,
        start: Vector3i,
        maxVolume: Int = 256,
        retries: Int = 1,
    ): Structure? {
        repeat(retries + 1) { attempt ->
            val def = StructureDefinitions.getAllMatching(world, start).firstOrNull() ?: return null
            val scanner = StructureScanFace(world,start, def , maxVolume)

            Atom.instance.logger.info { "DEBUH: found ${def.name}, attempting scan" }

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
