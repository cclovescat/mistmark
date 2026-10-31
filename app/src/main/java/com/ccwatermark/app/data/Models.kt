package com.ccwatermark.app.data

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class WatermarkPreset(
    val id: String,
    val name: String,
    val imagePath: String,
    val autoInvert: Boolean = true,
    val width: Int = 0,
    val height: Int = 0,
    val opacity: Float? = null,
)

val WatermarkPreset.layerOpacity: Float
    get() = opacity ?: 0.55f

data class PhotoItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val state: PhotoState = PhotoState.READY,
    val outputPath: String? = null,
    val errorMessage: String? = null,
    val appliedPresets: List<WatermarkPreset> = emptyList(),
    val invertEnabled: Boolean = true,
)

enum class PhotoState { READY, WORKING, DONE, SAVED, ERROR }

enum class ThemeMode { SYSTEM, MOON, DAY }

enum class FontSizeOption(val label: String, val scale: Float) {
    ULTRA_SMALL("超小", 0.82f),
    SMALL("小", 0.92f),
    STANDARD("标准", 1f),
    LARGE("大", 1.14f),
}
