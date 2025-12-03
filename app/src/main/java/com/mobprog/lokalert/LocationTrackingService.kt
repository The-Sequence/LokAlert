package com.mobprog.lokalert

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class LocationTrackingService : Service() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentAlertedAlarms = mutableSetOf<Int>()
    
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
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Monitoring your location alarms..."))
        startLocationUpdates()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
        stopAlarm()
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
    
    private fun checkProximityToAlarms(currentLocation: Location) {
        serviceScope.launch {
            try {
                val database = LokAlertDatabase.getDatabase(applicationContext)
                val alarms = database.alarmDao().getAllAlarms().first()
                
                val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                
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
                        if (!currentAlertedAlarms.contains(alarm.id)) {
                            // New alarm triggered
                            if (nearestAlarmDistance == null || distance < nearestAlarmDistance!!) {
                                nearestAlarmDistance = distance
                                triggeredAlarm = alarm
                            }
                        }
                    } else {
                        // User left the radius, allow re-triggering
                        currentAlertedAlarms.remove(alarm.id)
                    }
                }
                
                // Trigger the nearest alarm
                triggeredAlarm?.let { alarm ->
                    currentAlertedAlarms.add(alarm.id)
                    withContext(Dispatchers.Main) {
                        triggerAlarm(alarm, nearestAlarmDistance ?: 0f)
                    }
                }
                
                // Update notification with current status
                val activeAlarmsCount = alarms.count { it.isEnabled }
                updateNotification("Monitoring $activeAlarmsCount alarm(s)")
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun triggerAlarm(alarm: LocationAlarm, distance: Float) {
        // Show high-priority notification
        showAlarmNotification(alarm, distance)
        
        // Play sound
        playAlarmSound(alarm)
        
        // Vibrate
        vibrateDevice()
    }
    
    private fun showAlarmNotification(alarm: LocationAlarm, distance: Float) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🚨 Location Alarm: ${alarm.name}")
            .setContentText("You're ${String.format("%.0f", distance)}m away from your destination!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Use system icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(alarm.id + 10000, notification) // Different ID from service notification
    }
    
    private fun playAlarmSound(alarm: LocationAlarm) {
        try {
            stopAlarm() // Stop any currently playing alarm
            
            val soundUri = if (alarm.soundUri.isNotEmpty()) {
                Uri.parse(alarm.soundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                
                if (alarm.isGradualVolume) {
                    setVolume(0.1f, 0.1f)
                    // Gradually increase volume
                    serviceScope.launch {
                        for (i in 1..10) {
                            delay(500)
                            val volume = i / 10f
                            mediaPlayer?.setVolume(volume, volume)
                        }
                    }
                }
                
                isLooping = true
                prepare()
                start()
            }
            
            // Auto-stop after 30 seconds
            serviceScope.launch {
                delay(30000)
                stopAlarm()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }
    
    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
