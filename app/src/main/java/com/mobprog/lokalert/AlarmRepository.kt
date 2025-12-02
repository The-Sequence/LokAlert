package com.mobprog.lokalert

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository class for managing LocationAlarm data.
 * Provides a clean API for accessing alarm data through the Room database.
 */
class AlarmRepository(context: Context) {
    
    private val alarmDao = LokAlertDatabase.getDatabase(context).alarmDao()
    
    /**
     * Get all alarms as a Flow for reactive UI updates
     */
    fun getAllAlarms(): Flow<List<LocationAlarm>> {
        return alarmDao.getAllAlarms()
    }
    
    /**
     * Insert a new alarm into the database
     */
    suspend fun insertAlarm(alarm: LocationAlarm) {
        alarmDao.insertAlarm(alarm)
    }
    
    /**
     * Update an existing alarm
     */
    suspend fun updateAlarm(alarm: LocationAlarm) {
        alarmDao.updateAlarm(alarm)
    }
    
    /**
     * Delete an alarm from the database
     */
    suspend fun deleteAlarm(alarm: LocationAlarm) {
        alarmDao.deleteAlarm(alarm)
    }
}
