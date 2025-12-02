package org.shotrush.atom.content

enum class AnimalProduct(val id: String) {
    RawMeat("meat_raw"),
    UndercookedMeat("meat_undercooked"),
    CookedMeat("meat_cooked"),
    BurntMeat("meat_burnt"),
    RawLeather("leather_raw"),
    CuredLeather("leather_cured"),
    Leather("leather"),
    Bone("bone");

    companion object {
        val TypesById = entries.associateBy { it.id }

        fun byId(id: String) = TypesById[id] ?: error("No such AnimalProduct: $id")

        fun decodeFromItemKey(string: String): AnimalProduct {
            val key = string.substringAfter(':', missingDelimiterValue = string)
            require(key.startsWith("animal_")) { "Unrecognized item key format: $string" }
            val rest = key.removePrefix("animal_")
            val product = entries
                .sortedByDescending { it.id.length }
                .firstOrNull { rest.startsWith(it.id + "_") }
            return product ?: error("Unrecognized product ID: $rest")
        }
    }
}