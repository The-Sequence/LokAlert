package com.mobprog.lokalert

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit,
    mapsViewModel: MapsViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context) }
    
    // Detect screen size and orientation for responsive layout
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Determine if this is a wide/unfolded device vs a slab phone
    // Wide devices: tablets, unfolded foldables (usually > 600dp in smallest dimension)
    // Slab phones: narrow in one dimension (usually < 600dp height when in landscape)
    val smallestDimension = minOf(screenWidth, screenHeight)
    val isWideDevice = smallestDimension > 500.dp
    
    // Show rotation prompt for slab phones in landscape (not for wide devices)
    val shouldShowRotationPrompt = isLandscape && !isWideDevice
    
    // Use two-pane layout for wide screens OR landscape with sufficient width (only for wide devices)
    val isWideScreen = isWideDevice && (screenWidth > 600.dp || isLandscape)
    
    // Dynamic left pane width based on screen size
    val leftPaneWidth = when {
        screenWidth > 900.dp -> 320.dp
        screenWidth > 700.dp -> 280.dp
        isLandscape -> (screenWidth.value * 0.35f).dp.coerceIn(200.dp, 300.dp)
        else -> 280.dp
    }
    
    // Selected setting category for two-pane layout
    var selectedCategory by remember { mutableStateOf("Notifications") }
    
    if (shouldShowRotationPrompt) {
        // Show rotation prompt for slab phones in landscape
        RotateDevicePrompt()
    } else if (isWideScreen) {
        // Two-pane iPad-style layout
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Pane: Setting Categories (now scrollable)
            SettingsCategoriesList(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                isCompact = isLandscape && screenHeight < 500.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(leftPaneWidth)
            )
            
            // Divider
            SettingsVerticalDivider()
            
            // Right Pane: Selected Setting Details
            SettingsDetailPane(
                selectedCategory = selectedCategory,
                appPreferences = appPreferences,
                context = context,
                scope = scope,
                onColorChange = onColorChange,
                isRainbowEnabled = isRainbowEnabled,
                onRainbowToggle = onRainbowToggle,
                isCompact = isLandscape && screenHeight < 500.dp,
                mapsViewModel = mapsViewModel,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
        }
    } else {
        // Single column layout for phones (original behavior)
        SettingsSingleColumnLayout(
            appPreferences = appPreferences,
            context = context,
            scope = scope,
            onColorChange = onColorChange,
            isRainbowEnabled = isRainbowEnabled,
            onRainbowToggle = onRainbowToggle,
            mapsViewModel = mapsViewModel
        )
    }
}

@Composable
fun SettingsVerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun SettingsCategoriesList(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Category data with Material icons
    data class CategoryItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    
    val categories = listOf(
        CategoryItem("Notifications", Icons.Default.Notifications),
        CategoryItem("Sound & Haptics", Icons.AutoMirrored.Filled.VolumeUp),
        CategoryItem("Alarm Display", Icons.Default.Palette),
        CategoryItem("Appearance", Icons.Default.DarkMode),
        CategoryItem("Developer Options", Icons.Default.Build),
        CategoryItem("About", Icons.Default.Info)
    )
    
    // Dynamic padding and sizes based on compact mode
    val titleFontSize = if (isCompact) 22.sp else 28.sp
    val titlePadding = if (isCompact) 12.dp else 16.dp
    val categoryVerticalPadding = if (isCompact) 10.dp else 14.dp
    val categoryFontSize = if (isCompact) 14.sp else 16.sp
    val iconSize = if (isCompact) 20.dp else 24.dp
    
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(top = titlePadding)
    ) {
        Text(
            "Settings",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))
        
        categories.forEach { category ->
            val isSelected = category.name == selectedCategory
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelected(category.name) },
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = categoryVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.name,
                        modifier = Modifier
                            .size(iconSize)
                            .padding(end = 0.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = category.name,
                        fontSize = categoryFontSize,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
        
        // Add bottom spacing for scrolling safety
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsDetailPane(
    selectedCategory: String,
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit,
    isCompact: Boolean = false,
    mapsViewModel: MapsViewModel? = null,
    modifier: Modifier = Modifier
) {
    // For Alarm Display on wide screens, use special layout with fixed preview
    if (selectedCategory == "Alarm Display") {
        AlarmDisplayWideLayout(
            appPreferences = appPreferences,
            context = context,
            scope = scope,
            isCompact = isCompact,
            modifier = modifier
        )
    } else {
        // Dynamic padding based on compact mode
        val contentPadding = if (isCompact) 16.dp else 32.dp
        
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            when (selectedCategory) {
                "Notifications" -> NotificationsSettingDetail(appPreferences, scope, isCompact)
                "Sound & Haptics" -> SoundHapticsSettingDetail(appPreferences, context, scope, isCompact)
                "Appearance" -> AppearanceSettingDetail(appPreferences, scope, onColorChange, isRainbowEnabled, onRainbowToggle, isCompact)
                "Developer Options" -> DeveloperOptionsSettingDetail(appPreferences, scope, mapsViewModel, isCompact)
                "About" -> AboutUsDetailPane()
            }
        }
    }
}

// ============================================================================
// NOTIFICATIONS SETTINGS (formerly Cooldown)
// ============================================================================

@Composable
fun NotificationsSettingDetail(appPreferences: AppPreferences, scope: kotlinx.coroutines.CoroutineScope, isCompact: Boolean = false) {
    val isCooldownEnabled by appPreferences.isCooldownEnabled.collectAsState(initial = false)
    val cooldownMinutes by appPreferences.cooldownMinutes.collectAsState(initial = 5)
    
    // Dynamic sizing - use sp for scalability with system font settings
    val titleFontSize = if (isCompact) 20.sp else 24.sp
    val descFontSize = if (isCompact) 13.sp else 15.sp
    val optionFontSize = if (isCompact) 15.sp else 17.sp
    val subTextFontSize = if (isCompact) 12.sp else 14.sp
    val sectionSpacing = if (isCompact) 16.dp else 24.dp
    val rowVerticalPadding = if (isCompact) 10.dp else 14.dp
    
    Column {
        Text(
            text = "Repeat Prevention",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))
        Text(
            text = "Control how often alarms can trigger when you're near a location",
            fontSize = descFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = descFontSize * 1.4f
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // Disable option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        appPreferences.setCooldownEnabled(false)
                    }
                }
                .padding(vertical = rowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !isCooldownEnabled,
                onClick = {
                    scope.launch {
                        appPreferences.setCooldownEnabled(false)
                    }
                }
            )
            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alert every time",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = optionFontSize
                )
                Text(
                    text = "Alarm rings each time you enter the area",
                    fontSize = subTextFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = subTextFontSize * 1.3f
                )
            }
        }
        
        HorizontalDivider()
        
        // Enable option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        appPreferences.setCooldownEnabled(true)
                    }
                }
                .padding(vertical = rowVerticalPadding),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isCooldownEnabled,
                onClick = {
                    scope.launch {
                        appPreferences.setCooldownEnabled(true)
                    }
                }
            )
            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smart cooldown",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = optionFontSize
                )
                Text(
                    text = "Wait before alerting again after you leave and come back",
                    fontSize = subTextFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = subTextFontSize * 1.3f
                )
                
                if (isCooldownEnabled) {
                    Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 18.dp))
                    Text(
                        text = "Wait time",
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
                    ) {
                        listOf(1, 3, 5).forEach { minutes ->
                            FilterChip(
                                selected = cooldownMinutes == minutes,
                                onClick = {
                                    scope.launch {
                                        appPreferences.setCooldownMinutes(minutes)
                                    }
                                },
                                label = { Text("${minutes}m", fontSize = if (isCompact) 12.sp else 14.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
                    ) {
                        listOf(10, 15, 30).forEach { minutes ->
                            FilterChip(
                                selected = cooldownMinutes == minutes,
                                onClick = {
                                    scope.launch {
                                        appPreferences.setCooldownMinutes(minutes)
                                    }
                                },
                                label = { Text("${minutes}m", fontSize = if (isCompact) 12.sp else 14.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SOUND & HAPTICS SETTINGS (Sound + Vibration merged)
// ============================================================================

@Composable
fun SoundHapticsSettingDetail(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    isCompact: Boolean = false
) {
    val defaultAlarmSound by appPreferences.defaultAlarmSound.collectAsState(initial = "")
    val vibrationIntensity by appPreferences.vibrationIntensity.collectAsState(initial = 2)
    var showSoundPickerDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var sliderValue by remember(vibrationIntensity) { mutableFloatStateOf(vibrationIntensity.toFloat()) }
    var isSliding by remember { mutableStateOf(false) }
    
    // Dynamic sizing - improved for readability
    val titleFontSize = if (isCompact) 20.sp else 24.sp
    val descFontSize = if (isCompact) 13.sp else 15.sp
    val sectionSpacing = if (isCompact) 16.dp else 24.dp
    val sectionTitleFontSize = if (isCompact) 15.sp else 17.sp
    val subTextFontSize = if (isCompact) 12.sp else 14.sp
    
    // Get vibrator service
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }
    
    // Clean up media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator.cancel()
        }
    }
    
    // Vibrate while sliding - using USAGE_ALARM to bypass DND/silent mode
    LaunchedEffect(isSliding, sliderValue) {
        if (isSliding) {
            val intensity = sliderValue.toInt()
            val amplitude = when (intensity) {
                0 -> 85   // Low - matching AlarmOverlayActivity
                1 -> 170  // Medium - matching AlarmOverlayActivity
                2 -> 255  // Strong - max
                else -> 255
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = android.os.VibrationEffect.createOneShot(500, amplitude)
                
                // Use USAGE_ALARM attributes to bypass DND/silent mode
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ - use VibrationAttributes with USAGE_ALARM
                    val attributes = android.os.VibrationAttributes.Builder()
                        .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, attributes)
                } else {
                    // Android 8-12 - use AudioAttributes with USAGE_ALARM
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                }
                
                while (isSliding) {
                    delay(400)
                    if (isSliding) {
                        val currentAmplitude = when (sliderValue.toInt()) {
                            0 -> 85
                            1 -> 170
                            2 -> 255
                            else -> 255
                        }
                        val currentEffect = android.os.VibrationEffect.createOneShot(500, currentAmplitude)
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val attrs = android.os.VibrationAttributes.Builder()
                                .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                                .build()
                            vibrator.vibrate(currentEffect, attrs)
                        } else {
                            val audioAttrs = android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                            vibrator.vibrate(currentEffect, audioAttrs)
                        }
                    }
                }
            }
        } else {
            vibrator.cancel()
        }
    }
    
    fun playPreviewSound() {
        if (isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        } else {
            try {
                val uri = if (defaultAlarmSound.isNotEmpty()) {
                    Uri.parse(defaultAlarmSound)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, uri)
                    // Use USAGE_ALARM AudioAttributes to bypass DND/silent mode
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                        release()
                        mediaPlayer = null
                    }
                }
                isPlaying = true
                
                scope.launch {
                    delay(5000)
                    if (isPlaying) {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                    }
                }
            } catch (e: Exception) {
                isPlaying = false
            }
        }
    }
    
    fun getSoundTitle(uriString: String): String {
        if (uriString.isEmpty()) return "System Default"
        return try {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
            ringtone?.getTitle(context) ?: "Custom Sound"
        } catch (e: Exception) {
            "Custom Sound"
        }
    }
    
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri ->
            scope.launch {
                appPreferences.setDefaultAlarmSound(uri.toString())
            }
        }
    }
    
    val audioFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            scope.launch {
                appPreferences.setDefaultAlarmSound(it.toString())
            }
        }
    }
    
    Column {
        Text("Sound & Haptics", fontSize = titleFontSize, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
        Text(
            "Customize alarm sounds and vibration intensity",
            fontSize = descFontSize,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === SOUND SECTION ===
        Text("Alarm Sound", fontSize = sectionTitleFontSize, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getSoundTitle(defaultAlarmSound),
                fontSize = if (isCompact) 14.sp else 16.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = { playPreviewSound() }) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play preview",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
        ) {
            OutlinedButton(
                onClick = { playPreviewSound() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPlaying) "Stop" else "Preview")
            }
            
            Button(
                onClick = { showSoundPickerDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change")
            }
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === VIBRATION SECTION ===
        Text(
            text = "Vibration Strength",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))
        Text(
            text = "Hold the slider to feel the intensity",
            fontSize = subTextFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = subTextFontSize * 1.3f
        )
        
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    if (!isSliding) {
                        isSliding = true
                    }
                },
                onValueChangeFinished = {
                    isSliding = false
                    vibrator.cancel()
                    val intensity = sliderValue.toInt()
                    scope.launch {
                        appPreferences.setVibrationIntensity(intensity)
                    }
                },
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gentle",
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = if (sliderValue.toInt() == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (sliderValue.toInt() == 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    "Normal",
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = if (sliderValue.toInt() == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (sliderValue.toInt() == 1) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    "Strong",
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = if (sliderValue.toInt() == 2) FontWeight.Bold else FontWeight.Normal,
                    color = if (sliderValue.toInt() == 2) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
        
        Text(
            when (sliderValue.toInt()) {
                0 -> "💤 Gentle vibration for quiet environments"
                1 -> "📱 Normal vibration for everyday use"
                2 -> "🔔 Strong vibration to ensure you don't miss it"
                else -> ""
            },
            fontSize = if (isCompact) 11.sp else 13.sp,
            color = Color.Gray
        )
    }
    
    if (showSoundPickerDialog) {
        AlertDialog(
            onDismissRequest = { showSoundPickerDialog = false },
            title = { Text("Choose Alarm Sound") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSoundPickerDialog = false
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                }
                                ringtoneLauncher.launch(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("System Ringtones", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Choose from built-in sounds", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSoundPickerDialog = false
                                audioFileLauncher.launch("audio/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Custom Audio File", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("Browse for MP3, WAV, etc.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundPickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================================
// ALARM DISPLAY SETTINGS - WIDE SCREEN LAYOUT (Preview fixed at top)
// ============================================================================

@Composable
fun AlarmDisplayWideLayout(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val backgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    val showDistance by appPreferences.overlayShowDistance.collectAsState(initial = true)
    val showEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val primaryColor by appPreferences.overlayPrimaryColor.collectAsState(initial = "FF6B6B")
    val customEmoji by appPreferences.overlayEmoji.collectAsState(initial = "🚨")
    val testAlarmDelayEnabled by appPreferences.testAlarmDelayEnabled.collectAsState(initial = false)
    val testAlarmDelaySeconds by appPreferences.testAlarmDelaySeconds.collectAsState(initial = 3)
    
    // Countdown state
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }
    
    // Countdown effect
    LaunchedEffect(isCountingDown, countdownValue) {
        if (isCountingDown && countdownValue > 0) {
            delay(1000)
            countdownValue -= 1
        } else if (isCountingDown && countdownValue == 0) {
            isCountingDown = false
            // Launch the actual alarm
            val intent = Intent(context, AlarmOverlayActivity::class.java).apply {
                putExtra("ALARM_NAME", "Test Alarm")
                putExtra("SOUND_URI", "")
                putExtra("LATITUDE", 0.0)
                putExtra("LONGITUDE", 0.0)
                putExtra("IS_GRADUAL_VOLUME", false)
                putExtra("IS_TEST_MODE", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    val contentPadding = if (isCompact) 12.dp else 20.dp
    
    Column(modifier = modifier.fillMaxSize()) {
        // === FIXED PREVIEW SECTION (Not scrollable) ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding)
                .padding(top = if (isCompact) 8.dp else 16.dp)
        ) {
            // Full Preview Card with interactive dismiss control
            FullAlarmPreviewCard(
                backgroundStyle = backgroundStyle,
                primaryColorHex = primaryColor,
                showEmoji = showEmoji,
                showDistance = showDistance,
                emoji = customEmoji,
                dismissStyle = dismissStyle,
                isCompact = isCompact
            )
            
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
            
            // Test Button (Fixed below preview)
            Button(
                onClick = {
                    if (testAlarmDelayEnabled) {
                        // Start countdown
                        countdownValue = testAlarmDelaySeconds
                        isCountingDown = true
                    } else {
                        // Immediate test
                        val intent = Intent(context, AlarmOverlayActivity::class.java).apply {
                            putExtra("ALARM_NAME", "Test Alarm")
                            putExtra("SOUND_URI", "")
                            putExtra("LATITUDE", 0.0)
                            putExtra("LONGITUDE", 0.0)
                            putExtra("IS_GRADUAL_VOLUME", false)
                            putExtra("IS_TEST_MODE", true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                enabled = !isCountingDown,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isCountingDown) {
                        "Alarm in $countdownValue..."
                    } else if (testAlarmDelayEnabled) {
                        "Test Alarm in $testAlarmDelaySeconds seconds"
                    } else {
                        "Test Alarm"
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
            HorizontalDivider()
        }
        
        // === SCROLLABLE OPTIONS SECTION ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = contentPadding)
                .padding(bottom = contentPadding)
        ) {
            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
            
            AlarmDisplayOptions(
                appPreferences = appPreferences,
                scope = scope,
                dismissStyle = dismissStyle,
                backgroundStyle = backgroundStyle,
                showDistance = showDistance,
                showEmoji = showEmoji,
                primaryColor = primaryColor,
                customEmoji = customEmoji,
                isCompact = isCompact
            )
        }
    }
}

// ============================================================================
// ALARM DISPLAY SETTINGS - COMPACT SCREEN (Button to open dedicated screen)
// ============================================================================

@Composable
fun AlarmDisplaySettingDetailCompact(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onOpenFullScreen: () -> Unit,
    isCompact: Boolean = false
) {
    val dismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val backgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    val primaryColor by appPreferences.overlayPrimaryColor.collectAsState(initial = "FF6B6B")
    val customEmoji by appPreferences.overlayEmoji.collectAsState(initial = "🚨")
    
    // Dynamic sizing
    val titleFontSize = if (isCompact) 20.sp else 24.sp
    val descFontSize = if (isCompact) 12.sp else 14.sp
    
    Column {
        Text("Alarm Display", fontSize = titleFontSize, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
        Text(
            "Customize how your alarm looks when it goes off",
            fontSize = descFontSize,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))
        
        // Mini preview card
        MiniAlarmPreviewCard(
            backgroundStyle = backgroundStyle,
            primaryColorHex = primaryColor,
            emoji = customEmoji,
            dismissStyle = dismissStyle
        )
        
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
        
        // Button to open full customization screen
        Button(
            onClick = onOpenFullScreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Customize Alarm Display")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
        
        // Quick test button
        OutlinedButton(
            onClick = {
                val intent = Intent(context, AlarmOverlayActivity::class.java).apply {
                    putExtra("ALARM_NAME", "Test Alarm")
                    putExtra("SOUND_URI", "")
                    putExtra("LATITUDE", 0.0)
                    putExtra("LONGITUDE", 0.0)
                    putExtra("IS_GRADUAL_VOLUME", false)
                    putExtra("IS_TEST_MODE", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Alarm")
        }
    }
}

// Keep old function for backward compatibility (used in wide screen mode)
@Composable
fun AlarmDisplaySettingDetail(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    isCompact: Boolean = false
) {
    // For wide screens, this is used directly - just show the compact version with a no-op
    // Wide screens use AlarmDisplayWideLayout instead
    AlarmDisplaySettingDetailCompact(
        appPreferences = appPreferences,
        context = context,
        scope = scope,
        onOpenFullScreen = { /* Not used in wide layout */ },
        isCompact = isCompact
    )
}

// ============================================================================
// ALARM DISPLAY FULL SCREEN (For compact devices)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDisplayFullScreen(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit
) {
    val dismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val backgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    val showDistance by appPreferences.overlayShowDistance.collectAsState(initial = true)
    val showEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val primaryColor by appPreferences.overlayPrimaryColor.collectAsState(initial = "FF6B6B")
    val customEmoji by appPreferences.overlayEmoji.collectAsState(initial = "🚨")
    val testAlarmDelayEnabled by appPreferences.testAlarmDelayEnabled.collectAsState(initial = false)
    val testAlarmDelaySeconds by appPreferences.testAlarmDelaySeconds.collectAsState(initial = 3)
    
    // Countdown state
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }
    
    // Countdown effect
    LaunchedEffect(isCountingDown, countdownValue) {
        if (isCountingDown && countdownValue > 0) {
            delay(1000)
            countdownValue -= 1
        } else if (isCountingDown && countdownValue == 0) {
            isCountingDown = false
            // Launch the actual alarm
            val intent = Intent(context, AlarmOverlayActivity::class.java).apply {
                putExtra("ALARM_NAME", "Test Alarm")
                putExtra("SOUND_URI", "")
                putExtra("LATITUDE", 0.0)
                putExtra("LONGITUDE", 0.0)
                putExtra("IS_GRADUAL_VOLUME", false)
                putExtra("IS_TEST_MODE", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    BackHandler { onBack() }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Simple header with back button - no top padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Alarm Display",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
        }
        
        // === FIXED PREVIEW SECTION ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // Full Preview Card
            FullAlarmPreviewCard(
                backgroundStyle = backgroundStyle,
                primaryColorHex = primaryColor,
                showEmoji = showEmoji,
                showDistance = showDistance,
                emoji = customEmoji,
                dismissStyle = dismissStyle,
                isCompact = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Test Button
            Button(
                    onClick = {
                        if (testAlarmDelayEnabled) {
                            // Start countdown
                            countdownValue = testAlarmDelaySeconds
                            isCountingDown = true
                        } else {
                            // Immediate test
                            val intent = Intent(context, AlarmOverlayActivity::class.java).apply {
                                putExtra("ALARM_NAME", "Test Alarm")
                                putExtra("SOUND_URI", "")
                                putExtra("LATITUDE", 0.0)
                                putExtra("LONGITUDE", 0.0)
                                putExtra("IS_GRADUAL_VOLUME", false)
                                putExtra("IS_TEST_MODE", true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    },
                    enabled = !isCountingDown,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isCountingDown) {
                            "Alarm in $countdownValue..."
                        } else if (testAlarmDelayEnabled) {
                            "Test Alarm in $testAlarmDelaySeconds seconds"
                        } else {
                            "Test Alarm"
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
            }
            
            // === SCROLLABLE OPTIONS ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AlarmDisplayOptions(
                    appPreferences = appPreferences,
                    scope = scope,
                    dismissStyle = dismissStyle,
                    backgroundStyle = backgroundStyle,
                    showDistance = showDistance,
                    showEmoji = showEmoji,
                    primaryColor = primaryColor,
                    customEmoji = customEmoji,
                    isCompact = true
                )
            }
        }
    }
}

// ============================================================================
// THEME STYLE CARD (Square card with icon for theme options - like DismissStyleCard)
// ============================================================================

@Composable
fun ThemeStyleCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: String,
    label: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val cardHeight = if (isCompact) 80.dp else 100.dp
    
    Card(
        modifier = modifier
            .height(cardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Text(
                text = icon,
                fontSize = if (isCompact) 24.sp else 32.sp
            )
            
            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
            
            // Label
            Text(
                text = label,
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// COLOR THEME CARD (For selecting app color themes)
// ============================================================================

@Composable
fun ColorThemeCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    emoji: String,
    label: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val cardHeight = if (isCompact) 70.dp else 85.dp
    
    Card(
        modifier = modifier
            .height(cardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = if (isCompact) 20.sp else 24.sp
            )
            
            Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
            
            Text(
                text = label,
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// DISMISS STYLE CARD (Square card with icon for dismiss options)
// ============================================================================

@Composable
fun DismissStyleCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: String,
    label: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val cardHeight = if (isCompact) 80.dp else 100.dp
    
    Card(
        modifier = modifier
            .height(cardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Text(
                text = icon,
                fontSize = if (isCompact) 24.sp else 32.sp
            )
            
            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
            
            // Label
            Text(
                text = label,
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// ALARM DISPLAY OPTIONS (Shared between wide and full-screen layouts)
// ============================================================================

@Composable
fun AlarmDisplayOptions(
    appPreferences: AppPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    dismissStyle: Int,
    backgroundStyle: Int,
    showDistance: Boolean,
    showEmoji: Boolean,
    primaryColor: String,
    customEmoji: String,
    isCompact: Boolean = false
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    
    // Improved font sizing for readability
    val sectionSpacing = if (isCompact) 16.dp else 24.dp
    val sectionTitleFontSize = if (isCompact) 15.sp else 17.sp
    val optionFontSize = if (isCompact) 14.sp else 16.sp
    val subTextFontSize = if (isCompact) 12.sp else 14.sp
    
    Column {
        // === DISMISS STYLE ===
        Text(
            text = "How to Dismiss",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        // Horizontal row of square dismiss style options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            // Slide to dismiss
            DismissStyleCard(
                isSelected = dismissStyle == 0,
                onClick = { scope.launch { appPreferences.setOverlayDismissStyle(0) } },
                icon = "↔️",
                label = "Slide",
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
            
            // Swipe up
            DismissStyleCard(
                isSelected = dismissStyle == 1,
                onClick = { scope.launch { appPreferences.setOverlayDismissStyle(1) } },
                icon = "⬆️",
                label = "Swipe Up",
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
            
            // Tap button
            DismissStyleCard(
                isSelected = dismissStyle == 2,
                onClick = { scope.launch { appPreferences.setOverlayDismissStyle(2) } },
                icon = "👆",
                label = "Tap",
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === BACKGROUND STYLE ===
        Text(
            text = "Background Style",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
        ) {
            listOf(0 to "Gradient", 1 to "Solid", 2 to "Dark").forEach { (style, label) ->
                FilterChip(
                    selected = backgroundStyle == style,
                    onClick = { scope.launch { appPreferences.setOverlayBackgroundStyle(style) } },
                    label = { Text(label, fontSize = if (isCompact) 12.sp else 14.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === ACCENT COLOR ===
        Text(
            text = "Accent Color",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        val colorOptions = listOf(
            "FF6B6B" to Color(0xFFFF6B6B),
            "4FC3F7" to Color(0xFF4FC3F7),
            "81C784" to Color(0xFF81C784),
            "FFD54F" to Color(0xFFFFD54F),
            "BA68C8" to Color(0xFFBA68C8),
            "FF8A65" to Color(0xFFFF8A65)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colorOptions.forEach { (hex, color) ->
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 40.dp else 48.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (primaryColor == hex) 3.dp else 0.dp,
                            color = if (primaryColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { scope.launch { appPreferences.setOverlayPrimaryColor(hex) } },
                    contentAlignment = Alignment.Center
                ) {
                    if (primaryColor == hex) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === DISPLAY OPTIONS ===
        Text(
            text = "Display Options",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { appPreferences.setOverlayShowDistance(!showDistance) } }
                .padding(vertical = if (isCompact) 10.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show distance",
                    fontWeight = FontWeight.Medium,
                    fontSize = optionFontSize
                )
                Text(
                    text = "Display how far you are from the location",
                    fontSize = subTextFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = subTextFontSize * 1.3f
                )
            }
            Switch(
                checked = showDistance,
                onCheckedChange = { scope.launch { appPreferences.setOverlayShowDistance(it) } }
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { appPreferences.setOverlayShowEmoji(!showEmoji) } }
                .padding(vertical = if (isCompact) 10.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show alarm icon",
                    fontWeight = FontWeight.Medium,
                    fontSize = optionFontSize
                )
                Text(
                    text = "Display the animated alarm emoji",
                    fontSize = subTextFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = subTextFontSize * 1.3f
                )
            }
            Switch(
                checked = showEmoji,
                onCheckedChange = { scope.launch { appPreferences.setOverlayShowEmoji(it) } }
            )
        }
        
        // === EMOJI CUSTOMIZATION ===
        if (showEmoji) {
            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEmojiPicker = true }
                    .padding(vertical = if (isCompact) 10.dp else 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alarm Emoji",
                        fontWeight = FontWeight.Medium,
                        fontSize = optionFontSize
                    )
                    Text(
                        text = "Choose your alarm icon",
                        fontSize = subTextFontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(customEmoji, fontSize = 32.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === TEST ALARM DELAY ===
        val testAlarmDelayEnabled by appPreferences.testAlarmDelayEnabled.collectAsState(initial = false)
        val testAlarmDelaySeconds by appPreferences.testAlarmDelaySeconds.collectAsState(initial = 3)
        
        Text(
            text = "Test Alarm Options",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { appPreferences.setTestAlarmDelayEnabled(!testAlarmDelayEnabled) } }
                .padding(vertical = if (isCompact) 10.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Delayed test alarm",
                    fontWeight = FontWeight.Medium,
                    fontSize = optionFontSize
                )
                Text(
                    text = "Add countdown before alarm triggers",
                    fontSize = subTextFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = subTextFontSize * 1.3f
                )
            }
            Switch(
                checked = testAlarmDelayEnabled,
                onCheckedChange = { scope.launch { appPreferences.setTestAlarmDelayEnabled(it) } }
            )
        }
        
        // Delay seconds selector (only shown when enabled)
        if (testAlarmDelayEnabled) {
            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
            
            Text(
                text = "Countdown duration",
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
            ) {
                (1..5).forEach { seconds ->
                    FilterChip(
                        selected = testAlarmDelaySeconds == seconds,
                        onClick = {
                            scope.launch {
                                appPreferences.setTestAlarmDelaySeconds(seconds)
                            }
                        },
                        label = { Text("${seconds}s", fontSize = if (isCompact) 12.sp else 14.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
            
            Text(
                text = "💡 Turn off your screen after pressing \"Test Alarm\" to test wake-up behavior",
                fontSize = subTextFontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = subTextFontSize * 1.4f
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Emoji Picker Dialog
    if (showEmojiPicker) {
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = { Text("Choose Alarm Emoji") },
            text = {
                val emojiOptions = listOf(
                    "🚨", "🔔", "⏰", "🔊", "📍", "🎯",
                    "⚠️", "🚩", "📢", "💡", "🌟", "⭐",
                    "🎉", "🎊", "✨", "💫", "🔥", "⚡"
                )
                
                Column {
                    emojiOptions.chunked(6).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (customEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            scope.launch { appPreferences.setOverlayEmoji(emoji) }
                                            showEmojiPicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmojiPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================================
// FULL ALARM PREVIEW CARD (Complete with dismiss control)
// ============================================================================

@Composable
fun FullAlarmPreviewCard(
    backgroundStyle: Int,
    primaryColorHex: String,
    showEmoji: Boolean,
    showDistance: Boolean,
    emoji: String,
    dismissStyle: Int,
    isCompact:  Boolean = false
) {
    val primaryColor = remember(primaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor("#$primaryColorHex"))
        } catch (e: Exception) {
            Color(0xFFFF6B6B)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "preview")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val backgroundModifier = when (backgroundStyle) {
        0 -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha),
                    primaryColor.copy(red = (primaryColor.red + 0.1f).coerceIn(0f, 1f), alpha = alpha)
                )
            ),
            shape = RoundedCornerShape(16.dp)
        )
        1 -> Modifier.background(primaryColor.copy(alpha = 0.9f), shape = RoundedCornerShape(16.dp))
        2 -> Modifier.background(Color(0xFF1A1A1A), shape = RoundedCornerShape(16.dp))
        else -> Modifier.background(primaryColor.copy(alpha = alpha), shape = RoundedCornerShape(16.dp))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 220.dp else 280.dp)
                .then(backgroundModifier),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                if (showEmoji) {
                    Text(
                        text = emoji,
                        fontSize = ((if (isCompact) 28 else 36) * scale).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                Text(
                    text = "Preview Location",
                    fontSize = if (isCompact) 14.sp else 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (showDistance) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "250m away",
                        fontSize = if (isCompact) 10.sp else 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 20.dp))
                
                // Dismiss control preview
                when (dismissStyle) {
                    0 -> PreviewSliderToDismiss(accentColor = primaryColor, isCompact = isCompact)
                    1 -> PreviewSwipeUpToDismiss(isCompact = isCompact)
                    2 -> PreviewButtonToDismiss(accentColor = primaryColor, isCompact = isCompact)
                }
            }
        }
    }
}

// ============================================================================
// MINI PREVIEW CARD (For compact settings list)
// ============================================================================

@Composable
fun MiniAlarmPreviewCard(
    backgroundStyle: Int,
    primaryColorHex: String,
    emoji: String,
    dismissStyle: Int
) {
    val primaryColor = remember(primaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor("#$primaryColorHex"))
        } catch (e: Exception) {
            Color(0xFFFF6B6B)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "miniPreview")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val backgroundModifier = when (backgroundStyle) {
        0 -> Modifier.background(
            Brush.verticalGradient(listOf(primaryColor.copy(alpha = alpha), primaryColor.copy(alpha = alpha * 0.8f))),
            shape = RoundedCornerShape(12.dp)
        )
        1 -> Modifier.background(primaryColor.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
        2 -> Modifier.background(Color(0xFF1A1A1A), shape = RoundedCornerShape(12.dp))
        else -> Modifier.background(primaryColor.copy(alpha = alpha), shape = RoundedCornerShape(12.dp))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .then(backgroundModifier),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Preview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        when (dismissStyle) {
                            0 -> "Slide to dismiss"
                            1 -> "Swipe up"
                            2 -> "Button"
                            else -> ""
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// PREVIEW DISMISS CONTROLS (Non-functional, just visual)
// ============================================================================

@Composable
fun PreviewSliderToDismiss(accentColor: Color, isCompact: Boolean = false) {
    val trackWidth = if (isCompact) 180.dp else 220.dp
    val handleSize = if (isCompact) 40.dp else 48.dp
    
    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(handleSize + 8.dp)
            .background(
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(handleSize / 2 + 4.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = handleSize + 4.dp, end = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Slide to Dismiss",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = if (isCompact) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(handleSize)
                .background(
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Swipe",
                tint = accentColor,
                modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
            )
        }
    }
}

@Composable
fun PreviewSwipeUpToDismiss(isCompact: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "swipeHint")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
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
        Text("⌃", fontSize = if (isCompact) 18.sp else 22.sp, color = Color.White.copy(alpha = 0.6f))
        Text("⌃", fontSize = if (isCompact) 18.sp else 22.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.offset(y = (-8).dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Swipe up to dismiss",
            fontSize = if (isCompact) 10.sp else 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PreviewButtonToDismiss(accentColor: Color, isCompact: Boolean = false) {
    Box(
        modifier = Modifier
            .width(if (isCompact) 160.dp else 200.dp)
            .height(if (isCompact) 36.dp else 44.dp)
            .background(Color.White, shape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Dismiss Alarm",
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

// Keep old AlarmPreviewCard for backward compatibility (simple version)
@Composable
fun AlarmPreviewCard(
    backgroundStyle: Int,
    primaryColorHex: String,
    showEmoji: Boolean,
    showDistance: Boolean,
    emoji: String,
    isCompact: Boolean = false
) {
    FullAlarmPreviewCard(
        backgroundStyle = backgroundStyle,
        primaryColorHex = primaryColorHex,
        showEmoji = showEmoji,
        showDistance = showDistance,
        emoji = emoji,
        dismissStyle = 0,
        isCompact = isCompact
    )
}


// ============================================================================
// APPEARANCE SETTINGS (Dark Mode + Title Style merged)
// ============================================================================

@Composable
fun AppearanceSettingDetail(
    appPreferences: AppPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit,
    isCompact: Boolean = false
) {
    val darkModeValue by appPreferences.darkMode.collectAsState(initial = 0)
    val appThemeValue by appPreferences.appTheme.collectAsState(initial = 0)
    val isSystemDark = isSystemInDarkTheme()
    
    var showColorOptions by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(3) }
    
    // Dynamic sizing - improved for readability
    val titleFontSize = if (isCompact) 20.sp else 24.sp
    val descFontSize = if (isCompact) 13.sp else 15.sp
    val sectionSpacing = if (isCompact) 16.dp else 24.dp
    val sectionTitleFontSize = if (isCompact) 15.sp else 17.sp
    val optionFontSize = if (isCompact) 14.sp else 16.sp
    val subTextFontSize = if (isCompact) 12.sp else 14.sp
    
    val colorOptions = mapOf(
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Mono" to if (isSystemDark) Color.White else Color.Black
    )
    val colorOptionKeys = colorOptions.keys.toList()
    
    Column {
        Text(
            text = "Appearance",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))
        Text(
            text = "Customize how the app looks",
            fontSize = descFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = descFontSize * 1.3f
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === THEME SECTION - Card style like How to Dismiss ===
        Text(
            text = "App Theme",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        // Theme options: Light (0), Dark (1), Auto (3)
        // Note: 2 was AMOLED, now 3 is Auto following system
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            // Light
            ThemeStyleCard(
                isSelected = darkModeValue == 0,
                onClick = { scope.launch { appPreferences.setDarkMode(0) } },
                icon = "☀️",
                label = "Light",
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
            
            // Dark
            ThemeStyleCard(
                isSelected = darkModeValue == 1,
                onClick = { scope.launch { appPreferences.setDarkMode(1) } },
                icon = "🌙",
                label = "Dark",
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
            
            // Auto
            ThemeStyleCard(
                isSelected = darkModeValue == 3,
                onClick = { scope.launch { appPreferences.setDarkMode(3) } },
                icon = "🔄",
                label = "Auto",
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        Text(
            text = when (darkModeValue) {
                0 -> "☀️ Bright theme for daytime use"
                1 -> "🌙 Dark theme, easier on the eyes"
                3 -> "🔄 Follows your system theme setting"
                else -> "🌙 Dark theme, easier on the eyes"
            },
            fontSize = subTextFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = subTextFontSize * 1.3f
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === COLOR THEME SECTION ===
        Text(
            text = "Color Theme",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))
        Text(
            text = "Choose your app's color palette",
            fontSize = subTextFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        // Theme options
        val themeOptions = listOf(
            Triple(0, "🎨", "Standard"),
            Triple(1, "✨", "Express"),
            Triple(2, "🌊", "Ocean"),
            Triple(3, "🌅", "Sunset"),
            Triple(4, "🌲", "Forest"),
            Triple(5, "🕹️", "Retro"),
            Triple(6, "⚫", "Mono")
        )
        
        // First row of themes (4 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
            themeOptions.take(4).forEach { (index, emoji, label) ->
                ColorThemeCard(
                    isSelected = appThemeValue == index,
                    onClick = { scope.launch { appPreferences.setAppTheme(index) } },
                    emoji = emoji,
                    label = label,
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))
        
        // Second row of themes (3 items + spacer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
            themeOptions.drop(4).forEach { (index, emoji, label) ->
                ColorThemeCard(
                    isSelected = appThemeValue == index,
                    onClick = { scope.launch { appPreferences.setAppTheme(index) } },
                    emoji = emoji,
                    label = label,
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        // Theme description
        Text(
            text = when (appThemeValue) {
                0 -> "🎨 Clean Material 3 design with blue accents"
                1 -> "✨ Vibrant purple and pink, bold and expressive"
                2 -> "🌊 Calming blue and teal, like ocean waves"
                3 -> "🌅 Warm oranges and corals, sunset vibes"
                4 -> "🌲 Nature-inspired greens, fresh and earthy"
                5 -> "🕹️ 80s neon colors, retro gaming aesthetic"
                6 -> "⚫ Classic black and white, minimalist"
                else -> "🎨 Clean Material 3 design"
            },
            fontSize = subTextFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = subTextFontSize * 1.3f
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // === TITLE STYLE SECTION ===
        Text(
            text = "Title Style",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rainbow animation",
                    fontWeight = FontWeight.Medium,
                    fontSize = optionFontSize
                )
                Text(
                    text = "Animated color cycling effect",
                    fontSize = subTextFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isRainbowEnabled, onCheckedChange = onRainbowToggle)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = if (isCompact) 14.dp else 18.dp))
        
        Column(modifier = Modifier.clickable(enabled = !isRainbowEnabled) {
            showColorOptions = !showColorOptions
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Title Color",
                        fontWeight = FontWeight.Medium,
                        fontSize = optionFontSize,
                        color = if (isRainbowEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRainbowEnabled) "Disabled when rainbow is on" else "Choose a static color",
                        fontSize = subTextFontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (showColorOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isRainbowEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (showColorOptions && !isRainbowEnabled) {
                Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 18.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    colorOptionKeys.forEachIndexed { index, name ->
                        val colorValue = colorOptions[name]!!
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = colorOptionKeys.size),
                            onClick = {
                                selectedColorIndex = index
                                onColorChange(colorValue)
                            },
                            selected = index == selectedColorIndex
                        ) {
                            Text(name, fontSize = optionFontSize)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// DEVELOPER OPTIONS SETTINGS
// ============================================================================

@Composable
fun DeveloperOptionsSettingDetail(
    appPreferences: AppPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    mapsViewModel: MapsViewModel?,
    isCompact: Boolean = false
) {
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var profileToLoad by remember { mutableStateOf<DemoProfile?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successProfileName by remember { mutableStateOf("") }
    
    // Dynamic sizing
    val titleFontSize = if (isCompact) 20.sp else 24.sp
    val descFontSize = if (isCompact) 13.sp else 15.sp
    val sectionSpacing = if (isCompact) 16.dp else 24.dp
    val sectionTitleFontSize = if (isCompact) 15.sp else 17.sp
    
    // Success message auto-dismiss
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(3000)
            showSuccessMessage = false
        }
    }
    
    Column {
        Text(
            text = "Developer Options",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))
        Text(
            text = "Demo profiles for testing and presentations",
            fontSize = descFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = descFontSize * 1.3f
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // Warning Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading a profile will replace your current settings and saved locations",
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    lineHeight = (if (isCompact) 12.sp else 14.sp) * 1.3f
                )
            }
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // Success Message
        if (showSuccessMessage) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Profile \"$successProfileName\" loaded successfully!",
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(sectionSpacing))
        }
        
        // === DESIGN LANGUAGE SECTION ===
        Text(
            text = "Design Language",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))
        Text(
            text = "Switch between modern and retro UI styles",
            fontSize = if (isCompact) 12.sp else 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        DesignLanguageSelector(
            appPreferences = appPreferences,
            scope = scope,
            isCompact = isCompact
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // Demo Profiles Section
        Text(
            text = "Demo Profiles",
            fontSize = sectionTitleFontSize,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
        
        // Profile Cards
        DemoProfiles.allProfiles.forEach { profile ->
            DemoProfileCard(
                profile = profile,
                isSelected = selectedProfileId == profile.id,
                onClick = { selectedProfileId = profile.id },
                onLoadProfile = {
                    profileToLoad = profile
                    showConfirmDialog = true
                },
                isCompact = isCompact
            )
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
        }
    }
    
    // Confirmation Dialog
    if (showConfirmDialog && profileToLoad != null) {
        AlertDialog(
            onDismissRequest = { 
                showConfirmDialog = false
                profileToLoad = null
            },
            icon = {
                Text(profileToLoad!!.emoji, fontSize = 32.sp)
            },
            title = {
                Text("Load \"${profileToLoad!!.name}\"?")
            },
            text = {
                Column {
                    Text(
                        "This will:",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Replace all your current settings")
                    Text("• Delete all your saved locations")
                    Text("• Add ${profileToLoad!!.locations.size} demo location(s)")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This action cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val profile = profileToLoad!!
                        scope.launch {
                            // Apply all settings
                            appPreferences.setDarkMode(profile.darkMode)
                            appPreferences.setCooldownEnabled(profile.cooldownEnabled)
                            appPreferences.setCooldownMinutes(profile.cooldownMinutes)
                            appPreferences.setVibrationIntensity(profile.vibrationIntensity)
                            appPreferences.setOverlayDismissStyle(profile.overlayDismissStyle)
                            appPreferences.setOverlayBackgroundStyle(profile.overlayBackgroundStyle)
                            appPreferences.setOverlayShowDistance(profile.overlayShowDistance)
                            appPreferences.setOverlayShowEmoji(profile.overlayShowEmoji)
                            appPreferences.setOverlayPrimaryColor(profile.overlayPrimaryColor)
                            appPreferences.setOverlayEmoji(profile.overlayEmoji)
                            
                            // Load demo locations
                            mapsViewModel?.loadDemoProfile(profile.locations)
                            
                            successProfileName = profile.name
                            showSuccessMessage = true
                        }
                        showConfirmDialog = false
                        profileToLoad = null
                    }
                ) {
                    Text("Load Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmDialog = false
                    profileToLoad = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DemoProfileCard(
    profile: DemoProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLoadProfile: () -> Unit,
    isCompact: Boolean = false
) {
    val cardFontSize = if (isCompact) 14.sp else 16.sp
    val subFontSize = if (isCompact) 11.sp else 13.sp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Emoji
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 40.dp else 48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.emoji, fontSize = if (isCompact) 20.sp else 24.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Profile Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        fontSize = cardFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = profile.description,
                        fontSize = subFontSize,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
                
                // Selection indicator
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                    )
                }
            }
            
            // Expanded details when selected
            if (isSelected) {
                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
                
                // Profile Details Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileDetailChip(
                        icon = when (profile.darkMode) {
                            0 -> "☀️"
                            1 -> "🌙"
                            else -> "🔄"
                        },
                        label = when (profile.darkMode) {
                            0 -> "Light"
                            1 -> "Dark"
                            else -> "Auto"
                        },
                        isCompact = isCompact
                    )
                    ProfileDetailChip(
                        icon = when (profile.appTheme) {
                            0 -> "🎨"
                            1 -> "✨"
                            2 -> "🌊"
                            3 -> "🌅"
                            4 -> "🌲"
                            5 -> "🕹️"
                            else -> "⚫"
                        },
                        label = when (profile.appTheme) {
                            0 -> "Standard"
                            1 -> "Express"
                            2 -> "Ocean"
                            3 -> "Sunset"
                            4 -> "Forest"
                            5 -> "Retro"
                            else -> "Mono"
                        },
                        isCompact = isCompact
                    )
                    ProfileDetailChip(
                        icon = "📍",
                        label = "${profile.locations.size} places",
                        isCompact = isCompact
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))
                
                // More details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileDetailChip(
                        icon = profile.overlayEmoji,
                        label = "Alarm",
                        isCompact = isCompact
                    )
                    ProfileDetailChip(
                        icon = when (profile.vibrationIntensity) {
                            0 -> "💤"
                            1 -> "📱"
                            else -> "🔔"
                        },
                        label = when (profile.vibrationIntensity) {
                            0 -> "Gentle"
                            1 -> "Normal"
                            else -> "Strong"
                        },
                        isCompact = isCompact
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
                
                // Load Button
                Button(
                    onClick = onLoadProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load This Profile")
                }
            }
        }
    }
}

// ============================================================================
// DESIGN LANGUAGE SELECTOR
// ============================================================================

@Composable
fun DesignLanguageSelector(
    appPreferences: AppPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    val currentDesignLanguage by appPreferences.designLanguage.collectAsState(initial = 0)
    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingDesignLanguage by remember { mutableIntStateOf(0) }
    
    val cardFontSize = if (isCompact) 14.sp else 16.sp
    val subFontSize = if (isCompact) 11.sp else 13.sp
    
    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
        // Material 3 Option
        DesignLanguageCard(
            emoji = "🎨",
            title = "Material 3",
            description = "Modern, clean design with dynamic colors",
            isSelected = currentDesignLanguage == 0,
            onClick = {
                if (currentDesignLanguage != 0) {
                    pendingDesignLanguage = 0
                    showRestartDialog = true
                }
            },
            isCompact = isCompact
        )
        
        // iOS 6 Skeuomorphic Option
        DesignLanguageCard(
            emoji = "📱",
            title = "iOS 6 Classic",
            description = "Retro skeuomorphic design with glossy buttons",
            isSelected = currentDesignLanguage == 1,
            onClick = {
                if (currentDesignLanguage != 1) {
                    pendingDesignLanguage = 1
                    showRestartDialog = true
                }
            },
            isCompact = isCompact
        )
    }
    
    // Restart confirmation dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            icon = {
                Text(
                    text = if (pendingDesignLanguage == 1) "📱" else "🎨",
                    fontSize = 48.sp
                )
            },
            title = {
                Text(
                    text = "Change Design Language?",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (pendingDesignLanguage == 1) {
                            "Switch to iOS 6 Classic style"
                        } else {
                            "Switch to Material 3 style"
                        },
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔄", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The app will restart to apply the new design",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            appPreferences.setDesignLanguage(pendingDesignLanguage)
                            // Small delay to ensure preference is saved
                            delay(200)
                            // Restart the app
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            // Exit current activity
                            (context as? android.app.Activity)?.finish()
                            // Force process kill for clean restart
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    }
                ) {
                    Text("Restart Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DesignLanguageCard(
    emoji: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isCompact: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(if (isCompact) 44.dp else 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = if (isCompact) 24.sp else 28.sp)
            }
            
            Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = if (isCompact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = if (isCompact) 12.sp else 13.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun ProfileDetailChip(
    icon: String,
    label: String,
    isCompact: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = if (isCompact) 4.dp else 6.dp)
    ) {
        Text(icon, fontSize = if (isCompact) 12.sp else 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            fontSize = if (isCompact) 10.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun AboutUsDetailPane() {
    AboutUsScreen(onDismiss = {}, isEmbedded = true)
}

@Composable
fun SettingsSingleColumnLayout(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit,
    mapsViewModel: MapsViewModel? = null
) {
    // State for showing the alarm display full screen - lifted here to render outside scrollable
    var showAlarmDisplayFullScreen by remember { mutableStateOf(false) }
    var showAboutUs by remember { mutableStateOf(false) }
    
    if (showAlarmDisplayFullScreen) {
        // Render full screen OUTSIDE the scrollable column
        AlarmDisplayFullScreen(
            appPreferences = appPreferences,
            context = context,
            scope = scope,
            onBack = { showAlarmDisplayFullScreen = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            NotificationsSettingDetail(appPreferences, scope)
            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
            
            SoundHapticsSettingDetail(appPreferences, context, scope)
            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
            
            // Pass the callback to open full screen
            AlarmDisplaySettingDetailCompact(
                appPreferences = appPreferences,
                context = context,
                scope = scope,
                onOpenFullScreen = { showAlarmDisplayFullScreen = true }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
            
            AppearanceSettingDetail(appPreferences, scope, onColorChange, isRainbowEnabled, onRainbowToggle)
            
            // ============================================================================
            // DEVELOPER OPTIONS SETTINGS
            // ============================================================================
            
            DeveloperOptionsSettingDetail(appPreferences, scope, mapsViewModel)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = { showAboutUs = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("About Us")
            }
            
            if (showAboutUs) {
                AboutUsScreen(onDismiss = { showAboutUs = false })
            }
        }
    }
}

// ============================================================================
// ROTATE DEVICE PROMPT (For slab phones in landscape)
// ============================================================================

@Composable
fun RotateDevicePrompt() {
    // Animation for the rotation indicator
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    
    // Rotation animation: 90 degrees (landscape) to 0 degrees (portrait)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 90f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing animation for the icon
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated phone rotation visualization
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Phone outline that rotates
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(90.dp)
                        .rotate(rotation)
                        .border(3.dp, Color.White, RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Screen indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(70.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Rotation icon
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Rotate device",
                modifier = Modifier.size((48 * scale).dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Please Rotate Your Device",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "For the best experience, use this app in portrait mode",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Visual indicator showing landscape -> portrait
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Landscape phone icon (crossed out style)
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(32.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Text(
                    text = "→",
                    fontSize = 28.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // Portrait phone icon (highlighted)
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(52.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

