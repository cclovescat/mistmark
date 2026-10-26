package com.ccwatermark.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwatermark.app.ui.theme.BrandMint
import com.ccwatermark.app.ui.theme.Ink
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.PaletteIce
import com.ccwatermark.app.ui.theme.PalettePink
import com.ccwatermark.app.ui.theme.PaletteSky
import com.ccwatermark.app.ui.theme.SubtleFill
import com.ccwatermark.app.ui.theme.SubtleStroke
import com.ccwatermark.app.ui.theme.TextSecondary
import com.ccwatermark.app.ui.theme.isLightMistTheme

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        val light = isLightMistTheme
        Canvas(Modifier.fillMaxSize()) {
            val mintGlow = Offset(size.width * .08f, size.height * .06f)
            val violetGlow = Offset(size.width * .98f, size.height * .42f)
            val blueGlow = Offset(size.width * .25f, size.height * .95f)
            drawCircle(Brush.radialGradient(listOf(PalettePink.copy(alpha = if (light) .55f else .18f), Color.Transparent), center = mintGlow, radius = size.width * .7f), center = mintGlow, radius = size.width * .7f)
            drawCircle(Brush.radialGradient(listOf(PaletteIce.copy(alpha = if (light) .60f else .14f), Color.Transparent), center = violetGlow, radius = size.width * .75f), center = violetGlow, radius = size.width * .75f)
            drawCircle(Brush.radialGradient(listOf(PaletteSky.copy(alpha = if (light) .50f else .16f), Color.Transparent), center = blueGlow, radius = size.width * .8f), center = blueGlow, radius = size.width * .8f)
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 24,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner.dp)
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    val light = isLightMistTheme
    val fillColors = if (light) listOf(MaterialTheme.colorScheme.surface.copy(alpha = .82f), MaterialTheme.colorScheme.surface.copy(alpha = .56f)) else listOf(Color.White.copy(alpha = .105f), Color.White.copy(alpha = .045f))
    val borderColors = if (light) listOf(Color.White.copy(alpha = .90f), MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)) else listOf(Color.White.copy(alpha = .22f), Color.White.copy(alpha = .055f))
    Box(
        modifier.clip(shape).then(clickable)
            .background(Brush.linearGradient(fillColors))
            .border(1.dp, Brush.linearGradient(borderColors), shape)
            .padding(1.dp),
        content = content,
    )
}

@Composable
fun AppHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(BrandMint, PaletteSky))), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.WaterDrop, null, tint = Color(0xFF17313F), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text("雾印", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("MISTMARK STUDIO", color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
        }
        Surface(shape = CircleShape, color = SubtleFill, border = androidx.compose.foundation.BorderStroke(1.dp, SubtleStroke)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Mint, modifier = Modifier.size(13.dp))
                Text("  本地处理", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun GlassBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(72.dp), corner = 26) {
        Row(Modifier.fillMaxSize().padding(6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            val items = listOf(Triple("首页", Icons.Rounded.Home, 0), Triple("预设", Icons.Rounded.CollectionsBookmark, 1), Triple("设置", Icons.Rounded.Settings, 2))
            items.forEach { (label, icon, index) ->
                val active = selected == index
                Row(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(20.dp))
                        .background(if (active) SubtleFill else Color.Transparent)
                        .clickable { onSelect(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, label, tint = if (active) Mint else TextSecondary, modifier = Modifier.size(22.dp))
                    if (active) Text("  $label", color = Mint, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun AccentButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier.height(54.dp).clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Brush.linearGradient(listOf(BrandMint, PaletteSky)) else Brush.linearGradient(listOf(SubtleFill, SubtleFill)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) Color(0xFF17313F) else TextSecondary, modifier = Modifier.size(20.dp))
            Text("  $text", color = if (enabled) Color(0xFF17313F) else TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
