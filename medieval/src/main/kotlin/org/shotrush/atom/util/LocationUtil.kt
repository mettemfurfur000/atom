package org.shotrush.atom.util

object LocationUtil {
    private const val XZ_BITS = 27
    private const val Y_BITS = 10

    private const val XZ_MASK = (1L shl XZ_BITS) - 1L // 0x7FFFFFF
    private const val Y_MASK = (1L shl Y_BITS) - 1L   // 0x3FF

    // Offsets for signed mapping into unsigned bit fields
    private const val XZ_OFFSET = 1 shl (XZ_BITS - 1) // 2^26 = 67,108,864
    private const val Y_OFFSET = 128                  // shift [-128..] to [0..]

    private const val Y_MIN = -128
    private const val Y_MAX = 512 // logical cap per your requirement

    private val XZ_RANGE = -XZ_OFFSET..<XZ_OFFSET

    fun pack(x: Int, y: Int, z: Int): Long {
        if (x !in XZ_RANGE) throw IllegalArgumentException("x out of range: $x")
        if (z !in XZ_RANGE) throw IllegalArgumentException("z out of range: $z")
        if (y !in Y_MIN..Y_MAX) throw IllegalArgumentException("y out of range: $y")

        val ux = (x + XZ_OFFSET).toLong() and XZ_MASK
        val uy = (y + Y_OFFSET).toLong() and Y_MASK
        val uz = (z + XZ_OFFSET).toLong() and XZ_MASK

        // Layout: [X 27][Y 10][Z 27]
        return (ux shl (Y_BITS + XZ_BITS)) or
                (uy shl XZ_BITS) or
                (uz)
    }

    fun unpack(packed: Long): Triple<Int, Int, Int> {
        val uz = (packed and XZ_MASK).toInt()
        val uy = ((packed ushr XZ_BITS) and Y_MASK).toInt()
        val ux = ((packed ushr (XZ_BITS + Y_BITS)) and XZ_MASK).toInt()

        val x = ux - XZ_OFFSET
        val y = uy - Y_OFFSET
        val z = uz - XZ_OFFSET

        return Triple(x, y, z)
    }
}