package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText
import net.kyori.adventure.title.Title
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.permissions.Permissible
import org.bukkit.persistence.PersistentDataType
import org.shotrush.atom.Atom
import plutoproject.adventurekt.component
import plutoproject.adventurekt.text.style.textGold
import plutoproject.adventurekt.text.style.textGray
import plutoproject.adventurekt.text.style.textRed
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with
import kotlin.time.Duration.Companion.milliseconds

object PlayerChatListener : Listener {
    fun register(atom: Atom) {
        val eventDispatcher = mapOf(eventDef<AsyncChatEvent> {
            atom.asyncDispatcher
        }, eventDef<AsyncChatDecorateEvent> {
            atom.asyncDispatcher
        }, eventDef<PlayerJoinEvent> {
            atom.entityDispatcher(it.player)
        }, eventDef<PlayerQuitEvent> {
            atom.entityDispatcher(it.player)
        })
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        event.player.addCustomChatCompletions(Atom.instance.server.onlinePlayers.map { "@" + plainText().serialize(event.player.displayName()) })
    }

    @EventHandler
    fun on(event: PlayerQuitEvent) {
        val profile = plainText().serialize(event.player.displayName())

        for (online in Atom.instance.server.onlinePlayers) {
            online.removeCustomChatCompletions(listOf("@${profile}"))
        }
    }

    @EventHandler
    suspend fun onChatDecorateEvent(event: AsyncChatDecorateEvent) {
        val player = event.player() ?: return
        val plainText = plainText().serialize(event.result())
        val message = plainText.replace(Regex("(@\\w+) "), "<yellow>$1</yellow> ")
        if (message.isBlank()) return
        event.result(MiniMessage.miniMessage().deserialize(message))
    }

    @EventHandler
    suspend fun onChatEvent(event: AsyncChatEvent) {
        val sender = event.player
        if (sender.isOp) return
        event.viewers().removeIf { it != sender && (it !is Permissible || !it.isOp) }
        event.viewers().addAll(withContext(Atom.instance.entityDispatcher(sender)) {
            sender.world.getNearbyPlayers(event.player.location, 200.0)
        })
        event.message()
    }

}