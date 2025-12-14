package org.shotrush.atom.content

enum class AnimalType(val id: String) {
    Cow("cow"),
    Pig("pig"),
    Sheep("sheep"),
    Chicken("chicken"),
    Rabbit("rabbit"),
    Horse("horse"),
    Donkey("donkey"),
    Mule("mule"),
    Llama("llama"),
    Goat("goat"),
    Cat("cat"),
    Wolf("wolf"),
    Fox("fox"),
    Panda("panda"),
    PolarBear("polar_bear"),
    Ocelot("ocelot"),
    Camel("camel");


    companion object {
        val TypesById = entries.associateBy { it.id }

        @JvmStatic
        fun byId(id: String?) = id?.let { TypesById[it] }
    }
}