package org.shotrush.atom.content

import org.bukkit.event.Listener
import org.shotrush.atom.Atom

object RecipeManagement : Listener {
    fun handle(atom: Atom) {
        val server = atom.server
        server.clearRecipes()
    }
}