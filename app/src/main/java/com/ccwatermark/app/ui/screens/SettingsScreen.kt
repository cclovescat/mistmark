package com.ccwatermark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccwatermark.app.data.AppState
import com.ccwatermark.app.data.FontSizeOption
import com.ccwatermark.app.data.ThemeMode
import com.ccwatermark.app.ui.components.AccentButton
import com.ccwatermark.app.ui.components.GlassCard
import com.ccwatermark.app.ui.theme.DialogSurface
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.SubtleFill
import com.ccwatermark.app.ui.theme.TextSecondary
import com.ccwatermark.app.update.UpdateStatus
import com.ccwatermark.app.update.UpdateUiState
import com.ccwatermark.app.update.UpdateViewModel

@Composable
fun SettingsScreen(
    state: AppState,
    onThemeMode: (ThemeMode) -> Unit,
    onFontSize: (FontSizeOption) -> Unit,
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val updateState by updateViewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("设置", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.headlineLarge)
        Text("调整界面主题与字体显示。", Modifier.padding(top = 6.dp, bottom = 20.dp), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)

        GlassCard(Modifier.fillMaxWidth(), corner = 24) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, null, tint = Mint, modifier = Modifier.size(21.dp))
                    Text("  界面模式", style = MaterialTheme.typography.titleMedium)
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = state.themeMode == mode
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.MOON -> "月色"
                            ThemeMode.DAY -> "日光"
                        }
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                            ThemeMode.MOON -> Icons.Rounded.DarkMode
                            ThemeMode.DAY -> Icons.Rounded.LightMode
                        }
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                                .background(if (selected) Mint.copy(alpha = .14f) else SubtleFill)
                                .clickable { onThemeMode(mode) }.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(icon, label, tint = if (selected) Mint else TextSecondary, modifier = Modifier.size(22.dp))
                            Text(label, Modifier.padding(top = 6.dp), color = if (selected) Mint else TextSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Text(
                    when (state.themeMode) {
                        ThemeMode.SYSTEM -> "自动读取设备的暗色模式设置"
                        ThemeMode.MOON -> "柔和深色，适合低光环境"
                        ThemeMode.DAY -> "清透明亮，适合日间使用"
                    },
                    Modifier.padding(top = 12.dp), color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text("字体", Modifier.padding(top = 25.dp, bottom = 12.dp), style = MaterialTheme.typography.titleMedium)
        GlassCard(Modifier.fillMaxWidth(), corner = 24) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TextFields, null, tint = Mint, modifier = Modifier.size(21.dp))
                    Text("  字体大小", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(state.fontSize.label, color = Mint, style = MaterialTheme.typography.labelLarge)
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FontSizeOption.entries.forEach { option ->
                        val selected = state.fontSize == option
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                                .background(if (selected) Mint.copy(alpha = .14f) else SubtleFill)
                                .clickable { onFontSize(option) }.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(option.label, color = if (selected) Mint else TextSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Text("雾印，让每一次创作都清晰可读", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text("软件更新", Modifier.padding(top = 25.dp, bottom = 12.dp), style = MaterialTheme.typography.titleMedium)
        GlassCard(Modifier.fillMaxWidth(), corner = 22) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SystemUpdate, null, tint = Mint)
                    Text("  当前版本 ${updateState.currentVersion}", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    when (updateState.status) {
                        UpdateStatus.IDLE -> "通过 GitHub Releases 检查新版本"
                        UpdateStatus.CHECKING -> "正在检查更新…"
                        UpdateStatus.LATEST -> "当前已是最新版本"
                        UpdateStatus.AVAILABLE -> "发现新版本 ${updateState.release?.version ?: ""}"
                        UpdateStatus.DOWNLOADING -> "正在下载 ${(updateState.progress * 100).toInt()}%"
                        UpdateStatus.READY_TO_INSTALL -> "安装包已下载，可以安装"
                        UpdateStatus.ERROR -> updateState.errorMessage ?: "操作失败，请重试"
                    },
                    Modifier.padding(top = 8.dp),
                    color = if (updateState.status == UpdateStatus.ERROR) MaterialTheme.colorScheme.error else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                AccentButton(
                    when (updateState.status) {
                        UpdateStatus.CHECKING -> "正在检查…"
                        UpdateStatus.LATEST -> "重新检查"
                        UpdateStatus.AVAILABLE -> "查看新版本"
                        UpdateStatus.DOWNLOADING -> "正在下载…"
                        UpdateStatus.READY_TO_INSTALL -> "安装更新"
                        UpdateStatus.ERROR -> "重试"
                        else -> "检查更新"
                    },
                    Icons.Rounded.SystemUpdate,
                    updateViewModel::onUpdateClick,
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    enabled = updateState.status != UpdateStatus.CHECKING && updateState.status != UpdateStatus.DOWNLOADING,
                )
            }
        }

        Text("隐私与说明", Modifier.padding(top = 25.dp, bottom = 12.dp), style = MaterialTheme.typography.titleMedium)
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 24.dp), corner = 22) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Lock, null, tint = Mint)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("所有处理都留在设备上", style = MaterialTheme.typography.titleMedium)
                    Text("照片和水印处理均在本地完成，不会上传。仅检查更新时需要联网访问 GitHub；照片、水印预设和设置仅保存在本机，导出文件位于 DCIM/MistMark。", Modifier.padding(top = 5.dp), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    Text("照片以 JPEG 质量 100 导出，保留原始分辨率；透明区域将合成为白色背景。", Modifier.padding(top = 12.dp), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("智能反色以每个水印覆盖区域为单位计算亮度。极复杂背景仍建议降低透明度后检查导出效果。", Modifier.padding(top = 12.dp), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (updateState.showDialog && updateState.release != null) {
        UpdateDialog(
            state = updateState,
            onDismiss = updateViewModel::dismissDialog,
            onDownload = updateViewModel::downloadUpdate,
            onInstall = updateViewModel::installUpdate,
        )
    }
}

@Composable
private fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val release = state.release ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text(if (state.status == UpdateStatus.READY_TO_INSTALL) "安装新版本" else "发现新版本 ${release.version}") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("${release.title} · 当前版本 ${state.currentVersion}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(release.notes, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                if (state.status == UpdateStatus.DOWNLOADING) {
                    Text("正在下载 ${(state.progress * 100).toInt()}%", Modifier.padding(top = 16.dp), color = Mint, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator({ state.progress }, Modifier.fillMaxWidth().padding(top = 8.dp), color = Mint)
                }
                if (state.status == UpdateStatus.READY_TO_INSTALL) {
                    Text("下载完成。如果系统请求安装权限，请允许后返回并再次点击安装。", Modifier.padding(top = 16.dp), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                state.errorMessage?.let {
                    Text(it, Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            when (state.status) {
                UpdateStatus.AVAILABLE, UpdateStatus.ERROR -> TextButton(onDownload) { Text("下载并安装", color = Mint) }
                UpdateStatus.READY_TO_INSTALL -> TextButton(onInstall) { Text("安装更新", color = Mint) }
                UpdateStatus.DOWNLOADING -> TextButton({}, enabled = false) { Text("下载中") }
                else -> Unit
            }
        },
        dismissButton = {
            TextButton(onDismiss, enabled = state.status != UpdateStatus.DOWNLOADING) { Text("关闭") }
        },
    )
}
