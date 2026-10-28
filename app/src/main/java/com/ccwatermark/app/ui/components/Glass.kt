package com.ccwatermark.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwatermark.app.ui.theme.BrandMint
import com.ccwatermark.app.ui.theme.Ink
import com.ccwatermark.app.ui.theme.Mint
import com.ccwatermark.app.ui.theme.PaletteGray
import com.ccwatermark.app.ui.theme.PaletteSky
import com.ccwatermark.app.ui.theme.SubtleFill
import com.ccwatermark.app.ui.theme.SubtleStroke
import com.ccwatermark.app.ui.theme.TextSecondary
import com.ccwatermark.app.ui.theme.isLightMistTheme

private val PressEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val light = isLightMistTheme
    val colors = if (light) {
        listOf(Color(0xFFF4F6F8), PaletteGray, Color(0xFFE6EDF2))
    } else {
        listOf(Color(0xFF101217), Color(0xFF181A20), Color(0xFF151920))
    }
    val accent = Mint
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
        Canvas(Modifier.fillMaxSize()) {
            val glow = Offset(size.width * .92f, size.height * .08f)
            val radius = size.width * .72f
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = if (light) .13f else .10f), Color.Transparent),
                    center = glow,
                    radius = radius,
                ),
                center = glow,
                radius = radius,
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 24,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner.dp)
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    val light = isLightMistTheme
    val fill = if (light) {
        MaterialTheme.colorScheme.surface.copy(alpha = .94f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = .88f)
    }
    val borderColor = when {
        highlighted -> Mint.copy(alpha = .62f)
        light -> MaterialTheme.colorScheme.onSurface.copy(alpha = .075f)
        else -> Color.White.copy(alpha = .09f)
    }
    Box(
        modifier
            .shadow(
                elevation = if (light) 4.dp else 12.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (light) .05f else .16f),
                spotColor = Color.Black.copy(alpha = if (light) .08f else .24f),
            )
            .clip(shape)
            .then(clickable)
            .background(fill)
            .border(if (highlighted) 1.5.dp else 1.dp, borderColor, shape),
        content = content,
    )
}

@Composable
fun AppHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(BrandMint, PaletteSky))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.WaterDrop, null, tint = Color(0xFF17313F), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 11.dp).weight(1f)) {
            Text("雾印", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("MISTMARK STUDIO", color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp)
        }
        Surface(
            shape = CircleShape,
            color = SubtleFill,
            border = BorderStroke(1.dp, SubtleStroke),
        ) {
            Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Mint, modifier = Modifier.size(13.dp))
                Text("  本地处理", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun GlassBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(68.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f)),
        shadowElevation = 12.dp,
    ) {
        Row(Modifier.fillMaxSize().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val items = listOf(
                Triple("创作", Icons.Rounded.Home, 0),
                Triple("水印", Icons.Rounded.CollectionsBookmark, 1),
                Triple("设置", Icons.Rounded.Settings, 2),
            )
            items.forEach { (label, icon, index) ->
                val active = selected == index
                Row(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(18.dp))
                        .background(if (active) Mint.copy(alpha = .14f) else Color.Transparent)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = { onSelect(index) },
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = if (active) Mint else TextSecondary, modifier = Modifier.size(21.dp))
                    Text(
                        "  $label",
                        color = if (active) Mint else TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        meta?.let {
            Surface(shape = CircleShape, color = SubtleFill) {
                Text(
                    it,
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                color = Mint,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun AccentButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ActionButton(text, icon, onClick, modifier, enabled, primary = true)
}

@Composable
fun QuietButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ActionButton(text, icon, onClick, modifier, enabled, primary = false)
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    primary: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .98f else 1f,
        animationSpec = tween(120, easing = PressEasing),
        label = "buttonPress",
    )
    val shape = RoundedCornerShape(18.dp)
    val contentColor = when {
        !enabled -> TextSecondary
        primary -> Ink
        else -> Mint
    }
    val background = if (primary) {
        if (enabled) Brush.linearGradient(listOf(BrandMint, PaletteSky)) else Brush.linearGradient(listOf(SubtleFill, SubtleFill))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(52.dp)
            .clip(shape)
            .background(background)
            .then(if (primary) Modifier else Modifier.border(1.dp, if (enabled) Mint.copy(alpha = .32f) else SubtleStroke, shape))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text("  $text", color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}
