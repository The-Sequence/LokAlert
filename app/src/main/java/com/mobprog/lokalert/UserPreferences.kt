package com.mobprog.lokalert

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")


class Onboarding(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    suspend fun saveOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }
}

class AppPreferences(private val context: Context) {

    companion object {
        val COOLDOWN_ENABLED = booleanPreferencesKey("cooldown_enabled")
        val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
    }

    val isCooldownEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[COOLDOWN_ENABLED] ?: false
        }

    val cooldownMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[COOLDOWN_MINUTES] ?: 5
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
}