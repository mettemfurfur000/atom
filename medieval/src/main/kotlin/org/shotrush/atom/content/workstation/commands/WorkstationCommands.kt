//package org.shotrush.atom.content.workstation.commands
//
//import co.aikar.commands.BaseCommand
//import co.aikar.commands.annotation.*
//import net.momirealms.craftengine.core.plugin.CraftEngine
//import org.bukkit.entity.Player
//import org.shotrush.atom.Atom
//import org.shotrush.atom.content.workstation.leatherbed.*
//import net.momirealms.craftengine.core.world.BlockPos
//
//
//@CommandAlias("workstation|ws")
//@Description("Workstation management commands")
//class WorkstationCommands : BaseCommand() {
//
//    @Subcommand("cure accelerate")
//    @CommandPermission("atom.workstation.cure.accelerate")
//    @Description("Instantly completes the curing process for leather on a leather bed")
//    fun onAccelerateCure(player: Player) {
//        val targetBlock = player.getTargetBlockExact(5)
//        if (targetBlock == null) {
//            player.sendMessage("§cYou must be looking at a leather bed!")
//            return
//        }
//        val pos = BlockPos(targetBlock.x, targetBlock.y, targetBlock.z)
//
//
//        val accelerated = LeatherBedBlockBehavior.Companion.accelerateCuring(pos)
//
//        if (accelerated) {
//            player.sendMessage("§aSuccessfully accelerated the leather curing process!")
//        } else {
//            player.sendMessage("§cNo leather is currently curing on this bed, or this is not a leather bed!")
//        }
//    }
//
//    @Subcommand("cure status")
//    @CommandPermission("atom.workstation.cure.status")
//    @Description("Check the status of leather curing on a leather bed")
//    fun onCureStatus(player: Player) {
//        val targetBlock = player.getTargetBlockExact(5)
//        if (targetBlock == null) {
//            player.sendMessage("§cYou must be looking at a leather bed!")
//            return
//        }
//
//        val pos = BlockPos(targetBlock.x, targetBlock.y, targetBlock.z)
//        val timeRemaining = LeatherBedBlockBehavior.Companion.getCuringTimeRemaining(pos)
//
//        if (timeRemaining != null) {
//            val minutes = timeRemaining / 60000L
//            val seconds = (timeRemaining % 60000L) / 1000L
//            player.sendMessage("§eCuring in progress: §f${minutes}m ${seconds}s remaining")
//        } else {
//            player.sendMessage("§cNo leather is currently curing on this bed!")
//        }
//    }
//
//    @Subcommand("cure settime")
//    @CommandPermission("atom.workstation.cure.settime")
//    @CommandCompletion("@range:1-600")
//    @Description("Set the curing time for leather beds (in seconds)")
//    fun onSetCureTime(player: Player, @Default("600") seconds: Int) {
//        if (seconds < 1 || seconds > 3600) {
//            player.sendMessage("§cCuring time must be between 1 and 3600 seconds!")
//            return
//        }
//
//        LeatherBedBlockBehavior.Companion.setCuringTime(seconds * 1000L)
//        player.sendMessage("§aLeather curing time set to §f$seconds §aseconds!")
//    }
//}
