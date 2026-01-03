package org.shotrush.atom.commands

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.civlabs.atom.core.system.structure.StructureRegistry
import org.civlabs.atom.core.system.structure.StructureScanner
import org.civlabs.atom.core.util.sendMiniMessage
import org.joml.Vector3i
import org.shotrush.atom.Atom


object StructCommands {
    fun register() {
        commandTree("struct") {
            withPermission("atom.command.struct")
            literalArgument("scan", true) {
                playerExecutor { player, args ->
                    val target = player.getTargetBlockExact(6)?.location;
                    if (target == null) {
                        player.sendMiniMessage("Not pointing at a block");
                        return@playerExecutor
                    }

                    Atom.instance.launch(Atom.instance.globalRegionDispatcher) {
                        player.sendMiniMessage("<green>Scanning for the structure...</green>")
                        val structure = StructureScanner.scanAt(
                            player.world,
                            Vector3i(target.blockX, target.blockY, target.blockZ)
                        )
                        player.sendMiniMessage(
                            "<green>Scanned room ${structure?.id ?: "<red>failed</red>"}</green>"
                        )
                    }
                }
            }

            literalArgument("check", true) {
                playerExecutor { player, args ->
                    val target = player.getTargetBlockExact(6)?.location;
                    if (target == null) {
                        player.sendMiniMessage("Not pointing at a block");
                        return@playerExecutor
                    }
                    val struct = StructureRegistry.structureAt(target);

                    if (struct == null) {
                        player.sendMiniMessage("No structure")
                        return@playerExecutor
                    }

                    player.sendMiniMessage("Found structure ${struct.defName}:${struct.id}")
                }
            }
        }

    }

}