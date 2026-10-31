package com.ccwatermark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ccwatermark.app.data.AppState
import com.ccwatermark.app.data.PhotoItem
import com.ccwatermark.app.data.PhotoState
import com.ccwatermark.app.data.WatermarkPreset
import com.ccwatermark.app.ui.components.AccentButton
import com.ccwatermark.app.ui.components.GlassCard
import com.ccwatermark.app.ui.components.QuietButton
import com.ccwatermark.app.ui.components.SectionHeader
import com.ccwatermark.app.ui.components.WatermarkPreview
import com.ccwatermark.app.ui.theme.BrandMint
import com.ccwatermark.app.ui.theme.Lavender
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.PreviewSurface
import com.ccwatermark.app.ui.theme.SubtleFill
import com.ccwatermark.app.ui.theme.SubtleStroke
import com.ccwatermark.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AppState,
    selectedPresets: List<WatermarkPreset>,
    onOpacity: (Float) -> Unit,
    onAddPhotos: () -> Unit,
    onGoPresets: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectPhoto: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onClear: () -> Unit,
    onProcess: () -> Unit,
    onReprocess: () -> Unit,
    onToggleInvert: (String) -> Unit,
    onSave: () -> Unit,
) {
    val selectedPhoto = state.photos.find { it.id == state.selectedPhotoId } ?: state.photos.firstOrNull()
    var previewPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    val previewPhoto = state.photos.find { it.id == previewPhotoId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Column(Modifier.padding(top = 4.dp, bottom = 14.dp)) {
            Text("创作", style = MaterialTheme.typography.headlineLarge)
            Text(
                "${selectedPresets.size} 个水印 · ${state.photos.size} 张照片 · 仅在设备上处理",
                Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        GlassCard(
            Modifier.fillMaxWidth().height(310.dp),
            corner = 28,
            onClick = if (state.photos.isEmpty()) onAddPhotos else ({ previewPhotoId = selectedPhoto?.id }),
        ) {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(27.dp)).background(PreviewSurface),
            ) {
                if (previewPhoto == null) {
                    WatermarkPreview(selectedPhoto, selectedPresets, state.opacity, Modifier.fillMaxSize())
                } else if (selectedPhoto != null) {
                    AsyncImage(selectedPhoto.uri, "待处理照片", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
                if (selectedPhoto != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    ) {
                        Text(
                            when {
                                selectedPhoto.state == PhotoState.SAVED -> "已保存"
                                selectedPhoto.errorMessage != null -> "需要重试"
                                else -> "实时预览"
                            },
                            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            color = if (selectedPhoto.errorMessage != null) MaterialTheme.colorScheme.error else Mint,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = .58f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GridOn, null, Modifier.size(13.dp), tint = Color.White.copy(alpha = .82f))
                            Text("  原始尺寸", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (state.presets.isNotEmpty()) {
            SectionHeader(
                "水印组合",
                meta = "${selectedPresets.size}/${state.presets.size}",
                actionLabel = "管理",
                onAction = onGoPresets,
            )
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(state.presets, key = { it.id }) { item ->
                    val order = selectedPresets.indexOfFirst { it.id == item.id }
                    val selected = order >= 0
                    Row(
                        Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Mint.copy(alpha = .14f) else SubtleFill)
                            .border(1.dp, if (selected) Mint.copy(alpha = .58f) else SubtleStroke, RoundedCornerShape(14.dp))
                            .clickable { onSelectPreset(item.id) }
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selected) {
                            Box(Modifier.size(20.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${order + 1}", color = com.ccwatermark.app.ui.theme.Ink, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(
                            if (selected) "  ${item.name}" else item.name,
                            color = if (selected) Mint else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            GlassCard(Modifier.fillMaxWidth().padding(top = 20.dp), corner = 20, onClick = onGoPresets) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Lavender.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoFixHigh, null, tint = Lavender, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("先创建一枚水印", style = MaterialTheme.typography.titleMedium)
                        Text("导入透明 PNG，可保存多个并组合使用", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TextSecondary)
                }
            }
        }

        SectionHeader("透明度", meta = "${(state.opacity * 100).roundToInt()}%")
        GlassCard(Modifier.fillMaxWidth(), corner = 20) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Opacity, null, tint = Mint, modifier = Modifier.size(20.dp))
                Slider(
                    state.opacity,
                    onOpacity,
                    Modifier.weight(1f).padding(start = 12.dp),
                    valueRange = .05f..1f,
                    enabled = !state.busy,
                    colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = SubtleFill),
                )
            }
        }

        SectionHeader(
            if (state.photos.isEmpty()) "照片" else "照片队列",
            meta = if (state.photos.isEmpty()) null else "${state.photos.size} 张",
            actionLabel = if (state.photos.isEmpty()) null else "清空",
            onAction = if (state.photos.isEmpty()) null else onClear,
        )
        LazyRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            item(key = "add") {
                Box(
                    Modifier.size(76.dp).clip(RoundedCornerShape(18.dp))
                        .background(SubtleFill)
                        .border(1.dp, SubtleStroke, RoundedCornerShape(18.dp))
                        .clickable(onClick = onAddPhotos),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, "添加照片", tint = Mint, modifier = Modifier.size(28.dp))
                }
            }
            items(state.photos, key = { it.id }) { photo ->
                val selected = photo.id == selectedPhoto?.id
                Box(
                    Modifier.size(76.dp).clip(RoundedCornerShape(18.dp))
                        .border(if (selected) 2.5.dp else 1.dp, if (selected) Mint else SubtleStroke, RoundedCornerShape(18.dp))
                        .clickable { onSelectPhoto(photo.id); previewPhotoId = photo.id },
                ) {
                    AsyncImage(photo.uri, photo.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (photo.state == PhotoState.DONE || photo.state == PhotoState.SAVED || photo.state == PhotoState.ERROR) {
                        val failed = photo.state == PhotoState.ERROR || photo.errorMessage != null
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .background(if (failed) MaterialTheme.colorScheme.error else Color.Black.copy(alpha = .66f))
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when {
                                    photo.state == PhotoState.ERROR -> "处理失败"
                                    photo.errorMessage != null -> "保存失败"
                                    photo.state == PhotoState.SAVED -> "已保存"
                                    else -> "已生成"
                                },
                                color = if (failed) MaterialTheme.colorScheme.onError else BrandMint,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    if (photo.state == PhotoState.WORKING) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp), color = Mint, strokeWidth = 2.dp)
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).size(48.dp)
                            .clickable { onRemovePhoto(photo.id) },
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            "移除",
                            Modifier.padding(4.dp).size(22.dp).background(Color.Black.copy(alpha = .68f), CircleShape).padding(3.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
        selectedPhoto?.errorMessage?.let { message ->
            Surface(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = .10f),
            ) {
                Text(
                    message,
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.busy) {
            Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("正在处理，请保持应用开启", Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("${(state.progress * 100).toInt()}%", color = Mint, style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    { state.progress },
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(CircleShape),
                    color = Mint,
                    trackColor = SubtleFill,
                )
            }
        }

        val hasDone = state.photos.any { it.state == PhotoState.DONE }
        Spacer(Modifier.height(20.dp))
        if (hasDone) {
            AccentButton("保存全部到相册", Icons.Rounded.Download, onSave, Modifier.fillMaxWidth(), !state.busy)
            QuietButton(
                "按所选预设重新生成",
                Icons.Rounded.AutoFixHigh,
                onProcess,
                Modifier.fillMaxWidth().padding(top = 8.dp),
                state.photos.isNotEmpty() && selectedPresets.isNotEmpty() && !state.busy,
            )
        } else {
            AccentButton(
                if (state.photos.any { it.outputPath != null }) "按所选预设重新生成" else "生成水印照片",
                Icons.Rounded.AutoFixHigh,
                onProcess,
                Modifier.fillMaxWidth(),
                state.photos.isNotEmpty() && selectedPresets.isNotEmpty() && !state.busy,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(14.dp), tint = TextSecondary)
            Text("  保留原始分辨率 · 不上传任何照片", color = TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
        }
    }

    previewPhoto?.let { photo ->
        PhotoPreviewDialog(
            photo = photo,
            selectedPresets = selectedPresets,
            opacity = state.opacity,
            progress = state.progress,
            busy = state.busy,
            onDismiss = { previewPhotoId = null },
            onToggleInvert = { onToggleInvert(photo.id) },
            onReprocess = onReprocess,
        )
    }
}

@Composable
private fun PhotoPreviewDialog(
    photo: PhotoItem,
    selectedPresets: List<WatermarkPreset>,
    opacity: Float,
    progress: Float,
    busy: Boolean,
    onDismiss: () -> Unit,
    onToggleInvert: () -> Unit,
    onReprocess: () -> Unit,
) {
    val effectivePresets = photo.appliedPresets.ifEmpty { selectedPresets }
    val appliedIds = photo.appliedPresets.map { it.id }.toSet()
    val hasAdditionalPreset = selectedPresets.any { it.id !in appliedIds }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.90f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(photo.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                effectivePresets.isEmpty() -> "尚未选择水印"
                                photo.appliedPresets.isEmpty() -> "预览效果"
                                else -> "${photo.appliedPresets.size} 层水印 · 最终版本"
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onDismiss, Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, "关闭预览", tint = TextSecondary)
                    }
                }

                Box(Modifier.fillMaxWidth().weight(1f).background(PreviewSurface)) {
                    WatermarkPreview(photo, selectedPresets, opacity, Modifier.fillMaxSize())
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        if (effectivePresets.isEmpty()) "首页选择水印后可预览"
                        else "顺序：${effectivePresets.joinToString(" → ") { it.name }}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("此照片智能反色", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "仅影响允许反色的预设，原色水印保持不变",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            photo.invertEnabled,
                            { onToggleInvert() },
                            enabled = !busy && effectivePresets.any { it.autoInvert },
                        )
                    }
                    if (busy) {
                        LinearProgressIndicator(
                            { progress },
                            Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp).clip(CircleShape),
                            color = Mint,
                            trackColor = SubtleFill,
                        )
                    }
                    photo.errorMessage?.let {
                        Text(
                            it,
                            Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (photo.outputPath != null) {
                        AccentButton(
                            "追加所选水印",
                            Icons.Rounded.Add,
                            onReprocess,
                            Modifier.fillMaxWidth().padding(top = 14.dp),
                            !busy && hasAdditionalPreset,
                        )
                    }
                }
            }
        }
    }
}
