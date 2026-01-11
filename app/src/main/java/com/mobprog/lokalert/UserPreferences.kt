package com.mobprog.lokalert

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

// ============================================================================
// ONBOARDING PREFERENCES
// ============================================================================

class Onboarding(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TOUR_PROMPT_SHOWN = booleanPreferencesKey("tour_prompt_shown")
        val HELP_ICON_SPOTLIGHT_SHOWN = booleanPreferencesKey("help_icon_spotlight_shown")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    val isTourPromptShown: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TOUR_PROMPT_SHOWN] ?: false
        }

    val isHelpIconSpotlightShown: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HELP_ICON_SPOTLIGHT_SHOWN] ?: false
        }

    suspend fun saveOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun saveTourPromptShown() {
        context.dataStore.edit { preferences ->
            preferences[TOUR_PROMPT_SHOWN] = true
        }
    }

    suspend fun saveHelpIconSpotlightShown() {
        context.dataStore.edit { preferences ->
            preferences[HELP_ICON_SPOTLIGHT_SHOWN] = true
        }
    }
}

// ============================================================================
// APP PREFERENCES (Settings)
// ============================================================================

class AppPreferences(private val context: Context) {

    companion object {
        val COOLDOWN_ENABLED = booleanPreferencesKey("cooldown_enabled")
        val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
        val VIBRATION_INTENSITY = intPreferencesKey("vibration_intensity") // 0=Low, 1=Medium, 2=Strong
        val DARK_MODE = intPreferencesKey("dark_mode") // 0=Light, 1=Dark, 3=Auto
        val APP_THEME = intPreferencesKey("app_theme") // 0=Standard, 1=Expressive, 2=Ocean, 3=Sunset, 4=Forest, 5=Retro, 6=Monochrome
        val DEFAULT_ALARM_SOUND = stringPreferencesKey("default_alarm_sound")
        
        // Alarm Overlay Customization
        val OVERLAY_DISMISS_STYLE = intPreferencesKey("overlay_dismiss_style") // 0=Slider, 1=SwipeUp, 2=Button
        val OVERLAY_BACKGROUND_STYLE = intPreferencesKey("overlay_background_style") // 0=Gradient, 1=Solid, 2=Dark
        val OVERLAY_SHOW_DISTANCE = booleanPreferencesKey("overlay_show_distance")
        val OVERLAY_SHOW_EMOJI = booleanPreferencesKey("overlay_show_emoji")
        val OVERLAY_PRIMARY_COLOR = stringPreferencesKey("overlay_primary_color") // Hex color
        val OVERLAY_TEXT_COLOR = stringPreferencesKey("overlay_text_color") // Hex color
        val OVERLAY_EMOJI = stringPreferencesKey("overlay_emoji") // Custom emoji for alarm
        
        // Test Alarm Delay
        val TEST_ALARM_DELAY_ENABLED = booleanPreferencesKey("test_alarm_delay_enabled")
        val TEST_ALARM_DELAY_SECONDS = intPreferencesKey("test_alarm_delay_seconds") // 1-5 seconds
        
        // Developer Options
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val DESIGN_LANGUAGE = intPreferencesKey("design_language") // 0=Material3, 1=iOS6 Skeuomorphic
    }

    val isCooldownEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[COOLDOWN_ENABLED] ?: false
        }

    val cooldownMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[COOLDOWN_MINUTES] ?: 5
        }
    
    val vibrationIntensity: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATION_INTENSITY] ?: 2 // Default to Strong
        }
    
    val darkMode: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE] ?: 3 // Default to Auto (follows system)
        }
    
    val appTheme: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[APP_THEME] ?: 0 // Default to Standard theme
        }
    
    val defaultAlarmSound: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_ALARM_SOUND] ?: ""
        }
    
    // Alarm Overlay Customization Flows
    val overlayDismissStyle: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_DISMISS_STYLE] ?: 0 // Default to Slider
        }
    
    val overlayBackgroundStyle: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_BACKGROUND_STYLE] ?: 0 // Default to Gradient
        }
    
    val overlayShowDistance: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_SHOW_DISTANCE] ?: true // Show by default
        }
    
    val overlayShowEmoji: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_SHOW_EMOJI] ?: true // Show by default
        }
    
    val overlayPrimaryColor: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_PRIMARY_COLOR] ?: "FF6B6B" // Default red-orange
        }
    
    val overlayTextColor: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_TEXT_COLOR] ?: "FFFFFF" // Default white
        }
    
    val overlayEmoji: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[OVERLAY_EMOJI] ?: "🚨" // Default alarm emoji
        }
    
    val testAlarmDelayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TEST_ALARM_DELAY_ENABLED] ?: false
        }
    
    val testAlarmDelaySeconds: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[TEST_ALARM_DELAY_SECONDS] ?: 3 // Default 3 seconds
        }

    suspend fun setCooldownEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COOLDOWN_ENABLED] = enabled
        }
    }

    suspend fun setCooldownMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[COOLDOWN_MINUTES] = minutes
        }
    }
    
    suspend fun setVibrationIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_INTENSITY] = intensity
        }
    }
    
    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = mode
        }
    }
    
    suspend fun setAppTheme(theme: Int) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }
    
    suspend fun setDefaultAlarmSound(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_ALARM_SOUND] = uri
        }
    }
    
    // Alarm Overlay Customization Setters
    suspend fun setOverlayDismissStyle(style: Int) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_DISMISS_STYLE] = style
        }
    }
    
    suspend fun setOverlayBackgroundStyle(style: Int) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_BACKGROUND_STYLE] = style
        }
    }
    
    suspend fun setOverlayShowDistance(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_SHOW_DISTANCE] = show
        }
    }
    
    suspend fun setOverlayShowEmoji(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_SHOW_EMOJI] = show
        }
    }
    
    suspend fun setOverlayPrimaryColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_PRIMARY_COLOR] = color
        }
    }
    
    suspend fun  setOverlayTextColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_TEXT_COLOR] = color
        }
    }
    
    suspend fun setOverlayEmoji(emoji: String) {
        context.dataStore.edit { preferences ->
            preferences[OVERLAY_EMOJI] = emoji
        }
    }
    
    suspend fun setTestAlarmDelayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TEST_ALARM_DELAY_ENABLED] = enabled
        }
    }
    
    suspend fun setTestAlarmDelaySeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[TEST_ALARM_DELAY_SECONDS] = seconds.coerceIn(1, 5)
        }
    }
    
    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MODE_ENABLED] = enabled
        }
    }
    
    val designLanguage: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DESIGN_LANGUAGE] ?: 0 // Default to Material 3
        }
    
    suspend fun setDesignLanguage(language: Int) {
        context.dataStore.edit { preferences ->
            preferences[DESIGN_LANGUAGE] = language
        }
        // Also update the app icon to match the design language
        IconManager.setAppIcon(context, language)
    }
}