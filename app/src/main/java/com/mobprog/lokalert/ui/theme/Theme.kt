package com.mobprog.lokalert.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Pitch Black (AMOLED) Color Scheme
private val PitchBlackColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF6A1B9A),
    onSecondaryContainer = Color(0xFFE1BEE7),
    
    tertiary = Color(0xFFF48FB1),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFC2185B),
    onTertiaryContainer = Color(0xFFF8BBD0),
    
    error = Color(0xFFEF5350),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),
    
    background = Color(0xFF000000), // Pure black
    onBackground = Color(0xFFE0E0E0),
    
    surface = Color(0xFF000000), // Pure black
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    
    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF212121),
    
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainerLowest = Color(0xFF000000),
)

// Dark Gray Color Scheme
private val DarkGrayColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFBBDEFB),
    
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF7B1FA2),
    onSecondaryContainer = Color(0xFFE1BEE7),
    
    tertiary = Color(0xFFF48FB1),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFFC2185B),
    onTertiaryContainer = Color(0xFFF8BBD0),
    
    error = Color(0xFFEF5350),
    onError = Color(0xFF1A1A1A),
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),
    
    background = Color(0xFF1A1A1A), // Dark gray
    onBackground = Color(0xFFE0E0E0),
    
    surface = Color(0xFF1A1A1A), // Dark gray
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFBDBDBD),
    
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242),
    
    surfaceContainer = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF2E2E2E),
    surfaceContainerHighest = Color(0xFF383838),
    surfaceContainerLow = Color(0xFF1F1F1F),
    surfaceContainerLowest = Color(0xFF121212),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = Color(0xFF7B1FA2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1BEE7),
    onSecondaryContainer = Color(0xFF4A148C),
    
    tertiary = Color(0xFFC2185B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8BBD0),
    onTertiaryContainer = Color(0xFF880E4F),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),
    
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainerLowest = Color.White,
)

@Composable
fun LokAlertTheme(
    darkMode: Int = 0, // 0=Light, 1=Dark Gray, 2=Pitch Black
    dynamicColor: Boolean = true, // Disabled by default for consistent theming
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkMode > 0) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkMode == 2 -> PitchBlackColorScheme
        darkMode == 1 -> DarkGrayColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkMode == 0
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}