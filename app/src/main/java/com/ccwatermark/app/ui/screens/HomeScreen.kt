package com.ccwatermark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ccwatermark.app.data.AppState
import com.ccwatermark.app.data.PhotoState
import com.ccwatermark.app.data.WatermarkPreset
import com.ccwatermark.app.ui.components.AccentButton
import com.ccwatermark.app.ui.components.GlassCard
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
    preset: WatermarkPreset?,
    onOpacity: (Float) -> Unit,
    onAddPhotos: () -> Unit,
    onGoPresets: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectPhoto: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onClear: () -> Unit,
    onProcess: () -> Unit,
    onSave: () -> Unit,
) {
    val selectedPhoto = state.photos.find { it.id == state.selectedPhotoId } ?: state.photos.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp).height(340.dp), corner = 28, onClick = if (state.photos.isEmpty()) onAddPhotos else null) {
            Box(Modifier.fillMaxSize().padding(10.dp).clip(RoundedCornerShape(22.dp)).background(PreviewSurface)) {
                WatermarkPreview(selectedPhoto, preset, state.opacity, Modifier.fillMaxSize())
                if (selectedPhoto != null) {
                    GlassCard(Modifier.align(Alignment.TopStart).padding(12.dp), corner = 20) {
                        Text(if (selectedPhoto.state == PhotoState.SAVED) "已保存" else "实时预览", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Mint, style = MaterialTheme.typography.labelSmall)
                    }
                    GlassCard(Modifier.align(Alignment.BottomEnd).padding(12.dp), corner = 20) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GridOn, null, Modifier.size(13.dp), tint = TextSecondary)
                            Text("  原始尺寸平铺", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), corner = 20) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Opacity, null, tint = Mint, modifier = Modifier.size(20.dp))
                    Text("  水印透明度", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("${(state.opacity * 100).roundToInt()}%", color = Mint, style = MaterialTheme.typography.labelLarge)
                }
                Slider(
                    state.opacity,
                    onOpacity,
                    Modifier.fillMaxWidth(),
                    valueRange = .05f..1f,
                    enabled = !state.busy,
                    colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = SubtleFill),
                )
            }
        }

        if (state.presets.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("当前水印", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("管理预设", Modifier.clickable(onClick = onGoPresets), color = Mint, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                state.presets.forEach { item ->
                    val selected = item.id == preset?.id
                    Row(
                        Modifier.clip(RoundedCornerShape(15.dp)).background(if (selected) Mint.copy(alpha = .13f) else SubtleFill)
                            .border(1.dp, if (selected) Mint.copy(alpha = .55f) else SubtleStroke, RoundedCornerShape(15.dp))
                            .clickable { onSelectPreset(item.id) }.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), tint = Mint)
                        Text(if (selected) "  ${item.name}" else item.name, color = if (selected) Mint else TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), corner = 18, onClick = onGoPresets) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoFixHigh, null, tint = Lavender)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("先创建你的第一个水印", style = MaterialTheme.typography.titleMedium)
                        Text("上传一张水印图，之后可反复使用", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TextSecondary)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.photos.isEmpty()) "照片队列" else "照片队列 · ${state.photos.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (state.photos.isNotEmpty()) Text("清空", Modifier.clickable(onClick = onClear), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(18.dp)).background(SubtleFill).border(1.dp, SubtleStroke, RoundedCornerShape(18.dp)).clickable(onClick = onAddPhotos), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "添加照片", tint = Mint, modifier = Modifier.size(28.dp))
            }
            state.photos.forEach { photo ->
                val selected = photo.id == selectedPhoto?.id
                Box(Modifier.size(68.dp).clip(RoundedCornerShape(18.dp)).border(2.dp, if (selected) Mint else Color.Transparent, RoundedCornerShape(18.dp)).clickable { onSelectPhoto(photo.id) }) {
                    AsyncImage(photo.uri, photo.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (photo.state == PhotoState.DONE || photo.state == PhotoState.SAVED || photo.state == PhotoState.ERROR) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(if (photo.state == PhotoState.ERROR) MaterialTheme.colorScheme.error else Color.Black.copy(alpha = .65f)).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text(when (photo.state) {
                                PhotoState.SAVED -> "已保存"
                                PhotoState.ERROR -> "处理失败"
                                else -> "已生成"
                            }, color = if (photo.state == PhotoState.ERROR) MaterialTheme.colorScheme.onError else BrandMint, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (photo.state == PhotoState.WORKING) CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp), color = Mint, strokeWidth = 2.dp)
                    Icon(Icons.Rounded.Close, "移除", Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Color.Black.copy(alpha = .65f), CircleShape).clickable { onRemovePhoto(photo.id) }.padding(3.dp), tint = Color.White)
                }
            }
        }
        if (selectedPhoto?.state == PhotoState.ERROR) {
            Text(selectedPhoto.errorMessage ?: "照片处理失败，请重新生成", Modifier.fillMaxWidth().padding(top = 10.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.busy) {
            Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Row { Text("正在处理，请保持应用开启", Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.bodyMedium); Text("${(state.progress * 100).toInt()}%", color = Mint, style = MaterialTheme.typography.bodyMedium) }
                LinearProgressIndicator({ state.progress }, Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp).clip(CircleShape), color = Mint, trackColor = SubtleFill)
            }
        }
        val hasDone = state.photos.any { it.state == PhotoState.DONE }
        Spacer(Modifier.height(20.dp))
        if (hasDone) {
            AccentButton("保存全部到相册", Icons.Rounded.Download, onSave, Modifier.fillMaxWidth(), !state.busy)
        } else {
            AccentButton(if (state.photos.any { it.state == PhotoState.SAVED }) "重新生成水印照片" else "生成水印照片", Icons.Rounded.AutoFixHigh, onProcess, Modifier.fillMaxWidth(), state.photos.isNotEmpty() && preset != null && !state.busy)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(14.dp), tint = TextSecondary)
            Text("  保留原始分辨率 · 不上传任何照片", color = TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
        }
    }
}
