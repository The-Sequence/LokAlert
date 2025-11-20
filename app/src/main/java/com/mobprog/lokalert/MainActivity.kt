package com.mobprog.lokalert

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class Alarm(val id: Long, val time: String, val soundUri: String, val isEnabled: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LokAlertTheme {
                RequestPermissions()
                LokAlertApp()
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
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun LokAlertApp() {
    val context = LocalContext.current
    val defaultRingtoneUri = remember { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() }

    var alarms by remember {
        mutableStateOf(
            listOf(
                Alarm(id = 1L, time = "07:00 AM", soundUri = defaultRingtoneUri, isEnabled = true),
                Alarm(id = 2L, time = "08:30 AM", soundUri = defaultRingtoneUri, isEnabled = false),
                Alarm(id = 3L, time = "09:15 AM", soundUri = defaultRingtoneUri, isEnabled = true)
            )
        )
    }

    var currentScreen by remember { mutableStateOf("Search") }
    var titleColor by remember { mutableStateOf(Color(0xFF006DFF)) }
    var isRainbowEffectEnabled by remember { mutableStateOf(false) }

    val animatedTitleColor = remember {
        Animatable(
            titleColor,
            TwoWayConverter(
                convertToVector = { color: Color -> AnimationVector4D(color.red, color.green, color.blue, color.alpha) },
                convertFromVector = { vector -> Color(vector.v1, vector.v2, vector.v3, vector.v4) }
            )
        )
    }

    LaunchedEffect(isRainbowEffectEnabled, titleColor) {
        if (isRainbowEffectEnabled) {
            launch {
                val rainbowColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Magenta, Color.Yellow, Color.Red)
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

    Scaffold(
        topBar = { TopBar(animatedTitleColor.value) },
        bottomBar = { BottomNavBar(currentScreen) { currentScreen = it } },
        containerColor = Color(0xFFF0F0F0)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                "Search" -> SearchScreen()
                "Favorites" -> FavoritesScreen()
                "Alarms" -> AlarmsScreen(alarmsList = alarms, onAlarmsChange = { alarms = it })
                "Settings" -> SettingsScreen(
                    color = titleColor,
                    onColorChange = { titleColor = it },
                    isRainbowEnabled = isRainbowEffectEnabled,
                    onRainbowToggle = { isRainbowEffectEnabled = it }
                )
            }
        }
    }
}

@Composable
fun AlarmsScreen(alarmsList: List<Alarm>, onAlarmsChange: (List<Alarm>) -> Unit) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text("Alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(alarmsList, key = { it.id }) { alarm ->
                    AlarmItem(
                        alarm = alarm,
                        onToggle = { isEnabled ->
                            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                            onAlarmsChange(alarmsList.map { if (it.id == alarm.id) updatedAlarm else it })

                            if (isEnabled) {
                                scheduleAlarm(context, updatedAlarm)
                            } else {
                                cancelAlarm(context, updatedAlarm)
                            }
                        },
                        onDelete = {
                            cancelAlarm(context, alarm)
                            onAlarmsChange(alarmsList.filter { it.id != alarm.id })
                        },
                        onClick = {
                            alarmToEdit = alarm
                            showEditDialog = true
                        }
                    )
                    Divider()
                }
            }
        }

        FloatingActionButton(
            onClick = {
                alarmToEdit = null
                showEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }

    if (showEditDialog) {
        EditAlarmDialog(
            alarm = alarmToEdit,
            onDismiss = { showEditDialog = false },
            onSave = { updatedAlarm ->
                if (alarmToEdit == null) {
                    onAlarmsChange(alarmsList + updatedAlarm)
                    if (updatedAlarm.isEnabled) scheduleAlarm(context, updatedAlarm)
                } else {
                    onAlarmsChange(alarmsList.map { if (it.id == updatedAlarm.id) updatedAlarm else it })
                    if (updatedAlarm.isEnabled) {
                        scheduleAlarm(context, updatedAlarm)
                    } else {
                        cancelAlarm(context, updatedAlarm)
                    }
                }
                showEditDialog = false
            }
        )
    }
}

fun getRingtoneTitle(context: Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(alarm: Alarm?, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val context = LocalContext.current
    val defaultRingtoneUri = remember { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() }

    var time by remember { mutableStateOf(alarm?.time ?: "07:00 AM") }
    var soundUri by remember { mutableStateOf(alarm?.soundUri ?: defaultRingtoneUri) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let {
                soundUri = it.toString()
            }
        }
    )

    fun showTimePicker() {
        val calendar = Calendar.getInstance()
        try {
            val timeParts = time.split(":", " ")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            if (time.endsWith("PM") && hour != 12) hour += 12
            if (time.endsWith("AM") && hour == 12) hour = 0
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
        } catch (e: Exception) {
            // Fallback to current time if parsing fails
        }

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay >= 12) "PM" else "AM"
                val hour12 = if (hourOfDay == 0 || hourOfDay == 12) 12 else hourOfDay % 12
                time = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarm == null) "Add Alarm" else "Edit Alarm") },
        text = {
            Column {
                OutlinedTextField(
                    value = time,
                    onValueChange = { },
                    label = { Text("Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = getRingtoneTitle(context, soundUri),
                    onValueChange = { },
                    label = { Text("Sound") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                        }
                        ringtonePickerLauncher.launch(intent)
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val alarmId = alarm?.id ?: UUID.randomUUID().mostSignificantBits
                val newAlarm = alarm?.copy(time = time, soundUri = soundUri) ?: Alarm(id = alarmId, time = time, soundUri = soundUri, isEnabled = true)
                onSave(newAlarm)
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = alarm.time, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = getRingtoneTitle(context, alarm.soundUri), fontSize = 14.sp, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Alarm")
            }
        }
    }
}

fun scheduleAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("ALARM_ID", alarm.id)
        putExtra("ALARM_SOUND_URI", alarm.soundUri)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        try {
            val timeParts = alarm.time.split(":", " ")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            if (alarm.time.endsWith("PM") && hour != 12) hour += 12
            if (alarm.time.endsWith("AM") && hour == 12) hour = 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        } catch (e: Exception) {
            return@apply
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        return
    }

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    Toast.makeText(context, "Alarm set for ${alarm.time}", Toast.LENGTH_SHORT).show()
}

fun cancelAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    AlarmReceiver.stopRingtone()
    Toast.makeText(context, "Alarm cancelled", Toast.LENGTH_SHORT).show()
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LokAlertTheme {
        LokAlertApp()
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Options",
            modifier = Modifier.align(Alignment.CenterEnd)
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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
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
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { /* Place pin logic */ },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(30.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Click to Place Pin on Map")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun MapSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF1C2A38), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Map Placeholder",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun RecentSearchSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Recent Searches", fontWeight = FontWeight.Bold)
            Text("Clear", color = Color(0xFF007BFF))
        }
        Text(
            text = "No recent searches",
            color = Color.Gray,
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
fun SettingsScreen(color: Color, onColorChange: (Color) -> Unit, isRainbowEnabled: Boolean, onRainbowToggle: (Boolean) -> Unit) {
    var showColorOptions by remember { mutableStateOf(false) }
    val colorOptions = mapOf(
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Black" to Color.Black
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rainbow Title")
            Switch(checked = isRainbowEnabled, onCheckedChange = onRainbowToggle)
        }

        Divider()

        Column(modifier = Modifier.clickable(enabled = !isRainbowEnabled) { showColorOptions = !showColorOptions }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Title Color", color = if (isRainbowEnabled) Color.Gray else Color.Black)
                Icon(
                    if (showColorOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Title Color Options",
                    tint = if (isRainbowEnabled) Color.Gray else Color.Black
                )
            }

            if (showColorOptions && !isRainbowEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    colorOptions.forEach { (name, colorValue) ->
                        Button(onClick = { onColorChange(colorValue) }) {
                            Text(name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(currentScreen: String, onScreenSelected: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentScreen == "Search",
            onClick = { onScreenSelected("Search") },
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = currentScreen == "Favorites",
            onClick = { onScreenSelected("Favorites") },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            label = { Text("Favorites") }
        )
        NavigationBarItem(
            selected = currentScreen == "Alarms",
            onClick = { onScreenSelected("Alarms") },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text("Alarms") }
        )
        NavigationBarItem(
            selected = currentScreen == "Settings",
            onClick = { onScreenSelected("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}

