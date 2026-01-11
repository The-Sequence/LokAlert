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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
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
    darkMode: Int = 0, // 0=Light, 1=Dark Gray, 2=Pitch Black, 3=Auto
    onSetPinButtonPositioned: (Rect) -> Unit = {},
    onQuickAlarmButtonPositioned: (Rect) -> Unit = {},
    onSearchBarPositioned: (Rect) -> Unit = {},
    onMapAreaPositioned: (Rect) -> Unit = {},
    onMapMoved: () -> Unit = {},
    onSearchBarTapped: () -> Unit = {},
    onAlarmCreated: () -> Unit = {}
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val defaultAlarmSound by appPreferences.defaultAlarmSound.collectAsState(initial = "")
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Set default sound URI once on launch if empty, or use the default from preferences
    LaunchedEffect(defaultAlarmSound) {
        if (viewModel.alarmSoundUri.isEmpty() || defaultAlarmSound.isNotEmpty()) {
            viewModel.alarmSoundUri = if (defaultAlarmSound.isNotEmpty()) {
                defaultAlarmSound
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
            }
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
    // Resolve effective dark mode for map styling
    // 0=Light, 1=Dark, 2=AMOLED (deprecated), 3=Auto (follow system)
    val isSystemDark = isSystemInDarkTheme()
    val effectiveMapDarkMode = when (darkMode) {
        3 -> if (isSystemDark) 1 else 0  // Auto: follow system
        2 -> 1  // AMOLED deprecated, treat as dark
        else -> darkMode
    }
    val mapProperties = remember(hasLocationPermission, effectiveMapDarkMode) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = if (effectiveMapDarkMode > 0) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null
        )
    }
    
    // Map loading state
    var isMapLoaded by remember { mutableStateOf(false) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    
    // Timeout for map loading (if not loaded after 10 seconds, show error)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10000)
        if (!isMapLoaded) {
            mapLoadError = "Map failed to load. Check API key and internet connection."
            isMapLoaded = true // Stop showing loading indicator
        }
    }

    // --- File Pickers ---
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { viewModel.alarmSoundUri = it.toString() }
    }
    
    // Custom file picker for audio files from storage
    val audioFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Take persistable URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission might not be available for all URIs
            }
            viewModel.alarmSoundUri = it.toString()
        }
    }
    
    var showSoundOptions by remember { mutableStateOf(false) }
    
    // Quick Alarm state
    var showQuickAlarm by remember { mutableStateOf(false) }
    var quickAlarmLocation by remember { mutableStateOf<LatLng?>(null) }
    var quickAlarmRadius by remember { mutableStateOf(200f) }
    var quickAlarmLocationName by remember { mutableStateOf("") }
    var userCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    
    // Get user's current location for context-aware search
    LaunchedEffect(showQuickAlarm) {
        if (showQuickAlarm && userCurrentLocation == null) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            userCurrentLocation = LatLng(it.latitude, it.longitude)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to map center
                userCurrentLocation = cameraPositionState.position.target
            }
        }
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
                            getReadableLocationName(addr)
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
                            getReadableLocationName(addr)
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
                        
                        // Geocode to get proper address
                        val addressResults = withContext(Dispatchers.IO) {
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(target.latitude, target.longitude, 1)
                        }
                        val locationName = addressResults?.firstOrNull()?.let { addr ->
                            getReadableLocationName(addr)
                        } ?: query
                        
                        // Auto-populate alarm name with best readable name
                        viewModel.alarmName = getShortAlarmName(locationName)
                        
                        persistentPinnedLocationName = locationName
                        manualNameUpdate = true
                        
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
                        
                        Toast.makeText(context, "Location found: $locationName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Location not found. Try a different search term.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Search error: ${e.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
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
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        val size = coordinates.size
                        onSetPinButtonPositioned(
                            Rect(
                                offset = Offset(position.x, position.y),
                                size = Size(size.width.toFloat(), size.height.toFloat())
                            )
                        )
                    },
                    icon = { Icon(Icons.Filled.PushPin, "Set Pin") },
                    text = { Text(if (viewModel.markerPosition == null) "Set Pin" else "Edit Pin") }
                )
                
                // Quick Alarm FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        // Get current map center or user location
                        val location = cameraPositionState.position.target
                        quickAlarmLocation = location
                        quickAlarmRadius = 200f
                        
                        // Geocode the location
                        scope.launch {
                            try {
                                val geocoder = Geocoder(context)
                                val results = withContext(Dispatchers.IO) {
                                    @Suppress("DEPRECATION")
                                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                }
                                quickAlarmLocationName = results?.firstOrNull()?.let { addr ->
                                    getReadableLocationName(addr)
                                } ?: "Current Location"
                            } catch (e: Exception) {
                                quickAlarmLocationName = "Current Location"
                            }
                        }
                        
                        showQuickAlarm = true
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        val size = coordinates.size
                        onQuickAlarmButtonPositioned(
                            Rect(
                                offset = Offset(position.x, position.y),
                                size = Size(size.width.toFloat(), size.height.toFloat())
                            )
                        )
                    },
                    icon = { Text("⚡", fontSize = 20.sp) },
                    text = { Text("Quick Alarm") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .onGloballyPositioned { coordinates ->
                // Report map area bounds for guided tour
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                onMapAreaPositioned(
                    Rect(
                        offset = Offset(position.x, position.y),
                        size = Size(size.width.toFloat(), size.height.toFloat())
                    )
                )
            }
        ) {
            // Debug: Log that we're trying to render the map
            LaunchedEffect(Unit) {
                android.util.Log.d("MapsScreen", "Attempting to render GoogleMap")
                android.util.Log.d("MapsScreen", "Has location permission: $hasLocationPermission")
                android.util.Log.d("MapsScreen", "Camera position: ${cameraPositionState.position}")
            }
            
            // Track map movement for guided tour
            LaunchedEffect(cameraPositionState.isMoving) {
                if (cameraPositionState.isMoving) {
                    onMapMoved()
                }
            }
            
            // GoogleMap component
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapLoaded = {
                    android.util.Log.d("MapsScreen", "Map loaded successfully!")
                    isMapLoaded = true
                },
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
                                getReadableLocationName(addr)
                            } ?: "Pinned Location"
                            
                            // Auto-populate alarm name with readable short name
                            viewModel.alarmName = getShortAlarmName(locationName)
                            
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
                                        getReadableLocationName(addr)
                                    } ?: "Pinned Location"
                                    
                                    // Auto-populate alarm name with readable short name
                                    viewModel.alarmName = getShortAlarmName(locationName)
                                    
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
            
            // Map loading indicator or error message
            if (!isMapLoaded || mapLoadError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (mapLoadError != null) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Map Loading Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    mapLoadError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        // Reset and retry
                                        isMapLoaded = false
                                        mapLoadError = null
                                    }
                                ) {
                                    Text("Retry")
                                }
                            } else {
                                CircularProgressIndicator()
                                Text("Loading map...", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "This may take a few seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SearchSection(
                        onSearch = { query -> performSearch(query) },
                        onSuggestionClick = { suggestion -> performSearch(suggestion) },
                        onSearchBarFocused = { onSearchBarTapped() },
                        onPositioned = { bounds -> onSearchBarPositioned(bounds) }
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
                                    // Replace pin with searched location
                                    viewModel.markerPosition = searchedLocation
                                    
                                    // Geocode to get better location name
                                    scope.launch {
                                        try {
                                            val geocoder = Geocoder(context)
                                            val results = withContext(Dispatchers.IO) {
                                                @Suppress("DEPRECATION")
                                                geocoder.getFromLocation(searchedLocation!!.latitude, searchedLocation!!.longitude, 1)
                                            }
                                            val locationName = results?.firstOrNull()?.let { addr ->
                                                getReadableLocationName(addr)
                                            } ?: searchedLocationName
                                            
                                            // Update persistent info bar
                                            persistentPinnedLocationName = locationName
                                            manualNameUpdate = true
                                            
                                            // Auto-populate alarm name with readable short name
                                            viewModel.alarmName = getShortAlarmName(locationName)
                                        } catch (e: Exception) {
                                            persistentPinnedLocationName = searchedLocationName
                                            viewModel.alarmName = getShortAlarmName(searchedLocationName)
                                        }
                                    }
                                    
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
                        showSoundOptions = true
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
                    onSliderActiveChange = { /* Content animation only - sheet stays visible */ }
                )
            }
        }
    }
    
    // Sound selection options dialog
    if (showSoundOptions) {
        AlertDialog(
            onDismissRequest = { showSoundOptions = false },
            title = { Text("Choose Alarm Sound") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Select a sound for this alarm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // System Ringtones option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    if (viewModel.alarmSoundUri.isNotEmpty()) {
                                        try {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(viewModel.alarmSoundUri))
                                        } catch (e: Exception) { }
                                    }
                                }
                                ringtonePicker.launch(intent)
                                showSoundOptions = false
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
                    
                    // Custom audio file option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                audioFilePicker.launch("audio/*")
                                showSoundOptions = false
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
                TextButton(onClick = { showSoundOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Quick Alarm Dialog
    if (showQuickAlarm && quickAlarmLocation != null) {
        QuickAlarmDialog(
            quickAlarmLocation = quickAlarmLocation!!,
            quickAlarmLocationName = quickAlarmLocationName,
            quickAlarmRadius = quickAlarmRadius,
            onRadiusChange = { quickAlarmRadius = it },
            userCurrentLocation = userCurrentLocation,
            mapProperties = mapProperties,
            defaultAlarmSound = defaultAlarmSound,
            onDismiss = { showQuickAlarm = false },
            onConfirm = { location, name, radius ->
                // Create quick alarm with today only
                val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                
                // Use default alarm sound from settings, or system default if not set
                val soundUri = if (defaultAlarmSound.isNotEmpty()) {
                    defaultAlarmSound
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
                }
                
                val alarm = LocationAlarm(
                    name = getShortAlarmName(name),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = radius,
                    soundUri = soundUri,
                    isGradualVolume = false,
                    activeDays = setOf(currentDay),
                    isEnabled = true,
                    isFavorite = false
                )
                
                // Save the alarm using ViewModel's DAO
                scope.launch {
                    viewModel.dao.insertAlarm(alarm)
                    
                    withContext(Dispatchers.Main) {
                        // Show success message
                        Toast.makeText(
                            context,
                            "⚡ Quick alarm set for ${getShortAlarmName(name)}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Close dialog and navigate
                        showQuickAlarm = false
                        kotlinx.coroutines.delay(300)
                        onDone()
                    }
                }
            },
            onLocationChange = { newLocation, newName ->
                quickAlarmLocation = newLocation
                quickAlarmLocationName = newName
            }
        )
    }
}

// --- EXTRACTED COMPOSABLES AND HELPERS ---

@SuppressLint("UnrememberedMutableState")
@Composable
fun QuickAlarmDialog(
    quickAlarmLocation: LatLng,
    quickAlarmLocationName: String,
    quickAlarmRadius: Float,
    onRadiusChange: (Float) -> Unit,
    userCurrentLocation: LatLng?,
    mapProperties: MapProperties,
    defaultAlarmSound: String,
    onDismiss: () -> Unit,
    onConfirm: (LatLng, String, Float) -> Unit,
    onLocationChange: (LatLng, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get sound name for display
    val soundName = remember(defaultAlarmSound) {
        if (defaultAlarmSound.isEmpty()) {
            "default alarm sound"
        } else {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(defaultAlarmSound))
                ringtone.getTitle(context)
            } catch (e: Exception) {
                "default alarm sound"
            }
        }
    }
    
    var currentLocation by remember { mutableStateOf(quickAlarmLocation) }
    var currentLocationName by remember { mutableStateOf(quickAlarmLocationName) }
    
    // Search function using Geocoder (same as main map for consistency)
    fun performQuickAlarmSearch(query: String) {
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
                    
                    // Use the selected suggestion text directly as the name
                    // to avoid double-geocoding which causes inaccuracy
                    currentLocation = target
                    currentLocationName = query
                    onLocationChange(currentLocation, currentLocationName)
                    
                    Toast.makeText(context, "Location set: $query", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Location not found. Try a different search term.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Search error: ${e.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Detect screen size for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = screenWidth > 600.dp || (isLandscape && screenWidth > 500.dp)
    val isCompactHeight = screenHeight < 500.dp
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else if (isWideScreen) 0.65f else 0.92f)
                .fillMaxHeight(if (isCompactHeight) 0.95f else if (isWideScreen) 0.8f else 0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLandscape && isCompactHeight) {
                    // Landscape compact layout - side by side
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Left side - Map
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                                .padding(12.dp)
                        ) {
                            // Header
                            Text(
                                "⚡ Quick Alarm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Mini map preview
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                val quickMapCameraState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
                                }
                                
                                LaunchedEffect(currentLocation) {
                                    quickMapCameraState.animate(
                                        CameraUpdateFactory.newLatLngZoom(currentLocation, 15f),
                                        durationMs = 500
                                    )
                                }
                                
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = quickMapCameraState,
                                    properties = mapProperties,
                                    uiSettings = MapUiSettings(
                                        zoomControlsEnabled = false,
                                        myLocationButtonEnabled = false,
                                        scrollGesturesEnabled = false,
                                        zoomGesturesEnabled = false,
                                        tiltGesturesEnabled = false,
                                        rotationGesturesEnabled = false
                                    )
                                ) {
                                    Marker(
                                        state = MarkerState(position = currentLocation),
                                        title = "Alarm Location"
                                    )
                                    Circle(
                                        center = currentLocation,
                                        radius = quickAlarmRadius.toDouble(),
                                        strokeColor = MaterialTheme.colorScheme.tertiary,
                                        fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }
                        
                        // Right side - Controls
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search
                            QuickSearchSection(
                                onSearch = { query -> performQuickAlarmSearch(query) },
                                onSuggestionClick = { suggestion -> performQuickAlarmSearch(suggestion) }
                            )
                            
                            // Radius slider
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Alert Radius",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    ) {
                                        Text(
                                            "${quickAlarmRadius.toInt()}m",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                
                                Slider(
                                    value = quickAlarmRadius,
                                    onValueChange = onRadiusChange,
                                    valueRange = 100f..500f,
                                    steps = 7,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            // Info card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ℹ️", style = MaterialTheme.typography.bodySmall)
                                    Column {
                                        Text(
                                            "Today only • $soundName",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", fontSize = 12.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        onConfirm(currentLocation, currentLocationName, quickAlarmRadius)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Set", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Portrait layout - original stacked layout
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // Header - ample padding
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (isWideScreen) 24.dp else 20.dp,
                                bottom = if (isWideScreen) 12.dp else 10.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚡ Quick Alarm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Set up in seconds!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Content with map, slider, info - scrollable with better spacing
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (isWideScreen) 24.dp else 20.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 20.dp else 14.dp)
                    ) {
                        // Spacer for search bar that overlays
                        Spacer(modifier = Modifier.height(if (isWideScreen) 64.dp else 56.dp))
                        
                        // Mini map preview
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isWideScreen) 240.dp else 200.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val quickMapCameraState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
                            }
                            
                            // Animate camera when location changes
                            LaunchedEffect(currentLocation) {
                                quickMapCameraState.animate(
                                    CameraUpdateFactory.newLatLngZoom(currentLocation, 15f),
                                    durationMs = 500
                                )
                            }
                            
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = quickMapCameraState,
                                properties = mapProperties,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = false,
                                    myLocationButtonEnabled = false,
                                    scrollGesturesEnabled = false,
                                    zoomGesturesEnabled = false,
                                    tiltGesturesEnabled = false,
                                    rotationGesturesEnabled = false
                                )
                            ) {
                                Marker(
                                    state = MarkerState(position = currentLocation),
                                    title = "Alarm Location"
                                )
                                Circle(
                                    center = currentLocation,
                                    radius = quickAlarmRadius.toDouble(),
                                    strokeColor = MaterialTheme.colorScheme.tertiary,
                                    fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    strokeWidth = 3f
                                )
                            }
                        }
                        
                        // Radius slider
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Alert Radius",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Text(
                                        "${quickAlarmRadius.toInt()}m",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            
                            Slider(
                                value = quickAlarmRadius,
                                onValueChange = onRadiusChange,
                                valueRange = 100f..500f,
                                steps = 7,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "100m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "500m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Info card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ℹ️", style = MaterialTheme.typography.titleMedium)
                                Column {
                                    Text(
                                        "This alarm will run today only",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Uses $soundName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Action buttons at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isWideScreen) 24.dp else 20.dp,
                                vertical = if (isWideScreen) 20.dp else 16.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                onConfirm(currentLocation, currentLocationName, quickAlarmRadius)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Set Alarm", fontWeight = FontWeight.Bold)
                        }
                    }
                    }
                }
                
                // SearchSection overlay - positioned on top of content (only for portrait)
                if (!isLandscape || !isCompactHeight) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = if (isWideScreen) 24.dp else 20.dp)
                            .padding(top = if (isWideScreen) 84.dp else 76.dp) // Position below header
                    ) {
                        QuickSearchSection(
                            onSearch = { query -> performQuickAlarmSearch(query) },
                            onSuggestionClick = { suggestion -> performQuickAlarmSearch(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

// --- EXTRACTED COMPOSABLES AND HELPERS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchSection(
    onSearch: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var userHasSelectedSuggestion by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Places Client Setup
    val placesClient = remember {
        try {
            if (!Places.isInitialized()) {
                val packageInfo = context.packageManager.getApplicationInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_META_DATA
                )
                val apiKey = packageInfo.metaData?.getString("com.google.android.geo.API_KEY")
                if (apiKey != null) {
                    Places.initialize(context, apiKey)
                }
            }
            if (Places.isInitialized()) {
                Places.createClient(context)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    // Autocomplete Logic - limit to 3 suggestions
    LaunchedEffect(searchText) {
        if (userHasSelectedSuggestion) {
            userHasSelectedSuggestion = false
            return@LaunchedEffect
        }
        if (searchText.isNotEmpty() && placesClient != null) {
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(token)
                    .setQuery(searchText)
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        suggestions = response.autocompletePredictions
                            .take(3) // Limit to 3 suggestions
                            .map { it.getFullText(null).toString() }
                        expanded = suggestions.isNotEmpty()
                    }
                    .addOnFailureListener {
                        suggestions = emptyList()
                        expanded = false
                    }
            } catch (e: Exception) {
                suggestions = emptyList()
                expanded = false
            }
        } else {
            suggestions = emptyList()
            expanded = false
        }
    }

    // Compact UI for overlay
    Surface(
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        if (placesClient != null) "Search location..." else "Search unavailable",
                        fontSize = 14.sp
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.clickable {
                                searchText = ""
                                expanded = false
                                keyboardController?.hide()
                            }
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchText.isNotEmpty()) {
                            onSearch?.invoke(searchText)
                            expanded = false
                            keyboardController?.hide()
                        }
                    }
                )
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    userHasSelectedSuggestion = true
                                    searchText = suggestion
                                    expanded = false
                                    onSuggestionClick?.invoke(suggestion)
                                    keyboardController?.hide()
                                }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditLocationForm(
    viewModel: MapsViewModel,
    onPickRingtone: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onSliderActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Sound preview state
    var isPlayingPreview by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    // Clean up media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    // Play preview function
    fun playPreviewSound() {
        if (isPlayingPreview) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlayingPreview = false
        } else {
            try {
                val uri = if (viewModel.alarmSoundUri.isNotEmpty()) {
                    Uri.parse(viewModel.alarmSoundUri)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, uri)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    if (viewModel.isGradualVolume) {
                        setVolume(0.2f, 0.2f)
                    }
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlayingPreview = false
                        release()
                        mediaPlayer = null
                    }
                }
                isPlayingPreview = true
                if (viewModel.isGradualVolume) {
                    scope.launch {
                        var currentVolume = 0.2f
                        while (isPlayingPreview && currentVolume < 1.0f) {
                            kotlinx.coroutines.delay(500)
                            currentVolume = (currentVolume + 0.1f).coerceAtMost(1.0f)
                            try { mediaPlayer?.setVolume(currentVolume, currentVolume) } catch (_: Exception) { break }
                        }
                    }
                }
                scope.launch {
                    kotlinx.coroutines.delay(5000)
                    if (isPlayingPreview) {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlayingPreview = false
                    }
                }
            } catch (_: Exception) {
                isPlayingPreview = false
            }
        }
    }
    
    // Detect screen size for responsive layout
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isCompactHeight = screenHeight < 400.dp || (isLandscape && screenHeight < 500.dp)
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
    
    // Dynamic spacing based on screen size
    val contentSpacing = if (isCompactHeight) 10.dp else 16.dp
    val horizontalPadding = if (isCompactHeight) 12.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(contentSpacing)
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
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 10.dp else 16.dp)
            ) {
                Text(
                    "Set Alarm Location",
                    style = if (isCompactHeight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = viewModel.alarmName,
                    onValueChange = { viewModel.alarmName = it },
                    label = { Text("Alarm Name", fontSize = if (isCompactHeight) 12.sp else 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = if (isCompactHeight) 14.sp else 16.sp),
                    trailingIcon = if (viewModel.alarmName.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.alarmName = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(if (isCompactHeight) 16.dp else 20.dp)
                                )
                            }
                        }
                    } else null
                )

                Column {
                    Text("Active Days", fontWeight = FontWeight.SemiBold, fontSize = if (isCompactHeight) 13.sp else 14.sp)
                    Spacer(Modifier.height(if (isCompactHeight) 4.dp else 8.dp))
                    EnhancedDaySelector(viewModel.selectedDays, isCompact = isCompactHeight) { viewModel.selectedDays = it }
                }

                MapsPickerRow(
                    label = "Sound",
                    text = getMapRingtoneTitle(context, viewModel.alarmSoundUri),
                    onClick = onPickRingtone,
                    isCompact = isCompactHeight,
                    onPlayClick = { playPreviewSound() },
                    isPlaying = isPlayingPreview
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gentle wake-up", fontWeight = FontWeight.Medium, fontSize = if (isCompactHeight) 13.sp else 14.sp)
                        Text(
                            "Starts quiet, gets louder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = if (isCompactHeight) 11.sp else 12.sp
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 4.dp else 8.dp)
        ) {
            Text("Radius", fontWeight = FontWeight.SemiBold, fontSize = if (isCompactHeight) 13.sp else 14.sp)
            Slider(
                value = viewModel.radius,
                onValueChange = { viewModel.radius = it },
                valueRange = 100f..5000f,
                interactionSource = sliderInteractionSource,
                modifier = Modifier.fillMaxWidth()
            )
            Text("${viewModel.radius.toInt()} meters", style = MaterialTheme.typography.bodyMedium, fontSize = if (isCompactHeight) 12.sp else 14.sp)
        }

        // Buttons - STAY IN POSITION for consistency
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isCompactHeight) 4.dp else 8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remove", fontSize = if (isCompactHeight) 13.sp else 14.sp)
            }
            Button(onClick = onSave) {
                Text("Save Alarm", fontSize = if (isCompactHeight) 13.sp else 14.sp)
            }
        }

        Spacer(Modifier.height(if (isCompactHeight) 16.dp else 32.dp))
    }
}

@Composable
fun MapsPickerRow(
    label: String,
    text: String,
    onClick: () -> Unit,
    isCompact: Boolean = false,
    onPlayClick: (() -> Unit)? = null,
    isPlaying: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 48.dp else 56.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = if (isCompact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (isCompact) 10.sp else 12.sp
            )
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = if (isCompact) 13.sp else 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onPlayClick != null) {
                TextButton(
                    onClick = onPlayClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isPlaying) "Stop" else "Preview",
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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

/**
 * Extract a readable location name from address components.
 * Priority: featureName > thoroughfare (street) > locality > subAdminArea > adminArea
 */
fun getReadableLocationName(address: android.location.Address): String {
    // Try to build a meaningful name from address components
    val parts = mutableListOf<String>()
    
    // Add feature name (e.g., "Starbucks", "Central Park")
    address.featureName?.let { if (it.isNotBlank() && !it.matches(Regex("\\d+"))) parts.add(it) }
    
    // Add thoroughfare (street name) if different from feature name
    address.thoroughfare?.let { 
        if (it.isNotBlank() && it != address.featureName && !it.matches(Regex("\\d+"))) {
            parts.add(it)
        }
    }
    
    // Add locality (city/town) if not already included
    address.locality?.let { 
        if (it.isNotBlank() && !parts.contains(it)) {
            parts.add(it)
        }
    }
    
    // If we don't have enough info, add subAdminArea
    if (parts.size < 2) {
        address.subAdminArea?.let { 
            if (it.isNotBlank() && !parts.contains(it)) {
                parts.add(it)
            }
        }
    }
    
    // If still no good info, add adminArea (state/province)
    if (parts.isEmpty()) {
        address.adminArea?.let { if (it.isNotBlank()) parts.add(it) }
    }
    
    return if (parts.isNotEmpty()) {
        parts.take(2).joinToString(", ")
    } else {
        "Pinned Location"
    }
}

/**
 * Check if a string is a Plus Code (Open Location Code).
 * Plus Codes have the format: 8FVC9G8F+5W or similar patterns with a + in them.
 * This also matches Plus Codes with trailing city/region names like "9G8F+5W Singapore".
 */
fun isPlusCode(text: String): Boolean {
    val trimmed = text.trim()
    // Plus codes contain a '+' character
    if (!trimmed.contains('+')) return false

    // Extract the first "word" before any space/comma to check if it's the Plus Code part
    val firstPart = trimmed.split(Regex("[\\s,]+")).firstOrNull() ?: return false

    // Full Plus Code pattern (8 chars + 2+ chars): 8FVC9G8F+5W
    val fullPlusCodePattern = Regex("^[2-9CFGHJMPQRVWX]{8}\\+[2-9CFGHJMPQRVWX]{2,}$", RegexOption.IGNORE_CASE)

    // Short Plus Code pattern (4 chars + 2+ chars): 9G8F+5W (used with region)
    val shortPlusCodePattern = Regex("^[2-9CFGHJMPQRVWX]{4,6}\\+[2-9CFGHJMPQRVWX]{2,}$", RegexOption.IGNORE_CASE)

    // Check if the first part (before any space/comma) matches a Plus Code pattern
    return fullPlusCodePattern.matches(firstPart) || shortPlusCodePattern.matches(firstPart)
}

/**
 * Generate a short, user-friendly alarm name from a location name.
 * Takes the most relevant 2-3 words.
 * For Plus Codes, extracts only the city/region name (e.g., "9G8F+5W Singapore" -> "Singapore").
 */
fun getShortAlarmName(locationName: String): String {
    val trimmed = locationName.trim()

    // Check if it contains a Plus Code and extract the location part after it
    if (trimmed.contains('+')) {
        // Pattern to match Plus Code followed by optional location name
        // Plus Code format: alphanumeric+alphanumeric, then optional space and location
        val plusCodeWithLocation = Regex("^[A-Z0-9]{2,8}\\+[A-Z0-9]{2,}\\s*,?\\s*(.+)$", RegexOption.IGNORE_CASE)
        val match = plusCodeWithLocation.find(trimmed)
        if (match != null) {
            // Extract the location part after the Plus Code
            val locationPart = match.groupValues[1].trim()
            if (locationPart.isNotBlank()) {
                // Process the extracted location name
                return extractShortName(locationPart)
            }
        }

        // If it's just a Plus Code without a location name, return empty to let user input
        if (isPlusCode(trimmed)) {
            return ""
        }
    }

    // Normal processing for non-Plus Code names
    return extractShortName(trimmed)
}

/**
 * Extract a short name from a location string by taking the most relevant 2-3 words.
 */
private fun extractShortName(locationName: String): String {
    val commonWords = setOf("the", "a", "an", "at", "in", "on", "near", "by")
    val words = locationName
        .split(" ", ",", "-", "/")
        .map { it.trim() }
        .filter { it.isNotBlank() && it.lowercase() !in commonWords && !isPlusCode(it) }

    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0]
        else -> words.take(2).joinToString(" ")
    }
}

/**
 * Calculate distance in kilometers between two LatLng points using Haversine formula.
 */
fun calculateDistance(point1: LatLng, point2: LatLng): Double {
    val earthRadius = 6371.0 // Earth radius in kilometers
    
    val lat1Rad = Math.toRadians(point1.latitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
    val deltaLon = Math.toRadians(point2.longitude - point1.longitude)
    
    val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1Rad) * Math.cos(lat2Rad) *
            Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
    
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    
    return earthRadius * c
}
