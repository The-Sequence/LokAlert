package com.mobprog.lokalert

import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class Alarm(val time: String, val sound: String, val isEnabled: Boolean)



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Places API
        if (!Places.isInitialized()) {
            val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData?.getString("com.google.android.geo.API_KEY")
            if (apiKey != null) {
                Places.initialize(applicationContext, apiKey)
            }
        }
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
                intent.action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                context.startActivity(intent)
            }
        }
    }
}


@Composable
fun LokAlertApp() {
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
                "Maps" -> MapsScreen()
                "Alarms" -> AlarmsScreen()
                "Settings" -> SettingsScreen(
                    onColorChange = { titleColor = it },
                    isRainbowEnabled = isRainbowEffectEnabled,
                    onRainbowToggle = { isRainbowEffectEnabled = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LokAlertTheme {
        LokAlertApp()
    }
}

// Add this to the bottom of your MainActivity.kt file, outside any class
fun Modifier.bypassing(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(pass = PointerEventPass.Initial)
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
fun SearchSection(
    onPlacePinClick: (() -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Only create the client if Places is initialized
    val placesClient = remember {
        if (Places.isInitialized()) Places.createClient(context) else null
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && placesClient != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries("PH")
                .setSessionToken(token)
                .setQuery(searchText)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                    expanded = suggestions.isNotEmpty()
                }
                .addOnFailureListener { 
                    suggestions = emptyList()
                    expanded = false
                }
        } else {
            suggestions = emptyList()
            expanded = false
        }
    }

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
            trailingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    modifier = Modifier.clickable { onSearch?.invoke(searchText) }
                ) 
            },
            shape = RoundedCornerShape(30.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch?.invoke(searchText)
                    expanded = false
                }
            )
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f) // Adjust width as needed
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(text = suggestion) },
                    onClick = {
                        searchText = suggestion
                        expanded = false
                        onSuggestionClick?.invoke(suggestion)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "OR",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { onPlacePinClick?.invoke() },
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

@SuppressLint("MissingPermission")
@Composable
fun MapsScreen() {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableFloatStateOf(100f) } // Default radius 100 meters
    var showRadiusAdjustment by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f)
    }
    val coroutineScope = rememberCoroutineScope()
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            onMapClick = { 
                 // Optional: Click map to set pin as well
                 // markerPosition = latLng
            }
        ) {
            markerPosition?.let { position ->
                val markerState = rememberMarkerState(position = position)
                
                // Sync changes from marker drag back to our state
                if (markerState.dragState == com.google.maps.android.compose.DragState.END) {
                    markerPosition = markerState.position
                }

                Marker(
                    state = markerState,
                    title = "Selected Location",
                    draggable = true, // Make the marker draggable
                    onClick = {
                        // Keep default behavior (show info window) but we can add custom logic here
                        false 
                    }
                )
                Circle(
                    center = markerState.position, // Use markerState position to follow drag
                    radius = radius.toDouble(),
                    strokeColor = Color(0xFF006DFF),
                    strokeWidth = 2f,
                    fillColor = Color(0x22006DFF)
                )
            }
        }
        
        // Floating Action Button to set pin at center of screen (current camera target)
        Box(
             modifier = Modifier
                 .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                 .align(Alignment.BottomEnd)
                 .padding(16.dp)
                 .padding(bottom = 80.dp) // Add padding to avoid overlap with BottomNavBar
                 .pointerInput(Unit) {
                     detectDragGestures { change, dragAmount ->
                         change.consume()
                         offsetX += dragAmount.x
                         offsetY += dragAmount.y
                     }
                 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showRadiusAdjustment) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                         Column {
                             Text("Adjust Radius: ${radius.toInt()}m", fontSize = 12.sp)
                             Slider(
                                 value = radius,
                                 onValueChange = { radius = it },
                                 valueRange = 50f..2000f,
                                 steps = 19
                             )
                         }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (markerPosition != null) {
                        Button(
                            onClick = {
                                 showRadiusAdjustment = !showRadiusAdjustment
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(56.dp)
                        ) {
                             Icon(Icons.Default.Edit, contentDescription = "Adjust Radius")
                             Spacer(Modifier.width(8.dp))
                             Text("Adjust Radius")
                        }
                    }

                    Button(
                        onClick = {
                            markerPosition = cameraPositionState.position.target
                            showRadiusAdjustment = false // Hide radius adjustment when setting new location
                            Toast.makeText(context, "Location set!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(56.dp)
                    ) {
                         Icon(Icons.Default.LocationOn, contentDescription = "Set Location")
                         Spacer(Modifier.width(8.dp))
                         Text(if (markerPosition == null) "Set Location" else "Move Location")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.9f))
                .fillMaxWidth()
        ) {
            SearchSection(
                onPlacePinClick = {
                    if (hasLocationPermission) {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            location?.let {
                                val latLng = LatLng(it.latitude, it.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                    )
                                }
                            } ?: run {
                                Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        coroutineScope.launch {
                            try {
                                val geocoder = Geocoder(context)
                                val addresses = withContext(Dispatchers.IO) {
                                    @Suppress("DEPRECATION")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        geocoder.getFromLocationName(query, 1)
                                    } else {
                                        geocoder.getFromLocationName(query, 1)
                                    }
                                }
                                
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val latLng = LatLng(address.latitude, address.longitude)
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                    )
                                } else {
                                    Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onSuggestionClick = { suggestion ->
                    coroutineScope.launch {
                        try {
                            val geocoder = Geocoder(context)
                            val addresses = withContext(Dispatchers.IO) {
                                @Suppress("DEPRECATION")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    geocoder.getFromLocationName(suggestion, 1)
                                } else {
                                    geocoder.getFromLocationName(suggestion, 1)
                                }
                            }

                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val latLng = LatLng(address.latitude, address.longitude)
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                )
                            } else {
                                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen() {
    val context = LocalContext.current
    var alarms by remember {
        mutableStateOf(
            listOf(
                Alarm(time = "07:00 AM", sound = "Default", isEnabled = true),
                Alarm(time = "08:30 AM", sound = "Radar", isEnabled = false),
                Alarm(time = "09:15 AM", sound = "Chimes", isEnabled = true)
            )
        )
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    var alarmIndexToEdit by remember { mutableIntStateOf(-1) }

    if (showEditDialog && alarmToEdit != null) {
        EditAlarmDialog(
            alarm = alarmToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedAlarm ->
                val newList = alarms.toMutableList()
                newList[alarmIndexToEdit] = updatedAlarm
                alarms = newList
                showEditDialog = false

                if (updatedAlarm.isEnabled) {
                    scheduleAlarm(context, updatedAlarm)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            itemsIndexed(alarms) { index, alarm ->
                AlarmItem(
                    alarm = alarm,
                    onToggle = { isEnabled ->
                        val newList = alarms.toMutableList()
                        val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                        newList[index] = updatedAlarm
                        alarms = newList

                        if (isEnabled) {
                            scheduleAlarm(context, updatedAlarm)
                        } else {
                            // Cancel the alarm if it was disabled
                        }
                    },
                    onDelete = {
                        val newList = alarms.toMutableList()
                        newList.removeAt(index)
                        alarms = newList
                    },
                    onClick = {
                        alarmToEdit = alarm
                        alarmIndexToEdit = index
                        showEditDialog = true
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(alarm: Alarm, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    var time by remember(alarm) { mutableStateOf(alarm.time) }
    var sound by remember(alarm) { mutableStateOf(alarm.sound) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showTimePicker) {
        DisposableEffect(Unit) {
            val calendar = Calendar.getInstance()
            try {
                val timeParts = time.split(":", " ")
                var hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                val isPm = timeParts.getOrNull(2)?.equals("PM", true) == true

                if (isPm && hour < 12) {
                    hour += 12
                } else if (!isPm && hour == 12) { // 12 AM is 0 hour
                    hour = 0
                }
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
            } catch (_: Exception) {
                // Use current time as fallback if parsing fails
            }

            val timePickerDialog = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val amPm = if (hourOfDay >= 12) "PM" else "AM"
                    val hour = if (hourOfDay == 0 || hourOfDay == 12) 12 else hourOfDay % 12
                    time = String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, amPm)
                    showTimePicker = false
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // is24HourView = false for 12 hour format with AM/PM
            )
            timePickerDialog.setOnCancelListener {
                showTimePicker = false
            }
            timePickerDialog.show()

            onDispose {
                timePickerDialog.dismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Alarm") },
        text = {
            Column {
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    label = { Text("Time") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sound,
                    onValueChange = { sound = it },
                    label = { Text("Sound") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(alarm.copy(time = time, sound = sound)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun scheduleAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        try {
            val timeParts = alarm.time.split(":", " ")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val isPm = timeParts.getOrNull(2)?.equals("PM", true) == true

            if (isPm && hour < 12) {
                hour += 12
            } else if (!isPm && hour == 12) { // 12 AM is 0 hour
                hour = 0
            }

            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        } catch (_: Exception) {
            // Handle parsing error
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            // The RequestPermissions composable should handle this
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    Toast.makeText(context, "Alarm Scheduled!", Toast.LENGTH_SHORT).show()
}


@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
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
            Text(text = alarm.sound, fontSize = 14.sp, color = Color.Gray)
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

@Composable
fun SettingsScreen(onColorChange: (Color) -> Unit, isRainbowEnabled: Boolean, onRainbowToggle: (Boolean) -> Unit) {
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

        HorizontalDivider()

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
            selected = currentScreen == "Maps",
            onClick = { onScreenSelected("Maps") },
            icon = { Icon(Icons.Default.Place, contentDescription = null) },
            label = { Text("Maps") }
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
