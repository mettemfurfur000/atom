package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DyedItemColor
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Color
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.shotrush.atom.Atom
import org.shotrush.atom.matches
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.CoroutineContext

inline fun <reified T : Event> eventDef(noinline runner: (event: T) -> CoroutineContext): Pair<Class<out Event>, (event: Event) -> CoroutineContext> {
    return (T::class.java to runner) as Pair<Class<out Event>, (event: Event) -> CoroutineContext>
}

object TestListener : Listener {
    fun register(atom: Atom) {
        val eventDispatcher = mapOf(eventDef<PlayerInteractEvent> {
            atom.entityDispatcher(it.player)
        })
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (item.matches("atom:filled_fired_mold_axe_head")) {
            println("Player ${event.player.name} used a mold axe head!")
            // Pick a random material and set the name and color accordingly
            val materials = listOf(
                Pair("Iron", Color.fromRGB(216, 216, 216)),
                Pair("Copper", Color.fromRGB(184, 115, 51)),
                Pair("Bronze", Color.fromRGB(205, 127, 50)),
                Pair("Steel", Color.fromRGB(160, 160, 160))
            )
            val (materialName, materialRgb) = materials[ThreadLocalRandom.current().nextInt(materials.size)]

            val lore = item.lore() ?: return
            val loreCopy = lore.toMutableList()
            // replace lore at index 0
            loreCopy[0] = Component.text("Filled with: ").style {
                it.decoration(TextDecoration.ITALIC, false).color(
                    NamedTextColor.GRAY
                )
            }.append(
                Component.text(
                    materialName,
                    TextColor.color(materialRgb.red, materialRgb.green, materialRgb.blue)
                )
            )
            item.lore(loreCopy)
            item.setData(
                DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(
                    materialRgb
                )
            )
            item.setData(
                DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .hiddenComponents(setOf(DataComponentTypes.DYED_COLOR))
                    .build()
            )

        }
    }
}