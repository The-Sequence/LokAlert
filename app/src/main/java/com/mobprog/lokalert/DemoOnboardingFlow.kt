package com.mobprog.lokalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

/**
 * Demo Onboarding Flow Steps
 */
enum class DemoOnboardingStep {
    WELCOME,
    SET_CURRENT_LOCATION,
    SET_DESTINATION,
    CONFIGURE_ALARM,
    START_SIMULATION
}

/**
 * Demo Onboarding Flow - Apple-style guided experience
 * Walks users through setting up demo mode step by step
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DemoOnboardingFlow(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    darkMode: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Demo Mode Manager
    val demoModeManager = remember { DemoModeManager.getInstance(context) }
    val appPreferences = remember { AppPreferences(context) }
    
    // Current step
    var currentStep by remember { mutableStateOf(DemoOnboardingStep.WELCOME) }
    
    // Location states
    var mockLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var mockLocationName by remember { mutableStateOf("") }
    var destinationName by remember { mutableStateOf("") }
    var destinationRadius by remember { mutableStateOf(150) }
    var movementSpeed by remember { mutableStateOf(5) }
    
    // Alarm customization
    var alarmSoundUri by remember { mutableStateOf("") }
    var enableVibration by remember { mutableStateOf(true) }
    var enableGradualVolume by remember { mutableStateOf(true) }
    
    // Alarm overlay customization
    var dismissStyle by remember { mutableStateOf(0) } // 0=Slider, 1=SwipeUp, 2=Button
    var backgroundStyle by remember { mutableStateOf(0) } // 0=Gradient, 1=Solid, 2=Dark
    var showDistance by remember { mutableStateOf(true) }
    var showEmoji by remember { mutableStateOf(true) }
    var overlayEmoji by remember { mutableStateOf("🚨") }
    var primaryColor by remember { mutableStateOf("FF6B6B") } // Hex color
    
    // Search states
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Map state for location picker
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    // Initialize with user's current location or default
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }
    
    // Get default alarm sound
    LaunchedEffect(Unit) {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        alarmSoundUri = defaultUri?.toString() ?: ""
    }
    
    // Ringtone picker
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let {
            alarmSoundUri = it.toString()
        }
    }
    
    // Apply settings and start demo when complete
    fun startDemo() {
        scope.launch {
            // Set all demo configurations
            mockLocation?.let { demoModeManager.setMockLocation(it) }
            destinationLocation?.let { demoModeManager.setDestination(it) }
            demoModeManager.setDestinationRadius(destinationRadius)
            demoModeManager.setSpeed(movementSpeed)
            
            // Save alarm sound preference if set
            if (alarmSoundUri.isNotEmpty()) {
                appPreferences.setDefaultAlarmSound(alarmSoundUri)
            }
            
            // Save alarm overlay customization settings
            appPreferences.setOverlayDismissStyle(dismissStyle)
            appPreferences.setOverlayBackgroundStyle(backgroundStyle)
            appPreferences.setOverlayShowDistance(showDistance)
            appPreferences.setOverlayShowEmoji(showEmoji)
            appPreferences.setOverlayEmoji(overlayEmoji)
            appPreferences.setOverlayPrimaryColor(primaryColor)
            
            // Enable demo mode
            demoModeManager.setDemoModeEnabled(true)
            
            onComplete()
        }
    }
    
    // Detect configuration changes for foldable devices
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val isCompactHeight = screenHeightDp < 600.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Force recomposition when configuration changes (fold/unfold)
    key(screenWidthDp, screenHeightDp) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "StepTransition"
                ) { step ->
                    when (step) {
                    DemoOnboardingStep.WELCOME -> WelcomeStep(
                        onNext = { currentStep = DemoOnboardingStep.SET_CURRENT_LOCATION },
                        onSkip = onDismiss
                    )
                    DemoOnboardingStep.SET_CURRENT_LOCATION -> LocationPickerStep(
                        title = "Where are you starting from?",
                        subtitle = "Set your mock starting location",
                        emoji = "📍",
                        color = Color(0xFF2196F3),
                        selectedLocation = mockLocation,
                        locationName = mockLocationName,
                        darkMode = darkMode,
                        userLocation = userLocation,
                        onLocationSelected = { location, name ->
                            mockLocation = location
                            mockLocationName = name
                        },
                        onNext = { currentStep = DemoOnboardingStep.SET_DESTINATION },
                        onBack = { currentStep = DemoOnboardingStep.WELCOME }
                    )
                    DemoOnboardingStep.SET_DESTINATION -> LocationPickerStep(
                        title = "Where are you going?",
                        subtitle = "Set your destination to trigger the alarm",
                        emoji = "🎯",
                        color = Color(0xFF4CAF50),
                        selectedLocation = destinationLocation,
                        locationName = destinationName,
                        darkMode = darkMode,
                        userLocation = mockLocation ?: userLocation,
                        showRadiusSlider = true,
                        radiusValue = destinationRadius,
                        onRadiusChange = { destinationRadius = it },
                        onLocationSelected = { location, name ->
                            destinationLocation = location
                            destinationName = name
                        },
                        onNext = { currentStep = DemoOnboardingStep.CONFIGURE_ALARM },
                        onBack = { currentStep = DemoOnboardingStep.SET_CURRENT_LOCATION }
                    )
                    DemoOnboardingStep.CONFIGURE_ALARM -> AlarmConfigStep(
                        alarmSoundUri = alarmSoundUri,
                        enableVibration = enableVibration,
                        enableGradualVolume = enableGradualVolume,
                        dismissStyle = dismissStyle,
                        backgroundStyle = backgroundStyle,
                        showDistance = showDistance,
                        showEmoji = showEmoji,
                        overlayEmoji = overlayEmoji,
                        primaryColor = primaryColor,
                        onAlarmSoundChange = { alarmSoundUri = it },
                        onVibrationChange = { enableVibration = it },
                        onGradualVolumeChange = { enableGradualVolume = it },
                        onDismissStyleChange = { dismissStyle = it },
                        onBackgroundStyleChange = { backgroundStyle = it },
                        onShowDistanceChange = { showDistance = it },
                        onShowEmojiChange = { showEmoji = it },
                        onEmojiChange = { overlayEmoji = it },
                        onPrimaryColorChange = { primaryColor = it },
                        onPickRingtone = {
                            val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                if (alarmSoundUri.isNotEmpty()) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmSoundUri))
                                }
                            }
                            ringtonePicker.launch(intent)
                        },
                        onNext = { currentStep = DemoOnboardingStep.START_SIMULATION },
                        onBack = { currentStep = DemoOnboardingStep.SET_DESTINATION }
                    )
                    DemoOnboardingStep.START_SIMULATION -> SimulationReadyStep(
                        mockLocation = mockLocation,
                        mockLocationName = mockLocationName,
                        destinationLocation = destinationLocation,
                        destinationName = destinationName,
                        destinationRadius = destinationRadius,
                        movementSpeed = movementSpeed,
                        onSpeedChange = { movementSpeed = it },
                        onRadiusChange = { destinationRadius = it },
                        onStartDemo = { startDemo() },
                        onBack = { currentStep = DemoOnboardingStep.CONFIGURE_ALARM }
                    )
                }
            }
        }
    }
    } // End key block
}

/**
 * Welcome Step - Introduction to Demo Mode
 */
@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Close button
        IconButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        
        // SCROLLABLE CONTENT - with bottom padding to clear FAB area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(top = 60.dp, bottom = 120.dp), // Bottom padding clears FAB
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated emoji
            val infiniteTransition = rememberInfiniteTransition(label = "bounce")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounce"
            )
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "🗺️",
                fontSize = 72.sp,
                modifier = Modifier.offset(y = offsetY.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Welcome to Demo Mode",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "Experience LokAlert like you're on a real journey. We'll guide you through setting up a simulated trip to see how location-based alarms work.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
            
            Spacer(Modifier.height(40.dp))
            
            // Feature highlights
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureHighlight(
                    emoji = "📍",
                    title = "Set Your Location",
                    description = "Choose where your journey starts"
                )
                FeatureHighlight(
                    emoji = "🎯",
                    title = "Pick a Destination",
                    description = "Select where you want to be alerted"
                )
                FeatureHighlight(
                    emoji = "⏰",
                    title = "Experience the Alarm",
                    description = "Watch as you approach and get notified"
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // More prominent "Maybe Later" button
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    "Maybe Later",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.height(16.dp))
        }
        
        // BUTTON - FIXED position, raised from bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // HARDCODED: Raised from absolute bottom
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Let's Get Started", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun FeatureHighlight(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(emoji, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Location Picker Step - Interactive map for selecting location
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerStep(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color,
    selectedLocation: LatLng?,
    locationName: String,
    darkMode: Int,
    userLocation: LatLng?,
    showRadiusSlider: Boolean = false,
    radiusValue: Int = 150,
    onRadiusChange: (Int) -> Unit = {},
    onLocationSelected: (LatLng, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    
    // Map state
    val initialPosition = selectedLocation ?: userLocation ?: LatLng(40.7580, -73.9855)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
    }
    
    // Map styling
    val isSystemDark = isSystemInDarkTheme()
    val effectiveMapDarkMode = when (darkMode) {
        3 -> if (isSystemDark) 1 else 0
        2 -> 1
        else -> darkMode
    }
    val mapProperties = remember(effectiveMapDarkMode) {
        MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = if (effectiveMapDarkMode > 0) {
                try {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                } catch (e: Exception) {
                    null
                }
            } else null
        )
    }
    
    // Places API for search
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
    
    // Geocode function
    fun geocodeLocation(latLng: LatLng, onResult: (String) -> Unit) {
        scope.launch {
            try {
                val geocoder = Geocoder(context)
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                }
                val name = results?.firstOrNull()?.let { addr ->
                    addr.thoroughfare ?: addr.featureName ?: addr.locality ?: addr.subAdminArea
                } ?: "Selected Location"
                onResult(name)
            } catch (e: Exception) {
                onResult("Selected Location")
            }
        }
    }
    
    // Search function
    fun searchLocation(query: String) {
        if (query.length < 3) {
            searchSuggestions = emptyList()
            return
        }
        
        placesClient?.let { client ->
            val token = AutocompleteSessionToken.newInstance()
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build()
            
            client.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    searchSuggestions = response.autocompletePredictions.map {
                        it.getPrimaryText(null).toString() to it.getSecondaryText(null).toString()
                    }
                }
                .addOnFailureListener {
                    searchSuggestions = emptyList()
                }
        }
    }
    
    // Search location by text
    fun selectSearchResult(primaryText: String, secondaryText: String) {
        scope.launch {
            try {
                val geocoder = Geocoder(context)
                val searchText = "$primaryText, $secondaryText"
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(searchText, 1)
                }
                results?.firstOrNull()?.let { address ->
                    val latLng = LatLng(address.latitude, address.longitude)
                    onLocationSelected(latLng, primaryText)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                        durationMs = 500
                    )
                    searchQuery = ""
                    searchSuggestions = emptyList()
                    isSearchExpanded = false
                    keyboardController?.hide()
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            ),
            onMapClick = { latLng ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                geocodeLocation(latLng) { name ->
                    onLocationSelected(latLng, name)
                }
            }
        ) {
            // Selected location marker
            selectedLocation?.let { loc ->
                val markerState = rememberMarkerState(key = "selected", position = loc)
                
                LaunchedEffect(loc) {
                    markerState.position = loc
                }
                
                Marker(
                    state = markerState,
                    title = locationName.ifEmpty { "Selected Location" },
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (color == Color(0xFF4CAF50)) BitmapDescriptorFactory.HUE_GREEN
                        else BitmapDescriptorFactory.HUE_AZURE
                    ),
                    draggable = true
                )
                
                // Handle drag
                LaunchedEffect(markerState.position) {
                    if (markerState.position != loc) {
                        geocodeLocation(markerState.position) { name ->
                            onLocationSelected(markerState.position, name)
                        }
                    }
                }
                
                // Radius circle for destination
                if (showRadiusSlider) {
                    Circle(
                        center = loc,
                        radius = radiusValue.toDouble(),
                        strokeColor = color,
                        fillColor = color.copy(alpha = 0.2f),
                        strokeWidth = 4f
                    )
                }
            }
        }
        
        // Top navigation bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                // Back button and title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Placeholder for alignment
                    Spacer(Modifier.width(48.dp))
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        searchLocation(it)
                        isSearchExpanded = it.isNotEmpty()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for a place...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchSuggestions = emptyList()
                                isSearchExpanded = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchSuggestions.isNotEmpty()) {
                                val first = searchSuggestions.first()
                                selectSearchResult(first.first, first.second)
                            }
                            keyboardController?.hide()
                        }
                    )
                )
                
                // Search suggestions
                AnimatedVisibility(visible = isSearchExpanded && searchSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            searchSuggestions.take(5).forEach { (primary, secondary) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectSearchResult(primary, secondary) },
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                primary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                secondary,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        
        // ==================== TWO-LAYER BOTTOM APPROACH ====================
        // LAYER 2: Info Panel - positioned ABOVE the Continue button
        // This contains location info, hint, drop pin, and radius slider
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp), // HARDCODED: Clears button area (80dp offset + 56dp button + padding)
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Selected location display
                if (selectedLocation != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Selected Location",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                locationName.ifEmpty { "Location selected" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Radius slider (for destination only)
                    if (showRadiusSlider) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Radius: ${radiusValue}m",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = radiusValue.toFloat(),
                                onValueChange = { onRadiusChange(it.toInt()) },
                                valueRange = 50f..500f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Hint when no location selected
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = color.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👆", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Tap on the map or search for a location",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Drop Pin button
                    Button(
                        onClick = {
                            val centerLatLng = cameraPositionState.position.target
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            geocodeLocation(centerLatLng) { name ->
                                onLocationSelected(centerLatLng, name)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Text("📍", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Drop Pin at Center", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        
        // LAYER 1: Continue Button - FIXED position, raised from bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // HARDCODED: Raised from absolute bottom
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedLocation != null,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        // ==================== END TWO-LAYER APPROACH ====================
        
        // Center crosshair hint
        if (selectedLocation == null) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                ) {}
                Text(emoji, fontSize = 36.sp)
            }
        }
    }
}

/**
 * Alarm Configuration Step
 */
@Composable
private fun AlarmConfigStep(
    alarmSoundUri: String,
    enableVibration: Boolean,
    enableGradualVolume: Boolean,
    dismissStyle: Int,
    backgroundStyle: Int,
    showDistance: Boolean,
    showEmoji: Boolean,
    overlayEmoji: String,
    primaryColor: String,
    onAlarmSoundChange: (String) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onGradualVolumeChange: (Boolean) -> Unit,
    onDismissStyleChange: (Int) -> Unit,
    onBackgroundStyleChange: (Int) -> Unit,
    onShowDistanceChange: (Boolean) -> Unit,
    onShowEmojiChange: (Boolean) -> Unit,
    onEmojiChange: (String) -> Unit,
    onPrimaryColorChange: (String) -> Unit,
    onPickRingtone: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Screen size detection for responsive UI (from SettingsScreen)
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isCompact = screenWidthDp < 600.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Responsive sizing based on screen size
    val titleFontSize = if (isCompact) 20.sp else 24.sp
    val sectionTitleFontSize = if (isCompact) 16.sp else 18.sp
    val optionFontSize = if (isCompact) 14.sp else 16.sp
    val subtitleFontSize = if (isCompact) 12.sp else 14.sp
    val iconSize = if (isCompact) 40.dp else 48.dp
    val cardPadding = if (isCompact) 14.dp else 18.dp
    val sectionSpacing = if (isCompact) 16.dp else 24.dp
    
    // Available colors for accent color picker
    val accentColors = listOf(
        "FF6B6B" to "Red",
        "4ECDC4" to "Teal",
        "45B7D1" to "Blue",
        "96CEB4" to "Mint",
        "FFEAA7" to "Yellow",
        "DDA0DD" to "Plum",
        "FF8C00" to "Orange",
        "9B59B6" to "Purple"
    )
    
    // Available emojis for alarm
    val emojiOptions = listOf("🚨", "🔔", "⏰", "🌟", "🎯", "📍", "🚀", "⚡")
    
    // Get ringtone name
    val ringtoneName = remember(alarmSoundUri) {
        if (alarmSoundUri.isEmpty()) "Default Alarm"
        else {
            try {
                val uri = Uri.parse(alarmSoundUri)
                RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Custom Sound"
            } catch (e: Exception) {
                "Custom Sound"
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // SCROLLABLE CONTENT with bottom padding to clear FAB
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp) // Extra padding to clear button area
        ) {
            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("⏰ Customize Your Alarm", fontWeight = FontWeight.Bold, fontSize = titleFontSize)
                    Text(
                        "How should we alert you?",
                        fontSize = subtitleFontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Content cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                // ALARM SOUND SECTION
                Text(
                    "Sound & Vibration",
                    fontSize = sectionTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Alarm Sound Card (matching SettingsScreen style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickRingtone() },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(iconSize)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("🔔", fontSize = if (isCompact) 22.sp else 26.sp)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Alarm Sound", fontWeight = FontWeight.SemiBold, fontSize = optionFontSize)
                                Text(
                                    ringtoneName,
                                    fontSize = subtitleFontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Vibration toggle (matching SettingsScreen)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(iconSize)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("📳", fontSize = if (isCompact) 22.sp else 26.sp)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Vibration", fontWeight = FontWeight.SemiBold, fontSize = optionFontSize)
                                Text(
                                    "Vibrate when alarm triggers",
                                    fontSize = subtitleFontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = enableVibration,
                            onCheckedChange = onVibrationChange
                        )
                    }
                }
                
                // Gradual volume toggle (matching SettingsScreen)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(iconSize)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("📈", fontSize = if (isCompact) 22.sp else 26.sp)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Gentle Wake-up", fontWeight = FontWeight.SemiBold, fontSize = optionFontSize)
                                Text(
                                    "Volume increases gradually",
                                    fontSize = subtitleFontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = enableGradualVolume,
                            onCheckedChange = onGradualVolumeChange
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // ALARM DISPLAY SECTION
                Text(
                    "Alarm Display",
                    fontSize = sectionTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Dismiss Style - Large square cards (SettingsScreen style)
                Text("How to dismiss", fontSize = optionFontSize, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Slider option
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onDismissStyleChange(0) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (dismissStyle == 0) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (dismissStyle == 0) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("👆", fontSize = if (isCompact) 32.sp else 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Slider", fontSize = optionFontSize, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    // Swipe Up option
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onDismissStyleChange(1) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (dismissStyle == 1) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (dismissStyle == 1) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("⬆️", fontSize = if (isCompact) 32.sp else 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Swipe Up", fontSize = optionFontSize, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    // Button option
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onDismissStyleChange(2) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (dismissStyle == 2) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (dismissStyle == 2) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🔘", fontSize = if (isCompact) 32.sp else 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Button", fontSize = optionFontSize, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                
                Spacer(Modifier.height(sectionSpacing))
                
                // Background Style - Larger FilterChips (SettingsScreen style)
                Text("Background style", fontSize = optionFontSize, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        0 to "🌈 Gradient",
                        1 to "⬛ Solid",
                        2 to "🌙 Dark"
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = backgroundStyle == style,
                            onClick = { onBackgroundStyleChange(style) },
                            label = { Text(label, fontSize = optionFontSize) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (backgroundStyle == style) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
                
                Spacer(Modifier.height(sectionSpacing))
                
                // Accent Color - Larger color swatches (SettingsScreen style)
                Text("Accent color", fontSize = optionFontSize, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    accentColors.forEach { (color, name) ->
                        val colorValue = Color(android.graphics.Color.parseColor("#$color"))
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 44.dp else 52.dp)
                                .clip(CircleShape)
                                .background(colorValue)
                                .border(
                                    width = if (primaryColor == color) 4.dp else 0.dp,
                                    color = if (primaryColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onPrimaryColorChange(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (primaryColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = sectionSpacing))
                
                // DISPLAY OPTIONS SECTION
                Text(
                    "Display Options",
                    fontSize = sectionTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Show Distance toggle (SettingsScreen style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show distance", fontSize = optionFontSize, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Display distance to destination",
                                fontSize = subtitleFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showDistance,
                            onCheckedChange = onShowDistanceChange
                        )
                    }
                }
                
                // Show Emoji toggle (SettingsScreen style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show emoji", fontSize = optionFontSize, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Display custom emoji on alarm screen",
                                fontSize = subtitleFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showEmoji,
                            onCheckedChange = onShowEmojiChange
                        )
                    }
                }
                
                // Emoji picker (only when showEmoji is true)
                if (showEmoji) {
                    Spacer(Modifier.height(12.dp))
                    Text("Alarm emoji", fontSize = optionFontSize, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojiOptions.forEach { emoji ->
                            Surface(
                                modifier = Modifier
                                    .size(if (isCompact) 50.dp else 56.dp)
                                    .clickable { onEmojiChange(emoji) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (overlayEmoji == emoji) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                border = if (overlayEmoji == emoji) {
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(emoji, fontSize = if (isCompact) 24.sp else 28.sp)
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
            }
        }
        
        // FAB - FIXED position at bottom, never pushed by content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // HARDCODED: Raised from absolute bottom
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * Simulation Ready Step - Final review with live map preview
 */
@SuppressLint("MissingPermission")
@Composable
private fun SimulationReadyStep(
    mockLocation: LatLng?,
    mockLocationName: String,
    destinationLocation: LatLng?,
    destinationName: String,
    destinationRadius: Int,
    movementSpeed: Int,
    onSpeedChange: (Int) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onStartDemo: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Animation state for speed preview
    var isPreviewAnimating by remember { mutableStateOf(false) }
    val animationProgress = remember { Animatable(0f) }
    
    // Animated position for the preview marker - use derivedStateOf to prevent recomposition flicker
    val animatedPosition by remember(mockLocation, destinationLocation) {
        derivedStateOf {
            if (mockLocation != null && destinationLocation != null) {
                val progress = animationProgress.value
                val lat = mockLocation.latitude + (destinationLocation.latitude - mockLocation.latitude) * progress
                val lng = mockLocation.longitude + (destinationLocation.longitude - mockLocation.longitude) * progress
                LatLng(lat, lng)
            } else {
                mockLocation
            }
        }
    }
    
    // Calculate estimated time based on speed and distance
    val estimatedTime = remember(mockLocation, destinationLocation, movementSpeed) {
        if (mockLocation != null && destinationLocation != null && movementSpeed > 0) {
            val earthRadius = 6371000.0 // meters
            val lat1Rad = Math.toRadians(mockLocation.latitude)
            val lat2Rad = Math.toRadians(destinationLocation.latitude)
            val deltaLat = Math.toRadians(destinationLocation.latitude - mockLocation.latitude)
            val deltaLng = Math.toRadians(destinationLocation.longitude - mockLocation.longitude)
            val a = kotlin.math.sin(deltaLat / 2).let { it * it } +
                    kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                    kotlin.math.sin(deltaLng / 2).let { it * it }
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            val distance = earthRadius * c
            val timeSeconds = distance / movementSpeed
            timeSeconds.toInt()
        } else 0
    }
    
    // Format time for display
    val formattedTime = remember(estimatedTime) {
        when {
            estimatedTime < 60 -> "${estimatedTime}s"
            estimatedTime < 3600 -> "${estimatedTime / 60}m ${estimatedTime % 60}s"
            else -> "${estimatedTime / 3600}h ${(estimatedTime % 3600) / 60}m"
        }
    }
    
    // Start/restart preview animation when speed changes
    fun startPreviewAnimation() {
        scope.launch {
            isPreviewAnimating = true
            animationProgress.snapTo(0f)
            // Calculate animation duration based on speed (faster speed = shorter animation)
            val durationMs = (5000 / (movementSpeed.coerceIn(1, 50) / 5f)).toLong().coerceIn(1000, 10000)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMs.toInt(), easing = LinearEasing)
            )
            animationProgress.snapTo(0f)
            isPreviewAnimating = false
        }
    }
    
    // Map camera state - fit both locations
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mockLocation ?: LatLng(14.7012, 121.0764),
            14f
        )
    }
    
    // Fit both locations in view when they're available
    LaunchedEffect(mockLocation, destinationLocation) {
        if (mockLocation != null && destinationLocation != null) {
            try {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(mockLocation)
                    .include(destinationLocation)
                    .build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 80),
                    durationMs = 500
                )
            } catch (e: Exception) {
                // Fallback to mock location
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // SCROLLABLE CONTENT with bottom padding to clear FAB
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp) // Clears FAB area
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("🚀 Ready to Launch!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Preview your demo journey",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Live Map Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            tiltGesturesEnabled = false
                        )
                    ) {
                        // Start marker (blue)
                        mockLocation?.let { start ->
                            Marker(
                                state = rememberMarkerState(position = start),
                                title = "Start",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                        
                        // Destination marker (green) with radius
                        destinationLocation?.let { dest ->
                            Marker(
                                state = rememberMarkerState(position = dest),
                                title = "Destination",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                            
                            // Destination radius circle
                            Circle(
                                center = dest,
                                radius = destinationRadius.toDouble(),
                                strokeColor = Color(0xFF4CAF50),
                                fillColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                strokeWidth = 3f
                            )
                        }
                        
                        // Animated preview marker (cyan) - shows movement preview
                        if (isPreviewAnimating) {
                            animatedPosition?.let { position ->
                                val markerState = rememberMarkerState(position = position)
                                
                                LaunchedEffect(position) {
                                    markerState.position = position
                                }
                                
                                Marker(
                                    state = markerState,
                                    title = "Preview",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                                    zIndex = 100f
                                )
                            }
                        }
                        
                        // Path line
                        if (mockLocation != null && destinationLocation != null) {
                            Polyline(
                                points = listOf(mockLocation, destinationLocation),
                                color = Color(0xFF2196F3).copy(alpha = 0.7f),
                                width = 8f
                            )
                        }
                    }
                    
                    // Map overlay label
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Text(
                            "📍 Route Preview",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Scrollable settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Journey summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Journey Details", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("From", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        mockLocationName.ifEmpty { "Start" },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )
                                }
                            }
                            
                            Text("→", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("To", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        destinationName.ifEmpty { "Destination" },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("🎯", fontSize = 16.sp)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Radius control with visual feedback
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯 Alert Radius", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "${destinationRadius}m",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Radius slider - interactive!
                        Slider(
                            value = destinationRadius.toFloat(),
                            onValueChange = { onRadiusChange(it.toInt()) },
                            valueRange = 50f..500f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50)
                            )
                        )
                        
                        Text(
                            "Adjust the green circle on the map",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Speed control
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("⚡ Travel Speed", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    when {
                                        movementSpeed <= 2 -> "🚶 Walking slowly"
                                        movementSpeed <= 5 -> "🚶 Walking"
                                        movementSpeed <= 10 -> "🚴 Cycling"
                                        movementSpeed <= 20 -> "🚗 Driving in city"
                                        movementSpeed <= 35 -> "🚗 Driving on highway"
                                        else -> "🚀 High speed"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "$movementSpeed m/s",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Slider(
                            value = movementSpeed.toFloat(),
                            onValueChange = { onSpeedChange(it.toInt()) },
                            valueRange = 1f..50f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Estimated time and preview button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (estimatedTime > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⏱️", fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Est. time: $formattedTime",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            
                            TextButton(
                                onClick = { startPreviewAnimation() },
                                enabled = !isPreviewAnimating && mockLocation != null && destinationLocation != null
                            ) {
                                Text(
                                    if (isPreviewAnimating) "Previewing..." else "▶ Preview Speed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Watch the blue marker move towards the destination. When it enters the green circle, the alarm will trigger!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
        
        // BUTTON - FIXED position, raised from bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // HARDCODED: Raised from absolute bottom
                .padding(horizontal = 20.dp)
        ) {
            Button(
                onClick = onStartDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("▶", fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                Text("Start Demo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
