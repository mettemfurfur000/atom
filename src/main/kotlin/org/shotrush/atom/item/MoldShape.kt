package org.shotrush.atom.item

enum class MoldShape(val id: String, val mold: String, val vanillaItem: org.bukkit.Material? = null) {
    Axe("axe", "axe_head"),
    Hammer("hammer", "hammer_head"),
    Pickaxe("pickaxe", "pickaxe_head"),
    Shovel("shovel", "shovel_head"),
    Sword("sword", "sword_blade"),
    Knife("knife", "knife_blade"),
    Saw("saw", "saw_blade"),
    Ingot("ingot", "ingot"),
    DecoratedPot("decorated_pot", "decorated_pot", org.bukkit.Material.DECORATED_POT);

    companion object {
        val ShapeById = MoldShape.entries.associateBy { it.id }

        fun byId(id: String) = ShapeById[id] ?: error("No such MoldShape: $id")
    }

    fun isVanillaItem(): Boolean = vanillaItem != null
}