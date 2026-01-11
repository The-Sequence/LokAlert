package com.mobprog.lokalert.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Main theme composable for LokAlert
 * 
 * @param darkMode 0=Light, 1=Dark, 3=Auto (follows system)
 * @param themeType The theme style to use (Standard, Expressive, Ocean, etc.)
 * @param designLanguage 0=Material3, 1=iOS6 Skeuomorphic
 * @param dynamicColor Whether to use Android 12+ dynamic colors (overrides theme)
 * @param content The composable content
 */
@Composable
fun LokAlertTheme(
    darkMode: Int = 3, // 0=Light, 1=Dark, 3=Auto
    themeType: AppThemeType = AppThemeType.STANDARD,
    designLanguage: Int = 0, // 0=Material3, 1=iOS6
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    // Resolve the effective dark mode
    val isDark = when (darkMode) {
        3 -> isSystemDark  // Auto: follow system
        1 -> true          // Dark mode
        else -> false      // Light mode (0 or any other value)
    }
    
    // For iOS 6 style, we override with a specific color scheme
    val colorScheme = when {
        // iOS 6 design language uses its own color scheme
        designLanguage == 1 -> if (isDark) iOS6DarkColorScheme else iOS6LightColorScheme
        // Use dynamic color if enabled and on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Otherwise use the selected theme
        else -> getColorScheme(themeType, isDark)
    }
    
    // Determine design language enum
    val designLang = if (designLanguage == 1) DesignLanguage.IOS6_SKEUOMORPHIC else DesignLanguage.MATERIAL3
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // iOS 6 style uses blue navigation bar, so status bar should be different
            window.statusBarColor = if (designLanguage == 1) {
                iOS6Colors.NavBarGradientTop.toArgb()
            } else {
                colorScheme.background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = 
                if (designLanguage == 1) false else !isDark
        }
    }

    // Provide the design language via CompositionLocal
    CompositionLocalProvider(LocalDesignLanguage provides designLang) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (designLanguage == 1) iOS6Typography else Typography,
            content = content
        )
    }
}