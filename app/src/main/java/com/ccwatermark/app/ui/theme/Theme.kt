package com.ccwatermark.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ccwatermark.app.data.FontSizeOption
import com.ccwatermark.app.data.ThemeMode

val PalettePink = Color(0xFFFBD7E1)
val PaletteGray = Color(0xFFECEDEF)
val PaletteIce = Color(0xFFD6E9F0)
val PaletteSky = Color(0xFFBFE4F4)
val BrandMint = Color(0xFFAFE1FA)

val Ink: Color @Composable get() = MaterialTheme.colorScheme.background
val Mint: Color @Composable get() = MaterialTheme.colorScheme.primary
val Lavender: Color @Composable get() = MaterialTheme.colorScheme.secondary
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val isLightMistTheme: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() > .5f
val SubtleFill: Color @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLightMistTheme) .045f else .06f)
val SubtleStroke: Color @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLightMistTheme) .10f else .12f)
val PreviewSurface: Color @Composable get() = if (isLightMistTheme) PaletteIce else Color(0xFF20232B)
val DialogSurface: Color @Composable get() = MaterialTheme.colorScheme.surface

private val DarkColors = darkColorScheme(
    primary = BrandMint,
    onPrimary = Color(0xFF17313F),
    secondary = PalettePink,
    background = Color(0xFF181A20),
    onBackground = Color(0xFFF4F5F7),
    surface = Color(0xFF252831),
    onSurface = Color(0xFFF4F5F7),
    surfaceVariant = Color(0xFF343841),
    onSurfaceVariant = Color(0xFFC7CBD2),
    outline = Color(0xFF515762),
    error = Color(0xFFFFB4C0),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF416C80),
    onPrimary = Color.White,
    secondary = Color(0xFF8A5B6C),
    background = PaletteGray,
    onBackground = Color(0xFF24282E),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF24282E),
    surfaceVariant = PaletteIce,
    onSurfaceVariant = Color(0xFF5D6670),
    outline = Color(0xFFC4CCD2),
    error = Color(0xFFB3263B),
)

@Composable
fun MistMarkTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontSize: FontSizeOption = FontSizeOption.STANDARD,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.MOON -> true
        ThemeMode.DAY -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.let { activity ->
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    val base = Typography(
        headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    )
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = base.scaled(fontSize.scale),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
            content()
        }
    }
}

private fun TextStyle.scaled(factor: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * factor else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Typography.scaled(factor: Float): Typography = Typography(
    displayLarge = displayLarge.scaled(factor),
    displayMedium = displayMedium.scaled(factor),
    displaySmall = displaySmall.scaled(factor),
    headlineLarge = headlineLarge.scaled(factor),
    headlineMedium = headlineMedium.scaled(factor),
    headlineSmall = headlineSmall.scaled(factor),
    titleLarge = titleLarge.scaled(factor),
    titleMedium = titleMedium.scaled(factor),
    titleSmall = titleSmall.scaled(factor),
    bodyLarge = bodyLarge.scaled(factor),
    bodyMedium = bodyMedium.scaled(factor),
    bodySmall = bodySmall.scaled(factor),
    labelLarge = labelLarge.scaled(factor),
    labelMedium = labelMedium.scaled(factor),
    labelSmall = labelSmall.scaled(factor),
)
