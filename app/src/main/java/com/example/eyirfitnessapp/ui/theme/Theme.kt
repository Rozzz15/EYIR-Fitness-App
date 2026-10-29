package com.example.eyirfitnessapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = PrimaryGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F5EA),
    onSecondaryContainer = Color(0xFF052E16),
    tertiary = WaterColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSunken,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceRaised,
    surfaceContainer = Color(0xFFF7F9F8),
    surfaceContainerHigh = SurfaceSunken,
    surfaceContainerHighest = Border,
    outline = Border,
    outlineVariant = Hairline,
    error = Danger,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF04150A),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF04150A),
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = Color(0xFFDCFCE7),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF04151F),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceSunken,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = Color(0xFF0F1512),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceRaised,
    surfaceContainerHigh = Color(0xFF222A24),
    surfaceContainerHighest = Color(0xFF2B342D),
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E2621),
    error = Color(0xFFF87171),
    onError = Color(0xFF172019)
)

/** Shape scale shared by every component (M3 defaults are too square for cards). */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.Small),
    small = RoundedCornerShape(Radii.Chip),
    medium = RoundedCornerShape(Radii.Medium),
    large = RoundedCornerShape(Radii.Large),
    extraLarge = RoundedCornerShape(Radii.Card)
)

// Extended app colors for macros/status, exposed via MaterialTheme extended palette
data class ExtendedColors(
    val success: Color = Success,
    val warning: Color = Warning,
    val danger: Color = Danger,
    val protein: Color = ProteinColor,
    val carbs: Color = CarbsColor,
    val fat: Color = FatColor,
    val water: Color = WaterColor,
    val burn: Color = BurnColor
)

val MaterialTheme.extended: ExtendedColors
    get() = ExtendedColors()

@Composable
fun EYIRFitnessAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
