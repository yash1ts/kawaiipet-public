package com.kawaiipet.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SlimeWater,
    onPrimary = SurfaceLight,
    primaryContainer = SlimeWaterLight,
    onPrimaryContainer = OnSurfaceLight,
    secondary = SlimeWaterDeep,
    onSecondary = SurfaceLight,
    secondaryContainer = SlimeWaterDeepLight,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = SlimeSeafoam,
    onTertiary = SurfaceLight,
    tertiaryContainer = SlimeSeafoamLight,
    onTertiaryContainer = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = Color(0xFFDCEEF8),
    surfaceContainer = Color(0xFFD0E8F4),
    background = SurfaceLight,
    onBackground = OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SlimeWaterLight,
    onPrimary = SurfaceDark,
    primaryContainer = SlimeWaterDark,
    onPrimaryContainer = OnSurfaceDark,
    secondary = SlimeWaterDeepLight,
    onSecondary = SurfaceDark,
    secondaryContainer = SlimeWaterDeepDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = SlimeSeafoamLight,
    onTertiary = SurfaceDark,
    tertiaryContainer = SlimeSeafoam,
    onTertiaryContainer = SurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark
)

@Composable
fun KawaiiPetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Keep false so the slime-water palette stays consistent (not wallpaper colors). */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KawaiiTypography,
        content = content
    )
}
