package com.ccwatermark.app.data

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ccwatermark.app.engine.WatermarkEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PresetStore(application)
    private val engine = WatermarkEngine(application)
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            File(getApplication<Application>().cacheDir, "rendered").deleteRecursively()
            File(getApplication<Application>().cacheDir, "updates").listFiles()
                ?.filter { it.name.endsWith(".part") }
                ?.forEach { it.delete() }
        }
        viewModelScope.launch {
            runCatching { store.load() }.onSuccess { settings ->
                _state.value = _state.value.copy(
                    presets = settings.presets,
                    selectedPresetIds = settings.selectedIds,
                    opacity = settings.opacity,
                    themeMode = settings.themeMode,
                    fontSize = settings.fontSize,
                    loaded = true,
                )
            }.onFailure { _state.value = _state.value.copy(loaded = true, message = "读取设置失败，请重试") }
        }
    }

    val selectedPresets: List<WatermarkPreset> get() = _state.value.selectedPresetIds.mapNotNull { id -> _state.value.presets.find { it.id == id } }

    fun addPhotos(uris: List<Uri>) {
        if (_state.value.busy || uris.isEmpty()) return
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    runCatching {
                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    PhotoItem(UUID.randomUUID().toString(), uri, displayName(uri))
                }
            }
            if (_state.value.busy) return@launch
            val nextPhotos = _state.value.photos + items
            _state.value = _state.value.copy(
                photos = nextPhotos,
                selectedPhotoId = _state.value.selectedPhotoId ?: items.firstOrNull()?.id,
            )
        }
    }

    fun selectPhoto(id: String) { _state.value = _state.value.copy(selectedPhotoId = id) }

    fun removePhoto(id: String) {
        if (_state.value.busy) return
        val photo = _state.value.photos.find { it.id == id }
        photo?.outputPath?.let { File(it).delete() }
        val remaining = _state.value.photos.filterNot { it.id == id }
        _state.value = _state.value.copy(photos = remaining, selectedPhotoId = if (_state.value.selectedPhotoId == id) remaining.firstOrNull()?.id else _state.value.selectedPhotoId)
    }

    fun clearPhotos() {
        if (_state.value.busy) return
        _state.value.photos.forEach { it.outputPath?.let { path -> File(path).delete() } }
        _state.value = _state.value.copy(photos = emptyList(), selectedPhotoId = null)
    }

    fun selectPreset(id: String) {
        if (_state.value.busy) return
        launchSafely {
            val selectedIds = store.toggleSelection(id)
            _state.value = _state.value.copy(selectedPresetIds = selectedIds)
        }
    }

    fun importWatermark(uri: Uri, name: String, autoInvert: Boolean) {
        if (_state.value.busy) return
        launchSafely {
            val preset = store.importWatermark(uri, name, autoInvert)
            _state.value = _state.value.copy(
                presets = _state.value.presets + preset,
                selectedPresetIds = _state.value.selectedPresetIds + preset.id,
                message = "预设已保存并选中",
            )
        }
    }

    fun renamePreset(preset: WatermarkPreset, name: String) {
        if (_state.value.busy) return
        launchSafely {
            if (name.isBlank()) return@launchSafely
            store.rename(preset, name)
            _state.value = _state.value.copy(presets = _state.value.presets.map { if (it.id == preset.id) it.copy(name = name.trim()) else it })
        }
    }

    fun togglePreset(preset: WatermarkPreset) {
        if (_state.value.busy) return
        launchSafely {
            val updated = store.toggle(preset)
            _state.value = _state.value.copy(presets = _state.value.presets.map { if (it.id == updated.id) updated else it })
        }
    }

    fun deletePreset(preset: WatermarkPreset) {
        if (_state.value.busy) return
        launchSafely {
            store.delete(preset)
            val affected = _state.value.photos.count { photo -> photo.appliedPresets.any { it.id == preset.id } }
            val remaining = _state.value.presets.filterNot { it.id == preset.id }
            _state.value = _state.value.copy(
                presets = remaining,
                selectedPresetIds = _state.value.selectedPresetIds.filterNot { it == preset.id },
                photos = _state.value.photos.map { photo ->
                    if (photo.appliedPresets.none { it.id == preset.id }) photo
                    else photo.copy(appliedPresets = photo.appliedPresets.filterNot { it.id == preset.id })
                },
                message = if (affected == 0) "预设已删除" else "预设已删除，并从 $affected 张照片的水印链中移除",
            )
        }
    }

    fun setOpacity(value: Float) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(opacity = value)
        launchSafely { store.setOpacity(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.value = _state.value.copy(themeMode = mode)
        launchSafely { store.setThemeMode(mode) }
    }

    fun setFontSize(option: FontSizeOption) {
        _state.value = _state.value.copy(fontSize = option)
        launchSafely { store.setFontSize(option) }
    }

    fun processAll() {
        val presets = selectedPresets
        if (presets.isEmpty()) {
            _state.value = _state.value.copy(message = "请选择至少一个水印预设")
            return
        }
        if (_state.value.photos.isEmpty() || _state.value.busy) return
        val opacity = _state.value.opacity
        _state.value = _state.value.copy(busy = true, progress = 0f)
        viewModelScope.launch {
            val photos = _state.value.photos
            photos.forEachIndexed { index, item ->
                renderPhoto(item, presets, item.invertEnabled)
                _state.value = _state.value.copy(progress = (index + 1f) / photos.size)
            }
            val failed = _state.value.photos.count { it.state == PhotoState.ERROR }
            _state.value = _state.value.copy(busy = false, message = if (failed == 0) "水印照片已生成，可以批量保存" else "$failed 张照片处理失败，其余可正常保存")
        }
    }

    fun reprocessSelectedPhoto() {
        if (_state.value.busy) return
        val item = _state.value.photos.find { it.id == _state.value.selectedPhotoId } ?: return
        val appliedIds = item.appliedPresets.map { it.id }.toSet()
        val additional = selectedPresets.filterNot { it.id in appliedIds }
        if (item.outputPath == null || item.appliedPresets.isEmpty()) {
            _state.value = _state.value.copy(message = "请先生成照片，再追加其他水印")
            return
        }
        if (additional.isEmpty()) {
            _state.value = _state.value.copy(message = "请选择尚未应用的水印预设")
            return
        }
        _state.value = _state.value.copy(busy = true, progress = 0f)
        viewModelScope.launch {
            renderPhoto(item, item.appliedPresets + additional, item.invertEnabled)
            _state.value = _state.value.copy(busy = false, progress = 1f, message = "已追加水印，相册将只保存最终版本")
        }
    }

    fun togglePhotoInvert(id: String) {
        if (_state.value.busy) return
        val item = _state.value.photos.find { it.id == id } ?: return
        val invertEnabled = !item.invertEnabled
        updatePhoto(id) { it.copy(invertEnabled = invertEnabled) }
        if (item.appliedPresets.isEmpty()) return
        _state.value = _state.value.copy(busy = true, progress = 0f)
        viewModelScope.launch {
            renderPhoto(item, item.appliedPresets, invertEnabled)
            _state.value = _state.value.copy(busy = false, progress = 1f)
        }
    }

    fun saveAll() {
        if (_state.value.busy) return
        val pending = _state.value.photos.filter { it.state == PhotoState.DONE }
        if (pending.isEmpty()) return
        _state.value = _state.value.copy(busy = true, progress = 0f)
        viewModelScope.launch {
            var failures = 0
            pending.forEachIndexed { index, item ->
                try {
                    saveToGallery(item)
                    updatePhoto(item.id) { it.copy(state = PhotoState.SAVED, errorMessage = null) }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    failures++
                    updatePhoto(item.id) { it.copy(errorMessage = error.message ?: "无法保存照片") }
                }
                _state.value = _state.value.copy(progress = (index + 1f) / pending.size)
            }
            _state.value = _state.value.copy(busy = false, message = if (failures == 0) "已保存 ${pending.size} 张照片到相册 / MistMark" else "已保存，${failures} 张失败，请重试")
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }

    private suspend fun renderPhoto(item: PhotoItem, presets: List<WatermarkPreset>, invertEnabled: Boolean) {
        val previousPath = item.outputPath
        updatePhoto(item.id) { it.copy(state = PhotoState.WORKING, errorMessage = null) }
        try {
            val output = engine.renderToCache(item.uri, presets, _state.value.opacity, invertEnabled)
            if (previousPath != null && previousPath != output.absolutePath) File(previousPath).delete()
            updatePhoto(item.id) {
                it.copy(
                    state = PhotoState.DONE,
                    outputPath = output.absolutePath,
                    appliedPresets = presets,
                    invertEnabled = invertEnabled,
                    errorMessage = null,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            updatePhoto(item.id) {
                it.copy(
                    state = PhotoState.ERROR,
                    outputPath = previousPath,
                    errorMessage = error.message ?: "无法处理照片",
                )
            }
        }
    }

    private suspend fun saveToGallery(item: PhotoItem) = withContext(Dispatchers.IO) {
        val path = item.outputPath ?: error("尚未生成")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${item.name.substringBeforeLast('.')}_mistmark.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/MistMark")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = getApplication<Application>().contentResolver
        var uri: Uri? = null
        var bitmap: android.graphics.Bitmap? = null
        try {
            val decoded = BitmapFactory.decodeFile(path) ?: error("无法读取已生成的照片")
            bitmap = decoded
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("无法创建照片")
            resolver.openOutputStream(uri!!)?.buffered()?.use {
                check(decoded.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it))
            } ?: error("无法写入照片")
            val published = resolver.update(uri!!, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            check(published > 0) { "无法发布照片到相册" }
        } catch (error: Throwable) {
            uri?.let { resolver.delete(it, null, null) }
            throw error
        } finally {
            bitmap?.recycle()
        }
    }

    private fun updatePhoto(id: String, transform: (PhotoItem) -> PhotoItem) {
        _state.value = _state.value.copy(photos = _state.value.photos.map { if (it.id == id) transform(it) else it })
    }

    private fun displayName(uri: Uri): String {
        val fallback = "photo_${System.currentTimeMillis()}.jpg"
        return try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column)?.takeIf { it.isNotBlank() } else null
            } ?: fallback
        } catch (error: Exception) {
            fallback
        }
    }

    override fun onCleared() {
        _state.value.photos.forEach { photo -> photo.outputPath?.let { File(it).delete() } }
        super.onCleared()
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = _state.value.copy(message = error.message ?: "操作失败，请重试")
            }
        }
    }
}

data class AppState(
    val loaded: Boolean = false,
    val presets: List<WatermarkPreset> = emptyList(),
    val selectedPresetIds: List<String> = emptyList(),
    val photos: List<PhotoItem> = emptyList(),
    val selectedPhotoId: String? = null,
    val opacity: Float = 0.55f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSizeOption = FontSizeOption.STANDARD,
    val busy: Boolean = false,
    val progress: Float = 0f,
    val message: String? = null,
)
