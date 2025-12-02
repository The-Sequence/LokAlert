package com.mobprog.lokalert

import android.util.Log

/**
 * Centralized logging utility for LokAlert application.
 * Provides consistent logging across the app with configurable log levels.
 * Debug logging is automatically disabled in release builds.
 */
object LokAlertLogger {
    
    private const val TAG = "LokAlert"
    // Debug logging is enabled only in debug builds
    private val isDebugMode: Boolean
        get() = BuildConfig.DEBUG
    
    /**
     * Log debug message
     */
    fun d(message: String, tag: String = TAG) {
        if (isDebugMode) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log info message
     */
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }
    
    /**
     * Log warning message
     */
    fun w(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log error message
     */
    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log location search event
     */
    fun logLocationSearch(query: String, success: Boolean) {
        d("Location search: query='$query', success=$success", "LocationSearch")
    }
    
    /**
     * Log alarm creation
     */
    fun logAlarmCreated(name: String, radius: Float) {
        i("Alarm created: name='$name', radius=${radius}m", "AlarmManagement")
    }
    
    /**
     * Log alarm deletion
     */
    fun logAlarmDeleted(alarmId: Int) {
        i("Alarm deleted: id=$alarmId", "AlarmManagement")
    }
    
    /**
     * Log alarm triggered
     */
    fun logAlarmTriggered(alarmId: Int, alarmName: String) {
        i("Alarm triggered: id=$alarmId, name='$alarmName'", "AlarmTrigger")
    }
}
