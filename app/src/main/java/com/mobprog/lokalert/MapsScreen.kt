package com.mobprog.lokalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapsScreen(
    onNewSearch: (String) -> Unit,
    onDone: () -> Unit,
    viewModel: MapsViewModel = viewModel(),
) {


    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Set default sound URI once on launch if empty
    LaunchedEffect(Unit) {
        if (viewModel.alarmSoundUri.isEmpty()) {
            viewModel.alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        }
    }


    // --- Permissions & Location Setup ---
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocationPermission = it[Manifest.permission.ACCESS_FINE_LOCATION] == true || it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Move to user location on load
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 16f) }
            }
        }
    }

    // --- Map Style ---
    val isDarkTheme = isSystemInDarkTheme()
    val mapProperties = remember(isDarkTheme, hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = if (isDarkTheme) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null
        )
    }

    // --- File Pickers ---
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { viewModel.alarmSoundUri = it.toString() }
    }

    fun performSearch(query: String) {
        // 1. FIX: Report the search text back to the parent immediately
        onNewSearch(query)

        scope.launch {
            try {
                val geocoder = Geocoder(context)
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)
                }

                if (!results.isNullOrEmpty()) {
                    val location = results[0]
                    val target = LatLng(location.latitude, location.longitude)

                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(target, 15f)
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

    LaunchedEffect(viewModel.locationToFocus) {
        viewModel.locationToFocus?.let { target ->
            // Move camera to the selected saved location
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 16f)
            )
            // Reset the focus so it doesn't trigger again on rotation/recomposition
            viewModel.locationToFocus = null
        }
    }

    // --- UI Structure ---
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                // My Location FAB
                SmallFloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                loc?.let { scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)) } }
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) { Icon(Icons.Default.MyLocation, "My Location") }

                // Set Pin FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        if (viewModel.markerPosition == null) {
                            // Set pin at center target
                            viewModel.markerPosition = cameraPositionState.position.target
                            viewModel.showBottomSheet = true
                        } else {
                            viewModel.showBottomSheet = true
                        }
                    },
                    icon = { Icon(Icons.Filled.PushPin, "Set Pin") },
                    text = { Text(if (viewModel.markerPosition == null) "Set Pin" else "Edit Pin") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapLongClick = { latLng ->
                    viewModel.markerPosition = latLng
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ) {
                viewModel.markerPosition?.let { position ->
                    val markerState = rememberMarkerState(position = position)
                    // Sync drag
                    if (markerState.dragState == DragState.END) { viewModel.markerPosition = markerState.position }

                    Marker(state = markerState, title = "Selected Location", draggable = true)
                    Circle(
                        center = markerState.position,
                        radius = viewModel.radius.toDouble(),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeWidth = 2f
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                SearchSection(
                    onSearch = { query -> performSearch(query) },
                    onSuggestionClick = { suggestion -> performSearch(suggestion) }
                )
            }
        }
    }

    // --- Bottom Sheet Form ---
    if (viewModel.showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showBottomSheet = false },
            sheetState = sheetState
        ) {
            EditLocationForm(
                viewModel = viewModel,
                onPickRingtone = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(viewModel.alarmSoundUri))
                    }
                    ringtonePicker.launch(intent)
                },
                onCancel = {
                    viewModel.resetForm()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.showBottomSheet = false }
                },
                onSave = {
                    viewModel.saveAlarm {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDone()
                        }
                    }
                }
            )
        }
    }
}

// --- EXTRACTED COMPOSABLES AND HELPERS ---

@Composable
fun EditLocationForm(
    viewModel: MapsViewModel,
    onPickRingtone: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Edit Location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = viewModel.alarmName,
            onValueChange = { viewModel.alarmName = it },
            label = { Text("Alarm Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column {
            Text("Radius: ${viewModel.radius.toInt()}m", fontWeight = FontWeight.SemiBold)
            Slider(value = viewModel.radius, onValueChange = { viewModel.radius = it }, valueRange = 100f..2000f)
        }

        Text("Active Days", fontWeight = FontWeight.SemiBold)
        MapsDaySelector(viewModel.selectedDays) { viewModel.selectedDays = it }

        MapsPickerRow(
            label = "Sound",
            text = getMapRingtoneTitle(context, viewModel.alarmSoundUri),
            onClick = onPickRingtone
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gradual Volume", modifier = Modifier.weight(1f))
            Switch(checked = viewModel.isGradualVolume, onCheckedChange = { viewModel.isGradualVolume = it })
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Remove")
            }
            Button(onClick = onSave) {
                Text("Save Alarm")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsDaySelector(selectedDays: Set<Int>, onSelectionChange: (Set<Int>) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        val calendarDays = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)

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

fun getMapRingtoneTitle(context: Context, ringtoneUriString: String): String {
    if (ringtoneUriString.isEmpty()) return "Default Sound"
    return try {
        val uri = Uri.parse(ringtoneUriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            ?: uri.lastPathSegment?.substringBeforeLast('.') ?: "Custom Sound"
    } catch (exception: Exception) {
        "Unknown Sound"
    }
}