package com.ccwatermark.app.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WatermarkEngineTest {
    private lateinit var engine: WatermarkEngine
    private val bitmaps = mutableListOf<Bitmap>()

    @Before fun setUp() {
        engine = WatermarkEngine(ApplicationProvider.getApplicationContext())
    }

    @After fun tearDown() {
        bitmaps.forEach { if (!it.isRecycled) it.recycle() }
    }

    @Test fun transparentPhotoIsFlattenedOntoWhite() = runBlocking {
        val source = bitmap(2, 1, Color.TRANSPARENT)
        val watermark = bitmap(1, 1, Color.TRANSPARENT)

        val result = engine.render(source, watermark, 1f, false)

        assertEquals(Color.WHITE, result.getPixel(0, 0))
        assertEquals(Color.WHITE, result.getPixel(1, 0))
    }

    @Test fun visibleWatermarkStillBlendsAfterFlattening() = runBlocking {
        val source = bitmap(1, 1, Color.TRANSPARENT)
        val watermark = bitmap(1, 1, Color.BLACK)

        val result = engine.render(source, watermark, 0.5f, false)

        assertEquals(255, Color.alpha(result.getPixel(0, 0)))
        assertEquals(127, Color.red(result.getPixel(0, 0)))
    }

    @Test fun multipleWatermarksAreAppliedInOrder() = runBlocking {
        val source = bitmap(2, 1, Color.WHITE)
        val first = bitmap(1, 1, Color.RED)
        val second = bitmap(1, 1, Color.BLUE)

        var result = engine.render(source, first, 1f, false)
        result = engine.render(result, second, 1f, false)
        bitmaps += result

        assertEquals(Color.BLUE, result.getPixel(0, 0))
        assertEquals(Color.BLUE, result.getPixel(1, 0))
    }

    @Test fun inversionOnlySamplesVisibleWatermarkPixels() = runBlocking {
        val source = bitmap(2, 1, Color.BLACK).also {
            it.setPixel(1, 0, Color.WHITE)
        }
        val watermark = bitmap(2, 1, Color.TRANSPARENT).also {
            it.setPixel(0, 0, Color.BLACK)
        }

        val result = engine.render(source, watermark, 1f, true)
        bitmaps += result

        assertEquals(Color.WHITE, result.getPixel(0, 0))
        assertEquals(Color.WHITE, result.getPixel(1, 0))
    }

    @Test fun generatedPreviewIsSampledToRequestedSize() = runBlocking {
        val source = bitmap(3200, 2, Color.WHITE)
        val file = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "preview-test.png")
        FileOutputStream(file).use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        try {
            val preview = engine.previewFile(file.absolutePath, maxDimension = 1600)
            bitmaps += preview
            assertEquals(1600, preview.width)
        } finally {
            file.delete()
        }
    }

    @Test fun originalColorWatermarkIsNotInverted() = runBlocking {
        val source = bitmap(1, 1, Color.WHITE)
        val watermark = bitmap(1, 1, Color.GREEN)

        val result = engine.render(source, watermark, 1f, false)
        bitmaps += result

        assertEquals(Color.GREEN, result.getPixel(0, 0))
    }

    private fun bitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(color)
            bitmaps += it
        }
}
