package com.mobprog.lokalert

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context) }
    
    // Detect screen size for responsive layout
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp
    
    // Selected setting category for two-pane layout
    var selectedCategory by remember { mutableStateOf("Cooldown") }
    
    if (isWideScreen) {
        // Two-pane iPad-style layout
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Pane: Setting Categories
            SettingsCategoriesList(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
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
            onRainbowToggle = onRainbowToggle
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
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "Cooldown",
        "Default Sound",
        "Vibration",
        "Alarm Style",
        "Test Alarm",
        "Dark Mode",
        "Title Style",
        "About"
    )
    
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 16.dp)
    ) {
        Text(
            "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelected(category) },
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        fontSize = 16.sp,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        when (selectedCategory) {
            "Cooldown" -> CooldownSettingDetail(appPreferences, scope)
            "Default Sound" -> DefaultSoundSettingDetail(appPreferences, context, scope)
            "Vibration" -> VibrationSettingDetail(appPreferences, scope)
            "Alarm Style" -> AlarmStyleSettingDetail(appPreferences, scope)
            "Test Alarm" -> TestAlarmSettingDetail(appPreferences, context)
            "Dark Mode" -> DarkModeSettingDetail(appPreferences, scope)
            "Title Style" -> TitleStyleSettingDetail(onColorChange, isRainbowEnabled, onRainbowToggle)
            "About" -> AboutUsDetailPane()
        }
    }
}

// Individual Setting Detail Panes
@Composable
fun CooldownSettingDetail(appPreferences: AppPreferences, scope: kotlinx.coroutines.CoroutineScope) {
    val isCooldownEnabled by appPreferences.isCooldownEnabled.collectAsState(initial = false)
    val cooldownMinutes by appPreferences.cooldownMinutes.collectAsState(initial = 5)
    
    Column {
        Text("Alarm Repeat Prevention", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Stop alarms from going off repeatedly when you're near a location. (debug)",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Disable option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        appPreferences.setCooldownEnabled(false)
                    }
                }
                .padding(vertical = 12.dp),
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
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Alert every time", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Alarm rings each time you enter the area",
                    fontSize = 13.sp,
                    color = Color.Gray
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
                .padding(vertical = 12.dp),
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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart cooldown", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Wait before alerting again after you leave and come back",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                
                if (isCooldownEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Wait time", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5).forEach { minutes ->
                            FilterChip(
                                selected = cooldownMinutes == minutes,
                                onClick = {
                                    scope.launch {
                                        appPreferences.setCooldownMinutes(minutes)
                                    }
                                },
                                label = { Text("${minutes}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 30).forEach { minutes ->
                            FilterChip(
                                selected = cooldownMinutes == minutes,
                                onClick = {
                                    scope.launch {
                                        appPreferences.setCooldownMinutes(minutes)
                                    }
                                },
                                label = { Text("${minutes}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultSoundSettingDetail(
    appPreferences: AppPreferences,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val defaultAlarmSound by appPreferences.defaultAlarmSound.collectAsState(initial = "")
    var showSoundPickerDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    // Clean up media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
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
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                        release()
                        mediaPlayer = null
                    }
                }
                isPlaying = true
                
                // Auto-stop after 5 seconds
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
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }
            scope.launch {
                appPreferences.setDefaultAlarmSound(it.toString())
            }
        }
    }
    
    fun getSoundTitle(uriString: String): String {
        if (uriString.isEmpty()) return "System Default"
        return try {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
            ringtone?.getTitle(context) ?: run {
                val uri = Uri.parse(uriString)
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            it.getString(nameIndex) ?: "Custom Sound"
                        } else "Custom Sound"
                    } else "Custom Sound"
                } ?: "Custom Sound"
            }
        } catch (e: Exception) {
            uriString.substringAfterLast("/").substringBeforeLast(".").takeIf { it.isNotEmpty() } ?: "Custom Sound"
        }
    }
    
    Column {
        Text("Alarm Sound", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pick the sound that plays when you reach your destination. This applies to all new alarms.",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Current Sound", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getSoundTitle(defaultAlarmSound),
                fontSize = 16.sp,
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(
                        "Select the default sound for alarms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                                    if (defaultAlarmSound.isNotEmpty()) {
                                        try {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(defaultAlarmSound))
                                        } catch (e: Exception) { }
                                    }
                                }
                                ringtoneLauncher.launch(intent)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "System Ringtones",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Choose from built-in alarm sounds",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Custom Audio File",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Browse your device for MP3, WAV, etc.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
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

@Composable
fun VibrationSettingDetail(appPreferences: AppPreferences, scope: kotlinx.coroutines.CoroutineScope) {
    val context = LocalContext.current
    val vibrationIntensity by appPreferences.vibrationIntensity.collectAsState(initial = 2)
    var sliderValue by remember(vibrationIntensity) { mutableFloatStateOf(vibrationIntensity.toFloat()) }
    var isSliding by remember { mutableStateOf(false) }
    
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
    
    // Vibrate continuously while sliding based on current position
    LaunchedEffect(isSliding, sliderValue) {
        if (isSliding) {
            val intensity = sliderValue.toInt()
            val amplitude = when (intensity) {
                0 -> 64   // Low: ~25% intensity
                1 -> 128  // Medium: ~50% intensity
                2 -> 255  // Strong: 100% max intensity
                else -> 255
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Continuous vibration while holding
                val effect = android.os.VibrationEffect.createOneShot(500, amplitude)
                vibrator.vibrate(effect)
                // Repeat vibration while sliding
                while (isSliding) {
                    delay(400)
                    if (isSliding) {
                        val currentAmplitude = when (sliderValue.toInt()) {
                            0 -> 64
                            1 -> 128
                            2 -> 255
                            else -> 255
                        }
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, currentAmplitude))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } else {
            vibrator.cancel()
        }
    }
    
    // Stop vibration when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            vibrator.cancel()
        }
    }
    
    Column {
        Text("Vibration Strength", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Set how strong the phone vibrates when an alarm goes off. Hold the slider to feel the intensity.",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Slider with labels
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
            
            // Labels below slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gentle",
                    fontSize = 12.sp,
                    fontWeight = if (sliderValue.toInt() == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (sliderValue.toInt() == 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    "Normal",
                    fontSize = 12.sp,
                    fontWeight = if (sliderValue.toInt() == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (sliderValue.toInt() == 1) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    "Strong",
                    fontSize = 12.sp,
                    fontWeight = if (sliderValue.toInt() == 2) FontWeight.Bold else FontWeight.Normal,
                    color = if (sliderValue.toInt() == 2) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            when (sliderValue.toInt()) {
                0 -> "💤 Gentle vibration for quiet environments"
                1 -> "📱 Normal vibration for everyday use"
                2 -> "🔔 Strong vibration to ensure you don't miss it"
                else -> ""
            },
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun AlarmStyleSettingDetail(appPreferences: AppPreferences, scope: kotlinx.coroutines.CoroutineScope) {
    val dismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val backgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    val showDistance by appPreferences.overlayShowDistance.collectAsState(initial = true)
    val showEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val primaryColor by appPreferences.overlayPrimaryColor.collectAsState(initial = "FF6B6B")
    
    Column {
        Text("Alarm Appearance", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Customize how your alarm looks and feels when it goes off",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Dismiss Style Section
        Text("How to Dismiss", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                0 to ("Slide to dismiss" to "Drag the slider to stop the alarm"),
                1 to ("Swipe up" to "Swipe up anywhere to dismiss"),
                2 to ("Tap button" to "Simple tap to dismiss the alarm")
            ).forEach { (style, labelDesc) ->
                val (label, description) = labelDesc
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { appPreferences.setOverlayDismissStyle(style) }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (dismissStyle == style) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = dismissStyle == style,
                            onClick = { scope.launch { appPreferences.setOverlayDismissStyle(style) } }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(label, fontWeight = FontWeight.Medium)
                            Text(description, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        // Background Style Section
        Text("Background Style", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to "Gradient",
                1 to "Solid",
                2 to "Dark"
            ).forEach { (style, label) ->
                FilterChip(
                    selected = backgroundStyle == style,
                    onClick = { scope.launch { appPreferences.setOverlayBackgroundStyle(style) } },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Color Picker Section
        Text("Accent Color", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        val colorOptions = listOf(
            "FF6B6B" to Color(0xFFFF6B6B), // Red-orange (default)
            "4FC3F7" to Color(0xFF4FC3F7), // Light blue
            "81C784" to Color(0xFF81C784), // Green
            "FFD54F" to Color(0xFFFFD54F), // Yellow
            "BA68C8" to Color(0xFFBA68C8), // Purple
            "FF8A65" to Color(0xFFFF8A65)  // Orange
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colorOptions.forEach { (hex, color) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        // Toggle Options
        Text("Display Options", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { appPreferences.setOverlayShowDistance(!showDistance) } }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show distance", fontWeight = FontWeight.Medium)
                Text("Display how far you are from the location", fontSize = 12.sp, color = Color.Gray)
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
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show alarm icon", fontWeight = FontWeight.Medium)
                Text("Display the animated alarm emoji", fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = showEmoji,
                onCheckedChange = { scope.launch { appPreferences.setOverlayShowEmoji(it) } }
            )
        }
    }
}

@Composable
fun TestAlarmSettingDetail(appPreferences: AppPreferences, context: android.content.Context) {
    // Get saved alarms from database
    val database = remember { LokAlertDatabase.getDatabase(context) }
    val savedAlarms by database.alarmDao().getAllAlarms().collectAsState(initial = emptyList<LocationAlarm>())
    
    Column {
        Text("Test Your Alarm", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Preview how your alarm will look and sound with your current settings",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Test Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Launch test alarm with default settings
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Quick Test",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Test alarm with default settings",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test with Saved Location
        Text("Test with Saved Location", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Select one of your saved alarms to see how it would look",
            fontSize = 13.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (savedAlarms.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "No saved alarms yet. Create an alarm first to test with specific locations.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                savedAlarms.take(5).forEach { alarm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Launch test alarm with this location's settings
                                val intent = Intent(context, AlarmOverlayActivity::class.java).apply {
                                    putExtra("ALARM_NAME", alarm.name)
                                    putExtra("SOUND_URI", alarm.soundUri)
                                    putExtra("LATITUDE", alarm.latitude)
                                    putExtra("LONGITUDE", alarm.longitude)
                                    putExtra("IS_GRADUAL_VOLUME", alarm.isGradualVolume)
                                    putExtra("IS_TEST_MODE", true)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    alarm.name,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    "Radius: ${alarm.radius.toInt()}m",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Test",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                if (savedAlarms.size > 5) {
                    Text(
                        "Showing first 5 alarms",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "The test alarm will use your current Alarm Style settings. Customize them first to preview different looks!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun DarkModeSettingDetail(appPreferences: AppPreferences, scope: kotlinx.coroutines.CoroutineScope) {
    val darkModeValue by appPreferences.darkMode.collectAsState(initial = 0)
    
    Column {
        Text("App Theme", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Choose how the app looks",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                0 to "Light",
                1 to "Dark",
                2 to "AMOLED"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = darkModeValue == mode,
                    onClick = {
                        scope.launch {
                            appPreferences.setDarkMode(mode)
                        }
                    },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            when (darkModeValue) {
                0 -> "☀️ Bright theme for daytime use"
                1 -> "🌙 Dark gray theme, easier on the eyes"
                2 -> "⬛ True black for AMOLED screens, saves battery"
                else -> ""
            },
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TitleStyleSettingDetail(
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit
) {
    var showColorOptions by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(3) }
    
    val colorOptions = mapOf(
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Mono" to if (isSystemInDarkTheme()) Color.White else Color.Black
    )
    val colorOptionKeys = colorOptions.keys.toList()
    
    Column {
        Text("App Title Color", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Personalize how the LokAlert logo looks",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Rainbow animation", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text("Animated color cycling effect", fontSize = 13.sp, color = Color.Gray)
            }
            Switch(checked = isRainbowEnabled, onCheckedChange = onRainbowToggle)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        Column(modifier = Modifier.clickable(enabled = !isRainbowEnabled) {
            showColorOptions = !showColorOptions
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Title Color",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = if (isRainbowEnabled) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isRainbowEnabled) "Disabled when rainbow is on" else "Choose a static color",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Icon(
                    if (showColorOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isRainbowEnabled) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (showColorOptions && !isRainbowEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
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
                            Text(name)
                        }
                    }
                }
            }
        }
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
    onRainbowToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        CooldownSettingDetail(appPreferences, scope)
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
        
        DefaultSoundSettingDetail(appPreferences, context, scope)
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
        
        VibrationSettingDetail(appPreferences, scope)
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
        
        AlarmStyleSettingDetail(appPreferences, scope)
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
        
        TestAlarmSettingDetail(appPreferences, context)
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
        
        DarkModeSettingDetail(appPreferences, scope)
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
        
        TitleStyleSettingDetail(onColorChange, isRainbowEnabled, onRainbowToggle)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        var showAboutUs by remember { mutableStateOf(false) }
        
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
