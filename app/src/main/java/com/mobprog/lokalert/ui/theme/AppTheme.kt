package com.mobprog.lokalert.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Available app themes
 */
enum class AppThemeType(val displayName: String, val emoji: String, val description: String) {
    STANDARD("Standard", "🎨", "Clean Material 3 design"),
    EXPRESSIVE("Expressive", "✨", "Vibrant and bold colors"),
    OCEAN("Ocean Breeze", "🌊", "Calming blue tones"),
    SUNSET("Sunset Glow", "🌅", "Warm orange hues"),
    FOREST("Forest", "🌲", "Nature-inspired greens"),
    RETRO("Retro Neon", "🕹️", "80s neon vibes"),
    MONOCHROME("Monochrome", "⚫", "Classic black & white")
}

/**
 * Get the color scheme for a given theme type
 */
fun getColorScheme(themeType: AppThemeType, isDark: Boolean): ColorScheme {
    return when (themeType) {
        AppThemeType.STANDARD -> if (isDark) StandardDarkColorScheme else StandardLightColorScheme
        AppThemeType.EXPRESSIVE -> if (isDark) ExpressiveDarkColorScheme else ExpressiveLightColorScheme
        AppThemeType.OCEAN -> if (isDark) OceanDarkColorScheme else OceanLightColorScheme
        AppThemeType.SUNSET -> if (isDark) SunsetDarkColorScheme else SunsetLightColorScheme
        AppThemeType.FOREST -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
        AppThemeType.RETRO -> if (isDark) RetroDarkColorScheme else RetroLightColorScheme
        AppThemeType.MONOCHROME -> if (isDark) MonoDarkColorScheme else MonoLightColorScheme
    }
}

// ============================================================================
// COLOR SCHEMES
// ============================================================================

// Standard Material 3
val StandardLightColorScheme = lightColorScheme(
    primary = StandardLightPrimary,
    onPrimary = StandardLightOnPrimary,
    primaryContainer = StandardLightPrimaryContainer,
    onPrimaryContainer = StandardLightOnPrimaryContainer,
    secondary = StandardLightSecondary,
    onSecondary = StandardLightOnSecondary,
    secondaryContainer = StandardLightSecondaryContainer,
    onSecondaryContainer = StandardLightOnSecondaryContainer,
    tertiary = StandardLightTertiary,
    onTertiary = StandardLightOnTertiary,
    tertiaryContainer = StandardLightTertiaryContainer,
    onTertiaryContainer = StandardLightOnTertiaryContainer,
    background = StandardLightBackground,
    onBackground = StandardLightOnBackground,
    surface = StandardLightSurface,
    onSurface = StandardLightOnSurface,
    surfaceVariant = StandardLightSurfaceVariant,
    onSurfaceVariant = StandardLightOnSurfaceVariant,
    outline = StandardLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val StandardDarkColorScheme = darkColorScheme(
    primary = StandardDarkPrimary,
    onPrimary = StandardDarkOnPrimary,
    primaryContainer = StandardDarkPrimaryContainer,
    onPrimaryContainer = StandardDarkOnPrimaryContainer,
    secondary = StandardDarkSecondary,
    onSecondary = StandardDarkOnSecondary,
    secondaryContainer = StandardDarkSecondaryContainer,
    onSecondaryContainer = StandardDarkOnSecondaryContainer,
    tertiary = StandardDarkTertiary,
    onTertiary = StandardDarkOnTertiary,
    tertiaryContainer = StandardDarkTertiaryContainer,
    onTertiaryContainer = StandardDarkOnTertiaryContainer,
    background = StandardDarkBackground,
    onBackground = StandardDarkOnBackground,
    surface = StandardDarkSurface,
    onSurface = StandardDarkOnSurface,
    surfaceVariant = StandardDarkSurfaceVariant,
    onSurfaceVariant = StandardDarkOnSurfaceVariant,
    outline = StandardDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Expressive Material 3
val ExpressiveLightColorScheme = lightColorScheme(
    primary = ExpressiveLightPrimary,
    onPrimary = ExpressiveLightOnPrimary,
    primaryContainer = ExpressiveLightPrimaryContainer,
    onPrimaryContainer = ExpressiveLightOnPrimaryContainer,
    secondary = ExpressiveLightSecondary,
    onSecondary = ExpressiveLightOnSecondary,
    secondaryContainer = ExpressiveLightSecondaryContainer,
    onSecondaryContainer = ExpressiveLightOnSecondaryContainer,
    tertiary = ExpressiveLightTertiary,
    onTertiary = ExpressiveLightOnTertiary,
    tertiaryContainer = ExpressiveLightTertiaryContainer,
    onTertiaryContainer = ExpressiveLightOnTertiaryContainer,
    background = ExpressiveLightBackground,
    onBackground = ExpressiveLightOnBackground,
    surface = ExpressiveLightSurface,
    onSurface = ExpressiveLightOnSurface,
    surfaceVariant = ExpressiveLightSurfaceVariant,
    onSurfaceVariant = ExpressiveLightOnSurfaceVariant,
    outline = ExpressiveLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val ExpressiveDarkColorScheme = darkColorScheme(
    primary = ExpressiveDarkPrimary,
    onPrimary = ExpressiveDarkOnPrimary,
    primaryContainer = ExpressiveDarkPrimaryContainer,
    onPrimaryContainer = ExpressiveDarkOnPrimaryContainer,
    secondary = ExpressiveDarkSecondary,
    onSecondary = ExpressiveDarkOnSecondary,
    secondaryContainer = ExpressiveDarkSecondaryContainer,
    onSecondaryContainer = ExpressiveDarkOnSecondaryContainer,
    tertiary = ExpressiveDarkTertiary,
    onTertiary = ExpressiveDarkOnTertiary,
    tertiaryContainer = ExpressiveDarkTertiaryContainer,
    onTertiaryContainer = ExpressiveDarkOnTertiaryContainer,
    background = ExpressiveDarkBackground,
    onBackground = ExpressiveDarkOnBackground,
    surface = ExpressiveDarkSurface,
    onSurface = ExpressiveDarkOnSurface,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onSurfaceVariant = ExpressiveDarkOnSurfaceVariant,
    outline = ExpressiveDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Ocean Theme
val OceanLightColorScheme = lightColorScheme(
    primary = OceanLightPrimary,
    onPrimary = OceanLightOnPrimary,
    primaryContainer = OceanLightPrimaryContainer,
    onPrimaryContainer = OceanLightOnPrimaryContainer,
    secondary = OceanLightSecondary,
    onSecondary = OceanLightOnSecondary,
    secondaryContainer = OceanLightSecondaryContainer,
    onSecondaryContainer = OceanLightOnSecondaryContainer,
    tertiary = OceanLightTertiary,
    onTertiary = OceanLightOnTertiary,
    tertiaryContainer = OceanLightTertiaryContainer,
    onTertiaryContainer = OceanLightOnTertiaryContainer,
    background = OceanLightBackground,
    onBackground = OceanLightOnBackground,
    surface = OceanLightSurface,
    onSurface = OceanLightOnSurface,
    surfaceVariant = OceanLightSurfaceVariant,
    onSurfaceVariant = OceanLightOnSurfaceVariant,
    outline = OceanLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val OceanDarkColorScheme = darkColorScheme(
    primary = OceanDarkPrimary,
    onPrimary = OceanDarkOnPrimary,
    primaryContainer = OceanDarkPrimaryContainer,
    onPrimaryContainer = OceanDarkOnPrimaryContainer,
    secondary = OceanDarkSecondary,
    onSecondary = OceanDarkOnSecondary,
    secondaryContainer = OceanDarkSecondaryContainer,
    onSecondaryContainer = OceanDarkOnSecondaryContainer,
    tertiary = OceanDarkTertiary,
    onTertiary = OceanDarkOnTertiary,
    tertiaryContainer = OceanDarkTertiaryContainer,
    onTertiaryContainer = OceanDarkOnTertiaryContainer,
    background = OceanDarkBackground,
    onBackground = OceanDarkOnBackground,
    surface = OceanDarkSurface,
    onSurface = OceanDarkOnSurface,
    surfaceVariant = OceanDarkSurfaceVariant,
    onSurfaceVariant = OceanDarkOnSurfaceVariant,
    outline = OceanDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Sunset Theme
val SunsetLightColorScheme = lightColorScheme(
    primary = SunsetLightPrimary,
    onPrimary = SunsetLightOnPrimary,
    primaryContainer = SunsetLightPrimaryContainer,
    onPrimaryContainer = SunsetLightOnPrimaryContainer,
    secondary = SunsetLightSecondary,
    onSecondary = SunsetLightOnSecondary,
    secondaryContainer = SunsetLightSecondaryContainer,
    onSecondaryContainer = SunsetLightOnSecondaryContainer,
    tertiary = SunsetLightTertiary,
    onTertiary = SunsetLightOnTertiary,
    tertiaryContainer = SunsetLightTertiaryContainer,
    onTertiaryContainer = SunsetLightOnTertiaryContainer,
    background = SunsetLightBackground,
    onBackground = SunsetLightOnBackground,
    surface = SunsetLightSurface,
    onSurface = SunsetLightOnSurface,
    surfaceVariant = SunsetLightSurfaceVariant,
    onSurfaceVariant = SunsetLightOnSurfaceVariant,
    outline = SunsetLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val SunsetDarkColorScheme = darkColorScheme(
    primary = SunsetDarkPrimary,
    onPrimary = SunsetDarkOnPrimary,
    primaryContainer = SunsetDarkPrimaryContainer,
    onPrimaryContainer = SunsetDarkOnPrimaryContainer,
    secondary = SunsetDarkSecondary,
    onSecondary = SunsetDarkOnSecondary,
    secondaryContainer = SunsetDarkSecondaryContainer,
    onSecondaryContainer = SunsetDarkOnSecondaryContainer,
    tertiary = SunsetDarkTertiary,
    onTertiary = SunsetDarkOnTertiary,
    tertiaryContainer = SunsetDarkTertiaryContainer,
    onTertiaryContainer = SunsetDarkOnTertiaryContainer,
    background = SunsetDarkBackground,
    onBackground = SunsetDarkOnBackground,
    surface = SunsetDarkSurface,
    onSurface = SunsetDarkOnSurface,
    surfaceVariant = SunsetDarkSurfaceVariant,
    onSurfaceVariant = SunsetDarkOnSurfaceVariant,
    outline = SunsetDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Forest Theme
val ForestLightColorScheme = lightColorScheme(
    primary = ForestLightPrimary,
    onPrimary = ForestLightOnPrimary,
    primaryContainer = ForestLightPrimaryContainer,
    onPrimaryContainer = ForestLightOnPrimaryContainer,
    secondary = ForestLightSecondary,
    onSecondary = ForestLightOnSecondary,
    secondaryContainer = ForestLightSecondaryContainer,
    onSecondaryContainer = ForestLightOnSecondaryContainer,
    tertiary = ForestLightTertiary,
    onTertiary = ForestLightOnTertiary,
    tertiaryContainer = ForestLightTertiaryContainer,
    onTertiaryContainer = ForestLightOnTertiaryContainer,
    background = ForestLightBackground,
    onBackground = ForestLightOnBackground,
    surface = ForestLightSurface,
    onSurface = ForestLightOnSurface,
    surfaceVariant = ForestLightSurfaceVariant,
    onSurfaceVariant = ForestLightOnSurfaceVariant,
    outline = ForestLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val ForestDarkColorScheme = darkColorScheme(
    primary = ForestDarkPrimary,
    onPrimary = ForestDarkOnPrimary,
    primaryContainer = ForestDarkPrimaryContainer,
    onPrimaryContainer = ForestDarkOnPrimaryContainer,
    secondary = ForestDarkSecondary,
    onSecondary = ForestDarkOnSecondary,
    secondaryContainer = ForestDarkSecondaryContainer,
    onSecondaryContainer = ForestDarkOnSecondaryContainer,
    tertiary = ForestDarkTertiary,
    onTertiary = ForestDarkOnTertiary,
    tertiaryContainer = ForestDarkTertiaryContainer,
    onTertiaryContainer = ForestDarkOnTertiaryContainer,
    background = ForestDarkBackground,
    onBackground = ForestDarkOnBackground,
    surface = ForestDarkSurface,
    onSurface = ForestDarkOnSurface,
    surfaceVariant = ForestDarkSurfaceVariant,
    onSurfaceVariant = ForestDarkOnSurfaceVariant,
    outline = ForestDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Retro Neon Theme
val RetroLightColorScheme = lightColorScheme(
    primary = RetroLightPrimary,
    onPrimary = RetroLightOnPrimary,
    primaryContainer = RetroLightPrimaryContainer,
    onPrimaryContainer = RetroLightOnPrimaryContainer,
    secondary = RetroLightSecondary,
    onSecondary = RetroLightOnSecondary,
    secondaryContainer = RetroLightSecondaryContainer,
    onSecondaryContainer = RetroLightOnSecondaryContainer,
    tertiary = RetroLightTertiary,
    onTertiary = RetroLightOnTertiary,
    tertiaryContainer = RetroLightTertiaryContainer,
    onTertiaryContainer = RetroLightOnTertiaryContainer,
    background = RetroLightBackground,
    onBackground = RetroLightOnBackground,
    surface = RetroLightSurface,
    onSurface = RetroLightOnSurface,
    surfaceVariant = RetroLightSurfaceVariant,
    onSurfaceVariant = RetroLightOnSurfaceVariant,
    outline = RetroLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val RetroDarkColorScheme = darkColorScheme(
    primary = RetroDarkPrimary,
    onPrimary = RetroDarkOnPrimary,
    primaryContainer = RetroDarkPrimaryContainer,
    onPrimaryContainer = RetroDarkOnPrimaryContainer,
    secondary = RetroDarkSecondary,
    onSecondary = RetroDarkOnSecondary,
    secondaryContainer = RetroDarkSecondaryContainer,
    onSecondaryContainer = RetroDarkOnSecondaryContainer,
    tertiary = RetroDarkTertiary,
    onTertiary = RetroDarkOnTertiary,
    tertiaryContainer = RetroDarkTertiaryContainer,
    onTertiaryContainer = RetroDarkOnTertiaryContainer,
    background = RetroDarkBackground,
    onBackground = RetroDarkOnBackground,
    surface = RetroDarkSurface,
    onSurface = RetroDarkOnSurface,
    surfaceVariant = RetroDarkSurfaceVariant,
    onSurfaceVariant = RetroDarkOnSurfaceVariant,
    outline = RetroDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Monochrome Theme
val MonoLightColorScheme = lightColorScheme(
    primary = MonoLightPrimary,
    onPrimary = MonoLightOnPrimary,
    primaryContainer = MonoLightPrimaryContainer,
    onPrimaryContainer = MonoLightOnPrimaryContainer,
    secondary = MonoLightSecondary,
    onSecondary = MonoLightOnSecondary,
    secondaryContainer = MonoLightSecondaryContainer,
    onSecondaryContainer = MonoLightOnSecondaryContainer,
    tertiary = MonoLightTertiary,
    onTertiary = MonoLightOnTertiary,
    tertiaryContainer = MonoLightTertiaryContainer,
    onTertiaryContainer = MonoLightOnTertiaryContainer,
    background = MonoLightBackground,
    onBackground = MonoLightOnBackground,
    surface = MonoLightSurface,
    onSurface = MonoLightOnSurface,
    surfaceVariant = MonoLightSurfaceVariant,
    onSurfaceVariant = MonoLightOnSurfaceVariant,
    outline = MonoLightOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFE0E0E0),
    onErrorContainer = Color(0xFF410002)
)

val MonoDarkColorScheme = darkColorScheme(
    primary = MonoDarkPrimary,
    onPrimary = MonoDarkOnPrimary,
    primaryContainer = MonoDarkPrimaryContainer,
    onPrimaryContainer = MonoDarkOnPrimaryContainer,
    secondary = MonoDarkSecondary,
    onSecondary = MonoDarkOnSecondary,
    secondaryContainer = MonoDarkSecondaryContainer,
    onSecondaryContainer = MonoDarkOnSecondaryContainer,
    tertiary = MonoDarkTertiary,
    onTertiary = MonoDarkOnTertiary,
    tertiaryContainer = MonoDarkTertiaryContainer,
    onTertiaryContainer = MonoDarkOnTertiaryContainer,
    background = MonoDarkBackground,
    onBackground = MonoDarkOnBackground,
    surface = MonoDarkSurface,
    onSurface = MonoDarkOnSurface,
    surfaceVariant = MonoDarkSurfaceVariant,
    onSurfaceVariant = MonoDarkOnSurfaceVariant,
    outline = MonoDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF424242),
    onErrorContainer = Color(0xFFFFDAD6)
)
