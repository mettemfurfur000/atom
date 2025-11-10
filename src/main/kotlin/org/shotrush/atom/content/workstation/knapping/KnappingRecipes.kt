package org.shotrush.atom.content.workstation.knapping

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.item.Items

data class KnappingRecipe(val patterns: List<List<String>>, val result: String) {
    constructor(pattern: String, result: String) : this(listOf(pattern.split('\n')), result)
}

object KnappingRecipes {
    val axePattern: List<String> = listOf(
        "   # ",
        " ####",
        "#####",
        " ####",
        "   # "
    )
    val recipes = mutableListOf<KnappingRecipe>()

    init {
        recipes.add(KnappingRecipe(listOf(
            axePattern,
            invertX(axePattern),
        ), "axe_head"))
    }

    fun matchesPattern(
        grid: List<Boolean>,
        pattern: List<String>,
        filledChars: Set<Char> = setOf('#') // customize if needed
    ): Boolean {
        require(grid.size == N * N) { "grid must have size 25" }
        require(pattern.size == N && pattern.all { it.length == N }) {
            "pattern must be 5 rows of length 5"
        }

        for (r in 0 until N) {
            for (c in 0 until N) {
                val expected = pattern[r][c] in filledChars
                val actual = grid[r + c * N]
                if (actual != expected) return false
            }
        }
        return true
    }

    fun getResult(grid: List<Boolean>): ItemStack? {
        for (recipe in recipes) {
            for(pattern in recipe.patterns) {
                if (matchesPattern(grid, pattern)) {
                    return Items.getMold(recipe.result, "clay").buildItemStack()
                }
            }
        }
        return null
    }

    private const val N = 5

    fun invertX(pattern: List<String>): List<String> {
        require(pattern.size == N && pattern.all { it.length == N })
        return pattern.map { it.reversed() }
    }

    fun invertY(pattern: List<String>): List<String> {
        require(pattern.size == N && pattern.all { it.length == N })
        return pattern.asReversed()
    }

    fun rotate90(pattern: List<String>): List<String> {
        require(pattern.size == N && pattern.all { it.length == N })
        val out = Array(N) { CharArray(N) }
        for (r in 0 until N) {
            for (c in 0 until N) {
                out[c][N - 1 - r] = pattern[r][c]
            }
        }
        return out.map { String(it) }
    }

    fun rotateNeg90(pattern: List<String>): List<String> {
        require(pattern.size == N && pattern.all { it.length == N })
        val out = Array(N) { CharArray(N) }
        for (r in 0 until N) {
            for (c in 0 until N) {
                out[N - 1 - c][r] = pattern[r][c]
            }
        }
        return out.map { String(it) }
    }
}

