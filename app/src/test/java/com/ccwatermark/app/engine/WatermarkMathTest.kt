package com.ccwatermark.app.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkMathTest {
    @Test fun exactTilesCoverWholeImage() {
        assertArrayEquals(intArrayOf(0, 128, 256), WatermarkMath.tileOrigins(384, 128))
    }

    @Test fun partialEdgeTileIsIncluded() {
        assertArrayEquals(intArrayOf(0, 128, 256), WatermarkMath.tileOrigins(300, 128))
    }

    @Test fun tileLargerThanImageStartsAtOrigin() {
        assertArrayEquals(intArrayOf(0), WatermarkMath.tileOrigins(72, 128))
    }

    @Test fun emptyImageHasNoTiles() {
        assertArrayEquals(IntArray(0), WatermarkMath.tileOrigins(0, 128))
    }

    @Test fun localLuminanceChoosesContrastingInk() {
        assertTrue(WatermarkMath.useWhiteInk(0.2f))
        assertFalse(WatermarkMath.useWhiteInk(0.8f))
    }

    @Test fun luminanceUsesPerceptualWeights() {
        assertEquals(1f, WatermarkMath.luminance(255, 255, 255), 0.0001f)
        assertEquals(0f, WatermarkMath.luminance(0, 0, 0), 0.0001f)
    }
}
