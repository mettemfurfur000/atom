package org.shotrush.atom.content.workstation.knapping

import org.bukkit.inventory.ItemStack
import org.shotrush.atom.item.Items
import org.shotrush.atom.item.MoldShape
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

private const val N = 5


private fun idxColMajor(r: Int, c: Int): Int = r + c * N


data class Pattern(val rows: List<String>) {
    val height: Int = rows.size
    val width: Int = rows.maxOfOrNull { it.length } ?: 0

    init {
        require(height in 1..N) { "Pattern height must be 1..$N" }
        require(rows.isNotEmpty()) { "Pattern must have at least 1 row" }
        val w = rows.first().length
        require(w in 1..N) { "Pattern width must be 1..$N" }
        require(rows.all { it.length == w }) { "All rows must have equal length" }
    }

    operator fun get(r: Int): String = rows[r]
}


typealias Transform = (Pattern) -> Pattern

val invertX: Transform = { p -> Pattern(p.rows.map(String::reversed)) }
val invertY: Transform = { p -> Pattern(p.rows.asReversed()) }

val rotate90: Transform = { p ->
    val h = p.height
    val w = p.width
    val out = Array(w) { CharArray(h) }
    for (r in 0 until h) for (c in 0 until w) out[c][h - 1 - r] = p[r][c]
    Pattern(out.map { String(it) })
}

val rotateNeg90: Transform = { p ->
    val h = p.height
    val w = p.width
    val out = Array(w) { CharArray(h) }
    for (r in 0 until h) for (c in 0 until w) out[w - 1 - c][r] = p[r][c]
    Pattern(out.map { String(it) })
}


class PatternSetBuilder(
    private val filledChars: Set<Char> = setOf('#'),
) {
    private val patterns = mutableListOf<Pattern>()
    private var last: Pattern? = null

    
    fun rows(vararg r: String) {
        require(r.isNotEmpty()) { "Provide at least one row" }
        val w = r.first().length
        require(r.size in 1..N) { "Height must be 1..$N" }
        require(w in 1..N) { "Width must be 1..$N" }
        require(r.all { it.length == w }) { "All rows must have equal length" }
        pattern(Pattern(r.toList()), true)
    }

    fun pattern(p: Pattern, addLast: Boolean = false) {
        require(p.height in 1..N && p.width in 1..N) { "Pattern must be within $N x $N" }
        patterns += p
        if (addLast)
            last = p
    }

    fun transform(transform: Transform) {
        val prev = last ?: error("No previous pattern to transform")
        pattern(transform(prev))
    }

    fun build(): List<Pattern> = patterns.toList()
}

fun patternSet(block: PatternSetBuilder.() -> Unit): List<Pattern> =
    PatternSetBuilder().apply(block).build()

fun matchesPatternAnywhere(
    gridColumnMajor: List<Boolean>,
    pattern: Pattern,
    filledChars: Set<Char> = setOf('#'),
): Boolean {
    require(gridColumnMajor.size == N * N) { "grid must have size 25" }
    val ph = pattern.height
    val pw = pattern.width
    if (ph > N || pw > N) return false

    val maxR = N - ph
    val maxC = N - pw
    for (gr in 0..maxR) {
        for (gc in 0..maxC) {
            if (matchesAt(gridColumnMajor, pattern, gr, gc, filledChars) &&
                outsideRegionEmpty(gridColumnMajor, gr, gc, ph, pw)
            ) {
                return true
            }
        }
    }
    return false
}

private fun matchesAt(
    grid: List<Boolean>,
    pattern: Pattern,
    top: Int,
    left: Int,
    filledChars: Set<Char>,
): Boolean {
    for (r in 0 until pattern.height) {
        val prow = pattern[r]
        for (c in 0 until pattern.width) {
            val expected = prow[c] in filledChars
            val actual = grid[idxColMajor(top + r, left + c)]
            if (actual != expected) return false
        }
    }
    return true
}

private fun outsideRegionEmpty(
    grid: List<Boolean>,
    top: Int,
    left: Int,
    ph: Int,
    pw: Int,
): Boolean {
    for (r in 0 until N) {
        for (c in 0 until N) {
            val inside = r in top until (top + ph) && c in left until (left + pw)
            if (!inside && grid[idxColMajor(r, c)]) return false
        }
    }
    return true
}

data class KnappingRecipe(
    val id: String,
    val patterns: List<Pattern>,
    val result: MoldShape,
)

object KnappingRecipes {
    private val recipes = mutableMapOf<String, KnappingRecipe>()

    fun register(shape: MoldShape, block: PatternSetBuilder.() -> Unit) {
        val pats = patternSet(block)
        recipes[shape.id] = KnappingRecipe(id = shape.id, patterns = pats, result = shape)
    }

    fun getResult(gridColumnMajor: List<Boolean>): MoldShape? {
        for (recipe in recipes.values) {
            for (pattern in recipe.patterns) {
                
                val ok = if (pattern.height == N && pattern.width == N) {
                    matchesAt(gridColumnMajor, pattern, 0, 0, setOf('#')) &&
                            outsideRegionEmpty(gridColumnMajor, 0, 0, N, N) 
                } else {
                    matchesPatternAnywhere(gridColumnMajor, pattern, setOf('#'))
                }
                if (ok) {
                    return recipe.result
                }
            }
        }
        return null
    }

    init {
        register(MoldShape.Axe) {
            rows(
                "   # ",
                " ####",
                "#####",
                " ####",
                "   # ",
            )
            transform(invertX)
        }
        register(MoldShape.Pickaxe) {
            rows(
                " ### ",
                "#   #"
            )
        }
        register(MoldShape.Shovel) {
            rows(
                " # ",
                "###",
                "###",
                "###"
            )
            transform(invertY)
        }
        register(MoldShape.Sword) {
            rows(
                "   ##",
                "  ###",
                " ### ",
                "###  ",
                "##   "
            )
            transform(invertX)
        }
        register(MoldShape.Ingot) {
            rows(
                "#   #",
                "#####",
            )
        }
        register(MoldShape.Hammer) {
            rows(
                "#####",
                "#####",
                "#####",
            )
        }
        register(MoldShape.Knife) {
            rows(
                " #",
                "##",
                "##",
                "##",
            )
            transform(invertX)
        }
        register(MoldShape.Saw) {
            rows(
                "##  ",
                " ###",
                "  ##",
                "   #",
            )
            transform(invertX)
        }
    }
}