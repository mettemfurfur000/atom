package org.shotrush.atom.blocks.display

import net.momirealms.craftengine.core.entity.player.Player

interface DisplayElement {
    val id: String
    fun show(player: Player)
    fun update(player: Player)
    fun hide(player: Player)
    fun startTicking(player: Player, tm: TickManager)
    fun stopTicking(player: Player, tm: TickManager)
}