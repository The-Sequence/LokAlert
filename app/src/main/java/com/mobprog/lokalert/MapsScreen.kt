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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MapsScreen(onNewSearch: (String) -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var uiSettings by remember {
        mutableStateOf(MapUiSettings(
        zoomControlsEnabled = false,
        myLocationButtonEnabled = false
    ))
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val isDarkTheme = isSystemInDarkTheme()

    val mapProperties = remember(isDarkTheme, hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = if (isDarkTheme) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
            } else {
                null // Default Light Mode
            }
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableFloatStateOf(100f) } // Default radius 100 meters
    var showRadiusAdjustment by remember { mutableStateOf(false) }
    
    // -- ALARM STATE --
    var alarmSoundUri by remember { mutableStateOf(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()) }
    var alarmName by remember { mutableStateOf("") } // New state for alarm label
    var showSoundSelectionDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    // -- SOUND PICKER LAUNCHERS --
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri -> 
            alarmSoundUri = uri.toString() 
        }
    }

    val customFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                alarmSoundUri = it.toString()
            } catch (e: Exception) {
                // Fallback if persistable permission fails
                alarmSoundUri = it.toString()
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = { if (hasLocationPermission) {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                val latLng = LatLng(location.latitude, location.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                    )
                                }
                            } else {
                                Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
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
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Device location")
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (markerPosition == null) {
                            markerPosition = cameraPositionState.position.target
                            showRadiusAdjustment = false
                            Toast.makeText(context, "Location set!", Toast.LENGTH_SHORT).show()
                        } else {
                            markerPosition?.let { target ->
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLng(target)
                                    )
                                }
                            }
                            showBottomSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Set Pin"
                        )
                    },
                    text = {
                        Text(
                            text = if (markerPosition == null) "Set Pin" else "Edit Pin",
                            fontSize = 16.sp
                        )
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapClick = {
                    // Optional: Click map to set pin as well
                    // markerPosition = latLng
                }
            ) {
                markerPosition?.let { position ->
                    val markerState = rememberMarkerState(position = position)

                    // Sync changes from marker drag back to our state
                    if (markerState.dragState == DragState.END) {
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

            if (markerPosition == null) {
                Icon(
                    imageVector = Icons.Filled.Add, // Or use a custom crosshair icon
                    contentDescription = "Center Target",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp), // Make it large enough to see
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SearchSection(
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        onNewSearch(query) // Record search on manual search
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
                                    Toast.makeText(
                                        context,
                                        "Location not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "Error searching location",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                onSuggestionClick = { suggestion ->
                    onNewSearch(suggestion) // Record search on suggestion click
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
                                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .verticalScroll(rememberScrollState()), // Add scroll
                verticalArrangement = Arrangement.spacedBy(16.dp) 
            ) {
                // 1. Header Title
                Text(
                    text = "Edit Location",
                    style = MaterialTheme.typography.headlineSmall
                )

                // -- Custom Alarm Name Input --
                OutlinedTextField(
                    value = alarmName,
                    onValueChange = { alarmName = it },
                    label = { Text("Alarm Name") },
                    placeholder = { Text("Enter alarm label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Text(
                        text = "Alert Radius: ${radius.toInt()} meters",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 100f..1000f

                    )

                }
                
                // -- ALARM SOUND SELECTION --
                MapsPickerRow(label = "Alarm Sound", text = getRingtoneTitle(context, alarmSoundUri)) {
                    showSoundSelectionDialog = true
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            // Logic to remove the pin
                            markerPosition = null
                            showRadiusAdjustment = false
                            Toast.makeText(context, "Location removed!", Toast.LENGTH_SHORT).show()

                            // Close the sheet
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Delete")
                    }

                    // Done/Close Button
                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
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
                            // Launch file picker for audio
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

// Helper Composable specifically for MapsScreen to avoid ambiguity
@Composable
fun MapsPickerRow(label: String, text: String, onClick: () -> Unit) {
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

fun getRingtoneTitle(context: Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        // Try getting title from RingtoneManager first
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            // Fallback for custom URIs or if RingtoneManager can't get a title
            ?: uri.lastPathSegment?.substringBeforeLast('.') ?: "Custom Sound"
    } catch (e: Exception) {
        "Unknown Sound"
    }
}
