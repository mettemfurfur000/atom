package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import kotlinx.coroutines.delay
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.shotrush.atom.Atom
import plutoproject.adventurekt.component
import plutoproject.adventurekt.text.style.textGold
import plutoproject.adventurekt.text.style.textGray
import plutoproject.adventurekt.text.style.textRed
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with
import kotlin.time.Duration.Companion.milliseconds

object PlayerDataTrackingListener : Listener {
    fun register(atom: Atom) {
        val eventDispatcher = mapOf(eventDef<PlayerJoinEvent> {
            atom.entityDispatcher(it.player)
        }, eventDef<PlayerQuitEvent> {
            atom.entityDispatcher(it.player)
        }, eventDef<PlayerDeathEvent> {
            atom.entityDispatcher(it.player)
        })
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    val INITIAL_JOIN_TIME_KEY = NamespacedKey("atom", "join_time")
    val LAST_SEEN_KEY = NamespacedKey("atom", "last_seen")
    val LAST_DEATH_KEY = NamespacedKey("atom", "last_death")
    val TOTAL_PLAYTIME = NamespacedKey("atom", "playtime")

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val pdc = player.persistentDataContainer
        val now = System.currentTimeMillis()
        if (!pdc.has(INITIAL_JOIN_TIME_KEY)) {
            pdc.set(INITIAL_JOIN_TIME_KEY, PersistentDataType.LONG, now)
            pdc.set(LAST_SEEN_KEY, PersistentDataType.LONG, now)
            pdc.set(LAST_DEATH_KEY, PersistentDataType.LONG, player.world.fullTime)
            pdc.set(TOTAL_PLAYTIME, PersistentDataType.LONG, 0L)
            player.sendMessage(component {
                text("Welcome to ") with textGray
                text("CivLabs") with textRed
            })
        } else {
            val lastSeen = pdc.get(LAST_SEEN_KEY, PersistentDataType.LONG) ?: now
            val elapsed = (now - lastSeen).milliseconds
            pdc.set(LAST_SEEN_KEY, PersistentDataType.LONG, now)
            player.sendMessage(component {
                text("Welcome back to ") with textGray
                text("CivLabs") with textRed
            })
            player.sendMessage(component {
                text("You were last seen ") with textGray
                text(
                    elapsed.toComponents { days, hours, minutes, seconds, _ ->
                        buildString {
                            if (days > 0) append("${days}d")
                            if (hours > 0) append("${hours}h")
                            if (minutes > 0) append("${minutes}m")
                            if (isEmpty()) append("${seconds}s")
                            append(" ago")
                        }
                    }
                ) with textGold
            })
        }
        player.sendMessage("")
        player.sendMessage(
            MiniMessage.miniMessage()
                .deserialize("<gray>We are currently testing the <white><image:atom:badge_age_foraging> <gray>and the</gray> <image:atom:badge_age_copper>")
        )
        player.sendMessage("")
        delay(1500)
        player.showTitle(
            Title.title(
                MiniMessage.miniMessage().deserialize("<gray>Welcome to the"),
                MiniMessage.miniMessage().deserialize("<white><image:atom:badge_age_foraging>"),
                (1.5 * 20).toInt(), 8 * 20, (1.5 * 20).toInt()
            ),
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val pdc = player.persistentDataContainer
        val now = System.currentTimeMillis()
        val lastSeen = pdc.get(LAST_SEEN_KEY, PersistentDataType.LONG) ?: now
        val elapsed = now - lastSeen
        pdc.set(LAST_SEEN_KEY, PersistentDataType.LONG, now)
        val playtime = pdc.get(TOTAL_PLAYTIME, PersistentDataType.LONG) ?: 0L
        pdc.set(TOTAL_PLAYTIME, PersistentDataType.LONG, playtime + elapsed)
    }

    @EventHandler
    fun onPlayerDeathEvent(event: PlayerDeathEvent) {
        val player = event.player
        val pdc = player.persistentDataContainer
        pdc.set(LAST_DEATH_KEY, PersistentDataType.LONG, player.world.fullTime)
    }

    fun getPlayerAliveTime(player: Player): Long {
        val pdc = player.persistentDataContainer
        val lastDeath = pdc.get(LAST_DEATH_KEY, PersistentDataType.LONG) ?: return -1
        val currentGameTime = player.world.fullTime
        val aliveTime = currentGameTime - lastDeath
        return aliveTime
    }
}