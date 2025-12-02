package org.shotrush.atom

import org.bukkit.Color

enum class Age(val id: String, val rgb: Color) {
    Foraging("foraging", Color.fromRGB(128, 128, 128)),
    Copper("copper", Color.fromRGB(228, 128, 101)),
    Bronze("bronze", Color.fromRGB(200, 100, 50)),
    Iron("iron", Color.fromRGB(210, 210, 210)),
    Steel("steel", Color.fromRGB(128, 128, 128)),
    ;

    val enabled: Boolean
        get() = ordinal <= Bronze.ordinal

    val badge: String = "<image:atom:badge_age_$id>"
}