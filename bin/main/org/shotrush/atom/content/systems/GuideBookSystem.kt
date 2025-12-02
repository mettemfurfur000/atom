package org.shotrush.atom.content.systems

import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.api.annotation.RegisterSystem
import plutoproject.adventurekt.component
import plutoproject.adventurekt.text.and
import plutoproject.adventurekt.text.newline
import plutoproject.adventurekt.text.style.bold
import plutoproject.adventurekt.text.style.textBlack
import plutoproject.adventurekt.text.style.textDarkGray
import plutoproject.adventurekt.text.style.textDarkRed
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with

@RegisterSystem(
    id = "guide_book_system",
    priority = 10,
    toggleable = true,
    description = "Gives players an essential guide book on join/respawn"
)
class GuideBookSystem(private val plugin: Plugin) : Listener {

    companion object {
        private const val GUIDE_TITLE = "Atom Survival Guide"
        private const val GUIDE_AUTHOR = "Atom Dev Team"
        
        private fun createGuideBook(): ItemStack {
            val book = ItemStack(Material.WRITTEN_BOOK)
            val meta = book.itemMeta as BookMeta
            
            meta.title(Component.text(GUIDE_TITLE, NamedTextColor.GOLD))
            meta.author(Component.text(GUIDE_AUTHOR, NamedTextColor.YELLOW))
            meta.generation = BookMeta.Generation.ORIGINAL

            val p1 = component {
                text("Welcome to Atom") with textDarkRed and bold
                newline()
                newline()
                text("This world operates differently.") with textBlack
                newline()
                text("Survival requires understanding temperature, crafting, and metallurgy.") with textBlack
                newline()
                newline()
                text("Read this guide carefully to survive.") with textDarkGray
            }

            // Page 2: Physicality
            val p2 = Component.text()
                .append(Component.text("Physical Limits", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Your body is frail.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("- ", NamedTextColor.BLACK))
                .append(Component.text("Movement", NamedTextColor.BLUE)).append(Component.text(" is 20% slower.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("- ", NamedTextColor.BLACK))
                .append(Component.text("Mining", NamedTextColor.BLUE)).append(Component.text(" is 75% harder.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("- ", NamedTextColor.BLACK))
                .append(Component.text("Combat", NamedTextColor.BLUE)).append(Component.text(" is exhausting.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Plan every action.", NamedTextColor.DARK_GRAY))
                .build()

            // Page 3: Temperature
            val p3 = Component.text()
                .append(Component.text("Temperature", NamedTextColor.BLUE, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("- ", NamedTextColor.BLACK))
                .append(Component.text("Sprinting", NamedTextColor.RED)).append(Component.text(" warms you.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("- ", NamedTextColor.BLACK))
                .append(Component.text("Water/Rain", NamedTextColor.BLUE)).append(Component.text(" cools you down rapidly.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("- ", NamedTextColor.BLACK))
                .append(Component.text("Campfires", NamedTextColor.GOLD)).append(Component.text(" dry and warm you.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Beware of hypothermia and heatstroke.", NamedTextColor.DARK_RED))
                .build()

            // Page 4: Thirst
            val p4 = Component.text()
                .append(Component.text("Thirst & Water", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Drinking ", NamedTextColor.BLACK))
                .append(Component.text("Raw Water", NamedTextColor.DARK_GREEN))
                .append(Component.text(" causes sickness (Hunger).", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Purification:", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Boil a ", NamedTextColor.BLACK))
                .append(Component.text("Water Bottle", NamedTextColor.WHITE))
                .append(Component.text(" on a ", NamedTextColor.BLACK))
                .append(Component.text("Campfire", NamedTextColor.GOLD))
                .append(Component.text(" until hot.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("It becomes ", NamedTextColor.BLACK))
                .append(Component.text("Purified Water", NamedTextColor.AQUA))
                .append(Component.text(" (+Regen).", NamedTextColor.BLACK))
                .build()

            // Page 5: Pebbles & Tools
            val p5 = Component.text()
                .append(Component.text("Early Tools", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("1. Find ", NamedTextColor.BLACK))
                .append(Component.text("Pebbles", NamedTextColor.GRAY))
                .append(Component.text(" on the ground.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("2. Right-click a Pebble on ", NamedTextColor.BLACK))
                .append(Component.text("Stone/Rock", NamedTextColor.DARK_GRAY))
                .append(Component.text(" blocks repeatedly.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("3. Use ", NamedTextColor.BLACK))
                .append(Component.text("Sharpened Rocks", NamedTextColor.GRAY))
                .append(Component.text(" to craft basic tools.", NamedTextColor.BLACK))
                .build()

            // Page 6: Knapping & Ingredients
            val p6 = Component.text()
                .append(Component.text("Knapping", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Use materials on the ", NamedTextColor.BLACK))
                .append(Component.text("Knapping Station", NamedTextColor.DARK_AQUA))
                .append(Component.text(".", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Ingredients:", NamedTextColor.DARK_GREEN))
                .append(Component.newline())
                .append(Component.text("1. ", NamedTextColor.BLACK))
                .append(Component.text("Clay Ball", NamedTextColor.GRAY))
                .append(Component.text(": Add clay to match pattern (Molds).", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("2. ", NamedTextColor.BLACK))
                .append(Component.text("Pebble", NamedTextColor.GRAY))
                .append(Component.text(": Remove stone to match pattern (Tools).", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("3. ", NamedTextColor.BLACK))
                .append(Component.text("Honeycomb", NamedTextColor.GOLD))
                .append(Component.text(": Wax Molds.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Check the ", NamedTextColor.BLACK))
                .append(Component.text("Recipe Book", NamedTextColor.GREEN))
                .append(Component.text(" in the station for patterns.", NamedTextColor.BLACK))
                .build()

            // Page 7: Firing Molds
            val p7 = Component.text()
                .append(Component.text("Firing Molds", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Unfired Clay Molds are useless.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Place the ", NamedTextColor.BLACK))
                .append(Component.text("Unfired Mold", NamedTextColor.GRAY))
                .append(Component.text(" directly onto a ", NamedTextColor.BLACK))
                .append(Component.text("LIT Campfire", NamedTextColor.GOLD))
                .append(Component.text(".", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("Wait 60s for it to fire.", NamedTextColor.DARK_GRAY))
                .build()

            // Page 8: Casting (Metallurgy)
            val p8 = Component.text()
                .append(Component.text("Casting", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("1. Place a ", NamedTextColor.BLACK))
                .append(Component.text("Clay Cauldron", NamedTextColor.GRAY))
                .append(Component.text(" directly ABOVE a Lit Campfire.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("2. Add ", NamedTextColor.BLACK))
                .append(Component.text("Raw Ore", NamedTextColor.GOLD))
                .append(Component.text(" (Copper/Iron).", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("3. Wait for it to melt.", NamedTextColor.BLACK))
                .build()
            
            // Page 9: Casting (Filling)
            val p9 = Component.text()
                .append(Component.text("Casting Cont.", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("4. Right-click the cauldron with an ", NamedTextColor.BLACK))
                .append(Component.text("Empty Fired Mold", NamedTextColor.RED))
                .append(Component.text(" to fill it.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("5. ", NamedTextColor.BLACK))
                .append(Component.text("Throw the filled mold", NamedTextColor.GOLD))
                .append(Component.text(" on the ground.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("6. Wait 30 seconds for it to cool and break open.", NamedTextColor.BLACK))
                .build()

            // Page 10: Leather Working
            val p10 = Component.text()
                .append(Component.text("Leather Working", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("1. Craft a ", NamedTextColor.BLACK))
                .append(Component.text("Leather Bed", NamedTextColor.YELLOW))
                .append(Component.text(".", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("2. Place ", NamedTextColor.BLACK))
                .append(Component.text("Leather/Hide", NamedTextColor.GOLD))
                .append(Component.text(" on it.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("3. Use a ", NamedTextColor.BLACK))
                .append(Component.text("Sharpened Rock", NamedTextColor.GRAY))
                .append(Component.text(" or ", NamedTextColor.BLACK))
                .append(Component.text("Knife", NamedTextColor.GRAY))
                .append(Component.text(" to scrape and process it.", NamedTextColor.BLACK))
                .build()

            // Page 11: Domestication
            val p11 = Component.text()
                .append(Component.text("Domestication", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Animals are wild.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("Breeding animals increases their ", NamedTextColor.BLACK))
                .append(Component.text("Domestication Level", NamedTextColor.GREEN))
                .append(Component.text(".", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Level 5+", NamedTextColor.GOLD))
                .append(Component.text(" animals are fully domesticated and will follow herd commands.", NamedTextColor.BLACK))
                .build()

            // Page 12: Mechanical Power
            val p12 = Component.text()
                .append(Component.text("Mechanics", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Use ", NamedTextColor.BLACK))
                .append(Component.text("Cogs", NamedTextColor.GRAY))
                .append(Component.text(" to transmit power.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("To create a ", NamedTextColor.BLACK))
                .append(Component.text("Power Source", NamedTextColor.RED))
                .append(Component.text(":", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("Right-click a Cog with a ", NamedTextColor.BLACK))
                .append(Component.text("Wrench", NamedTextColor.BLUE))
                .append(Component.text(".", NamedTextColor.BLACK))
                .build()

            meta.addPages(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
            book.itemMeta = meta
            return book
        }
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        ensureGuideBook(event.player)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        ensureGuideBook(event.player)
    }
    
    // Prevent dropping the guide book
    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        if (isGuideBook(item)) {
            event.isCancelled = true
            event.player.sendMessage(Component.text("You cannot drop the guide book.", NamedTextColor.RED))
        }
    }

    // Prevent moving the guide book to other inventories (chests, etc)
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        if (isGuideBook(item)) {
            // Allow moving in own inventory, but not to top inventory if it's not the player's crafting/inventory
            if (event.clickedInventory != event.whoClicked.inventory) {
                // If they are trying to move it OUT of their inventory
                 if (event.clickedInventory?.type != InventoryType.PLAYER) {
                     // This logic is a bit simplistic, but 'unremoveable' usually implies "keep it on you"
                     // We simply cancel if they try to put it into a chest/container
                     if (event.clickedInventory != null && event.clickedInventory!!.type != InventoryType.PLAYER && event.clickedInventory!!.type != InventoryType.CRAFTING) {
                         event.isCancelled = true
                     }
                 }
            }
            
            // Also block shift-clicking into other inventories
            if (event.isShiftClick && event.view.topInventory.type != InventoryType.CRAFTING) {
                 event.isCancelled = true
            }
        }
    }

    private fun ensureGuideBook(player: Player) {
        val inventory = player.inventory
        
        // Check if player already has the book
        for (item in inventory.contents) {
            if (item != null && isGuideBook(item)) {
                return
            }
        }
        
        // Give book
        inventory.addItem(createGuideBook())
        player.sendMessage(Component.text("You have received the Atom Survival Guide.", NamedTextColor.GREEN))
    }

    private fun isGuideBook(item: ItemStack): Boolean {
        if (item.type != Material.WRITTEN_BOOK) return false
        val meta = item.itemMeta as? BookMeta ?: return false
        return meta.title() == Component.text(GUIDE_TITLE, NamedTextColor.GOLD) && 
               meta.author() == Component.text(GUIDE_AUTHOR, NamedTextColor.YELLOW)
    }
}
