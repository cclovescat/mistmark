package com.ccwatermark.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ccwatermark.app.data.AppState
import com.ccwatermark.app.data.WatermarkPreset
import com.ccwatermark.app.ui.components.GlassCard
import com.ccwatermark.app.ui.components.NewPresetDialog
import com.ccwatermark.app.ui.components.RenamePresetDialog
import com.ccwatermark.app.ui.components.SectionHeader
import com.ccwatermark.app.ui.theme.DialogSurface
import com.ccwatermark.app.ui.theme.Lavender
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.SubtleStroke
import com.ccwatermark.app.ui.theme.TextSecondary
import java.io.File

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
                    "上传一次，随时组合。每个预设独立保存反色偏好。",
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
}

@Composable
private fun PresetCard(
    preset: WatermarkPreset,
    selected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
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
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, SubtleStroke, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(File(preset.imagePath), preset.name, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                }
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
