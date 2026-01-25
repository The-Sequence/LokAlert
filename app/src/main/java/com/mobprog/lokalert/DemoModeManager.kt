package com.mobprog.lokalert

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Demo Mode Manager - Manages mock location simulation for demonstration purposes.
 * 
 * Features:
 * - Mock current location (blue dot)
 * - Mock destination with adjustable radius
 * - Configurable movement speed
 * - Automatic movement toward destination
 * - Triggers real geofence alerts when entering alarm radii
 */
class DemoModeManager(private val context: Context) {
    
    private val appPreferences = AppPreferences(context)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Current mock location state
    private val _mockLocation = MutableStateFlow<LatLng?>(null)
    val mockLocation: StateFlow<LatLng?> = _mockLocation.asStateFlow()
    
    // Movement state
    private var movementJob: Job? = null
    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()
    
    // Progress toward destination (0.0 to 1.0)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // Demo mode enabled state
    private val _isDemoModeEnabled = MutableStateFlow(false)
    val isDemoModeEnabled: StateFlow<Boolean> = _isDemoModeEnabled.asStateFlow()
    
    // Current speed in m/s
    private val _speedMps = MutableStateFlow(5)
    val speedMps: StateFlow<Int> = _speedMps.asStateFlow()
    
    // Destination
    private val _destination = MutableStateFlow<LatLng?>(null)
    val destination: StateFlow<LatLng?> = _destination.asStateFlow()
    
    // Destination radius
    private val _destinationRadius = MutableStateFlow(100)
    val destinationRadius: StateFlow<Int> = _destinationRadius.asStateFlow()
    
    // Start position (for restart/reset purposes)
    private var startPosition: LatLng? = null
    
    // Distance to destination in meters (calculated in real-time)
    private val _distanceToDestination = MutableStateFlow(0.0)
    val distanceToDestination: StateFlow<Double> = _distanceToDestination.asStateFlow()
    
    // Signal to restart demo onboarding flow (used after alarm dismissal)
    private val _restartOnboardingRequested = MutableStateFlow(false)
    val restartOnboardingRequested: StateFlow<Boolean> = _restartOnboardingRequested.asStateFlow()
    
    // Flag to prevent preference collectors from restoring state during restart
    private var isRestartPending = false
    
    init {
        // Load initial state from preferences
        scope.launch {
            launch {
                appPreferences.demoModeEnabled.collect { enabled ->
                    _isDemoModeEnabled.value = enabled
                    if (!enabled) {
                        stopMovement()
                    }
                }
            }
            
            launch {
                combine(
                    appPreferences.demoMockLatitude,
                    appPreferences.demoMockLongitude
                ) { lat, lng ->
                    LatLng(lat, lng)
                }.collect { location ->
                    if (_mockLocation.value == null || !_isMoving.value) {
                        _mockLocation.value = location
                        startPosition = location
                    }
                }
            }
            
            launch {
                combine(
                    appPreferences.demoDestinationLatitude,
                    appPreferences.demoDestinationLongitude
                ) { lat, lng ->
                    LatLng(lat, lng)
                }.collect { dest ->
                    _destination.value = dest
                }
            }
            
            launch {
                appPreferences.demoDestinationRadius.collect { radius ->
                    _destinationRadius.value = radius
                }
            }
            
            launch {
                appPreferences.demoSpeedMps.collect { speed ->
                    _speedMps.value = speed
                }
            }
            
            // Continuously update distance to destination
            launch {
                combine(_mockLocation, _destination) { mock, dest ->
                    if (mock != null && dest != null) {
                        calculateDistance(mock, dest)
                    } else {
                        0.0
                    }
                }.collect { distance ->
                    _distanceToDestination.value = distance
                }
            }
            
            launch {
                appPreferences.demoIsMoving.collect { moving ->
                    if (moving && !_isMoving.value && _isDemoModeEnabled.value) {
                        startMovement()
                    } else if (!moving && _isMoving.value) {
                        stopMovement()
                    }
                }
            }
        }
    }
    
    /**
     * Start moving the mock location toward the destination
     */
    fun startMovement() {
        if (movementJob?.isActive == true) return
        
        val dest = _destination.value ?: return
        val start = _mockLocation.value ?: startPosition ?: return
        
        _isMoving.value = true
        scope.launch { appPreferences.setDemoIsMoving(true) }
        
        movementJob = scope.launch {
            val totalDistance = calculateDistance(start, dest)
            var distanceTraveled = 0.0
            var currentPosition = start
            
            // Update every 100ms for smooth animation
            val updateInterval = 100L
            
            while (isActive && distanceTraveled < totalDistance) {
                delay(updateInterval)
                
                // Calculate distance to move this tick
                val speedMps = _speedMps.value.toDouble()
                val distanceThisTick = speedMps * (updateInterval / 1000.0)
                distanceTraveled += distanceThisTick
                
                // Calculate new position
                val fraction = (distanceTraveled / totalDistance).coerceIn(0.0, 1.0)
                currentPosition = interpolatePosition(start, dest, fraction)
                
                _mockLocation.value = currentPosition
                _progress.value = fraction.toFloat()
                
                // Update preferences periodically (every 500ms) for service to pick up
                if ((distanceTraveled.toLong() % 5) == 0L) {
                    appPreferences.setDemoMockLocation(currentPosition.latitude, currentPosition.longitude)
                }
            }
            
            // Arrived at destination
            _mockLocation.value = dest
            _progress.value = 1f
            _isMoving.value = false
            appPreferences.setDemoIsMoving(false)
            appPreferences.setDemoMockLocation(dest.latitude, dest.longitude)
        }
    }
    
    /**
     * Stop movement and keep current position
     */
    fun stopMovement() {
        movementJob?.cancel()
        movementJob = null
        _isMoving.value = false
        scope.launch { appPreferences.setDemoIsMoving(false) }
    }
    
    /**
     * Reset mock location to start position
     */
    fun resetToStart() {
        stopMovement()
        startPosition?.let { start ->
            _mockLocation.value = start
            _progress.value = 0f
            scope.launch {
                appPreferences.setDemoMockLocation(start.latitude, start.longitude)
            }
        }
    }
    
    /**
     * Set a new start position for the mock location
     */
    fun setMockLocation(location: LatLng) {
        stopMovement()
        _mockLocation.value = location
        startPosition = location
        _progress.value = 0f
        scope.launch {
            appPreferences.setDemoMockLocation(location.latitude, location.longitude)
        }
    }
    
    /**
     * Set the destination location
     */
    fun setDestination(location: LatLng) {
        _destination.value = location
        scope.launch {
            appPreferences.setDemoDestination(location.latitude, location.longitude)
        }
    }
    
    /**
     * Set the destination radius
     */
    fun setDestinationRadius(radius: Int) {
        _destinationRadius.value = radius.coerceIn(10, 1000)
        scope.launch {
            appPreferences.setDemoDestinationRadius(radius)
        }
    }
    
    /**
     * Set movement speed in meters per second
     */
    fun setSpeed(speedMps: Int) {
        _speedMps.value = speedMps.coerceIn(1, 50)
        scope.launch {
            appPreferences.setDemoSpeedMps(speedMps)
        }
    }
    
    /**
     * Enable or disable demo mode
     */
    fun setDemoModeEnabled(enabled: Boolean) {
        _isDemoModeEnabled.value = enabled
        if (!enabled) {
            stopMovement()
            // Clear demo state when disabling
            _destination.value = null
            _mockLocation.value = null
            startPosition = null
            _progress.value = 0f
        }
        scope.launch {
            appPreferences.setDemoModeEnabled(enabled)
        }
    }
    
    /**
     * Request restart of demo onboarding flow (called from alarm overlay)
     */
    fun requestRestartOnboarding() {
        // Set flag to prevent preference collectors from restoring state
        isRestartPending = true
        
        // Reset demo state for fresh start
        stopMovement()
        _destination.value = null
        _mockLocation.value = null
        startPosition = null
        _progress.value = 0f
        _restartOnboardingRequested.value = true
    }
    
    /**
     * Acknowledge that restart onboarding has been handled
     */
    fun acknowledgeRestartRequest() {
        _restartOnboardingRequested.value = false
        // Clear the restart pending flag so preferences can sync again
        isRestartPending = false
    }
    
    /**
     * Exit demo mode completely (called when user declines restart)
     */
    fun exitDemoMode() {
        isRestartPending = false
        setDemoModeEnabled(false)
    }
    
    /**
     * Get current mock location as Android Location object (for service integration)
     */
    fun getCurrentMockLocationAsAndroidLocation(): Location? {
        val mockLoc = _mockLocation.value ?: return null
        return Location("demo_provider").apply {
            latitude = mockLoc.latitude
            longitude = mockLoc.longitude
            accuracy = 1f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
        }
    }
    
    /**
     * Calculate distance between two points in meters using Haversine formula
     */
    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        
        val lat1Rad = Math.toRadians(start.latitude)
        val lat2Rad = Math.toRadians(end.latitude)
        val deltaLatRad = Math.toRadians(end.latitude - start.latitude)
        val deltaLngRad = Math.toRadians(end.longitude - start.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLngRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Interpolate between two positions based on fraction (0.0 to 1.0)
     */
    private fun interpolatePosition(start: LatLng, end: LatLng, fraction: Double): LatLng {
        val lat = start.latitude + (end.latitude - start.latitude) * fraction
        val lng = start.longitude + (end.longitude - start.longitude) * fraction
        return LatLng(lat, lng)
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        scope.cancel()
    }
    
    companion object {
        @Volatile
        private var instance: DemoModeManager? = null
        
        fun getInstance(context: Context): DemoModeManager {
            return instance ?: synchronized(this) {
                instance ?: DemoModeManager(context.applicationContext).also { instance = it }
            }
        }
        
        // Pre-defined demo scenarios (Philippines - NU Fairview area)
        val DEMO_SCENARIOS = listOf(
            DemoScenario(
                id = "nu_to_sm_fairview",
                name = "NU → SM Fairview",
                description = "Walk from NU Fairview to SM Fairview",
                emoji = "🏫",
                startLocation = LatLng(14.7012, 121.0764),
                destination = LatLng(14.7045, 121.0785),
                destinationRadius = 100,
                suggestedSpeed = 5
            ),
            DemoScenario(
                id = "fairview_terraces",
                name = "To Fairview Terraces",
                description = "Walk to Fairview Terraces Mall",
                emoji = "🛍️",
                startLocation = LatLng(14.7012, 121.0764),
                destination = LatLng(14.6985, 121.0758),
                destinationRadius = 150,
                suggestedSpeed = 3
            ),
            DemoScenario(
                id = "commute_to_commonwealth",
                name = "Commute to Commonwealth",
                description = "Driving to Commonwealth Ave",
                emoji = "🚗",
                startLocation = LatLng(14.7012, 121.0764),
                destination = LatLng(14.6571, 121.0565),
                destinationRadius = 200,
                suggestedSpeed = 20
            ),
            DemoScenario(
                id = "mrt_north_ave",
                name = "To MRT North Ave",
                description = "Commute to MRT North Avenue",
                emoji = "🚇",
                startLocation = LatLng(14.7012, 121.0764),
                destination = LatLng(14.6523, 121.0323),
                destinationRadius = 100,
                suggestedSpeed = 25
            ),
            DemoScenario(
                id = "bike_to_park",
                name = "Bike to La Mesa Ecopark",
                description = "Cycling to La Mesa Ecopark",
                emoji = "🚴",
                startLocation = LatLng(14.7012, 121.0764),
                destination = LatLng(14.6860, 121.0725),
                destinationRadius = 50,
                suggestedSpeed = 8
            )
        )
    }
}

/**
 * Pre-defined demo scenario
 */
data class DemoScenario(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val startLocation: LatLng,
    val destination: LatLng,
    val destinationRadius: Int,
    val suggestedSpeed: Int
) {
    // Calculated properties for UI display
    val distanceKm: String
        get() {
            val distance = calculateDistance(startLocation, destination)
            return String.format("%.1f", distance)
        }
    
    val estimatedDuration: String
        get() {
            val distance = calculateDistance(startLocation, destination)
            val timeSeconds = (distance * 1000) / suggestedSpeed
            val minutes = (timeSeconds / 60).toInt()
            return when {
                minutes < 1 -> "<1 min"
                minutes < 60 -> "$minutes min"
                else -> "${minutes / 60}h ${minutes % 60}m"
            }
        }
    
    val difficulty: String
        get() = when {
            suggestedSpeed <= 3 -> "Beginner"
            suggestedSpeed <= 15 -> "Intermediate"
            else -> "Advanced"
        }
    
    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371.0 // km
        val lat1Rad = Math.toRadians(start.latitude)
        val lat2Rad = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLon = Math.toRadians(end.longitude - start.longitude)
        
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}
