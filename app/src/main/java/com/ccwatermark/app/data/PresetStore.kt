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
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private val Context.store by preferencesDataStore("mistmark")

class PresetStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val presetsKey = stringPreferencesKey("presets")
    private val selectedKey = stringPreferencesKey("selected_preset")
    private val opacityKey = floatPreferencesKey("opacity")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val fontSizeKey = stringPreferencesKey("font_size")
    private val mutex = Mutex()

    suspend fun load(): StoredSettings = mutex.withLock { read() }

    private suspend fun read(): StoredSettings {
        val preferences = context.store.data.first()
        val presets = preferences[presetsKey]?.let {
            runCatching { json.decodeFromString(ListSerializer(WatermarkPreset.serializer()), it) }.getOrNull()
        }?.filter { File(it.imagePath).exists() } ?: emptyList()
        return StoredSettings(
            presets = presets,
            selectedId = preferences[selectedKey]?.takeIf { id -> presets.any { it.id == id } } ?: presets.firstOrNull()?.id,
            opacity = preferences[opacityKey]?.coerceIn(0.05f, 1f) ?: 0.55f,
            themeMode = preferences[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            fontSize = preferences[fontSizeKey]?.let { runCatching { FontSizeOption.valueOf(it) }.getOrNull() } ?: FontSizeOption.STANDARD,
        )
    }

    suspend fun importWatermark(uri: Uri, name: String, autoInvert: Boolean): WatermarkPreset = mutex.withLock {
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
        val preset = WatermarkPreset(id, name.trim().ifBlank { "未命名水印" }, output.absolutePath, autoInvert, bounds.outWidth, bounds.outHeight)
        val current = read()
        savePresets(current.presets + preset, preset.id)
        preset
    }

    suspend fun rename(preset: WatermarkPreset, name: String): Unit = mutex.withLock {
        val current = read()
        savePresets(current.presets.map { if (it.id == preset.id) it.copy(name = name.trim()) else it }, current.selectedId)
    }

    suspend fun toggle(preset: WatermarkPreset): WatermarkPreset = mutex.withLock {
        val current = read()
        val presets = current.presets.map { if (it.id == preset.id) it.copy(autoInvert = !it.autoInvert) else it }
        val updated = presets.first { it.id == preset.id }
        savePresets(presets, current.selectedId)
        updated
    }

    suspend fun delete(preset: WatermarkPreset) {
        mutex.withLock {
            val current = read()
            val remaining = current.presets.filterNot { it.id == preset.id }
            savePresets(remaining, if (current.selectedId == preset.id) remaining.firstOrNull()?.id else current.selectedId)
        }
        // Metadata commits first; a failed file cleanup can only leave an orphan, never a broken preset.
        File(preset.imagePath).delete()
    }

    suspend fun select(id: String): Unit = mutex.withLock {
        val current = read()
        savePresets(current.presets, id)
    }

    suspend fun setOpacity(value: Float): Unit = mutex.withLock {
        context.store.edit { it[opacityKey] = value.coerceIn(0.05f, 1f) }
    }

    suspend fun setThemeMode(mode: ThemeMode): Unit = mutex.withLock {
        context.store.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setFontSize(option: FontSizeOption): Unit = mutex.withLock {
        context.store.edit { it[fontSizeKey] = option.name }
    }

    private suspend fun savePresets(presets: List<WatermarkPreset>, selectedId: String?) {
        context.store.edit {
            it[presetsKey] = json.encodeToString(ListSerializer(WatermarkPreset.serializer()), presets)
            if (selectedId == null) it.remove(selectedKey) else it[selectedKey] = selectedId
        }
    }
}

data class StoredSettings(
    val presets: List<WatermarkPreset>,
    val selectedId: String?,
    val opacity: Float,
    val themeMode: ThemeMode,
    val fontSize: FontSizeOption,
)
