package org.shotrush.atom.commands

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.*
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.minecraft.core.Direction
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.joml.Vector3i
import org.shotrush.atom.Atom
import org.shotrush.atom.sendMiniMessage
import org.shotrush.atom.core.api.scheduler.SchedulerAPI
import org.shotrush.atom.systems.room.FaceOpenProvider
import org.shotrush.atom.systems.room.RoomRegistry
import org.shotrush.atom.systems.room.RoomScanner
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object RoomCommands {
    // Debug: FaceOpen visualizer state
    private val faceOpenActive = ConcurrentHashMap<UUID, ScheduledTask>()
    private val COLOR_OPEN = Particle.DustOptions(Color.fromRGB(20, 220, 60), 1.1f)
    private val COLOR_CLOSED = Particle.DustOptions(Color.fromRGB(220, 20, 60), 1.1f)
    private val COLOR_OCCUPY = Particle.DustOptions(Color.fromRGB(120, 120, 255), 0.9f)

    fun register() {
        commandTree("room") {
            literalArgument("current", true) {
                withPermission("atom.command.room.current")
                playerExecutor { player, arguments ->
                    val room = RoomRegistry.roomAt(player.location)
                    if (room == null) {
                        player.sendMiniMessage("<red>You are not in a room</red>")
                    } else {
                        player.sendMiniMessage("<green>You are in room ${room.id}</green>")
                    }
                }
            }
            // Debug subcommand: visualize FaceOpenProvider around the player
            literalArgument("faceopen", true) {
                withPermission("atom.debug.faceopen")
                playerExecutor { player, _ ->
                    toggleFaceOpenDebug(player)
                }
            }
            literalArgument("scan") {
                withPermission("atom.command.room.scan")
                integerArgument("maxVolume", 10, Short.MAX_VALUE.toInt(), true) {
                    playerExecutor { player, arguments ->
                        val maxVolume = arguments["maxVolume"] as? Int ?: 800
                        Atom.instance.launch(Atom.instance.entityDispatcher(player)) {
                            player.sendMiniMessage("<green>Scanning for rooms...</green>")
                            val scan = RoomScanner.scanAt(
                                player.world,
                                Vector3i(player.location.blockX, player.location.blockY, player.location.blockZ),
                                maxVolume = maxVolume,
                                retries = 0
                            )
                            player.sendMiniMessage(
                                "<green>Scanned room ${scan?.id ?: "<red>failed</red>"}</green>"
                            )
                        }
                    }
                }
            }
            literalArgument("save") {
                withPermission("atom.command.room.save")
                anyExecutor { executor, arguments ->
                    Atom.instance.launch {
                        executor.sendMiniMessage("<green>Saving rooms...</green>")
                        try {
                            RoomRegistry.saveAllToDisk()
                            executor.sendMiniMessage("<green>Saved rooms</green>")
                        } catch (e: Exception) {
                            executor.sendMiniMessage("<red>Failed to save rooms</red>")
                            e.printStackTrace()
                        }
                    }
                }
            }
            literalArgument("load") {
                withPermission("atom.command.room.load")
                anyExecutor { executor, arguments ->
                    Atom.instance.launch {
                        executor.sendMiniMessage("<green>Loading rooms...</green>")
                        try {
                            RoomRegistry.readAllFromDisk()
                            executor.sendMiniMessage("<green>Loaded rooms</green>")
                        } catch (e: Exception) {
                            executor.sendMiniMessage("<red>Failed to load rooms</red>")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun toggleFaceOpenDebug(player: Player) {
        val id = player.uniqueId
        val existing = faceOpenActive.remove(id)
        if (existing != null) {
            existing.cancel()
            player.sendMiniMessage("<yellow>[FaceOpen]</yellow> <gray>Debug visualization</gray> <red>disabled</red>.")
            return
        }

        val task = SchedulerAPI.runTaskTimer(player, { task ->
            if (!player.isOnline) {
                task?.cancel()
                faceOpenActive.remove(id)
                return@runTaskTimer
            }
            try {
                renderFaceOpenAround(player)
            } catch (_: Throwable) {
                task?.cancel()
                faceOpenActive.remove(id)
            }
        }, 1L, 6L)

        faceOpenActive[id] = task
        player.sendMiniMessage("<yellow>[FaceOpen]</yellow> <gray>Debug visualization</gray> <green>enabled</green>. <gray>Run again to disable.</gray>")
    }

    private fun isAirLike(mat: Material): Boolean {
        return mat == Material.AIR ||
                mat == Material.CAVE_AIR ||
                mat == Material.VOID_AIR
    }
    private fun renderFaceOpenAround(player: Player, radius: Int = 3) {
        val world = player.world
        val bx = player.location.blockX
        val by = player.location.blockY
        val bz = player.location.blockZ

        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    val x = bx + dx
                    val y = by + dy
                    val z = bz + dz

                    val mat = world.getBlockAt(x, y, z).type
                    if (isAirLike(mat)) continue

                    // Skip if not occupiable to reduce clutter
                    if (!FaceOpenProvider.canOccupy(world, x, y, z)) {
                        // for (dir in Direction.entries) drawFaceOutline(world, x, y, z, dir, COLOR_OCCUPY)
                        continue
                    }

                    // Draw per-face outline
                    for (dir in Direction.entries) {
                        val open = FaceOpenProvider.isOpen(world, x, y, z, dir)
                        drawFaceOutline(world, x, y, z, dir, if (open) COLOR_OPEN else COLOR_CLOSED)
                    }
                }
            }
        }
    }

    private fun drawFaceOutline(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        dir: Direction,
        color: Particle.DustOptions,
        segments: Int = 2,
    ) {
        val ox = x + 0.5
        val oy = y + 0.5
        val oz = z + 0.5

        val half = 0.5
        val step = 1.0 / max(1, segments)
        val outward = 0.52

        fun spawn(px: Double, py: Double, pz: Double) {
            world.spawnParticle(Particle.DUST, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0, color)
        }

        when (dir) {
            Direction.UP, Direction.DOWN -> {
                val yFace = oy + if (dir == Direction.UP) half + 0.02 else -half - 0.02
                // four edges of the square
                for (t in 0..segments) {
                    val s = -half + t * step
                    spawn(ox - half, yFace, oz + s)
                    spawn(ox + half, yFace, oz + s)
                    spawn(ox + s, yFace, oz - half)
                    spawn(ox + s, yFace, oz + half)
                }
            }
            Direction.NORTH, Direction.SOUTH -> {
                val zFace = oz + if (dir == Direction.SOUTH) half + 0.02 else -half - 0.02
                for (t in 0..segments) {
                    val s = -half + t * step
                    spawn(ox - half, oy + s, zFace)
                    spawn(ox + half, oy + s, zFace)
                    spawn(ox + s, oy - half, zFace)
                    spawn(ox + s, oy + half, zFace)
                }
            }
            Direction.EAST, Direction.WEST -> {
                val xFace = ox + if (dir == Direction.EAST) half + 0.02 else -half - 0.02
                for (t in 0..segments) {
                    val s = -half + t * step
                    spawn(xFace, oy + s, oz - half)
                    spawn(xFace, oy + s, oz + half)
                    spawn(xFace, oy - half, oz + s)
                    spawn(xFace, oy + half, oz + s)
                }
            }
        }
    }
}