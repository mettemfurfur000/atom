package org.shotrush.atom.commands

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.nestedArguments
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.shotrush.atom.item.Material
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

object MoldCommand {
    fun register() {
        commandAPICommand("filled-mold") {
            withPermission("atom.admin.filledmold")
            multiLiteralArgument("type", literals = arrayOf("wax", "fired"))
            multiLiteralArgument("shape", literals = MoldShape.entries.map { it.id }.toTypedArray())
            multiLiteralArgument("material", literals = Material.entries.map { it.id }.toTypedArray())
            playerExecutor { player, args ->
                val type = MoldType.byId(args["type"] as String)
                val shape = MoldShape.byId(args["shape"] as String)
                val material = Material.byId(args["material"] as String)
                val mold = Molds.getFilledMold(shape, type, material)
                player.inventory.addItem(mold)
            }
        }
    }
}