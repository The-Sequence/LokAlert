package com.mobprog.lokalert

/**
 * Application-wide constants for LokAlert
 */
object Constants {
    // Alarm Configuration
    const val MIN_RADIUS = 100f
    const val MAX_RADIUS = 1000f
    const val DEFAULT_RADIUS = 100f
    const val MAX_ALARM_NAME_LENGTH = 50
    const val DEFAULT_ALARM_NAME = "Time to wake up!"
    
    // Map Configuration
    const val DEFAULT_MAP_ZOOM = 10f
    const val LOCATION_SEARCH_ZOOM = 15f
    const val USER_LOCATION_ZOOM = 16f
    
    // UI Configuration
    const val MAX_RECENT_SEARCHES = 5
}
