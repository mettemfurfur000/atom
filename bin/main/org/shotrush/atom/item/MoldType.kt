package org.shotrush.atom.item

enum class MoldType(val id: String) {
    Clay("clay"),
    Fired("fired"),
    Wax("wax");

    companion object {
        val TypesById = entries.associateBy { it.id }

        fun byId(id: String) = TypesById[id] ?: error("No such MoldType: $id")
    }
}