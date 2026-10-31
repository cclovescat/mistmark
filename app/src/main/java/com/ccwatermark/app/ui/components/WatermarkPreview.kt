package com.ccwatermark.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ccwatermark.app.data.PhotoItem
import com.ccwatermark.app.data.PhotoState
import com.ccwatermark.app.data.WatermarkPreset
import com.ccwatermark.app.engine.WatermarkEngine
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.SubtleFill
import com.ccwatermark.app.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private data class PreviewState(
    val bitmap: Bitmap? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)

private data class PreviewKey(
    val uri: Uri?,
    val state: PhotoState?,
    val outputPath: String?,
    val invertEnabled: Boolean?,
    val presets: List<WatermarkPreset>,
    val opacity: Float,
)

@Composable
fun WatermarkPreview(photo: PhotoItem?, presets: List<WatermarkPreset>, opacity: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val engine = remember(context.applicationContext) { WatermarkEngine(context.applicationContext) }
    val effectivePresets = photo?.appliedPresets?.ifEmpty { presets } ?: emptyList()
    val hasOutput = photo?.outputPath != null
    val key = PreviewKey(
        uri = photo?.uri,
        state = if (hasOutput) null else photo?.state,
        outputPath = photo?.outputPath,
        invertEnabled = if (hasOutput) null else photo?.invertEnabled,
        presets = if (hasOutput) emptyList() else effectivePresets,
        opacity = if (hasOutput) 0f else opacity,
    )
    val preview by produceState(
        PreviewState(),
        key.uri,
        key.state,
        key.outputPath,
        key.invertEnabled,
        key.presets,
        key.opacity,
    ) {
        value = PreviewState(loading = photo != null && effectivePresets.isNotEmpty() && photo.state != PhotoState.WORKING)
        try {
            value = when {
                photo == null -> PreviewState()
                photo.outputPath != null -> PreviewState(bitmap = engine.previewFile(photo.outputPath))
                photo.state == PhotoState.WORKING || effectivePresets.isEmpty() -> PreviewState()
                else -> {
                    delay(180)
                    PreviewState(bitmap = engine.preview(photo.uri, effectivePresets, opacity, photo.invertEnabled))
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            value = PreviewState(failed = true)
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            photo == null -> EmptyPreview()
            preview.bitmap != null -> RenderedPreview(preview.bitmap!!)
            else -> AsyncImage(photo.uri, "待处理照片", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (preview.loading) CircularProgressIndicator(Modifier.size(28.dp), color = Mint, strokeWidth = 2.dp)
        if (preview.failed) {
            Text("预览生成失败，请重新处理", Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.Black.copy(alpha = .72f), CircleShape).padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
        } else if (photo != null && effectivePresets.isEmpty()) {
            Text("选择水印预设后，即可在这里实时预览", Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.Black.copy(alpha = .65f), CircleShape).padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RenderedPreview(bitmap: Bitmap) {
    DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
    Image(bitmap.asImageBitmap(), "水印实时预览", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
}

@Composable
private fun EmptyPreview() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Box(Modifier.size(72.dp).background(SubtleFill, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Mint, modifier = Modifier.size(34.dp))
        }
        Text("让作品留下你的印记", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleMedium)
        Text("支持 JPG、PNG、WebP，可一次选择多张照片", Modifier.padding(top = 7.dp), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
