package com.ccwatermark.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.ccwatermark.app.data.WatermarkPreset
import com.ccwatermark.app.ui.theme.DialogSurface
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.TextSecondary

@Composable
fun NewPresetDialog(initialName: String = "", onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var invert by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("新建水印预设") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("预设名称") }, singleLine = true)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("智能黑白反色", style = MaterialTheme.typography.titleMedium)
                        Text("根据照片局部明暗自动切换", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(invert, { invert = it })
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(name, invert) }, enabled = name.isNotBlank()) { Text("保存预设", color = Mint) } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    )
}

@Composable
fun RenamePresetDialog(preset: WatermarkPreset, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(preset.id) { mutableStateOf(preset.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("重命名预设") },
        text = { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true) },
        confirmButton = { TextButton({ onConfirm(name) }, enabled = name.isNotBlank()) { Text("完成", color = Mint) } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    )
}
