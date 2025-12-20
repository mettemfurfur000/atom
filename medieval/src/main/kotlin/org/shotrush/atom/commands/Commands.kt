package org.shotrush.atom.commands

import co.aikar.commands.PaperCommandManager
import org.shotrush.atom.commands.debug.TemperatureDebugCommand
import java.sql.Struct

object Commands {
    fun register(manager: PaperCommandManager) {
        manager.registerCommand(TemperatureDebugCommand())
        LivingCommands.register()
        MoldCommand.register()
        RoomCommands.register()
        StructCommands.register()
    }
}