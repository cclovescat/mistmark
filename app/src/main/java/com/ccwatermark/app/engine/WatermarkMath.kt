package com.ccwatermark.app.engine

object WatermarkMath {
    fun tileOrigins(length: Int, tileLength: Int): IntArray {
        require(length >= 0) { "Image size must not be negative" }
        require(tileLength > 0) { "Tile size must be positive" }
        if (length == 0) return IntArray(0)
        return IntArray((length + tileLength - 1) / tileLength) { it * tileLength }
    }

    fun useWhiteInk(backgroundLuma: Float): Boolean = backgroundLuma < 0.5f

    fun luminance(red: Int, green: Int, blue: Int): Float =
        (0.2126f * red + 0.7152f * green + 0.0722f * blue) / 255f
}
