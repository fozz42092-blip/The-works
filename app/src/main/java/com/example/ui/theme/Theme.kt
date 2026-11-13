package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = HydroNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6ECFA),
    onPrimaryContainer = HydroNavyDark,
    secondary = AquiferTeal,
    onSecondary = Color.White,
    secondaryContainer = AquiferTealLight,
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = HydroCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = LimestoneBg,
    onBackground = InkDark,
    surface = CrispSurface,
    onSurface = InkDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineSubtle,
    error = FloodCrimson,
    onError = Color.White,
    errorContainer = FloodCrimsonContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = HydroCyanBright,
    onPrimary = HydroNavyDark,
    primaryContainer = Color(0xFF0E4A73),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = AquiferTealLight,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = DarkAbyssBg,
    onBackground = InkLight,
    surface = DarkTrenchSurface,
    onSurface = InkLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = InkLightMuted,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = FloodCrimsonContainer
)

val WaterTableShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun WaterTableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WaterTableTypography,
        shapes = WaterTableShapes,
        content = content
    )
}
