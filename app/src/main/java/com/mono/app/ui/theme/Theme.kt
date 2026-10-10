package com.mono.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.app.R

/** MONO design tokens. Use these everywhere; do not hardcode colours or radii in screens. */
object MonoColors {
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF0F0F0)
    val Ink = Color(0xFF0B0B0B)
    val Muted = Color(0xFF8C8C8C)
    val Border = Color(0xFFE4E4E4)
    val Divider = Color(0xFFEDEDED)
    val Shadow = Color(0x26000000)
    val Scrim = Color(0x66000000)
}

object MonoRadius {
    val Card = 22.dp
    val Sheet = 28.dp
    val Field = 14.dp
    val Chip = 20.dp
}

object MonoSpace {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
    val ScreenH = 20.dp
}

object MonoElevation {
    val Card = 4.dp
    val Nav = 14.dp
    val Fab = 10.dp
}

object MonoMotion {
    const val Fast = 150
    const val Normal = 250
    const val Slow = 400
}

val MonoFont = FontFamily(Font(R.font.mono_font))

private fun style(size: Int, spacing: Double = 0.0, family: FontFamily = MonoFont) = TextStyle(
    fontFamily = family, fontSize = size.sp, letterSpacing = spacing.sp, lineHeight = (size * 1.25).sp,
)

val MonoTypography = Typography(
    displayLarge = style(68, 1.0),
    displayMedium = style(46, 1.0),
    headlineSmall = style(24, 0.5),
    titleMedium = style(20, 0.3),
    titleSmall = style(17),
    bodyLarge = style(18),
    bodyMedium = style(16),
    labelLarge = style(16, 1.0),
    labelMedium = style(14, 1.2),
    labelSmall = style(12, 1.5),
)

/** Readable system font for long-form note text. */
val NoteBodyStyle = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp)

@Composable
fun MonoTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = MonoColors.Ink, onPrimary = MonoColors.Surface,
        secondary = MonoColors.Ink, onSecondary = MonoColors.Surface,
        background = MonoColors.Background, onBackground = MonoColors.Ink,
        surface = MonoColors.Surface, onSurface = MonoColors.Ink,
        surfaceVariant = MonoColors.SurfaceAlt, onSurfaceVariant = MonoColors.Muted,
        outline = MonoColors.Border, outlineVariant = MonoColors.Divider,
        surfaceTint = Color.Transparent,
        primaryContainer = MonoColors.SurfaceAlt, onPrimaryContainer = MonoColors.Ink,
        secondaryContainer = MonoColors.SurfaceAlt, onSecondaryContainer = MonoColors.Ink,
        surfaceContainer = MonoColors.Surface, surfaceContainerHigh = MonoColors.Surface,
        surfaceContainerHighest = MonoColors.SurfaceAlt, surfaceContainerLow = MonoColors.Surface,
        surfaceContainerLowest = MonoColors.Surface,
    )
    MaterialTheme(colorScheme = scheme, typography = MonoTypography, content = content)
}
