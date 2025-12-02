package com.mobprog.lokalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// Constants
private const val MIN_RADIUS = 100f
private const val MAX_RADIUS = 1000f
private const val DEFAULT_RADIUS = 100f
private const val MAX_ALARM_NAME_LENGTH = 50
private const val DEFAULT_ALARM_NAME = "Time to wake up!"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapsScreen(
    onNewSearch: (String) -> Unit,
    onDone: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Map State
    var uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(isMyLocationEnabled = hasLocationPermission)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f) }

    // -- EDIT LOCATION FORM STATE --
    var radius by remember { mutableFloatStateOf(DEFAULT_RADIUS) }
    var alarmSoundUri by remember { mutableStateOf(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()) }
    var alarmName by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }
    var isGradualVolume by remember { mutableStateOf(false) }
    var showSoundSelectionDialog by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    // Launchers
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocationPermission = it[Manifest.permission.ACCESS_FINE_LOCATION] == true || it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { alarmSoundUri = it.toString() }
    }

    val customFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                alarmSoundUri = it.toString()
            } catch (e: Exception) {
                alarmSoundUri = it.toString()
            }
        }
    }

    // Initial Location Check
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { 
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                location?.let {
                                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)) }
                                }
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) { Icon(Icons.Default.MyLocation, "Device location") }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (markerPosition == null) {
                            markerPosition = cameraPositionState.position.target
                            Toast.makeText(context, "Location set! Edit details below.", Toast.LENGTH_SHORT).show()
                            showBottomSheet = true
                        } else {
                            showBottomSheet = true
                        }
                    },
                    icon = { Icon(Icons.Filled.PushPin, "Set Pin") },
                    text = { Text(if (markerPosition == null) "Set Pin" else "Edit Pin") }
                )
            }
        }
    ) { paddingValues -> 
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapClick = { markerPosition = it }
            ) {
                markerPosition?.let { position ->
                    val markerState = rememberMarkerState(position = position)
                    if (markerState.dragState == DragState.END) { markerPosition = markerState.position }
                    Marker(state = markerState, title = "Selected Location", draggable = true)
                    Circle(center = markerState.position, radius = radius.toDouble(), strokeColor = MaterialTheme.colorScheme.primary, strokeWidth = 2f, fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                }
            }
            SearchSection(
                onSearch = { query ->
                    onNewSearch(query)
                    scope.launch {
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
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                markerPosition = latLng
                                LokAlertLogger.logLocationSearch(query, success = true)
                            } else {
                                LokAlertLogger.logLocationSearch(query, success = false)
                                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            LokAlertLogger.e("Location search failed for query: $query", throwable = e)
                            Toast.makeText(context, "Failed to search location: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onSuggestionClick = { suggestion ->
                    onNewSearch(suggestion)
                    scope.launch {
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
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                markerPosition = latLng // Set the pin on suggestion click
                            } else {
                                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Failed to search location: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDraggingSlider) 0.8f else 1.0f),
            scrimColor = if (isDraggingSlider) Color.Transparent else BottomSheetDefaults.ScrimColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Always visible section (Title and Slider)
                Text("Edit Location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                AnimatedVisibility(
                    visible = !isDraggingSlider, 
                    enter = fadeIn(animationSpec = tween(300)), 
                    exit = fadeOut(animationSpec = tween(0))
                ) {
                     Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = alarmName,
                            onValueChange = { newValue ->
                                // Limit alarm name to MAX_ALARM_NAME_LENGTH characters
                                if (newValue.length <= MAX_ALARM_NAME_LENGTH) {
                                    alarmName = newValue
                                }
                            },
                            label = { Text("Alarm Name") },
                            placeholder = { Text(DEFAULT_ALARM_NAME) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("${alarmName.length}/$MAX_ALARM_NAME_LENGTH characters")
                            }
                        )

                        Column {
                            Text("Active Days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            MapsDaySelector(selectedDays) { selectedDays = it }
                        }

                        MapsPickerRow(label = "Alarm Sound", text = getMapRingtoneTitle(context, alarmSoundUri)) {
                            showSoundSelectionDialog = true
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGradualVolume = !isGradualVolume }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gradual Volume", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Alarm starts soft and gets louder", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Switch(checked = isGradualVolume, onCheckedChange = { isGradualVolume = it })
                        }
                     }
                }
                
                Column {
                    Text("Alert Radius: ${radius.toInt()} meters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = radius, 
                        onValueChange = { 
                            radius = it 
                            isDraggingSlider = true
                        }, 
                        valueRange = MIN_RADIUS..MAX_RADIUS,
                        onValueChangeFinished = { isDraggingSlider = false }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = {
                            markerPosition = null
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove Pin")
                    }

                    Button(
                        onClick = {
                            markerPosition?.let { latLng ->
                                val finalName = if (alarmName.isBlank()) DEFAULT_ALARM_NAME else alarmName.trim()
                                scope.launch {
                                    try {
                                        repository.insertAlarm(
                                            LocationAlarm(
                                                name = finalName,
                                                latitude = latLng.latitude,
                                                longitude = latLng.longitude,
                                                radius = radius,
                                                soundUri = alarmSoundUri,
                                                isGradualVolume = isGradualVolume,
                                                activeDays = selectedDays
                                            )
                                        )
                                        LokAlertLogger.logAlarmCreated(finalName, radius)
                                        Toast.makeText(context, "Location Alarm Saved!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        LokAlertLogger.e("Failed to save alarm", throwable = e)
                                        Toast.makeText(context, "Failed to save alarm: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } ?: run {
                                Toast.makeText(context, "Please set a location first", Toast.LENGTH_SHORT).show()
                            }
                            markerPosition = null
                            alarmName = ""
                            selectedDays = emptySet()
                            isGradualVolume = false
                            alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
                            
                            scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                showBottomSheet = false
                                onDone()
                            }
                        },
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
    
    if (showSoundSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showSoundSelectionDialog = false },
            title = { Text("Choose Sound Source") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showSoundSelectionDialog = false
                            ringtonePickerLauncher.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmSoundUri))
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("System Ringtones") }
                    
                    TextButton(
                        onClick = {
                            showSoundSelectionDialog = false
                            customFilePickerLauncher.launch("audio/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Custom File (.mp3)") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSoundSelectionDialog = false }) { Text("Cancel") } }
        )
    }
}


// Private helper to avoid ambiguity with MainActivity
@Composable
private fun MapsPickerRow(label: String, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

// Private helper to avoid ambiguity with MainActivity
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapsDaySelector(selectedDays: Set<Int>, onSelectionChange: (Set<Int>) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        val calendarDays = listOf(java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY, java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY, java.util.Calendar.SATURDAY)

        daysOfWeek.forEachIndexed { index, dayLabel ->
            val day = calendarDays[index]
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectionChange(if (isSelected) selectedDays - day else selectedDays + day) },
                label = { Text(dayLabel) },
                leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null
            )
        }
    }
}

// Private helper to avoid ambiguity
fun getMapRingtoneTitle(context: Context, ringtoneUriString: String): String {
    return try {
        val uri = Uri.parse(ringtoneUriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            ?: uri.lastPathSegment?.substringBeforeLast('.') ?: "Custom Sound"
    } catch (exception: Exception) {
        "Unknown Sound"
    }
}
