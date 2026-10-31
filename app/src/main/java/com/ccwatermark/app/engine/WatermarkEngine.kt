package com.ccwatermark.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ccwatermark.app.data.WatermarkPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class WatermarkEngine(private val context: Context) {
    suspend fun preview(photo: Uri, presets: List<WatermarkPreset>, opacity: Float, invertEnabled: Boolean): Bitmap =
        withContext(Dispatchers.Default) {
            require(presets.isNotEmpty()) { "请选择至少一个水印预设" }
            val originalSize = readSize(photo)
            var result = decodeUri(photo, 1600)
            try {
                val originalOrientedWidth = if (readOrientation(photo) in ROTATED_ORIENTATIONS) originalSize.second else originalSize.first
                val previewScale = result.width.toFloat() / originalOrientedWidth
                for (preset in presets) {
                    val watermark = BitmapFactory.decodeFile(preset.imagePath)
                        ?: error("无法读取水印图")
                    val previewWatermark = if (previewScale < .99f) {
                        Bitmap.createScaledBitmap(
                            watermark,
                            maxOf(1, (watermark.width * previewScale).toInt()),
                            maxOf(1, (watermark.height * previewScale).toInt()),
                            true,
                        )
                    } else watermark
                    try {
                        result = render(result, previewWatermark, opacity, invertEnabled && preset.autoInvert)
                    } finally {
                        if (previewWatermark !== watermark) previewWatermark.recycle()
                        watermark.recycle()
                    }
                }
                result
            } catch (error: Throwable) {
                if (!result.isRecycled) result.recycle()
                throw error
            }
        }

    suspend fun previewFile(path: String, maxDimension: Int = 1600): Bitmap = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取预览图" }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        BitmapFactory.decodeFile(path, options) ?: error("无法读取预览图")
    }

    suspend fun renderToCache(photo: Uri, presets: List<WatermarkPreset>, opacity: Float, invertEnabled: Boolean): File =
        withContext(Dispatchers.Default) {
            require(presets.isNotEmpty()) { "请选择至少一个水印预设" }
            var result = decodeUri(photo, Int.MAX_VALUE)
            var output: File? = null
            try {
                for (preset in presets) {
                    val watermark = BitmapFactory.decodeFile(preset.imagePath)
                        ?: error("无法读取水印图")
                    result = try {
                        render(result, watermark, opacity, invertEnabled && preset.autoInvert)
                    } finally {
                        watermark.recycle()
                    }
                }
                val outputFile = File(context.cacheDir, "rendered/${System.nanoTime()}.png")
                output = outputFile
                outputFile.parentFile?.mkdirs()
                outputFile.outputStream().buffered().use {
                    check(result.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                outputFile
            } catch (error: Throwable) {
                output?.delete()
                throw error
            } finally {
                if (!result.isRecycled) result.recycle()
            }
        }

    suspend fun render(source: Bitmap, watermark: Bitmap, opacity: Float, autoInvert: Boolean): Bitmap {
        require(source.width > 0 && source.height > 0)
        require(watermark.width > 0 && watermark.height > 0)
        val prepared = if (source.hasAlpha()) {
            Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).also { flattened ->
                val canvas = Canvas(flattened)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(source, 0f, 0f, null)
                flattened.setHasAlpha(false)
                source.recycle()
            }
        } else source
        val output = if (prepared.isMutable) prepared else prepared.copy(Bitmap.Config.ARGB_8888, true)
        if (prepared !== output) prepared.recycle()
        try {
            if (!autoInvert) {
                val canvas = Canvas(output)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
                }
                for (y in WatermarkMath.tileOrigins(output.height, watermark.height)) {
                    for (x in WatermarkMath.tileOrigins(output.width, watermark.width)) {
                        currentCoroutineContext().ensureActive()
                        canvas.drawBitmap(watermark, x.toFloat(), y.toFloat(), paint)
                    }
                }
                return output
            }

            // Work one tile at a time instead of allocating another full-size pixel buffer.
            // This keeps memory use predictable for high-resolution photos and large batches.
            val wmPixels = IntArray(watermark.width * watermark.height)
            watermark.getPixels(wmPixels, 0, watermark.width, 0, 0, watermark.width, watermark.height)
            val tilePixels = IntArray(watermark.width * watermark.height)
            val globalAlpha = opacity.coerceIn(0f, 1f)
            for (top in WatermarkMath.tileOrigins(output.height, watermark.height)) {
                for (left in WatermarkMath.tileOrigins(output.width, watermark.width)) {
                    val right = min(left + watermark.width, output.width)
                    val bottom = min(top + watermark.height, output.height)
                    val tileWidth = right - left
                    val tileHeight = bottom - top
                    output.getPixels(tilePixels, 0, tileWidth, left, top, tileWidth, tileHeight)
                    val white = useWhiteInk(tilePixels, tileWidth, tileHeight, wmPixels, watermark.width)
                    for (y in 0 until tileHeight) {
                        currentCoroutineContext().ensureActive()
                        for (x in 0 until tileWidth) {
                            val wm = wmPixels[y * watermark.width + x]
                            val alpha = Color.alpha(wm) / 255f * globalAlpha
                            if (alpha <= 0f) continue
                            val index = y * tileWidth + x
                            val background = tilePixels[index]
                            // Transparent areas remain untouched; visible ink is white on dark and black on light areas.
                            val ink = if (white) 255 else 0
                            tilePixels[index] = Color.argb(
                                255,
                                blend(Color.red(background), ink, alpha),
                                blend(Color.green(background), ink, alpha),
                                blend(Color.blue(background), ink, alpha),
                            )
                        }
                    }
                    output.setPixels(tilePixels, 0, tileWidth, left, top, tileWidth, tileHeight)
                }
            }
            return output
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
    }

    private fun blend(background: Int, ink: Int, alpha: Float): Int =
        (background + (ink - background) * alpha).toInt().coerceIn(0, 255)

    private fun useWhiteInk(pixels: IntArray, width: Int, height: Int, watermarkPixels: IntArray, watermarkWidth: Int): Boolean {
        val stepX = maxOf(1, width / 32)
        val stepY = maxOf(1, height / 32)
        val histogram = DoubleArray(256)
        var totalWeight = 0.0
        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val alpha = Color.alpha(watermarkPixels[y * watermarkWidth + x])
                if (alpha == 0) continue
                val weight = alpha / 255.0
                val color = pixels[y * width + x]
                val luminance = WatermarkMath.relativeLuminance(Color.red(color), Color.green(color), Color.blue(color))
                val bin = (luminance * 255).toInt().coerceIn(0, 255)
                histogram[bin] += weight
                totalWeight += weight
            }
        }
        if (totalWeight == 0.0) return false
        var accumulated = 0.0
        var median = 0
        for (index in histogram.indices) {
            accumulated += histogram[index]
            if (accumulated >= totalWeight / 2) {
                median = index
                break
            }
        }
        return WatermarkMath.useWhiteInk(median / 255f)
    }

    private fun readSize(uri: Uri): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取照片" }
        return bounds.outWidth to bounds.outHeight
    }

    private fun decodeUri(uri: Uri, maxDimension: Int): Bitmap {
        val bounds = readSize(uri)
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
            inSampleSize = calculateSampleSize(bounds.first, bounds.second, maxDimension)
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("无法读取照片")
        val orientation = readOrientation(uri)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        }
        if (matrix.isIdentity) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private fun readOrientation(uri: Uri): Int = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > maxDimension) sample *= 2
        return sample
    }

    companion object {
        private val ROTATED_ORIENTATIONS = setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
        )
    }
}
