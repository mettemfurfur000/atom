package org.shotrush.atom.commands.debug

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import org.bukkit.entity.Player
import org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
import org.shotrush.atom.core.api.world.SeasonAPI
import org.shotrush.atom.core.util.ChatUtil

@CommandAlias("tempdebug")
@CommandPermission("atom.debug.temp")
@Description("Debug temperature calculations")
class TemperatureDebugCommand : BaseCommand() {

    @Default
    fun onDebug(player: Player) {
        val world = player.world
        
        val mcTemp = player.location.block.temperature
        val baseC = (mcTemp * 30.0) - 5.0
        val timeMod = EnvironmentalFactorAPI.getTimeModifier(world)
        val weatherMod = EnvironmentalFactorAPI.getWeatherModifier(world, baseC)
        val seasonMod = SeasonAPI.getCurrentSeason(world).tempModifier
        val heatSources = EnvironmentalFactorAPI.getNearbyHeatSources(player.location, 5)
        val finalAmbient = EnvironmentalFactorAPI.getAmbientTemperature(player)
        
        player.sendMessage(ChatUtil.color("<gold>--- Temperature Debug ---"))
        player.sendMessage(ChatUtil.color("<gray>Biome MC Temp: <white>" + String.format("%.2f", mcTemp)))
        player.sendMessage(ChatUtil.color("<gray>Base Celsius: <white>" + String.format("%.1f", baseC)))
        player.sendMessage(ChatUtil.color("<gray>Time Modifier: <white>" + String.format("%.1f", timeMod)))
        player.sendMessage(ChatUtil.color("<gray>Weather Mod: <white>" + String.format("%.1f", weatherMod)))
        player.sendMessage(ChatUtil.color("<gray>Season Mod: <white>" + String.format("%.1f", seasonMod) + " (" + SeasonAPI.getCurrentSeason(world).name + ")"))
        player.sendMessage(ChatUtil.color("<gray>Heat Sources: <white>" + String.format("%.1f", heatSources)))
        player.sendMessage(ChatUtil.color("<yellow>Final Ambient: <white>" + String.format("%.1f", finalAmbient)))
    }
}
