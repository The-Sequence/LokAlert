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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlin.math.abs
import kotlin.math.roundToInt

class AlarmOverlayActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private var originalRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
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
                try {
                    val keyguardManager = getSystemService(KeyguardManager::class.java)
                    keyguardManager?.requestDismissKeyguard(this, null)
                } catch (e: Exception) {
                    // Keyguard dismiss failed, continue anyway
                }
            }
            
            // Initialize audio manager and override system settings
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            setupAudioOverride()
            
            // Initialize vibrator
            vibrator = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            // Log but continue - alarm should still show even if some features fail
            e.printStackTrace()
        }
        
        // Get alarm data from intent with safe defaults
        val alarmName = intent?.getStringExtra("ALARM_NAME") ?: "Location Alarm"
        val soundUri = intent?.getStringExtra("SOUND_URI") ?: ""
        val latitude = intent?.getDoubleExtra("LATITUDE", 0.0) ?: 0.0
        val longitude = intent?.getDoubleExtra("LONGITUDE", 0.0) ?: 0.0
        val isGradualVolume = intent?.getBooleanExtra("IS_GRADUAL_VOLUME", false) ?: false
        
        // Get vibration intensity from settings
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val appPreferences = AppPreferences(applicationContext)
                val vibrationIntensity = appPreferences.vibrationIntensity.first()
                
                // Start vibration
                startVibration(vibrationIntensity)
                
                // Start playing alarm sound (with gradual volume if enabled)
                playAlarmSound(soundUri, isGradualVolume)
            } catch (e: Exception) {
                // Log error but continue showing the alarm UI
                e.printStackTrace()
            }
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
                        // Disable the default closing animation
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
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
            
            // Force alarm to play regardless of DND or ringer mode
            try {
                // CRITICAL: Use STREAM_ALARM which bypasses DND by default
                // Do NOT change ringer mode - this can interfere with system state
                // STREAM_ALARM always plays regardless of DND/silent/vibrate mode
                
                // Ensure alarm volume is audible (at least 70% of max)
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetVolume = (maxVolume * 0.7f).toInt().coerceAtLeast(1)
                val currentVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                
                if (currentVolume < targetVolume) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
                }
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
            val pattern = when (intensity) {
                0 -> longArrayOf(0, 200, 500, 200, 500) // Low
                1 -> longArrayOf(0, 400, 300, 400, 300) // Medium
                2 -> longArrayOf(0, 800, 200, 800, 200) // Strong
                else -> longArrayOf(0, 800, 200, 800, 200)
            }
            
            val amplitude = when (intensity) {
                0 -> 85   // Low - increased from 64
                1 -> 170  // Medium - increased from 128
                2 -> 255  // Strong - max
                else -> 255
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect with USAGE_ALARM to bypass DND
                val effect = VibrationEffect.createWaveform(
                    pattern,
                    intArrayOf(0, amplitude, 0, amplitude, 0),
                    0 // Repeat from index 0
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ - use VibrationAttributes with USAGE_ALARM
                    val attributes = android.os.VibrationAttributes.Builder()
                        .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                        .build()
                    vib.vibrate(effect, attributes)
                } else {
                    // Android 8-12 - use AudioAttributes with USAGE_ALARM
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    vib.vibrate(effect, audioAttributes)
                }
            } else {
                // Pre-Android O - simple vibration pattern
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
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
                
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                
                isLooping = true
                
                if (isGradualVolume) {
                    setVolume(0.1f, 0.1f)
                } else {
                    setVolume(1.0f, 1.0f)
                }
                
                prepare()
                start()
            }
            
            if (isGradualVolume) {
                volumeRampJob = CoroutineScope(Dispatchers.Main).launch {
                    val startVolume = 0.1f
                    val endVolume = 1.0f
                    val rampDuration = 30000L
                    val steps = 60
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
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Get customization preferences
    val dismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val backgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    val showDistance by appPreferences.overlayShowDistance.collectAsState(initial = true)
    val showEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val primaryColorHex by appPreferences.overlayPrimaryColor.collectAsState(initial = "FF6B6B")
    val customEmoji by appPreferences.overlayEmoji.collectAsState(initial = "🚨")
    
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
    
    // iOS-style swipe up state
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = screenHeight * 0.3f // 30% of screen height to dismiss
    val swipeProgress = (abs(swipeOffset) / swipeThreshold).coerceIn(0f, 1f)
    
    // Animated swipe offset for spring back
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )
    
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
            delay(1000)
        }
    }
    
    // Performance: Check if device is low-end to optimize animations
    val isLowEndDevice = rememberIsLowEndDevice()
    val animDuration = if (isLowEndDevice) 1500 else 1000
    
    // Pulsing animation for the background (optimized for device performance)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Background based on style - for swipe mode, we need full opacity initially
    // For other modes, backgrounds are always fully opaque
    val isSwipeMode = dismissStyle == 1
    
    val backgroundModifier = when (backgroundStyle) {
        0 -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isSwipeMode) 1f else alpha),
                    secondaryColor.copy(alpha = if (isSwipeMode) 1f else alpha)
                )
            )
        )
        1 -> Modifier.background(primaryColor)
        2 -> Modifier.background(Color(0xFF1A1A1A))
        else -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isSwipeMode) 1f else alpha),
                    secondaryColor.copy(alpha = if (isSwipeMode) 1f else alpha)
                )
            )
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // For non-swipe modes, add a full black background to ensure opacity
        if (!isSwipeMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        
        // For swipe up mode: show a scrim that fades as user swipes
        if (isSwipeMode && swipeProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * (1f - swipeProgress)))
            )
        }
        
        // Main alarm overlay content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animatedSwipeOffset.roundToInt()) }
                .then(
                    if (isSwipeMode && swipeProgress > 0f) {
                        // Swipe up: overlay fades and scales as you swipe, revealing what's behind
                        Modifier
                            .alpha(1f - (swipeProgress * 0.6f))
                            .scale(1f - (swipeProgress * 0.05f))
                    } else {
                        Modifier
                    }
                )
                .then(backgroundModifier)
                .then(
                    if (isSwipeMode) {
                        Modifier.draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                // Only allow swiping up (negative delta)
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
                // Animated alarm icon (conditionally shown) - now uses custom emoji
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
                        text = customEmoji,
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
                    1 -> SwipeUpToDismiss(swipeProgress = swipeProgress)
                    2 -> ButtonToDismiss(onDismiss = onDismiss, accentColor = primaryColor)
                    else -> SliderToDismiss(onDismiss = onDismiss, accentColor = primaryColor)
                }
            }
        }
    }
}

@Composable
fun SwipeUpToDismiss(swipeProgress: Float = 0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "swipeHint")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = bounceOffset.dp)
    ) {
        // Animated chevrons
        Text(
            text = "⌃",
            fontSize = 28.sp,
            color = Color.White.copy(alpha = 0.6f + (swipeProgress * 0.4f))
        )
        Text(
            text = "⌃",
            fontSize = 28.sp,
            color = Color.White.copy(alpha = 0.4f + (swipeProgress * 0.4f)),
            modifier = Modifier.offset(y = (-12).dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
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