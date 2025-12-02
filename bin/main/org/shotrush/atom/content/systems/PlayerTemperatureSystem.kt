package org.shotrush.atom.content.systems

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.core.api.combat.ArmorProtectionAPI
import org.shotrush.atom.core.api.combat.TemperatureEffectsAPI
import org.shotrush.atom.core.api.player.PlayerDataAPI
import org.shotrush.atom.core.api.scheduler.SchedulerAPI
import org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
import org.shotrush.atom.core.util.ActionBarManager
import java.util.*
import kotlin.math.max
import kotlin.math.min

@RegisterSystem(
    id = "player_temperature_system",
    priority = 4,
    dependencies = ["action_bar_manager", "thirst_system"],
    toggleable = true,
    description = "Manages player body temperature"
)
class PlayerTemperatureSystem(private val plugin: Plugin) : Listener {

    companion object {
        @JvmStatic
        lateinit var instance: PlayerTemperatureSystem
            private set
        
        private const val NORMAL_TEMP = 37.0
        private const val MAX_TEMP = 44.0
        private const val MIN_TEMP = 20.0 // Lowered from 30.0 to allow freezing death
    }

    private val playerTemperatures = HashMap<UUID, Double>()
    private val playerWetness = HashMap<UUID, Double>()

    init {
        instance = this
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerId = player.uniqueId
        
        val savedTemp = PlayerDataAPI.getDouble(player, "temperature.body", NORMAL_TEMP)
        val savedWetness = PlayerDataAPI.getDouble(player, "temperature.wetness", 0.0)
        
        playerTemperatures[playerId] = savedTemp
        playerWetness[playerId] = savedWetness
        startTemperatureTickForPlayer(player)
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerId = player.uniqueId
        
        val temp = playerTemperatures.getOrDefault(playerId, NORMAL_TEMP)
        val wetness = playerWetness.getOrDefault(playerId, 0.0)
        
        PlayerDataAPI.setDouble(player, "temperature.body", temp)
        PlayerDataAPI.setDouble(player, "temperature.wetness", wetness)
        
        playerTemperatures.remove(playerId)
        playerWetness.remove(playerId)
    }
    
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val playerId = player.uniqueId
        
        playerTemperatures[playerId] = NORMAL_TEMP
        playerWetness[playerId] = 0.0
        
        PlayerDataAPI.setDouble(player, "temperature.body", NORMAL_TEMP)
        PlayerDataAPI.setDouble(player, "temperature.wetness", 0.0)
    }
    
    private fun startTemperatureTickForPlayer(player: Player) {
        SchedulerAPI.runTaskTimer(player, { task ->
            if (!player.isOnline) {
                task.cancel()
                return@runTaskTimer
            }
            updatePlayerState(player)
        }, 1L, 20L)
    }
    
    private fun updatePlayerState(player: Player) {
        val playerId = player.uniqueId
        val currentTemp = playerTemperatures.getOrDefault(playerId, NORMAL_TEMP)
        var currentWetness = playerWetness.getOrDefault(playerId, 0.0)

        currentWetness = updateWetness(player, currentWetness)
        playerWetness[playerId] = currentWetness
        

        val ambientTemp = EnvironmentalFactorAPI.getAmbientTemperature(player)

        val armorInsulation = ArmorProtectionAPI.getInsulationValue(player)

        val effectiveInsulation = armorInsulation * (1.0 - (currentWetness * 0.8))

        val activityHeat = if (player.isSprinting) 2.0 else 0.0
        

        var conductivity = 0.005 * (1.0 - effectiveInsulation)
        if (currentWetness > 0) {
             conductivity += (currentWetness * 0.02) 
        }
        
        val targetTemp = ambientTemp + activityHeat
        var change = (targetTemp - currentTemp) * conductivity

        change = max(-0.5, min(0.5, change))
        
        var newTemp = currentTemp + change

        newTemp = applyHomeostasis(player, newTemp)
        
        newTemp = max(MIN_TEMP, min(MAX_TEMP, newTemp))
        playerTemperatures[playerId] = newTemp
        
        applyEffects(player, newTemp, currentWetness)
    }
    

    private fun applyHomeostasis(player: Player, bodyTemp: Double): Double {
        var temp = bodyTemp
        val thirstSystem = ThirstSystem.instance
        val foodLevel = player.foodLevel
        val hydration = thirstSystem?.getThirst(player)?.toDouble() ?: 20.0
        

        val SWEAT_THRESHOLD = 37.5 // Vasodilation starts earlier, but effective sweating here
        val SHIVER_THRESHOLD = 36.5 // Vasoconstriction starts earlier, shivering here

        if (temp > SWEAT_THRESHOLD) {
            val heatStress = temp - SWEAT_THRESHOLD

            val hydrationFactor = 0.5 + (min(20.0, hydration) / 40.0)

            val baseCooling = 0.02
            val activeCooling = (0.15 + (heatStress * 0.02)) * hydrationFactor
            
            temp -= (baseCooling + activeCooling)

            val thirstChance = 0.02 + (heatStress * 0.02)
            if (Math.random() < thirstChance && thirstSystem != null) {
                thirstSystem.addThirst(player, -1)
            }

            if (Math.random() < 0.005) {
                player.foodLevel = max(0, foodLevel - 1)
            }
        } 

        else if (temp < SHIVER_THRESHOLD) {

            val coldStress = SHIVER_THRESHOLD - temp

            val energyFactor = 0.5 + (foodLevel / 40.0)

            val baseWarming = 0.02 // Basal metabolic rate
            val activeWarming = (0.15 + (coldStress * 0.02)) * energyFactor
            
            temp += (baseWarming + activeWarming)

            val hungerChance = 0.01 + (coldStress * 0.02)
            if (Math.random() < hungerChance) {
                player.foodLevel = max(0, foodLevel - 1)
            }

            if (Math.random() < 0.01 && thirstSystem != null) {
                thirstSystem.addThirst(player, -1)
            }
        }
        
        return temp
    }
    
    private fun updateWetness(player: Player, current: Double): Double {
        var wetness = current
        val inWater = player.isInWater
        val inRain = player.world.hasStorm() && player.location.block.lightFromSky > 10 // Roughly exposed to sky
        
        if (inWater) {
            wetness += 0.05
        } else if (inRain) {
            wetness += 0.005
        } else {

            var dryRate = 0.002

            if (EnvironmentalFactorAPI.getNearbyHeatSources(player.location, 3) > 0) {
                dryRate = 0.02
            }
            wetness -= dryRate
        }
        
        return max(0.0, min(1.0, wetness))
    }
    
    private fun applyEffects(player: Player, temp: Double, wetness: Double) {
        TemperatureEffectsAPI.applyBodyTemperatureEffects(player, temp)
        
        val manager = ActionBarManager.getInstance() ?: return

        var tempColorName = "green"
        if (temp > 45) tempColorName = "red"
        else if (temp > 35) tempColorName = "gold"
        else if (temp < 5) tempColorName = "blue"
        else if (temp < 15) tempColorName = "aqua"


        var wetStr = ""
        if (wetness > 0.1) {
            wetStr = " <blue>" + (wetness * 100).toInt() + "% Wet</blue>"
        }

        // Debug/Display
        val message = String.format("<%s>%.1f°C</%s>%s", tempColorName, temp, tempColorName, wetStr)
        manager.setMessage(player, "body_temp", message)
    }
    
    fun getPlayerTemperature(player: Player): Double {
        return playerTemperatures.getOrDefault(player.uniqueId, NORMAL_TEMP)
    }
}
