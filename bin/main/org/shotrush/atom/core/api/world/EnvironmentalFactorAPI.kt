package org.shotrush.atom.core.api.world

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.Lightable
import org.bukkit.entity.Player
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.PI

object EnvironmentalFactorAPI {

    private fun convertMcTempToCelsius(mcTemp: Double): Double {
        return if (mcTemp < 0.2) {
            (mcTemp * 100.0) - 15.0
        } else if (mcTemp < 1.0) {
            5.0 + ((mcTemp - 0.2) * 31.25)
        } else {
            30.0 + ((mcTemp - 1.0) * 30.0)
        }
    }
    
    @JvmStatic
    fun getTimeModifier(world: World): Double {
        val time = world.time

        val angle = ((time - 8000) / 24000.0) * 2.0 * PI

        return cos(angle) * 5.0
    }
    

    @JvmStatic
    fun getWeatherModifier(world: World, baseTemp: Double): Double {
        if (world.hasStorm()) {

            if (baseTemp < 0.0) {
                return -5.0
            }
            return -3.0
        }
        return 0.0
    }
    
    @JvmStatic
    fun getNearbyHeatSources(location: Location, radius: Int): Double {
        var heatChange = 0.0

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val block = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                    val distance = sqrt((x*x + y*y + z*z).toDouble())
                    
                    if (distance == 0.0) continue
                    
                    val influence = 1.0 / (distance * distance)
                    val type = block.type
                    
                    if (type == Material.FIRE || type == Material.SOUL_FIRE) heatChange += 15.0 * influence
                    else if (type == Material.LAVA) heatChange += 30.0 * influence
                    else if (type == Material.MAGMA_BLOCK) heatChange += 10.0 * influence
                    else if (type == Material.CAMPFIRE) {
                         val data = block.blockData
                         if (data is Lightable && data.isLit) heatChange += 10.0 * influence
                    }
                    else if (type == Material.SOUL_CAMPFIRE) {
                         val data = block.blockData
                         if (data is Lightable && data.isLit) heatChange += 8.0 * influence
                    }
                    else if (type == Material.ICE || type == Material.PACKED_ICE) heatChange -= 5.0 * influence
                    else if (type == Material.BLUE_ICE) heatChange -= 8.0 * influence
                    else if (type == Material.SNOW || type == Material.SNOW_BLOCK) heatChange -= 3.0 * influence
                }
            }
        }
        
        return heatChange
    }
    
    @JvmStatic
    fun getAmbientTemperature(player: Player): Double {
        return getAmbientTemperature(player.location)
    }

    @JvmStatic
    fun getAmbientTemperature(loc: Location): Double {
        val world = loc.world
        val mcTemp = loc.block.temperature
        
        var temp = convertMcTempToCelsius(mcTemp)

        if (world.environment == World.Environment.NETHER) {

             temp = max(temp, 50.0)
        }
        
        temp += getTimeModifier(world)
        
        temp += getWeatherModifier(world, temp)
        temp += SeasonAPI.getCurrentSeason(world).tempModifier
        temp += getNearbyHeatSources(loc, 5)
        

        if (loc.block.lightFromSky < 10 && loc.blockY < 60) {
             temp = (temp + 15.0) / 2.0
        }
        
        return temp
    }
}
