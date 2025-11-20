package com.mobprog.lokalert

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// --- DATA MODEL ---
data class Alarm(
    val id: Long,
    val name: String,
    val time: String,
    val soundUri: String,
    val isEnabled: Boolean,
    val days: Set<Int> = emptySet()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isTriggeredByAlarm = intent.getBooleanExtra("TRIGGERED_BY_ALARM", false)
        val alarmName = intent.getStringExtra("ALARM_NAME")

        setContent {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences("LokAlertPrefs", Context.MODE_PRIVATE)
            }
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_mode", systemDark))
            }

            LokAlertTheme(darkTheme = isDarkTheme) {
                RequestPermissions()
                LokAlertApp(
                    isTriggeredByAlarm = isTriggeredByAlarm,
                    alarmName = alarmName,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { newMode ->
                        isDarkTheme = newMode
                        prefs.edit().putBoolean("dark_mode", newMode).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun RequestPermissions() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.canUseFullScreenIntent()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            context.startActivity(intent)
        }
    }
}

@Composable
fun LokAlertApp(
    isTriggeredByAlarm: Boolean,
    alarmName: String?,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val defaultRingtoneUri = remember {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
    }

    var currentScreen by remember { mutableStateOf("Search") }
    val defaultTitleColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF0C2D48)
    var titleColor by remember { mutableStateOf(defaultTitleColor) }
    var isRainbowEffectEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(isDarkTheme) {
        if (!isRainbowEffectEnabled) {
            titleColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF0C2D48)
        }
    }

    var alarms by remember {
        mutableStateOf(
            listOf(
                Alarm(
                    id = 1L,
                    name = "Morning Alarm",
                    time = "07:00 AM",
                    soundUri = defaultRingtoneUri,
                    isEnabled = true,
                    days = emptySet()
                ),
                Alarm(
                    id = 2L,
                    name = "Work Meeting",
                    time = "08:30 AM",
                    soundUri = defaultRingtoneUri,
                    isEnabled = false,
                    days = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
                )
            )
        )
    }

    val animatedTitleColor = remember {
        Animatable(
            titleColor,
            TwoWayConverter(
                convertToVector = { color: Color ->
                    AnimationVector4D(color.red, color.green, color.blue, color.alpha)
                },
                convertFromVector = { vector ->
                    Color(vector.v1, vector.v2, vector.v3, vector.v4)
                }
            )
        )
    }

    LaunchedEffect(isRainbowEffectEnabled, titleColor) {
        if (isRainbowEffectEnabled) {
            launch {
                val rainbowColors = listOf(
                    Color.Red, Color.Green, Color.Blue,
                    Color.Magenta, Color.Yellow, Color.Red
                )
                while (true) {
                    for (color in rainbowColors) {
                        animatedTitleColor.animateTo(
                            color,
                            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                        )
                    }
                }
            }
        } else {
            animatedTitleColor.animateTo(titleColor, animationSpec = tween(500))
        }
    }

    if (isTriggeredByAlarm) {
        AlarmDismissScreen(alarmName ?: "Alarm")
    } else {
        Scaffold(
            topBar = { TopBar(animatedTitleColor.value) },
            bottomBar = { BottomNavBar(currentScreen) { currentScreen = it } },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    "Search" -> SearchScreen()
                    "Favorites" -> FavoritesScreen()
                    "Alarms" -> AlarmsScreen(
                        alarmsList = alarms,
                        onAlarmsChange = { alarms = it }
                    )

                    "Settings" -> SettingsScreen(
                        color = titleColor,
                        onColorChange = { titleColor = it },
                        isRainbowEnabled = isRainbowEffectEnabled,
                        onRainbowToggle = { isRainbowEffectEnabled = it },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = onThemeToggle
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "LokAlert",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
fun SearchScreen() {
    Column {
        SearchSection()
        MapSection()
        RecentSearchSection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSection() {
    var searchText by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search locations...") },
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(30.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "OR",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = { },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Click to Place Pin on Map")
        }
    }
}

@Composable
fun MapSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF1C2A38)),
        contentAlignment = Alignment.Center
    ) {
        Text("Map Placeholder", color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun RecentSearchSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Recent Searches", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Clear", color = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "No recent searches",
            color = Color.Gray,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun FavoritesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Favorites", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You haven't added any favorite locations yet.", color = Color.Gray)
    }
}

@Composable
fun SettingsScreen(
    color: Color,
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("LokAlertPrefs", Context.MODE_PRIVATE) }
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean("vibrate", true)) }
    var fadeInEnabled by remember { mutableStateOf(prefs.getBoolean("fade_in", false)) }
    var geofenceRadius by remember { mutableStateOf(prefs.getInt("geo_radius", 500).toFloat()) }
    var mapType by remember {
        mutableStateOf(prefs.getString("map_type", "Standard") ?: "Standard")
    }
    var showMapTypeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionHeader("Alarm & Notifications")
        SettingsSwitchRow("Vibrate on Alarm", vibrateEnabled) {
            vibrateEnabled = it
            prefs.edit().putBoolean("vibrate", it).apply()
        }
        SettingsSwitchRow("Gradual Fade-In", fadeInEnabled) {
            fadeInEnabled = it
            prefs.edit().putBoolean("fade_in", it).apply()
        }
        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionHeader("Location & Map")
        Text(
            text = "Geofence Radius: ${geofenceRadius.toInt()}m",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = geofenceRadius,
            onValueChange = { geofenceRadius = it },
            onValueChangeFinished = {
                prefs.edit().putInt("geo_radius", geofenceRadius.toInt()).apply()
            },
            valueRange = 100f..2000f,
            steps = 19
        )
        SettingsActionRow("Map Type", mapType) { showMapTypeDialog = true }
        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionHeader("Appearance")
        SettingsSwitchRow("Dark Mode", isDarkTheme) { onThemeToggle(it) }
        SettingsSwitchRow("Rainbow Title Effect", isRainbowEnabled) { onRainbowToggle(it) }
        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionHeader("System")
        Button(
            onClick = {
                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM).build()
                    )
                    setDataSource(context, Settings.System.DEFAULT_ALARM_ALERT_URI)
                    prepare()
                    start()
                }
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                }, 3000)
                Toast.makeText(context, "Playing Test Sound...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("Test Alarm Volume") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Open Settings > Apps > LokAlert > Battery",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Manage Battery Optimization") }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Usage Tips",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        SettingsTipItem(
            "Define Your Geofence Area",
            "Select the area you want to monitor carefully for accurate alerts."
        )
        SettingsTipItem(
            "Enable Location Services",
            "Ensure GPS/location access is active for proper functioning of the geofence."
        )
        SettingsTipItem(
            "Customize Alert Settings",
            "Adjust notification sounds, vibration, or popup alerts according to your preference."
        )
    }
    if (showMapTypeDialog) {
        AlertDialog(
            onDismissRequest = { showMapTypeDialog = false },
            title = { Text("Select Map Type") },
            text = {
                Column {
                    listOf("Standard", "Satellite", "Hybrid").forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mapType = type
                                    prefs.edit().putString("map_type", type).apply()
                                    showMapTypeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (mapType == type), onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = type)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapTypeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Divider(
            color = Color.LightGray,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 14.sp, color = Color.Gray)
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsTipItem(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(text = body, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
    }
}

@Composable
fun AlarmDismissScreen(alarmName: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = alarmName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "ALARM",
            fontSize = 18.sp,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                context.stopService(Intent(context, AlarmService::class.java))
                val activity = context as? ComponentActivity
                activity?.finish()
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("Dismiss", fontSize = 20.sp, color = Color.Black)
        }
    }
}

@Composable
fun AlarmsScreen(alarmsList: List<Alarm>, onAlarmsChange: (List<Alarm>) -> Unit) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(alarmsList, key = { it.id }) { alarm ->
                    AlarmItem(
                        alarm = alarm,
                        onToggle = { isEnabled ->
                            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                            onAlarmsChange(alarmsList.map {
                                if (it.id == alarm.id) updatedAlarm else it
                            })
                            if (isEnabled) scheduleAlarm(context, updatedAlarm)
                            else cancelAlarm(context, updatedAlarm)
                        },
                        onDelete = {
                            cancelAlarm(context, alarm)
                            onAlarmsChange(alarmsList.filter { it.id != alarm.id })
                        },
                        onClick = { alarmToEdit = alarm; showEditDialog = true }
                    )
                    Divider()
                }
            }
        }
        FloatingActionButton(
            onClick = { alarmToEdit = null; showEditDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }
    if (showEditDialog) {
        EditAlarmDialog(alarm = alarmToEdit, onDismiss = { showEditDialog = false }) { updatedAlarm ->
            if (alarmToEdit == null) {
                onAlarmsChange(alarmsList + updatedAlarm)
                if (updatedAlarm.isEnabled) scheduleAlarm(context, updatedAlarm)
            } else {
                onAlarmsChange(alarmsList.map {
                    if (it.id == updatedAlarm.id) updatedAlarm else it
                })
                if (updatedAlarm.isEnabled) scheduleAlarm(context, updatedAlarm)
                else cancelAlarm(context, updatedAlarm)
            }
            showEditDialog = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(alarm: Alarm?, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val context = LocalContext.current
    val defaultRingtoneUri = remember {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
    }
    var name by remember { mutableStateOf(alarm?.name ?: "Alarm") }
    var time by remember { mutableStateOf(alarm?.time ?: "07:00 AM") }
    var soundUri by remember { mutableStateOf(alarm?.soundUri ?: defaultRingtoneUri) }
    var selectedDays by remember { mutableStateOf(alarm?.days ?: emptySet()) }
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri ->
            soundUri = uri.toString()
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            soundUri = it.toString()
        }
    }

    fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, h, m ->
                val amPm = if (h >= 12) "PM" else "AM"
                val h12 = if (h == 0 || h == 12) 12 else h % 12
                time = String.format(Locale.getDefault(), "%02d:%02d %s", h12, m, amPm)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarm == null) "Add Alarm" else "Edit Alarm") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    label = { Text("Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker() },
                    trailingIcon = { Icon(Icons.Default.DateRange, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Repeat", style = MaterialTheme.typography.labelMedium)

                // --- FIX: SCROLLABLE ROW & 2-LETTER LABELS ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dayLabels = mapOf(
                        Calendar.SUNDAY to "Su", Calendar.MONDAY to "Mo", Calendar.TUESDAY to "Tu",
                        Calendar.WEDNESDAY to "We", Calendar.THURSDAY to "Th",
                        Calendar.FRIDAY to "Fr", Calendar.SATURDAY to "Sa"
                    )

                    listOf(
                        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
                    ).forEach { day ->
                        val label = dayLabels[day] ?: "?"
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(label, textAlign = TextAlign.Center) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = getRingtoneTitle(context, soundUri),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Notifications, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(
                        onClick = {
                            ringtonePicker.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                                        RingtoneManager.TYPE_ALARM
                                    )
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,
                                        true
                                    )
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                    )
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        Uri.parse(soundUri)
                                    )
                                }
                            )
                        },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) { Text("System", fontSize = 12.sp) }
                    Button(
                        onClick = { filePicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) { Text("My Files", fontSize = 12.sp) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Alarm(
                            alarm?.id ?: UUID.randomUUID().mostSignificantBits,
                            name,
                            time,
                            soundUri,
                            true,
                            selectedDays
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = alarm.time, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row {
                Text(text = alarm.name, fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Text(text = formatDays(alarm.days), fontSize = 14.sp, color = Color.Gray)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    result = it.getString(
                        it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
                    )
                }
            }
        } catch (e: Exception) {
        }
    }
    return result ?: uri.path?.substringAfterLast('/') ?: "Custom Audio"
}

fun getRingtoneTitle(context: Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        if (uriString.contains("media")) {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Unknown"
        } else {
            getFileName(context, uri)
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

fun formatDays(days: Set<Int>): String {
    if (days.isEmpty()) return "One-time alarm"
    if (days.size == 7) return "Every day"
    val dayNames = DateFormatSymbols.getInstance().shortWeekdays
    return days.sorted().joinToString(", ") { dayNames[it] }
}

fun getNextAlarmCalendar(alarm: Alarm): Calendar? {
    val now = Calendar.getInstance()
    val alarmTime = Calendar.getInstance()
    try {
        val parts = alarm.time.split(":", " ")
        var h = parts[0].toInt()
        val m = parts[1].toInt()
        if (alarm.time.endsWith("PM") && h != 12) h += 12
        if (alarm.time.endsWith("AM") && h == 12) h = 0
        alarmTime.set(Calendar.HOUR_OF_DAY, h)
        alarmTime.set(Calendar.MINUTE, m)
        alarmTime.set(Calendar.SECOND, 0)
    } catch (e: Exception) {
        return null
    }
    if (alarm.days.isEmpty()) {
        if (alarmTime.before(now)) alarmTime.add(Calendar.DATE, 1)
        return alarmTime
    }
    for (i in 0..7) {
        val next = Calendar.getInstance().apply { add(Calendar.DATE, i) }
        if (alarm.days.contains(next.get(Calendar.DAY_OF_WEEK))) {
            val candidate = Calendar.getInstance().apply {
                time = next.time
                set(Calendar.HOUR_OF_DAY, alarmTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, alarmTime.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
            }
            if (candidate.after(now)) return candidate
        }
    }
    return null
}

fun scheduleAlarm(context: Context, alarm: Alarm) {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("ALARM_ID", alarm.id)
        putExtra("ALARM_NAME", alarm.name)
        putExtra("ALARM_SOUND_URI", alarm.soundUri)
    }
    val pi = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val calendar = getNextAlarmCalendar(alarm)
    if (calendar != null) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pi
        )
        Toast.makeText(
            context,
            "Alarm set for ${DateFormat.getDateTimeInstance().format(calendar.time)}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun cancelAlarm(context: Context, alarm: Alarm) {
    val pi = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    context.stopService(Intent(context, AlarmService::class.java))
    Toast.makeText(context, "Alarm cancelled", Toast.LENGTH_SHORT).show()
}

@Composable
fun BottomNavBar(currentScreen: String, onScreenSelected: (String) -> Unit) {
    NavigationBar {
        listOf(
            "Search" to Icons.Default.Search,
            "Favorites" to Icons.Default.Favorite,
            "Alarms" to Icons.Default.Info,
            "Settings" to Icons.Default.Settings
        ).forEach { (name, icon) ->
            NavigationBarItem(
                selected = currentScreen == name,
                onClick = { onScreenSelected(name) },
                icon = { Icon(icon, null) },
                label = { Text(name) }
            )
        }
    }
}