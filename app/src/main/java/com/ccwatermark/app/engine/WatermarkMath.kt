package com.ccwatermark.app.engine

import kotlin.math.pow
import kotlin.math.sqrt

object WatermarkMath {
    fun tileOrigins(length: Int, tileLength: Int): IntArray {
        require(length >= 0) { "Image size must not be negative" }
        require(tileLength > 0) { "Tile size must be positive" }
        if (length == 0) return IntArray(0)
        return IntArray((length + tileLength - 1) / tileLength) { it * tileLength }
    }

    fun useWhiteInk(backgroundLuma: Float): Boolean = backgroundLuma < sqrt(0.0525f) - 0.05f

    fun luminance(red: Int, green: Int, blue: Int): Float =
        (0.2126f * red + 0.7152f * green + 0.0722f * blue) / 255f

    fun relativeLuminance(red: Int, green: Int, blue: Int): Float {
        val redLinear = linearChannel(red)
        val greenLinear = linearChannel(green)
        val blueLinear = linearChannel(blue)
        return 0.2126f * redLinear + 0.7152f * greenLinear + 0.0722f * blueLinear
    }

    private fun linearChannel(value: Int): Float {
        val channel = value / 255f
        return if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
    }
}
