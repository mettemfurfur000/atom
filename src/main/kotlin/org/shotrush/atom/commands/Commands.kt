package org.shotrush.atom.commands

object Commands {
    fun register() {
        LivingCommands.register()
        MoldCommand.register()
    }
}