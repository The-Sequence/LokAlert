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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    
    // State for pin replacement overlay
    var searchedLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchedLocationName by remember { mutableStateOf("") }
    var currentPinnedLocationName by remember { mutableStateOf("") }
    var showPinReplaceOverlay by remember { mutableStateOf(false) }
    var persistentPinnedLocationName by remember { mutableStateOf("") }
    var manualNameUpdate by remember { mutableStateOf(false) }
    
    // Update persistent pinned location name when marker changes (only if not manually set)
    LaunchedEffect(viewModel.markerPosition) {
        if (!manualNameUpdate) {
            viewModel.markerPosition?.let { position ->
                // Add a small delay to ensure manual updates happen first
                kotlinx.coroutines.delay(100)
                
                // Double-check flag hasn't been set during delay
                if (!manualNameUpdate) {
                    try {
                        val geocoder = Geocoder(context)
                        val results = withContext(Dispatchers.IO) {
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(position.latitude, position.longitude, 1)
                        }
                        persistentPinnedLocationName = results?.firstOrNull()?.let { addr ->
                            addr.featureName ?: addr.locality ?: addr.subAdminArea ?: "Pinned Location"
                        } ?: "Pinned Location"
                    } catch (e: Exception) {
                        persistentPinnedLocationName = "Pinned Location"
                    }
                }
            }
        }
    }
    
    // Separate effect to reset the manual flag
    LaunchedEffect(manualNameUpdate) {
        if (manualNameUpdate) {
            kotlinx.coroutines.delay(1000)
            manualNameUpdate = false
        }
    }
    
    fun performSearch(query: String) {
        // 1. Report the search text back to the parent immediately
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
                    
                    // Check if there's already a pin set
                    if (viewModel.markerPosition != null) {
                        // Get current pinned location name
                        val currentResults = withContext(Dispatchers.IO) {
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(viewModel.markerPosition!!.latitude, viewModel.markerPosition!!.longitude, 1)
                        }
                        currentPinnedLocationName = currentResults?.firstOrNull()?.let { addr ->
                            addr.featureName ?: addr.locality ?: addr.subAdminArea ?: "Unknown Location"
                        } ?: "Pinned Location"
                        
                        // Store searched location and show overlay
                        searchedLocation = target
                        searchedLocationName = query
                        showPinReplaceOverlay = true
                        
                        // Animate camera to the searched location
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
                    } else {
                        // No existing pin, directly set new pin and animate
                        viewModel.markerPosition = target
                        
                        // Auto-populate alarm name with first two words
                        val words = query.split(" ", ",", "-")
                        viewModel.alarmName = words.take(2).joinToString(" ").trim()
                        
                        // Update persistent info bar immediately
                        persistentPinnedLocationName = query
                        manualNameUpdate = true // Prevent LaunchedEffect from overwriting
                        
                        // Animate camera to the searched location
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
                    }
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
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16f))
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
                        }
                        viewModel.showBottomSheet = true
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
                    
                    // Geocode and update alarm name
                    scope.launch {
                        try {
                            val geocoder = Geocoder(context)
                            val results = withContext(Dispatchers.IO) {
                                @Suppress("DEPRECATION")
                                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                            }
                            val locationName = results?.firstOrNull()?.let { addr ->
                                addr.featureName ?: addr.locality ?: addr.subAdminArea ?: "Pinned Location"
                            } ?: "Pinned Location"
                            
                            // Auto-populate alarm name with first two words
                            val words = locationName.split(" ", ",", "-")
                            viewModel.alarmName = words.take(2).joinToString(" ").trim()
                            
                            // Update persistent info bar
                            persistentPinnedLocationName = locationName
                            manualNameUpdate = true
                        } catch (e: Exception) {
                            persistentPinnedLocationName = "Pinned Location"
                        }
                    }
                }
            ) {
                // Always show pin with radius circle when markerPosition is set
                viewModel.markerPosition?.let { position ->
                    // Use stable key so marker updates instead of recreating
                    val markerState = rememberMarkerState(key = "alarm_pin", position = position)
                    
                    // Track if user is dragging to prevent position updates during drag
                    var isDragging by remember { mutableStateOf(false) }
                    
                    // Only update marker position when not dragging
                    LaunchedEffect(position) {
                        if (!isDragging && markerState.position != position) {
                            markerState.position = position
                        }
                    }
                    
                    // Handle drag events
                    LaunchedEffect(markerState.dragState) {
                        when (markerState.dragState) {
                            DragState.START -> {
                                isDragging = true
                            }
                            DragState.END -> {
                                isDragging = false
                                val newPosition = markerState.position
                                viewModel.markerPosition = newPosition
                                
                                // Geocode and update alarm name and persistent location name
                                try {
                                    val geocoder = Geocoder(context)
                                    val results = withContext(Dispatchers.IO) {
                                        @Suppress("DEPRECATION")
                                        geocoder.getFromLocation(newPosition.latitude, newPosition.longitude, 1)
                                    }
                                    val locationName = results?.firstOrNull()?.let { addr ->
                                        addr.featureName ?: addr.locality ?: addr.subAdminArea ?: "Pinned Location"
                                    } ?: "Pinned Location"
                                    
                                    // Auto-populate alarm name with first two words
                                    val words = locationName.split(" ", ",", "-")
                                    viewModel.alarmName = words.take(2).joinToString(" ").trim()
                                    
                                    // Update persistent info bar
                                    persistentPinnedLocationName = locationName
                                    manualNameUpdate = true
                                } catch (e: Exception) {
                                    persistentPinnedLocationName = "Pinned Location"
                                }
                            }
                            else -> {}
                        }
                    }

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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SearchSection(
                        onSearch = { query -> performSearch(query) },
                        onSuggestionClick = { suggestion -> performSearch(suggestion) }
                    )
                    
                    // Persistent pinned location info bar
                    if (viewModel.markerPosition != null && !showPinReplaceOverlay) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Pinned: $persistentPinnedLocationName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            
            // Pin replacement overlay - below search bar and info bar
            if (showPinReplaceOverlay && searchedLocation != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 180.dp) // Below search bar + info bar
                        .fillMaxWidth(0.75f) // Smaller width
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) // More transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📍 Pinned: $currentPinnedLocationName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            
                            Button(
                                onClick = {
                                    // Replace pin with searched location FIRST
                                    viewModel.markerPosition = searchedLocation
                                    
                                    // Update persistent info bar immediately
                                    persistentPinnedLocationName = searchedLocationName
                                    manualNameUpdate = true // Prevent LaunchedEffect from overwriting
                                    
                                    // Auto-populate alarm name with first two words
                                    val words = searchedLocationName.split(" ", ",", "-")
                                    viewModel.alarmName = words.take(2).joinToString(" ").trim()
                                    
                                    // Close overlay and clear search state
                                    showPinReplaceOverlay = false
                                    searchedLocation = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Set to: $searchedLocationName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            TextButton(
                                onClick = {
                                    showPinReplaceOverlay = false
                                    searchedLocation = null // Clear preview when keeping current
                                },
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("Keep Current", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Bottom Sheet Form ---
    if (viewModel.showBottomSheet) {
        // Clear searchedLocation when bottom sheet opens to ensure clean state
        LaunchedEffect(Unit) {
            searchedLocation = null
        }
        
        var sliderActive by remember { mutableStateOf(false) }
        val sheetAlpha = if (sliderActive) 0.5f else 1f
        val scrimAlpha = if (sliderActive) 0.05f else 0.32f
        val windowTransition = updateTransition(targetState = sliderActive, label = "WindowFoldTransition")
        val sheetMaxHeight by windowTransition.animateDp(
            label = "SheetMaxHeight",
            transitionSpec = { 
                if (targetState) {
                    // Folding down - smooth with slight bounce
                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                } else {
                    // Folding up - smooth, no bounce - SYNCED with content
                    spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
                }
            }
        ) { active -> if (active) 350.dp else 2000.dp }

        ModalBottomSheet(
            onDismissRequest = { viewModel.showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = sheetAlpha),
            scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = scrimAlpha)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
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
                        searchedLocation = null // Clear preview pin when canceling
                        scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.showBottomSheet = false }
                    },
                onSave = {
                    // Check if we're editing an existing alarm (from Locations)
                    if (viewModel.editingAlarmId != null) {
                        // Update ALL alarm fields (name, days, sound, gradual volume, radius)
                        viewModel.updateAlarmComplete(
                            alarmId = viewModel.editingAlarmId!!,
                            newName = viewModel.alarmName,
                            newActiveDays = viewModel.selectedDays,
                            newSoundUri = viewModel.alarmSoundUri,
                            newIsGradualVolume = viewModel.isGradualVolume,
                            newRadius = viewModel.radius
                        )
                        viewModel.resetForm()
                        searchedLocation = null // Clear preview pin after saving
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            // Smooth delay then return to Locations
                            scope.launch {
                                kotlinx.coroutines.delay(300)
                                onDone()
                            }
                        }
                    } else {
                        // Normal save - new alarm
                        viewModel.saveAlarm {
                            searchedLocation = null // Clear preview pin after saving
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDone()
                            }
                        }
                    }
                },
                    onSliderActiveChange = { sliderActive = it }
                )
            }
        }
    }
}

// --- EXTRACTED COMPOSABLES AND HELPERS ---

@Composable
fun EditLocationForm(
    viewModel: MapsViewModel,
    onPickRingtone: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onSliderActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sliderInteractionSource = remember { MutableInteractionSource() }
    var sliderActive by remember { mutableStateOf(false) }

    LaunchedEffect(sliderInteractionSource) {
        sliderInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press, is DragInteraction.Start -> {
                    if (!sliderActive) {
                        sliderActive = true
                        onSliderActiveChange(true)
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel,
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    if (sliderActive) {
                        sliderActive = false
                        onSliderActiveChange(false)
                    }
                }
            }
        }
    }

    val transition = updateTransition(targetState = sliderActive, label = "ContentCollapseTransition")
    
    // Animate upper content collapsing/folding down - synced animations
    val upperContentHeight by transition.animateDp(
        label = "UpperContentHeight",
        transitionSpec = { 
            if (targetState) {
                // Folding down - smooth with slight bounce
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            } else {
                // Folding up - smooth, no bounce
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
            }
        }
    ) { active -> if (active) 0.dp else 2000.dp }
    
    val upperContentAlpha by transition.animateFloat(
        label = "UpperContentAlpha",
        transitionSpec = { 
            // Same timing as height for perfect sync
            if (targetState) {
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            } else {
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
            }
        }
    ) { active -> if (active) 0f else 1f }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper content that collapses when slider is active
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = upperContentHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .alpha(upperContentAlpha),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Edit Location",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = viewModel.alarmName,
                    onValueChange = { viewModel.alarmName = it },
                    label = { Text("Alarm Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Clear button as clickable text link
                if (viewModel.alarmName.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.alarmName = "" },
                        modifier = Modifier.padding(start = 0.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column {
                    Text("Active Days", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    MapsDaySelector(viewModel.selectedDays) { viewModel.selectedDays = it }
                }

                MapsPickerRow(
                    label = "Sound",
                    text = getMapRingtoneTitle(context, viewModel.alarmSoundUri),
                    onClick = onPickRingtone
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gradual Volume", modifier = Modifier.weight(1f))
                    Switch(
                        checked = viewModel.isGradualVolume,
                        onCheckedChange = { viewModel.isGradualVolume = it }
                    )
                }
            }
        }

        // Radius slider - STAYS IN POSITION, always visible
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Radius", fontWeight = FontWeight.SemiBold)
            Slider(
                value = viewModel.radius,
                onValueChange = { viewModel.radius = it },
                valueRange = 100f..5000f,
                interactionSource = sliderInteractionSource,
                modifier = Modifier.fillMaxWidth()
            )
            Text("${viewModel.radius.toInt()} meters", style = MaterialTheme.typography.bodyMedium)
        }

        // Buttons - STAY IN POSITION for consistency
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
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
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

@Composable
fun MapsDaySelector(selectedDays: Set<Int>, onSelectionChange: (Set<Int>) -> Unit) {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val calendarDays = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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