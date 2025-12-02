package org.shotrush.atom.commands

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.shotrush.atom.listener.PlayerDataTrackingListener
import plutoproject.adventurekt.audience.send
import plutoproject.adventurekt.text.style.textGray
import plutoproject.adventurekt.text.style.textRed
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with
import kotlin.math.floor

object LivingCommands {
    fun register() {
        commandTree("alive") {
            playerExecutor { player, arguments ->
                val aliveTime = PlayerDataTrackingListener.getPlayerAliveTime(player)
                val ticksPerDay = 24000L
                val days = aliveTime.toDouble() / ticksPerDay.toDouble()

                val weeks = floor(days / 7.0).toInt()
                val remainingDays = days % 7
                val remainingHours = (remainingDays % 1)*24
                val daysSolid = floor(remainingDays).toInt()
                val hoursSolid = floor(remainingHours / 1.0).toInt()

                player.send {
                    text("You have been alive for ") with textGray
                    if (weeks > 0) {
                        text(weeks) with textRed
                        text(" week") with textGray
                        if (weeks > 1) {
                            text("s") with textGray
                        }
                    }
                    if (daysSolid > 0) {
                        if (weeks > 0) {
                            text(", ") with textGray
                        }
                        text("$daysSolid day${if (daysSolid > 1) "s" else ""}") with textGray
                    }
                    if (hoursSolid > 0) {
                        if (daysSolid > 0 || weeks > 0) {
                            text(", ") with textGray
                        }
                        text("$hoursSolid hour${if (hoursSolid > 1) "s" else ""}") with textGray
                    }
                }
            }
        }
    }
}