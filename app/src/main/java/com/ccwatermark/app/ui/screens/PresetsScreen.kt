package com.ccwatermark.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ccwatermark.app.data.AppState
import com.ccwatermark.app.data.WatermarkPreset
import com.ccwatermark.app.engine.WatermarkEngine
import com.ccwatermark.app.ui.components.GlassCard
import com.ccwatermark.app.ui.components.NewPresetDialog
import com.ccwatermark.app.ui.components.RenamePresetDialog
import com.ccwatermark.app.ui.components.SectionHeader
import com.ccwatermark.app.ui.theme.DialogSurface
import com.ccwatermark.app.ui.theme.Lavender
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.SubtleStroke
import com.ccwatermark.app.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException

@Composable
fun PresetsScreen(
    state: AppState,
    onImport: (Uri, String, Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onRename: (WatermarkPreset, String) -> Unit,
    onToggle: (WatermarkPreset) -> Unit,
    onDelete: (WatermarkPreset) -> Unit,
) {
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var renaming by remember { mutableStateOf<WatermarkPreset?>(null) }
    var deleting by remember { mutableStateOf<WatermarkPreset?>(null) }
    var previewing by remember { mutableStateOf<WatermarkPreset?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pendingUri = uri }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Text("水印库", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "上传一次，随时组合。每个预设独立保存反色与透明度偏好。",
                    Modifier.padding(top = 4.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item(key = "add") {
            GlassCard(
                Modifier.fillMaxWidth(),
                corner = 22,
                onClick = { if (!state.busy) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).background(Mint.copy(alpha = .13f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Mint)
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("导入水印", style = MaterialTheme.typography.titleMedium)
                        Text("推荐透明背景 PNG，保留原始像素", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Rounded.UploadFile, null, tint = TextSecondary)
                }
            }
        }
        item(key = "library-title") {
            SectionHeader(
                "已保存",
                meta = "${state.presets.size} 个 · 已选 ${state.selectedPresetIds.size}",
            )
        }
        if (state.presets.isEmpty()) {
            item(key = "empty") {
                GlassCard(Modifier.fillMaxWidth(), corner = 22) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 42.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.UploadFile, null, Modifier.size(42.dp), tint = Lavender.copy(alpha = .72f))
                        Text("水印库还是空的", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "从上方导入第一张水印图",
                            Modifier.padding(top = 5.dp),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        items(state.presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                selected = preset.id in state.selectedPresetIds,
                busy = state.busy,
                onSelect = { onSelect(preset.id) },
                onPreview = { previewing = preset },
                onRename = { renaming = preset },
                onToggle = { onToggle(preset) },
                onDelete = { deleting = preset },
            )
        }
        item(key = "bottom-space") { Spacer(Modifier.height(4.dp)) }
    }

    pendingUri?.let { uri ->
        NewPresetDialog(
            initialName = "我的水印 ${state.presets.size + 1}",
            onDismiss = { pendingUri = null },
            onConfirm = { name, invert ->
                onImport(uri, name, invert)
                pendingUri = null
            },
        )
    }
    renaming?.let { preset ->
        RenamePresetDialog(preset, onDismiss = { renaming = null }, onConfirm = { name -> onRename(preset, name); renaming = null })
    }
    deleting?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = DialogSurface,
            title = { Text("删除“${preset.name}”？") },
            text = { Text("只会删除应用内的水印预设，不影响已经处理好的照片。") },
            confirmButton = { TextButton({ onDelete(preset); deleting = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ deleting = null }) { Text("取消") } },
        )
    }
    previewing?.let { preset ->
        PresetPreviewDialog(preset = preset, onDismiss = { previewing = null })
    }
}

private data class ThumbnailState(
    val bitmap: Bitmap? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)

@Composable
private fun PresetThumbnail(preset: WatermarkPreset, onClick: () -> Unit) {
    val context = LocalContext.current
    val engine = remember(context.applicationContext) { WatermarkEngine(context.applicationContext) }
    var thumbnail by remember(preset.imagePath) { mutableStateOf(ThumbnailState(loading = true)) }
    LaunchedEffect(preset.imagePath) {
        thumbnail = ThumbnailState(loading = true)
        thumbnail = try {
            ThumbnailState(bitmap = engine.previewFile(preset.imagePath, maxDimension = 320))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            ThumbnailState(failed = true)
        }
    }
    Box(
        Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF858C96))
            .border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TransparencyGrid(Modifier.matchParentSize())
        when {
            thumbnail.bitmap != null -> PresetBitmapImage(
                bitmap = thumbnail.bitmap!!,
                name = preset.name,
                modifier = Modifier.fillMaxSize().padding(5.dp).graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                },
            )
            thumbnail.loading -> CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
            thumbnail.failed -> Icon(
                Icons.Rounded.BrokenImage,
                "水印预览加载失败",
                modifier = Modifier.size(22.dp),
                tint = Color.White.copy(alpha = .9f),
            )
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                .background(Color.Black.copy(alpha = .55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Fullscreen, "查看大图", Modifier.size(12.dp), tint = Color.White)
        }
    }
}

@Composable
private fun PresetBitmapImage(bitmap: Bitmap, name: String, modifier: Modifier) {
    DisposableEffect(bitmap) {
        onDispose { if (!bitmap.isRecycled) bitmap.recycle() }
    }
    Image(
        bitmap.asImageBitmap(),
        name,
        modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun TransparencyGrid(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cell = 8.dp.toPx()
        val light = Color(0xFFA2A8B1)
        val dark = Color(0xFF737A84)
        var row = 0
        var y = 0f
        while (y < size.height) {
            var column = 0
            var x = 0f
            while (x < size.width) {
                drawRect(
                    color = if ((row + column) % 2 == 0) light else dark,
                    topLeft = Offset(x, y),
                    size = Size(minOf(cell, size.width - x), minOf(cell, size.height - y)),
                )
                x += cell
                column++
            }
            y += cell
            row++
        }
    }
}

@Composable
private fun PresetPreviewDialog(preset: WatermarkPreset, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val engine = remember(context.applicationContext) { WatermarkEngine(context.applicationContext) }
    var preview by remember(preset.imagePath) { mutableStateOf(ThumbnailState(loading = true)) }
    var zoom by rememberSaveable(preset.id) { mutableStateOf(1f) }
    var backdrop by rememberSaveable(preset.id) { mutableStateOf(0) }
    LaunchedEffect(preset.imagePath) {
        preview = try {
            ThumbnailState(bitmap = engine.previewFile(preset.imagePath, maxDimension = 1600))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            ThumbnailState(failed = true)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f),
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
                        Text(preset.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${preset.width} × ${preset.height} px · 原始尺寸平铺",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onDismiss, Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, "关闭预览", tint = TextSecondary)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when (backdrop) {
                                1 -> Color.White
                                2 -> Color(0xFF16181D)
                                else -> Color(0xFF858C96)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (backdrop == 0) TransparencyGrid(Modifier.matchParentSize())
                    when {
                        preview.bitmap != null -> PresetBitmapImage(
                            bitmap = preview.bitmap!!,
                            name = preset.name,
                            modifier = Modifier.fillMaxSize().padding(16.dp).graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                            },
                        )
                        preview.failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.BrokenImage, null, tint = Color.White, modifier = Modifier.size(30.dp))
                            Text("水印文件读取失败", Modifier.padding(top = 8.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                        else -> CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.dp)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("背景", Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        listOf("棋盘", "白色", "深色").forEachIndexed { index, label ->
                            PreviewChoice(label, backdrop == index) { backdrop = index }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("缩放", Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        listOf(1f, 1.5f, 2f).forEach { value ->
                            PreviewChoice("${(value * 100).toInt()}%", zoom == value) { zoom = value }
                        }
                    }
                    Text(
                        "150% 和 200% 会裁剪边缘，仅用于检查细节，不影响导出效果",
                        Modifier.padding(top = 8.dp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) Mint.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) Mint.copy(alpha = .58f) else MaterialTheme.colorScheme.outline.copy(alpha = .18f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier.heightIn(min = 44.dp).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (selected) Mint else TextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: WatermarkPreset,
    selected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    GlassCard(
        Modifier.fillMaxWidth(),
        corner = 22,
        onClick = onSelect,
        highlighted = selected,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PresetThumbnail(preset, onPreview)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, "已选择", Modifier.padding(start = 6.dp).size(17.dp), tint = Mint)
                        }
                    }
                    Text(
                        "${preset.width} × ${preset.height} px · 原始尺寸平铺",
                        Modifier.padding(top = 4.dp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "更多", tint = TextSecondary) }
                    DropdownMenu(menu, { menu = false }, Modifier.background(DialogSurface)) {
                        DropdownMenuItem({ Text("重命名") }, { menu = false; onRename() })
                        DropdownMenuItem(
                            { Text("删除预设", color = MaterialTheme.colorScheme.error) },
                            { menu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("智能黑白反色", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (preset.autoInvert) "按覆盖区域自动选择黑印或白印" else "保留上传图原貌",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(preset.autoInvert, { onToggle() }, enabled = !busy)
            }
        }
    }
}
