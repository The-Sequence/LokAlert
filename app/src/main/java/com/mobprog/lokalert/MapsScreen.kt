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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

    // Demo Mode State
    val demoModeManager = remember { DemoModeManager.getInstance(context) }
    val isDemoModeEnabled by demoModeManager.isDemoModeEnabled.collectAsState()
    val mockLocation by demoModeManager.mockLocation.collectAsState()
    val demoDestination by demoModeManager.destination.collectAsState()
    val demoDestinationRadius by demoModeManager.destinationRadius.collectAsState()
    val isDemoMoving by demoModeManager.isMoving.collectAsState()
    val demoProgress by demoModeManager.progress.collectAsState()
    val demoSpeed by demoModeManager.speedMps.collectAsState()
    val distanceToDestination by demoModeManager.distanceToDestination.collectAsState()
    val restartOnboardingRequested by demoModeManager.restartOnboardingRequested.collectAsState()

    // Speed slider visibility during demo movement
    var isSpeedSliderExpanded by remember { mutableStateOf(false) }
    var previousSpeedSliderExpanded: Boolean? by remember { mutableStateOf(null) }

    // Tutorial state
    var showDemoTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(0) }

    // Spotlight bounds for tutorial
    var playButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var speedSliderBounds by remember { mutableStateOf<Rect?>(null) }
    var demoBannerBounds by remember { mutableStateOf<Rect?>(null) }

    // Track MapsScreen container offset to adjust spotlight positioning
    var mapsScreenWindowOffset by remember { mutableStateOf(Offset.Zero) }

    // Configuration for responsive layout
    val mapsScreenConfiguration = LocalConfiguration.current
    val isLandscapeOrientation = mapsScreenConfiguration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val demoBannerTopPadding = if (isLandscapeOrientation) 24.dp else 48.dp // Status bar / notch padding

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

    // Demo Onboarding Flow state
    var showDemoOnboarding by remember { mutableStateOf(false) }

    // Handle restart onboarding request from alarm overlay
    LaunchedEffect(restartOnboardingRequested) {
        if (restartOnboardingRequested) {
            // First show the onboarding flow
            showDemoOnboarding = true
            // Small delay to ensure onboarding is visible
            kotlinx.coroutines.delay(100)
            // Then acknowledge the request (this clears isRestartPending)
            demoModeManager.acknowledgeRestartRequest()
        }
    }

    // Show tutorial when demo mode first becomes active after onboarding
    // Skip showing tutorial if a restart is pending (user selected "Try Another")
    LaunchedEffect(isDemoModeEnabled, mockLocation, demoDestination, showDemoOnboarding, restartOnboardingRequested) {
        if (isDemoModeEnabled && mockLocation != null && demoDestination != null && !showDemoOnboarding && !restartOnboardingRequested) {
            // Small delay to let the UI settle
            kotlinx.coroutines.delay(500)
            showDemoTutorial = true
            tutorialStep = 0
        }
    }

    // Demo mode camera tracking state
    var shouldFollowDemoLocation by remember { mutableStateOf(true) }

    // Focus camera on mock location when demo mode is enabled and following is on
    LaunchedEffect(isDemoModeEnabled, mockLocation, shouldFollowDemoLocation) {
        if (isDemoModeEnabled && mockLocation != null && shouldFollowDemoLocation) {
            mockLocation?.let { loc ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(loc, 16f),
                    durationMs = 500
                )
            }
        }
    }

    // Also focus camera when demo mode is first enabled
    LaunchedEffect(isDemoModeEnabled) {
        if (isDemoModeEnabled && mockLocation != null) {
            mockLocation?.let { loc ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(loc, 16f),
                    durationMs = 500
                )
            }
        }
    }

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

                        // Auto-open bottom sheet for alarm configuration
                        viewModel.showBottomSheet = true

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

    // Handle pending search query (from recent searches or other navigation)
    LaunchedEffect(viewModel.pendingSearchQuery) {
        viewModel.pendingSearchQuery?.let { query ->
            // Clear the pending query first to prevent re-triggering
            viewModel.pendingSearchQuery = null
            // Execute the search
            performSearch(query)
        }
    }

    // --- UI Structure ---
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.navigationBarsPadding()
            ) {
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

                // Try Demo FAB - only show when demo mode is NOT active
                if (!isDemoModeEnabled) {
                    ExtendedFloatingActionButton(
                        onClick = { showDemoOnboarding = true },
                        icon = { Text("🎮", fontSize = 20.sp) },
                        text = { Text("Try Demo") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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
                // Track the MapsScreen container's window offset for spotlight positioning
                mapsScreenWindowOffset = Offset(position.x, position.y)
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

                // Demo Mode: Custom blue dot for mock location and destination
                if (isDemoModeEnabled && mockLocation != null) {
                    val mockMarkerState = rememberMarkerState(key = "demo_mock_location", position = mockLocation!!)

                    // Update marker position when mock location changes
                    LaunchedEffect(mockLocation) {
                        mockLocation?.let { loc ->
                            mockMarkerState.position = loc
                        }
                    }

                    Marker(
                        state = mockMarkerState,
                        title = "Mock Location (Demo)",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        zIndex = 100f
                    )

                    // Demo destination marker and radius circle
                    demoDestination?.let { dest ->
                        val destMarkerState = rememberMarkerState(key = "demo_destination", position = dest)

                        LaunchedEffect(dest) {
                            destMarkerState.position = dest
                        }

                        Marker(
                            state = destMarkerState,
                            title = "Demo Destination",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            zIndex = 99f
                        )

                        // Destination radius circle
                        Circle(
                            center = dest,
                            radius = demoDestinationRadius.toDouble(),
                            strokeColor = Color(0xFF4CAF50),
                            fillColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            strokeWidth = 3f
                        )
                    }

                    // Draw path line from mock location to destination
                    demoDestination?.let { dest ->
                        mockLocation?.let { start ->
                            Polyline(
                                points = listOf(start, dest),
                                color = Color(0xFF2196F3).copy(alpha = 0.6f),
                                width = 8f
                            )
                        }
                    }
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
                    // Demo Mode Indicator Banner
                    if (isDemoModeEnabled) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = demoBannerTopPadding) // Status bar / notch padding
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInWindow()
                                    val size = coordinates.size
                                    demoBannerBounds = Rect(
                                        offset = Offset(position.x, position.y),
                                        size = Size(size.width.toFloat(), size.height.toFloat())
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.95f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Route,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Demo Mode Active",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            val speedText = when {
                                                demoSpeed < 2 -> "${demoSpeed} m/s • Walking"
                                                demoSpeed < 8 -> "${demoSpeed} m/s • Jogging"
                                                demoSpeed < 15 -> "${demoSpeed} m/s • Cycling"
                                                else -> "${demoSpeed} m/s • Driving"
                                            }
                                            // Format distance display
                                            val distanceText = if (distanceToDestination >= 1000) {
                                                String.format("%.1f km", distanceToDestination / 1000)
                                            } else {
                                                "${distanceToDestination.toInt()} m"
                                            }
                                            Text(
                                                text = if (isDemoMoving) "Moving • $distanceText left • ${(demoProgress * 100).toInt()}%" else "$speedText • $distanceText to destination",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Follow toggle button
                                        FilledTonalIconButton(
                                            onClick = { shouldFollowDemoLocation = !shouldFollowDemoLocation },
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = if (shouldFollowDemoLocation)
                                                    Color.White.copy(alpha = 0.3f)
                                                else
                                                    Color.White.copy(alpha = 0.15f),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                if (shouldFollowDemoLocation) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (shouldFollowDemoLocation) "Auto-follow ON" else "Auto-follow OFF",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (isDemoMoving) {
                                            FilledTonalIconButton(
                                                onClick = { demoModeManager.stopMovement() },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = Color.White.copy(alpha = 0.25f),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Pause,
                                                    contentDescription = "Pause",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            FilledTonalIconButton(
                                                onClick = { demoModeManager.startMovement() },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .onGloballyPositioned { coordinates ->
                                                        val position = coordinates.positionInWindow()
                                                        val size = coordinates.size
                                                        playButtonBounds = Rect(
                                                            offset = Offset(position.x, position.y),
                                                            size = Size(size.width.toFloat(), size.height.toFloat())
                                                        )
                                                    }
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        if (!isDemoMoving && demoProgress > 0.01f) {
                                            FilledTonalIconButton(
                                                onClick = { demoModeManager.resetToStart() },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = Color.White.copy(alpha = 0.25f),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Reset",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        // Close/Disable Demo Mode button
                                        FilledTonalIconButton(
                                            onClick = { 
                                                demoModeManager.setDemoModeEnabled(false)
                                            },
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = Color.White.copy(alpha = 0.2f),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Exit Demo Mode",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search bar - hide during demo movement to prevent UI overlap
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isDemoMoving,
                        enter = androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(300)
                        )
                    ) {
                        SearchSection(
                            onSearch = { query -> performSearch(query) },
                            onSuggestionClick = { suggestion -> performSearch(suggestion) },
                            onSearchBarFocused = { onSearchBarTapped() },
                            onPositioned = { bounds -> onSearchBarPositioned(bounds) },
                            applyTopPadding = !isDemoModeEnabled // Don't apply internal top padding when demo banner is above
                        )
                    }

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

            // FLOATING SPEED SLIDER - positioned at bottom, doesn't push content
            if (isDemoModeEnabled && isDemoMoving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, bottom = 100.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInWindow()
                                val size = coordinates.size
                                speedSliderBounds = Rect(
                                    offset = Offset(position.x, position.y),
                                    size = Size(size.width.toFloat(), size.height.toFloat())
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header row with collapse button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSpeedSliderExpanded = !isSpeedSliderExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Speed: ${demoSpeed} m/s",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            when {
                                                demoSpeed < 2 -> "Walking"
                                                demoSpeed < 8 -> "Jogging"
                                                demoSpeed < 15 -> "Cycling"
                                                else -> "Driving"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                // Explicit collapse button
                                IconButton(
                                    onClick = { isSpeedSliderExpanded = !isSpeedSliderExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (isSpeedSliderExpanded) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = if (isSpeedSliderExpanded) "Collapse" else "Expand",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Expandable slider
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSpeedSliderExpanded,
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Slider(
                                        value = demoSpeed.toFloat(),
                                        onValueChange = { demoModeManager.setSpeed(it.toInt()) },
                                        valueRange = 1f..50f,
                                        steps = 48,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("1", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("25", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("50 m/s", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pin replacement overlay - below search bar and info bar
            if (showPinReplaceOverlay && searchedLocation != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 180.dp) // Below search bar + info bar
                        .fillMaxWidth(0.75f) // Smaller width
                        .padding(horizontal = 32.dp) // Center horizontally
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
                        // Normal save - new alarm (including when using saved location)
                        // If using a saved location and no name provided, suggest a name
                        if (viewModel.alarmName.isBlank() && viewModel.selectedSavedLocationForNewAlarm != null) {
                            viewModel.alarmName = "New ${viewModel.selectedSavedLocationForNewAlarm!!.name}"
                        }
                        viewModel.saveAlarm {
                            searchedLocation = null // Clear preview pin after saving
                            onAlarmCreated() // Notify that alarm was created
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

    // Demo Onboarding Flow Dialog
    if (showDemoOnboarding) {
        DemoOnboardingFlow(
            onDismiss = { showDemoOnboarding = false },
            onComplete = { showDemoOnboarding = false },
            darkMode = darkMode
        )
    }

    // Auto-expand floating speed slider during tutorial step 1 so spotlight hits the controls
    // Also auto-start demo movement for step 1 since the slider only appears when moving
    LaunchedEffect(showDemoTutorial, tutorialStep) {
        if (showDemoTutorial && tutorialStep == 1) {
            // Start demo movement if not already moving (slider only appears when moving)
            if (!isDemoMoving) {
                demoModeManager.startMovement()
            }
            // Small delay to let the slider appear
            kotlinx.coroutines.delay(300)
            // Save previous state and expand
            if (previousSpeedSliderExpanded == null) {
                previousSpeedSliderExpanded = isSpeedSliderExpanded
            }
            isSpeedSliderExpanded = true
        } else if (!showDemoTutorial && previousSpeedSliderExpanded != null) {
            // Only restore when tutorial is completely dismissed
            isSpeedSliderExpanded = previousSpeedSliderExpanded!!
            previousSpeedSliderExpanded = null
        }
    }

    // Demo Tutorial Overlay - shows after demo setup completes
    if (showDemoTutorial && isDemoModeEnabled && !showDemoOnboarding) {
        val rawSpotlightBounds = when (tutorialStep) {
            0 -> playButtonBounds
            1 -> speedSliderBounds
            2 -> demoBannerBounds
            else -> null
        }

        // Adjust spotlight bounds by subtracting the MapsScreen container's window offset
        // This converts window coordinates to local MapsScreen coordinates
        val adjustedSpotlightBounds = rawSpotlightBounds?.let { bounds ->
            Rect(
                offset = Offset(
                    bounds.left - mapsScreenWindowOffset.x,
                    bounds.top - mapsScreenWindowOffset.y
                ),
                size = Size(bounds.width, bounds.height)
            )
        }

        DemoTutorialOverlay(
            tutorialStep = tutorialStep,
            spotlightBounds = adjustedSpotlightBounds,
            onNextStep = {
                if (tutorialStep < 2) {
                    tutorialStep++
                } else {
                    showDemoTutorial = false
                }
            },
            onDismiss = { showDemoTutorial = false }
        )
    }
}
}

/**
 * Tutorial overlay that guides users through demo mode controls with spotlight effect.
 * Uses the same PathFillType.EvenOdd approach as the main GuidedTourOverlay
 */
@Composable
fun DemoTutorialOverlay(
    tutorialStep: Int,
    spotlightBounds: Rect?,
    onNextStep: () -> Unit,
    onDismiss: () -> Unit
) {
    val tutorialMessages = listOf(
        Triple("🎮 Demo Mode Ready!", "Your mock journey is set up! The blue marker shows your simulated location.", "Tap the green Play button to start"),
        Triple("⚡ Adjust Speed", "When moving, tap the speed panel at the bottom to adjust how fast you travel.", "Tap the speed control to expand"),
        Triple("🎯 Watch the Magic!", "As you approach the destination (green circle), the alarm will trigger automatically!", "Tap anywhere to close")
    )

    val (title, description, action) = tutorialMessages[tutorialStep]

    // Pulsing animation - same approach as GuidedTourOverlay
    var pulsePhase by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulsePhase = (pulsePhase + 0.05f) % (2f * 3.14159f)
            kotlinx.coroutines.delay(30)
        }
    }
    val pulseAlpha = (kotlin.math.sin(pulsePhase.toDouble()) * 0.3 + 0.7).toFloat()
    val pulseScale = (kotlin.math.sin(pulsePhase.toDouble()) * 0.02 + 1.02).toFloat()

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas for spotlight effect - using EvenOdd fill type like main tutorial
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val overlayPath = androidx.compose.ui.graphics.Path().apply {
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            }

            // Cut out spotlight if bounds are available
            spotlightBounds?.let { bounds ->
                val isCircular = tutorialStep == 0 // Circle for Play button
                val padding = 36f // ~12dp

                if (isCircular) {
                    val centerX = bounds.left + bounds.width / 2
                    val centerY = bounds.top + bounds.height / 2
                    val radius = (maxOf(bounds.width, bounds.height) / 2) + padding

                    overlayPath.addOval(
                        androidx.compose.ui.geometry.Rect(
                            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                            radius = radius
                        )
                    )

                    // Draw pulsing highlight border
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                        radius = radius * pulseScale,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
                    )
                } else {
                    val spotlightRect = androidx.compose.ui.geometry.RoundRect(
                        left = bounds.left - padding,
                        top = bounds.top - padding,
                        right = bounds.right + padding,
                        bottom = bounds.bottom + padding,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f)
                    )

                    overlayPath.addRoundRect(spotlightRect)

                    // Draw pulsing highlight border
                    val scaledPadding = padding * pulseScale
                    drawRoundRect(
                        color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                        topLeft = androidx.compose.ui.geometry.Offset(bounds.left - scaledPadding, bounds.top - scaledPadding),
                        size = androidx.compose.ui.geometry.Size(bounds.width + scaledPadding * 2, bounds.height + scaledPadding * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
                    )
                }
            }

            // Fill overlay with semi-transparent black
            drawPath(
                path = overlayPath,
                color = Color.Black.copy(alpha = 0.7f)
            )
        }

        // Dialog card - centered
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == tutorialStep) 12.dp else 8.dp)
                                .background(
                                    if (index == tutorialStep)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                        )
                    }
                }

                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "👆 $action",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Skip Tutorial")
                    }

                    Button(
                        onClick = onNextStep,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (tutorialStep < 2) "Next" else "Got it!")
                    }
                }
            }
        }
    }
}

/**
 * Quick Alarm setup dialog - streamlined for speed
 */
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
                    Toast.makeText(
                        context,
                        "Location not found. Try a different search term.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Search error: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Detect screen size for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
                                onSuggestionClick = { suggestion ->
                                    performQuickAlarmSearch(
                                        suggestion
                                    )
                                }
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
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
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
                                        onConfirm(
                                            currentLocation,
                                            currentLocationName,
                                            quickAlarmRadius
                                        )
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
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            ),
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
                                        onConfirm(
                                            currentLocation,
                                            currentLocationName,
                                            quickAlarmRadius
                                        )
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
