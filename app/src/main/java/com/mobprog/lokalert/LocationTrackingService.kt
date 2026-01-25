package com.mobprog.lokalert

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class LocationTrackingService : Service() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var currentAlertedAlarms = mutableSetOf<Int>()
    private val alarmCooldownTimestamps = mutableMapOf<Int, Long>()
    private lateinit var appPreferences: AppPreferences
    
    // Demo mode support
    private lateinit var demoModeManager: DemoModeManager
    private var demoModeObserverJob: Job? = null
    private var isUsingDemoMode = false
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds
        
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(applicationContext)
        demoModeManager = DemoModeManager.getInstance(applicationContext)
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Monitoring your location alarms..."))
        startLocationUpdates()
        startDemoModeObserver()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopDemoModeObserver()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location to trigger alarms"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("LokAlert Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    checkProximityToAlarms(location)
                }
            }
        }
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    /**
     * Start observing demo mode mock location updates
     * When demo mode is enabled and mock location changes, trigger proximity checks
     */
    private fun startDemoModeObserver() {
        demoModeObserverJob?.cancel()
        
        demoModeObserverJob = serviceScope.launch {
            // Observe both demo mode enabled state and mock location
            demoModeManager.isDemoModeEnabled.collect { isDemoEnabled ->
                android.util.Log.d("LocationService", "Demo mode enabled: $isDemoEnabled")
                
                if (isDemoEnabled) {
                    // Demo mode is ON - stop GPS and observe mock location
                    if (!isUsingDemoMode) {
                        android.util.Log.d("LocationService", "Switching to demo mode location source")
                        withContext(Dispatchers.Main) {
                            stopLocationUpdates()
                        }
                        isUsingDemoMode = true
                    }
                    
                    // Start observing mock location changes
                    launch {
                        demoModeManager.mockLocation.collect { mockLocation ->
                            mockLocation?.let { latLng ->
                                android.util.Log.d("LocationService", "Demo location update: ${latLng.latitude}, ${latLng.longitude}")
                                
                                // Convert LatLng to Android Location
                                val location = Location("demo").apply {
                                    latitude = latLng.latitude
                                    longitude = latLng.longitude
                                    accuracy = 10f // Simulated accuracy
                                    time = System.currentTimeMillis()
                                }
                                
                                // Trigger proximity check with demo location
                                checkProximityToAlarms(location)
                            }
                        }
                    }
                } else {
                    // Demo mode is OFF - use real GPS
                    if (isUsingDemoMode) {
                        android.util.Log.d("LocationService", "Switching back to GPS location source")
                        withContext(Dispatchers.Main) {
                            startLocationUpdates()
                        }
                        isUsingDemoMode = false
                    }
                }
            }
        }
    }
    
    /**
     * Stop observing demo mode updates
     */
    private fun stopDemoModeObserver() {
        demoModeObserverJob?.cancel()
        demoModeObserverJob = null
    }
    
    private fun checkProximityToAlarms(currentLocation: Location) {
        serviceScope.launch {
            try {
                val database = LokAlertDatabase.getDatabase(applicationContext)
                val alarms = database.alarmDao().getAllAlarms().first()
                
                // Get cooldown settings
                val isCooldownEnabled = appPreferences.isCooldownEnabled.first()
                val cooldownMinutes = appPreferences.cooldownMinutes.first()
                val cooldownPeriod = cooldownMinutes * 60 * 1000L // Convert to milliseconds
                
                val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                val currentTime = System.currentTimeMillis()
                
                var nearestAlarmDistance: Float? = null
                var triggeredAlarm: LocationAlarm? = null
                
                for (alarm in alarms) {
                    if (!alarm.isEnabled) continue
                    if (alarm.activeDays.isNotEmpty() && !alarm.activeDays.contains(currentDay)) continue
                    
                    val alarmLocation = Location("").apply {
                        latitude = alarm.latitude
                        longitude = alarm.longitude
                    }
                    
                    val distance = currentLocation.distanceTo(alarmLocation)
                    
                    // Check if within radius
                    if (distance <= alarm.radius) {
                        // Check if cooldown is enabled and if alarm is in cooldown
                        val canTrigger = if (isCooldownEnabled) {
                            val lastTriggered = alarmCooldownTimestamps[alarm.id] ?: 0L
                            (currentTime - lastTriggered) > cooldownPeriod
                        } else {
                            // If cooldown disabled, use the old logic (only trigger once per entry)
                            !currentAlertedAlarms.contains(alarm.id)
                        }
                        
                        if (canTrigger) {
                            // New alarm triggered
                            if (nearestAlarmDistance == null || distance < nearestAlarmDistance) {
                                nearestAlarmDistance = distance
                                triggeredAlarm = alarm
                            }
                        }
                    } else {
                        // User left the radius
                        if (!isCooldownEnabled) {
                            // Only remove from alerted set if cooldown is disabled
                            currentAlertedAlarms.remove(alarm.id)
                        }
                        // Don't clear cooldown timestamp - let it expire naturally
                    }
                }
                
                // Trigger the nearest alarm
                triggeredAlarm?.let { alarm ->
                    if (isCooldownEnabled) {
                        alarmCooldownTimestamps[alarm.id] = currentTime
                    } else {
                        currentAlertedAlarms.add(alarm.id)
                    }
                    
                    withContext(Dispatchers.Main) {
                        triggerAlarm(alarm, nearestAlarmDistance ?: 0f)
                    }
                }
                
                // ========================================
                // DEMO MODE: Check proximity to demo destination
                // ========================================
                val isDemoModeActive = appPreferences.demoModeEnabled.first()
                if (isDemoModeActive && triggeredAlarm == null) {
                    // Get demo destination from DemoModeManager (access from outer scope)
                    val demoDestination = this@LocationTrackingService.demoModeManager.destination.value
                    val demoDestinationRadius = this@LocationTrackingService.demoModeManager.destinationRadius.value
                    
                    if (demoDestination != null) {
                        val demoDestLocation = Location("").apply {
                            latitude = demoDestination.latitude
                            longitude = demoDestination.longitude
                        }
                        
                        val distanceToDest = currentLocation.distanceTo(demoDestLocation)
                        
                        android.util.Log.d("LocationService", "Demo Destination check: distance=${distanceToDest}m, radius=${demoDestinationRadius}m")
                        
                        // Check if within demo destination radius
                        if (distanceToDest <= demoDestinationRadius) {
                            android.util.Log.d("LocationService", "✓ Within demo destination radius!")
                            
                            // Check if we haven't already triggered demo destination alarm
                            val DEMO_DEST_ALARM_ID = -999 // Special ID for demo destination
                            val canTriggerDemo = if (isCooldownEnabled) {
                                val lastTriggered = alarmCooldownTimestamps[DEMO_DEST_ALARM_ID] ?: 0L
                                (currentTime - lastTriggered) > cooldownPeriod
                            } else {
                                !currentAlertedAlarms.contains(DEMO_DEST_ALARM_ID)
                            }
                            
                            if (canTriggerDemo) {
                                android.util.Log.d("LocationService", "🚨 TRIGGERING DEMO DESTINATION ALARM at distance ${distanceToDest}m")
                                
                                // Mark as triggered
                                if (isCooldownEnabled) {
                                    alarmCooldownTimestamps[DEMO_DEST_ALARM_ID] = currentTime
                                } else {
                                    currentAlertedAlarms.add(DEMO_DEST_ALARM_ID)
                                }
                                
                                // Create a temporary alarm object for demo destination
                                val demoAlarm = LocationAlarm(
                                    id = DEMO_DEST_ALARM_ID,
                                    name = "🎯 Demo Destination Reached",
                                    latitude = demoDestination.latitude,
                                    longitude = demoDestination.longitude,
                                    radius = demoDestinationRadius.toFloat(),
                                    soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM).toString(),
                                    isGradualVolume = false,
                                    activeDays = emptySet(),
                                    isEnabled = true,
                                    isFavorite = false
                                )
                                
                                withContext(Dispatchers.Main) {
                                    triggerAlarm(demoAlarm, distanceToDest)
                                }
                            } else {
                                android.util.Log.d("LocationService", "  ✗ Demo destination already triggered (cooldown or already alerted)")
                            }
                        } else {
                            // Outside demo destination radius - clear triggered state
                            val DEMO_DEST_ALARM_ID = -999
                            if (!isCooldownEnabled) {
                                currentAlertedAlarms.remove(DEMO_DEST_ALARM_ID)
                            }
                        }
                    }
                }
                
                // Update notification with current status
                val activeAlarmsCount = alarms.count { it.isEnabled }
                val isDemoEnabled = appPreferences.demoModeEnabled.first()
                val statusText = if (isDemoEnabled) {
                    "📍 Demo Mode - Monitoring $activeAlarmsCount alarm(s)"
                } else {
                    "Monitoring $activeAlarmsCount alarm(s)"
                }
                updateNotification(statusText)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun triggerAlarm(alarm: LocationAlarm, @Suppress("UNUSED_PARAMETER") distance: Float) {
        android.util.Log.d("LocationService", "Launching AlarmOverlayActivity for alarm: ${alarm.name}")
        
        // Check if this is a demo mode alarm (id = -999)
        val isDemoAlarm = alarm.id == -999
        
        // Launch full-screen alarm overlay
        val intent = Intent(this, AlarmOverlayActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_NAME", alarm.name)
            putExtra("SOUND_URI", alarm.soundUri)
            putExtra("IS_GRADUAL_VOLUME", alarm.isGradualVolume)
            putExtra("LATITUDE", alarm.latitude)
            putExtra("LONGITUDE", alarm.longitude)
            putExtra("IS_DEMO_MODE", isDemoAlarm)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
