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
        val DARK_MODE = intPreferencesKey("dark_mode") // 0=Light, 1=Dark Gray, 2=Pitch Black
        val DEFAULT_ALARM_SOUND = stringPreferencesKey("default_alarm_sound")
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
            preferences[DARK_MODE] ?: 0 // Default to Light
        }
    
    val defaultAlarmSound: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_ALARM_SOUND] ?: ""
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
    
    suspend fun setDefaultAlarmSound(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_ALARM_SOUND] = uri
        }
    }
}