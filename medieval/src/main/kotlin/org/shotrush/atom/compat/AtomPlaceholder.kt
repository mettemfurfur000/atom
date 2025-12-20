package org.shotrush.atom.compat

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.shotrush.atom.content.systems.ThirstSystem

object AtomPlaceholder : PlaceholderExpansion() {
    override fun getIdentifier(): String = "atom"

    override fun getAuthor(): String = "Atom Team"

    override fun getVersion(): String = "1.0.0"

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        val split = params.split("_")
        val (category) = split
        if(category == "thirst") {
            return player?.let { player ->
                return ThirstSystem.instance.getThirst(player).toString()
            }
        }
        return super.onPlaceholderRequest(player, params)
    }
}