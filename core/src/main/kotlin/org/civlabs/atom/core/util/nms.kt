package org.civlabs.atom.core.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState as NMSBlockState
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlockState

fun Block.nms(): NMSBlockState = (this.state as CraftBlockState).handle

fun Location.nms(): BlockPos = BlockPos(blockX, blockY, blockZ)

fun World.nms(): ServerLevel = (this as? CraftWorld)?.handle ?: error("World is not a CraftWorld, ${this.javaClass.simpleName}")