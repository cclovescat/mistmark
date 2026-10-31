package com.ccwatermark.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private val Context.store by preferencesDataStore("mistmark")

class PresetStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val presetsKey = stringPreferencesKey("presets")
    private val selectedKey = stringPreferencesKey("selected_preset")
    private val selectedPresetsKey = stringPreferencesKey("selected_presets")
    private val opacityKey = floatPreferencesKey("opacity")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val fontSizeKey = stringPreferencesKey("font_size")
    private val mutex = Mutex()

    suspend fun load(): StoredSettings = mutex.withLock { read() }

    private suspend fun read(): StoredSettings {
        val preferences = context.store.data.first()
        val defaultOpacity = preferences[opacityKey]?.coerceIn(0.05f, 1f) ?: 0.55f
        val presets = preferences[presetsKey]?.let {
            runCatching { json.decodeFromString(ListSerializer(WatermarkPreset.serializer()), it) }.getOrNull()
        }?.filter { File(it.imagePath).exists() }
            ?.map { it.copy(opacity = it.opacity?.coerceIn(0.05f, 1f) ?: defaultOpacity) }
            ?: emptyList()
        val storedIds = preferences[selectedPresetsKey]?.let {
            runCatching { json.decodeFromString(ListSerializer(String.serializer()), it) }.getOrNull()
        } ?: listOfNotNull(preferences[selectedKey])
        val selectedIds = storedIds.distinct().filter { id -> presets.any { it.id == id } }
        return StoredSettings(
            presets = presets,
            selectedIds = selectedIds,
            opacity = defaultOpacity,
            themeMode = preferences[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            fontSize = preferences[fontSizeKey]?.let { runCatching { FontSizeOption.valueOf(it) }.getOrNull() } ?: FontSizeOption.STANDARD,
        )
    }

    suspend fun importWatermark(uri: Uri, name: String, autoInvert: Boolean, opacity: Float): WatermarkPreset = mutex.withLock {
        val directory = File(context.filesDir, "watermarks").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val output = File(directory, "$id.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            output.outputStream().buffered().use { input.copyTo(it) }
        } ?: error("无法读取水印图")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(output.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            output.delete()
            error("请选择有效的 PNG 或 JPG 水印图")
        }
        val preset = WatermarkPreset(
            id = id,
            name = name.trim().ifBlank { "未命名水印" },
            imagePath = output.absolutePath,
            autoInvert = autoInvert,
            width = bounds.outWidth,
            height = bounds.outHeight,
            opacity = opacity.coerceIn(0.05f, 1f),
        )
        val current = read()
        savePresets(current.presets + preset, current.selectedIds + preset.id)
        preset
    }

    suspend fun rename(preset: WatermarkPreset, name: String): Unit = mutex.withLock {
        val current = read()
        savePresets(current.presets.map { if (it.id == preset.id) it.copy(name = name.trim()) else it }, current.selectedIds)
    }

    suspend fun toggle(preset: WatermarkPreset): WatermarkPreset = mutex.withLock {
        val current = read()
        val presets = current.presets.map { if (it.id == preset.id) it.copy(autoInvert = !it.autoInvert) else it }
        val updated = presets.first { it.id == preset.id }
        savePresets(presets, current.selectedIds)
        updated
    }

    suspend fun delete(preset: WatermarkPreset) {
        mutex.withLock {
            val current = read()
            val remaining = current.presets.filterNot { it.id == preset.id }
            savePresets(remaining, current.selectedIds.filterNot { it == preset.id })
        }
        // Metadata commits first; a failed file cleanup can only leave an orphan, never a broken preset.
        File(preset.imagePath).delete()
    }

    suspend fun toggleSelection(id: String): List<String> = mutex.withLock {
        val current = read()
        require(current.presets.any { it.id == id }) { "水印预设不存在" }
        val selectedIds = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
        savePresets(current.presets, selectedIds)
        selectedIds
    }

    suspend fun setOpacity(value: Float): Unit = mutex.withLock {
        context.store.edit { it[opacityKey] = value.coerceIn(0.05f, 1f) }
    }

    suspend fun setPresetOpacity(id: String, value: Float): Unit = mutex.withLock {
        val current = read()
        require(current.presets.any { it.id == id }) { "水印预设不存在" }
        savePresets(
            current.presets.map { if (it.id == id) it.copy(opacity = value.coerceIn(0.05f, 1f)) else it },
            current.selectedIds,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode): Unit = mutex.withLock {
        context.store.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setFontSize(option: FontSizeOption): Unit = mutex.withLock {
        context.store.edit { it[fontSizeKey] = option.name }
    }

    private suspend fun savePresets(presets: List<WatermarkPreset>, selectedIds: List<String>) {
        context.store.edit {
            it[presetsKey] = json.encodeToString(ListSerializer(WatermarkPreset.serializer()), presets)
            it[selectedPresetsKey] = json.encodeToString(ListSerializer(String.serializer()), selectedIds)
            it.remove(selectedKey)
        }
    }
}

data class StoredSettings(
    val presets: List<WatermarkPreset>,
    val selectedIds: List<String>,
    val opacity: Float,
    val themeMode: ThemeMode,
    val fontSize: FontSizeOption,
)
