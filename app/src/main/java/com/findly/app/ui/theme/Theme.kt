package com.findly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Brand colours that Material's scheme has no slot for. */
@Immutable
data class FindlyExtraColors(
    val bestPrice: Color,
    val onBestPrice: Color,
    val discount: Color,
    val onDiscount: Color,
    val rating: Color,
    val sponsored: Color,
    val demo: Color,
    val skeleton: Color,
    val skeletonHighlight: Color,
    val imageBackdrop: List<Color>,
)

private val Ink = Color(0xFF111113)
private val Paper = Color(0xFFF7F7F5)
private val Indigo = Color(0xFF4C5BFF)
private val IndigoLight = Color(0xFF8A94FF)
private val Coral = Color(0xFFE8553B)
private val CoralLight = Color(0xFFFF7A5C)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E9F2),
    onPrimaryContainer = Ink,
    secondary = Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E8FF),
    onSecondaryContainer = Color(0xFF1B2280),
    tertiary = Coral,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEFEC),
    onSurfaceVariant = Color(0xFF66666E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFA),
    surfaceContainer = Color(0xFFF3F3F1),
    surfaceContainerHigh = Color(0xFFEDEDEA),
    surfaceContainerHighest = Color(0xFFE7E7E3),
    outline = Color(0xFFD9D9D4),
    outlineVariant = Color(0xFFE8E8E4),
    error = Color(0xFFD93025),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F2F0),
    onPrimary = Ink,
    primaryContainer = Color(0xFF2A2A31),
    onPrimaryContainer = Color(0xFFF2F2F0),
    secondary = IndigoLight,
    onSecondary = Color(0xFF0D1160),
    secondaryContainer = Color(0xFF262B66),
    onSecondaryContainer = Color(0xFFDDE0FF),
    tertiary = CoralLight,
    onTertiary = Color(0xFF3A0B00),
    background = Color(0xFF0B0B0D),
    onBackground = Color(0xFFF2F2F0),
    surface = Color(0xFF151518),
    onSurface = Color(0xFFF2F2F0),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = Color(0xFF0E0E10),
    surfaceContainerLow = Color(0xFF131316),
    surfaceContainer = Color(0xFF18181C),
    surfaceContainerHigh = Color(0xFF1F1F24),
    surfaceContainerHighest = Color(0xFF26262C),
    outline = Color(0xFF34343B),
    outlineVariant = Color(0xFF26262C),
    error = Color(0xFFFF6B5E),
)

private val LightExtra = FindlyExtraColors(
    bestPrice = Color(0xFF168A4A),
    onBestPrice = Color.White,
    discount = Coral,
    onDiscount = Color.White,
    rating = Color(0xFFF5A524),
    sponsored = Color(0xFF8A6D00),
    demo = Color(0xFF6B4EFF),
    skeleton = Color(0xFFECECE8),
    skeletonHighlight = Color(0xFFF7F7F4),
    imageBackdrop = listOf(Color(0xFFF1F1EE), Color(0xFFE6E6E1)),
)

private val DarkExtra = FindlyExtraColors(
    bestPrice = Color(0xFF3DD68C),
    onBestPrice = Color(0xFF00240F),
    discount = CoralLight,
    onDiscount = Color(0xFF2B0800),
    rating = Color(0xFFFFC94D),
    sponsored = Color(0xFFE5C55C),
    demo = Color(0xFFA996FF),
    skeleton = Color(0xFF1C1C21),
    skeletonHighlight = Color(0xFF2A2A30),
    imageBackdrop = listOf(Color(0xFF1C1C21), Color(0xFF141417)),
)

val LocalFindlyColors = staticCompositionLocalOf { LightExtra }

private val FindlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun FindlyTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalFindlyColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = FindlyTypography,
            shapes = FindlyShapes,
            content = content,
        )
    }
}

object FindlyTheme {
    val extra: FindlyExtraColors
        @Composable get() = LocalFindlyColors.current
}
