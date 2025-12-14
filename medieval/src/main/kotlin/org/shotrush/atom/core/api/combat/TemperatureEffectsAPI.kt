package org.shotrush.atom.core.api.combat

import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.awt.Color
import kotlin.math.min
import kotlin.math.max

object TemperatureEffectsAPI {

    @JvmStatic
    fun applyHeatDamage(player: Player, temperature: Double, hasProtection: Boolean) {
        if (hasProtection) return

        if (temperature >= 100) {
            player.fireTicks = 40
            spawnSweatParticles(player, 3)
        } else if (temperature >= 50) {
            player.fireTicks = 20
            spawnSweatParticles(player, 1)
        }
    }

    @JvmStatic
    fun applyColdDamage(player: Player, temperature: Double, hasProtection: Boolean) {
        if (hasProtection) return

        if (temperature <= -20) {
            player.damage(2.0)
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 1, false, false
            ))
            spawnColdParticles(player, 3)
        } else if (temperature <= -10) {
            player.damage(1.0)
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 0, false, false
            ))
            spawnColdParticles(player, 1)
        }
    }

    @JvmStatic
    fun applyBodyTemperatureEffects(player: Player, bodyTemp: Double) {
        if (bodyTemp >= 45.0) {
            player.damage(0.5)
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 1, false, false
            ))
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.NAUSEA, 100, 0, false, false
            ))
            spawnSweatParticles(player, 5)
        } else if (bodyTemp >= 40.0) {
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 0, false, false
            ))
            spawnSweatParticles(player, 2)
        } else if (bodyTemp >= 35.0) {
            spawnSweatParticles(player, 1)
        } else if (bodyTemp <= 0.0) {
            player.damage(0.5)
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 2, false, false
            ))
            player.freezeTicks = min(player.freezeTicks + 10, 140)
            spawnColdParticles(player, 5)
        } else if (bodyTemp <= 5.0) {
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 1, false, false
            ))
            player.freezeTicks = min(player.freezeTicks + 5, 140)
            spawnColdParticles(player, 3)
        } else if (bodyTemp <= 10.0) {
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 0, false, false
            ))
            spawnColdParticles(player, 1)
        }
    }

    private fun hsv(h: Float, s: Float, v: Float): IntArray {
        val rgb = Color.HSBtoRGB(h, s, v)
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return intArrayOf(r, g, b)
    }

    @JvmStatic
    fun getBodyTempDisplay(temp: Double): String {
        var currentTemp = temp
        val MIN_TEMP = -20.0
        val MAX_TEMP = 60.0
        val OPTIMAL_MIN = 15.0
        val OPTIMAL_MAX = 30.0

        currentTemp = max(MIN_TEMP, min(MAX_TEMP, currentTemp))

        val hue: Float
        if (currentTemp < OPTIMAL_MIN) {
            val t = (currentTemp - MIN_TEMP) / (OPTIMAL_MIN - MIN_TEMP)
            hue = (2.0 / 3.0 - t * (2.0 / 3.0 - 1.0 / 3.0)).toFloat()
        } else if (currentTemp > OPTIMAL_MAX) {
            val t = (currentTemp - OPTIMAL_MAX) / (MAX_TEMP - OPTIMAL_MAX)
            hue = (1.0 / 3.0 - t * (1.0 / 3.0 - 0.0)).toFloat()
        } else {
            hue = 1f / 3f
        }


        val rgb = hsv(hue, 1.0f, 1.0f)
        val hex = String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2])
        val text = String.format("%.1f°C", currentTemp)
        return "<$hex>$text</$hex>"
    }

    private fun spawnSweatParticles(player: Player, count: Int) {

        val loc = player.eyeLocation.add(0.0, 0.5, 0.0)

        player.world.spawnParticle(
                Particle.SPLASH, 
                loc,
                count * 2, // slightly more particles
                0.3, 0.2, 0.3, // spread around head
                0.0 // speed
        )

        if (Math.random() < 0.3) {
            player.world.spawnParticle(
                Particle.DRIPPING_WATER,
                loc,
                1,
                0.2, 0.1, 0.2,
                0.0
            )
        }
    }

    private fun spawnColdParticles(player: Player, count: Int) {
        val eyeLoc = player.eyeLocation
        val direction = eyeLoc.direction
        

        val mouthLoc = eyeLoc.clone().subtract(0.0, 0.2, 0.0).add(direction.clone().multiply(0.5))

        
        player.world.spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                mouthLoc,
                0,
                direction.x * 0.05, 0.02, direction.z * 0.05,
                1.0
        )

        if (count > 3) {
             player.world.spawnParticle(
                Particle.SNOWFLAKE,
                mouthLoc,
                2,
                0.1, 0.1, 0.1,
                0.01
            )
        }
    }
}
