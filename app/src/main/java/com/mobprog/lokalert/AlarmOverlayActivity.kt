package com.mobprog.lokalert

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AlarmOverlayActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private var originalRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Wake up the screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Dismiss keyguard if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager.requestDismissKeyguard(this, null)
        }
        
        // Initialize audio manager and override system settings
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioOverride()
        
        // Initialize vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // Get alarm data from intent
        val alarmName = intent.getStringExtra("ALARM_NAME") ?: "Location Alarm"
        val soundUri = intent.getStringExtra("SOUND_URI") ?: ""
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        val isGradualVolume = intent.getBooleanExtra("IS_GRADUAL_VOLUME", false)
        
        // Get vibration intensity from settings
        CoroutineScope(Dispatchers.Main).launch {
            val appPreferences = AppPreferences(applicationContext)
            val vibrationIntensity = appPreferences.vibrationIntensity.first()
            
            // Start vibration
            startVibration(vibrationIntensity)
            
            // Start playing alarm sound (with gradual volume if enabled)
            playAlarmSound(soundUri, isGradualVolume)
        }
        
        setContent {
            val appPreferences = remember { AppPreferences(applicationContext) }
            val darkMode by appPreferences.darkMode.collectAsState(initial = 0)
            
            LokAlertTheme(darkMode = darkMode) {
                AlarmOverlayScreen(
                    alarmName = alarmName,
                    latitude = latitude,
                    longitude = longitude,
                    onDismiss = {
                        stopAlarm()
                        finish()
                    }
                )
            }
        }
    }
    
    private fun setupAudioOverride() {
        audioManager?.let { am ->
            // Save original settings
            originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            originalRingerMode = am.ringerMode
            
            // Override Do Not Disturb and set to max volume
            try {
                // Set ringer mode to normal (bypass silent/vibrate)
                am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                
                // Set alarm volume to maximum
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun restoreAudioSettings() {
        audioManager?.let { am ->
            try {
                // Restore original settings
                am.ringerMode = originalRingerMode
                am.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun startVibration(intensity: Int) {
        vibrator?.let { vib ->
            // Define vibration patterns based on intensity
            // Pattern: [delay, vibrate, delay, vibrate, ...]
            val pattern = when (intensity) {
                0 -> longArrayOf(0, 200, 500, 200, 500) // Low: short pulses with long pauses
                1 -> longArrayOf(0, 400, 300, 400, 300) // Medium: medium pulses
                2 -> longArrayOf(0, 800, 200, 800, 200) // Strong: long intense pulses
                else -> longArrayOf(0, 800, 200, 800, 200)
            }
            
            // Amplitude for Android 8.0+
            val amplitude = when (intensity) {
                0 -> 64   // Low: ~25% intensity
                1 -> 128  // Medium: ~50% intensity
                2 -> 255  // Strong: 100% max intensity
                else -> 255
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect for better control
                val effect = VibrationEffect.createWaveform(
                    pattern,
                    intArrayOf(0, amplitude, 0, amplitude, 0),
                    0 // Repeat from index 0 (continuous)
                )
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0) // 0 = repeat from beginning
            }
        }
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
    }
    
    private var volumeRampJob: kotlinx.coroutines.Job? = null
    
    private fun playAlarmSound(soundUriString: String, isGradualVolume: Boolean = false) {
        try {
            val uri = if (soundUriString.isNotEmpty()) {
                Uri.parse(soundUriString)
            } else {
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                
                // Use ALARM stream to bypass DND
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                
                isLooping = true
                
                // Start at low volume if gradual, otherwise max
                if (isGradualVolume) {
                    setVolume(0.1f, 0.1f) // Start at 10% volume
                } else {
                    setVolume(1.0f, 1.0f) // Max volume
                }
                
                prepare()
                start()
            }
            
            // If gradual volume is enabled, ramp up the volume over time
            if (isGradualVolume) {
                volumeRampJob = CoroutineScope(Dispatchers.Main).launch {
                    val startVolume = 0.1f
                    val endVolume = 1.0f
                    val rampDuration = 30000L // 30 seconds to reach max volume
                    val steps = 60 // Number of volume steps
                    val stepDelay = rampDuration / steps
                    val volumeIncrement = (endVolume - startVolume) / steps
                    
                    var currentVolume = startVolume
                    for (i in 0 until steps) {
                        delay(stepDelay)
                        currentVolume = (startVolume + (volumeIncrement * (i + 1))).coerceIn(startVolume, endVolume)
                        mediaPlayer?.setVolume(currentVolume, currentVolume)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAlarm() {
        stopVibration()
        
        // Cancel volume ramp job if running
        volumeRampJob?.cancel()
        volumeRampJob = null
        
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
        
        restoreAudioSettings()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}

@Composable
fun AlarmOverlayScreen(
    alarmName: String,
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    
    // Get customization preferences
    val dismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val backgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    val showDistance by appPreferences.overlayShowDistance.collectAsState(initial = true)
    val showEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val primaryColorHex by appPreferences.overlayPrimaryColor.collectAsState(initial = "FF6B6B")
    
    // Parse color from hex
    val primaryColor = remember(primaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor("#$primaryColorHex"))
        } catch (e: Exception) {
            Color(0xFFFF6B6B)
        }
    }
    val secondaryColor = remember(primaryColor) {
        primaryColor.copy(
            red = (primaryColor.red + 0.1f).coerceIn(0f, 1f),
            green = (primaryColor.green + 0.05f).coerceIn(0f, 1f)
        )
    }
    
    // Track current distance
    var currentDistance by remember { mutableStateOf<Float?>(null) }
    
    // Continuously update location
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        while (true) {
            try {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val alarmLocation = android.location.Location("").apply {
                                this.latitude = latitude
                                this.longitude = longitude
                            }
                            val userLocation = android.location.Location("").apply {
                                this.latitude = it.latitude
                                this.longitude = it.longitude
                            }
                            currentDistance = userLocation.distanceTo(alarmLocation)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1000) // Update every second
        }
    }
    
    // Pulsing animation for the background
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Background based on style
    val backgroundModifier = when (backgroundStyle) {
        0 -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha),
                    secondaryColor.copy(alpha = alpha)
                )
            )
        )
        1 -> Modifier.background(primaryColor.copy(alpha = 0.9f))
        2 -> Modifier.background(Color(0xFF1A1A1A))
        else -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha),
                    secondaryColor.copy(alpha = alpha)
                )
            )
        )
    }
    
    // Swipe up to dismiss modifier
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 300f
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(backgroundModifier)
            .then(
                if (dismissStyle == 1) {
                    Modifier.draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            swipeOffset = (swipeOffset + delta).coerceAtMost(0f)
                        },
                        onDragStopped = {
                            if (swipeOffset < -swipeThreshold) {
                                onDismiss()
                            } else {
                                swipeOffset = 0f
                            }
                        }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated alarm icon (conditionally shown)
            if (showEmoji) {
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Text(
                    text = "🚨",
                    fontSize = (80 * scale).sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            
            // Location name
            Text(
                text = "You have entered the radius of your set location:",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = alarmName,
                fontSize = 32.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Distance info (conditionally shown)
            if (showDistance) {
                currentDistance?.let { distance ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "You are",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${distance.roundToInt()}m",
                                fontSize = 48.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "away from your destination!",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Dismiss control based on style
            when (dismissStyle) {
                0 -> SliderToDismiss(onDismiss = onDismiss, accentColor = primaryColor)
                1 -> SwipeUpToDismiss()
                2 -> ButtonToDismiss(onDismiss = onDismiss, accentColor = primaryColor)
                else -> SliderToDismiss(onDismiss = onDismiss, accentColor = primaryColor)
            }
        }
    }
}

@Composable
fun SwipeUpToDismiss() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "↑",
            fontSize = 32.sp,
            color = Color.White
        )
        Text(
            text = "Swipe up to dismiss",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ButtonToDismiss(onDismiss: () -> Unit, accentColor: Color = Color(0xFFFF6B6B)) {
    Button(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = accentColor
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Text(
            text = "Dismiss Alarm",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SliderToDismiss(onDismiss: () -> Unit, accentColor: Color = Color(0xFFFF6B6B)) {
    val density = LocalDensity.current
    val trackWidth = 300.dp
    val handleSize = 62.dp
    val maxSwipe = with(density) { (trackWidth - handleSize).toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val progress = (offsetX / maxSwipe).coerceIn(0f, 1f)
    
    // Check if fully swiped
    LaunchedEffect(offsetX) {
        if (offsetX >= maxSwipe * 0.85f) {
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(70.dp)
            .background(
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(35.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (handleSize.toPx() + offsetX).toDp() })
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(35.dp)
                )
        )
        
        // Track label
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = handleSize + 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Slide to Dismiss",
                color = Color.White.copy(alpha = 1f - progress),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // Draggable handle
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(4.dp)
                .size(handleSize - 8.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape((handleSize - 8.dp) / 2)
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, maxSwipe)
                    },
                    onDragStopped = {
                        // Snap back if not swiped far enough
                        if (offsetX < maxSwipe * 0.85f) {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Swipe",
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
