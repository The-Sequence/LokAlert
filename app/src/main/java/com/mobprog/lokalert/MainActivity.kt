package com.mobprog.lokalert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.Places
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Places API
        if (!Places.isInitialized()) {
            val apiKey = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            ).metaData?.getString("com.google.android.geo.API_KEY")
            if (apiKey != null) {
                Places.initialize(applicationContext, apiKey)
            }
        }

        setContent {
            LokAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LokAlertAppEntryPoint()
                }
            }
        }
    }
}

@Composable
fun LokAlertAppEntryPoint() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { Onboarding(context) }
    val isOnboardingCompleted by userPreferences.isOnboardingCompleted.collectAsState(initial = null)
    
    // Check location permissions - use mutableStateOf to make it reactive
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
    
    // Continuously check for permission changes while in onboarding
    LaunchedEffect(isOnboardingCompleted) {
        if (isOnboardingCompleted == false) {
            // Keep checking permissions every 500ms while in onboarding
            while (true) {
                kotlinx.coroutines.delay(500)
                val currentPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (currentPermission != hasLocationPermission) {
                    hasLocationPermission = currentPermission
                }
            }
        }
    }

    when {
        isOnboardingCompleted == null -> {
            // Loading state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        isOnboardingCompleted == false || !hasLocationPermission -> {
            // Show onboarding if not completed OR if permissions not granted
            OnboardingScreen(
                onFinished = {
                    scope.launch { 
                        userPreferences.saveOnboardingCompleted()
                        // Recheck permissions immediately after onboarding
                        hasLocationPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                }
            )
        }
        else -> {
            // Onboarding completed AND permissions granted
            val context = LocalContext.current
            
            // Start in-app location tracking for proximity alarms
            LaunchedEffect(Unit) {
                startLocationTracking(context)
            }
            
            LokAlertApp()
        }
    }
}

private suspend fun startLocationTracking(context: Context) {
    val database = LokAlertDatabase.getDatabase(context)
    val fusedLocationClient = com.google.android.gms.location.LocationServices
        .getFusedLocationProviderClient(context)
    
    // Track which alarms have been triggered recently (cooldown)
    val triggeredAlarms = mutableMapOf<Int, Long>()
    val cooldownPeriod = 5 * 60 * 1000L // 5 minutes cooldown
    
    // Check location every 10 seconds
    while (true) {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let { currentLoc ->
                        // Check proximity to all enabled alarms
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val alarms = database.alarmDao().getAllAlarms().first()
                            val currentDay = java.util.Calendar.getInstance()
                                .get(java.util.Calendar.DAY_OF_WEEK)
                            val currentTime = System.currentTimeMillis()
                            
                            alarms.filter { it.isEnabled }.forEach { alarm ->
                                // Check if alarm is active for today
                                if (alarm.activeDays.isEmpty() || alarm.activeDays.contains(currentDay)) {
                                    // Check cooldown
                                    val lastTriggered = triggeredAlarms[alarm.id] ?: 0L
                                    if (currentTime - lastTriggered > cooldownPeriod) {
                                        val alarmLocation = android.location.Location("").apply {
                                            latitude = alarm.latitude
                                            longitude = alarm.longitude
                                        }
                                        val userLocation = android.location.Location("").apply {
                                            latitude = currentLoc.latitude
                                            longitude = currentLoc.longitude
                                        }
                                        
                                        val distance = userLocation.distanceTo(alarmLocation)
                                        
                                        // If within radius, trigger alarm
                                        if (distance <= alarm.radius) {
                                            triggeredAlarms[alarm.id] = currentTime
                                            kotlinx.coroutines.CoroutineScope(
                                                kotlinx.coroutines.Dispatchers.Main
                                            ).launch {
                                                triggerLocationAlarm(context, alarm, distance)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Wait 10 seconds before next check
        kotlinx.coroutines.delay(10000)
    }
}

private fun triggerLocationAlarm(context: Context, alarm: LocationAlarm, distance: Float) {
    try {
        // Launch full-screen alarm overlay
        val intent = android.content.Intent(context, AlarmOverlayActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_NAME", alarm.name)
            putExtra("SOUND_URI", alarm.soundUri)
            putExtra("IS_GRADUAL_VOLUME", alarm.isGradualVolume)
            putExtra("LATITUDE", alarm.latitude)
            putExtra("LONGITUDE", alarm.longitude)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun LokAlertApp() {
    var currentScreen by remember { mutableStateOf("Maps") }
    var titleColor by remember { mutableStateOf(Color(0xFF006DFF)) }
    var isRainbowEffectEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val mapsViewModel: MapsViewModel = viewModel()

    // State for Search History
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }
    
    // Guided Tour State
    var showGuidedTour by remember { mutableStateOf(false) }
    var tourStep by remember { mutableIntStateOf(0) }
    
    // UI Element positions for spotlight
    var topBarBounds by remember { mutableStateOf<Rect?>(null) }
    var bottomNavBounds by remember { mutableStateOf<Rect?>(null) }
    var helpIconBounds by remember { mutableStateOf<Rect?>(null) }
    var settingsIconBounds by remember { mutableStateOf<Rect?>(null) }
    var mapTabBounds by remember { mutableStateOf<Rect?>(null) }
    var locationsTabBounds by remember { mutableStateOf<Rect?>(null) }
    var searchBarBounds by remember { mutableStateOf<Rect?>(null) }
    var mapAreaBounds by remember { mutableStateOf<Rect?>(null) }
    var setPinButtonBounds by remember { mutableStateOf<Rect?>(null) }
    
    // Tour-controlled screen (overrides user selection during tour)
    val effectiveScreen = if (showGuidedTour) {
        when (tourStep) {
            0, 1 -> currentScreen // Step 0-1: Stay on current screen
            2, 3, 4 -> "Maps" // Step 2-4: Maps screen
            5, 6 -> "Locations" // Step 5-6: Locations screen
            else -> currentScreen
        }
    } else {
        currentScreen
    }

    // --- Animation Logic ---
    val animatedTitleColor = remember {
        Animatable(
            titleColor,
            TwoWayConverter(
                convertToVector = { color: Color ->
                    AnimationVector4D(color.red, color.green, color.blue, color.alpha)
                },
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

    BackHandler(enabled = currentScreen == "Settings") {
        currentScreen = "Maps"
    }

    // --- Helper Functions ---
    fun addRecentSearch(location: String) {
        val MAX_HISTORY = 5
        if (!recentSearches.contains(location)) {
            recentSearches = listOf(location) + recentSearches
            if (recentSearches.size > MAX_HISTORY) {
                recentSearches = recentSearches.take(MAX_HISTORY)
            }
        }
    }


    // --- Main Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { 
                TopBar(
                    color = animatedTitleColor.value,
                    onSettingsClick = { if (!showGuidedTour) currentScreen = "Settings" },
                    onHelpClick = { showGuidedTour = true; tourStep = 0 },
                    onHelpIconPositioned = { helpIconBounds = it },
                    onSettingsIconPositioned = { settingsIconBounds = it },
                    onTopBarPositioned = { topBarBounds = it },
                    showBackButton = effectiveScreen == "Settings",
                    onBackClick = { currentScreen = "Maps" }
                ) 
            },
            bottomBar = {
                if (effectiveScreen != "Settings") {
                    BottomNavBar(
                        currentScreen = effectiveScreen,
                        onScreenSelected = { if (!showGuidedTour) currentScreen = it },
                        onBottomNavPositioned = { bottomNavBounds = it },
                        onMapTabPositioned = { mapTabBounds = it },
                        onLocationsTabPositioned = { locationsTabBounds = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (effectiveScreen) {
                    "Maps" -> MapsScreen(
                        onNewSearch = { query -> addRecentSearch(query) },
                        onDone = { 
                            // Smooth transition with slight delay for visual feedback
                            scope.launch {
                                kotlinx.coroutines.delay(300) // Allow sheet close animation to complete
                                currentScreen = "Locations"
                            }
                        },
                        viewModel = mapsViewModel
                    )
                    "Locations" -> LocationsScreen(
                        recentSearches = recentSearches,
                        viewModel = mapsViewModel,
                        onViewOnMap = {
                            // Switch to Map screen when "View on Map" is clicked in the sheet
                            if (!showGuidedTour) currentScreen = "Maps"
                        }
                    )
                    "Settings" -> SettingsScreen(
                        onColorChange = { titleColor = it },
                        isRainbowEnabled = isRainbowEffectEnabled,
                        onRainbowToggle = { isRainbowEffectEnabled = it }
                    )
                }
            }
        }
        
        // Guided Tour Overlay
        if (showGuidedTour) {
            GuidedTourOverlay(
                step = tourStep,
                helpIconBounds = helpIconBounds,
                settingsIconBounds = settingsIconBounds,
                topBarBounds = topBarBounds,
                bottomNavBounds = bottomNavBounds,
                mapTabBounds = mapTabBounds,
                locationsTabBounds = locationsTabBounds,
                searchBarBounds = searchBarBounds,
                mapAreaBounds = mapAreaBounds,
                setPinButtonBounds = setPinButtonBounds,
                onNext = { tourStep++ },
                onBack = { tourStep-- },
                onFinish = { showGuidedTour = false; tourStep = 0 }
            )
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

// Helper to disable touch pass-through (if needed for overlays)
fun Modifier.bypassing(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(pass = PointerEventPass.Initial)
        }
    }
}

@Composable
fun TopBar(
    color: Color, 
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    onHelpIconPositioned: (Rect) -> Unit = {},
    onSettingsIconPositioned: (Rect) -> Unit = {},
    onTopBarPositioned: (Rect) -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                onTopBarPositioned(
                    Rect(
                        offset = Offset(position.x, position.y),
                        size = Size(size.width.toFloat(), size.height.toFloat())
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Left Side Icons (Back button OR Help icon)
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showBackButton) {
                // Back Button
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                // Help Icon (only show when back button is not showing)
                IconButton(
                    onClick = { 
                        showHelpDialog = true
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        val size = coordinates.size
                        onHelpIconPositioned(
                            Rect(
                                offset = Offset(position.x, position.y),
                                size = Size(size.width.toFloat(), size.height.toFloat())
                            )
                        )
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        
        // App Title (Center)
        Text(
            text = "LokAlert",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        // Settings Icon (Aligned Right)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInWindow()
                    val size = coordinates.size
                    onSettingsIconPositioned(
                        Rect(
                            offset = Offset(position.x, position.y),
                            size = Size(size.width.toFloat(), size.height.toFloat())
                        )
                    )
                }
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
    
    // Interactive Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false },
            onStartTour = { 
                showHelpDialog = false
                onHelpClick()
            }
        )
    }
}

@Composable
fun SettingsScreen(
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit
) {

    var showColorOptions by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(3) } // Default to "Mono"
    val colorOptions = mapOf(
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Mono" to if (isSystemInDarkTheme()) Color.White else Color.Black
    )
    val colorOptionKeys = colorOptions.keys.toList()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rainbow Title")
            Switch(checked = isRainbowEnabled, onCheckedChange = onRainbowToggle)
        }

        HorizontalDivider()

        Column(modifier = Modifier.clickable(enabled = !isRainbowEnabled) {
            showColorOptions = !showColorOptions
        }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SingleChoiceSegmentedButtonRow {
                        colorOptionKeys.forEachIndexed { index, name ->
                            val colorValue = colorOptions[name]!!
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = colorOptionKeys.size),
                                onClick = {
                                    selectedColorIndex = index
                                    onColorChange(colorValue)
                                },
                                selected = index == selectedColorIndex
                            ) {
                                Text(name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentScreen: String, 
    onScreenSelected: (String) -> Unit,
    onBottomNavPositioned: (Rect) -> Unit = {},
    onMapTabPositioned: (Rect) -> Unit = {},
    onLocationsTabPositioned: (Rect) -> Unit = {}
) {
    NavigationBar(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            val size = coordinates.size
            onBottomNavPositioned(
                Rect(
                    offset = Offset(position.x, position.y),
                    size = Size(size.width.toFloat(), size.height.toFloat())
                )
            )
        }
    ) {
        NavigationBarItem(
            selected = currentScreen == "Maps",
            onClick = { onScreenSelected("Maps") },
            icon = { Icon(Icons.Default.Map, contentDescription = null) },
            label = { Text("Map") },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                onMapTabPositioned(
                    Rect(
                        offset = Offset(position.x, position.y),
                        size = Size(size.width.toFloat(), size.height.toFloat())
                    )
                )
            }
        )
        NavigationBarItem(
            selected = currentScreen == "Locations",
            onClick = { onScreenSelected("Locations") },
            icon = { Icon(Icons.Default.Place, contentDescription = null) },
            label = { Text("Locations") },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                onLocationsTabPositioned(
                    Rect(
                        offset = Offset(position.x, position.y),
                        size = Size(size.width.toFloat(), size.height.toFloat())
                    )
                )
            }
        )
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit, onStartTour: () -> Unit = {}) {
    // Show Help Options
    AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Welcome to LokAlert",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Never miss your stop or forget location-based tasks!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HelpOption(
                                emoji = "🗺️",
                                title = "Map Screen",
                                description = "Search, pin, and set location alarms"
                            )
                            HelpOption(
                                emoji = "📍",
                                title = "Locations",
                                description = "Manage saved alarms and favorites"
                            )
                            HelpOption(
                                emoji = "⚙️",
                                title = "Settings",
                                description = "Customize app appearance"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onStartTour) {
                    Text("Start Interactive Tour")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Got it!")
                }
            }
        )
}

@Composable
fun HelpOption(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GuidedTourDialog(onClose: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    val tourSteps = listOf(
        TourStep(
            emoji = "🗺️",
            title = "Map Screen",
            description = "This is where you set up location-based alarms.",
            details = listOf(
                "Search for any location using the search bar",
                "Long-press on the map to drop a pin",
                "Drag the pin to fine-tune the position",
                "Tap 'My Location' to jump to your current position"
            )
        ),
        TourStep(
            emoji = "📍",
            title = "Setting an Alarm",
            description = "Configure your location alarm:",
            details = listOf(
                "Tap 'Set Pin' or 'Edit Pin' button",
                "Name your alarm (e.g., 'Office', 'Home')",
                "Choose active days of the week",
                "Pick your alarm sound",
                "Adjust the detection radius with the slider"
            )
        ),
        TourStep(
            emoji = "📋",
            title = "Locations Screen",
            description = "Manage all your saved alarms:",
            details = listOf(
                "View recent searches",
                "Toggle alarms on/off",
                "Favorite important locations with ❤️",
                "View on map or delete alarms",
                "Filter to show only favorites"
            )
        ),
        TourStep(
            emoji = "💡",
            title = "Pro Tips",
            description = "Get the most out of LokAlert:",
            details = listOf(
                "The circle on the map shows your alarm radius",
                "Gradual volume starts quiet and increases",
                "Long-press anywhere to quickly set a pin",
                "Drag the pin to adjust exact location",
                "Enable location permissions for best results"
            )
        )
    )
    
    AlertDialog(
        onDismissRequest = onClose,
        icon = {
            Text(
                text = tourSteps[currentStep].emoji,
                fontSize = 40.sp
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tourSteps[currentStep].title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step ${currentStep + 1} of ${tourSteps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tourSteps[currentStep].description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tourSteps[currentStep].details.forEach { detail ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep < tourSteps.size - 1) {
                Button(onClick = { currentStep++ }) {
                    Text("Next")
                }
            } else {
                Button(onClick = onClose) {
                    Text("Finish")
                }
            }
        },
        dismissButton = {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Back")
                }
            } else {
                TextButton(onClick = onClose) {
                    Text("Skip")
                }
            }
        }
    )
}

data class TourStep(
    val emoji: String,
    val title: String,
    val description: String,
    val details: List<String>
)

@Composable
fun GuidedTourOverlay(
    step: Int,
    helpIconBounds: Rect?,
    settingsIconBounds: Rect?,
    topBarBounds: Rect?,
    bottomNavBounds: Rect?,
    mapTabBounds: Rect?,
    locationsTabBounds: Rect?,
    searchBarBounds: Rect?,
    mapAreaBounds: Rect?,
    setPinButtonBounds: Rect?,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    // Define comprehensive tour steps
    val tourSteps = listOf(
        // Step 0: Welcome & Icons
        InteractiveTourStep(
            title = "Welcome to LokAlert! 👋",
            message = "Let's take a quick tour! These are your Help and Settings icons - tap Help anytime for guidance!",
            spotlightBounds = helpIconBounds,
            secondarySpotlightBounds = settingsIconBounds,
            bubblePosition = BubblePosition.BELOW,
            useCircularSpotlight = true
        ),
        // Step 1: Navigation Bar
        InteractiveTourStep(
            title = "Navigation Bar",
            message = "Use these tabs to switch between Map and Locations screens. Let's explore the Map!",
            spotlightBounds = bottomNavBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = false
        ),
        // Step 2: Map Tab
        InteractiveTourStep(
            title = "Map Screen",
            message = "This is the Map tab where you'll set up your location-based alarms.",
            spotlightBounds = mapTabBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = false
        ),
        // Step 3: Search Bar
        InteractiveTourStep(
            title = "Search for Locations",
            message = "Use the search bar to find any location, or long-press on the map to drop a pin!",
            spotlightBounds = searchBarBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.BELOW,
            useCircularSpotlight = false
        ),
        // Step 4: Map Area
        InteractiveTourStep(
            title = "Interactive Map",
            message = "Long-press to pin a location, or drag the pin to adjust it. The circle shows your alarm radius!",
            spotlightBounds = mapAreaBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.CENTER,
            useCircularSpotlight = false
        ),
        // Step 5: Locations Tab
        InteractiveTourStep(
            title = "Locations Screen",
            message = "Tap Locations to manage saved alarms, view recent searches, and mark favorites!",
            spotlightBounds = locationsTabBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = false
        ),
        // Step 6: Completion
        InteractiveTourStep(
            title = "You're All Set! 🎉",
            message = "You're ready to create location-based alarms! Never miss your stop again!",
            spotlightBounds = null,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.CENTER,
            useCircularSpotlight = false
        )
    )
    
    val currentTourStep = tourSteps.getOrNull(step) ?: return
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Block all touches except the overlay
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
    ) {
        // Dark overlay with spotlight cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Create path with EvenOdd fill for proper spotlight cutouts
            val overlayPath = Path().apply {
                fillType = PathFillType.EvenOdd // CRITICAL: This makes the cutouts transparent!
                // Fill entire screen
                addRect(Rect(Offset.Zero, size))
            }
            
            // Cut out primary spotlight if there's a target
            currentTourStep.spotlightBounds?.let { bounds ->
                if (currentTourStep.useCircularSpotlight) {
                    // Use circular spotlight for icons - focus on icon only, not button
                    val centerX = bounds.left + (bounds.right - bounds.left) / 2
                    val centerY = bounds.top + (bounds.bottom - bounds.top) / 2
                    // Smaller radius - just 8dp padding around the icon itself
                    val radius = maxOf(bounds.width, bounds.height) / 2 + 8.dp.toPx()
                    
                    overlayPath.addOval(
                        Rect(
                            center = Offset(centerX, centerY),
                            radius = radius
                        )
                    )
                } else {
                    // Use rectangular spotlight for larger elements
                    val padding = 20.dp.toPx()
                    val spotlightRect = RoundRect(
                        left = bounds.left - padding,
                        top = bounds.top - padding,
                        right = bounds.right + padding,
                        bottom = bounds.bottom + padding,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                    
                    overlayPath.addRoundRect(spotlightRect)
                }
            }
            
            // Cut out secondary spotlight (for Settings icon when showing Help icon)
            currentTourStep.secondarySpotlightBounds?.let { bounds ->
                if (currentTourStep.useCircularSpotlight) {
                    // Use circular spotlight for icons
                    val centerX = bounds.left + (bounds.right - bounds.left) / 2
                    val centerY = bounds.top + (bounds.bottom - bounds.top) / 2
                    val radius = maxOf(bounds.width, bounds.height) / 2 + 8.dp.toPx()
                    
                    overlayPath.addOval(
                        Rect(
                            center = Offset(centerX, centerY),
                            radius = radius
                        )
                    )
                }
            }
            
            // Draw with reduced opacity for proper cutouts
            drawPath(
                path = overlayPath,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }
        
        // Speech bubble overlay
        SpeechBubble(
            title = currentTourStep.title,
            message = currentTourStep.message,
            position = currentTourStep.bubblePosition,
            spotlightBounds = currentTourStep.spotlightBounds,
            step = step,
            totalSteps = tourSteps.size,
            onNext = if (step < tourSteps.size - 1) onNext else null,
            onBack = if (step > 0) onBack else null,
            onFinish = if (step == tourSteps.size - 1) onFinish else null,
            onSkip = onFinish
        )
    }
}

enum class BubblePosition {
    ABOVE, BELOW, CENTER
}

data class InteractiveTourStep(
    val title: String,
    val message: String,
    val spotlightBounds: Rect?,
    val secondarySpotlightBounds: Rect? = null, // For highlighting multiple elements
    val bubblePosition: BubblePosition,
    val useCircularSpotlight: Boolean = false // Use circular spotlight for icons
)

@Composable
fun BoxScope.SpeechBubble(
    title: String,
    message: String,
    position: BubblePosition,
    spotlightBounds: Rect?,
    step: Int,
    totalSteps: Int,
    onNext: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onFinish: (() -> Unit)?,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    
    // Calculate bubble position based on spotlight - make it narrower and more square
    val modifier = when (position) {
        BubblePosition.ABOVE -> {
            spotlightBounds?.let {
                val offsetY = with(density) { (it.top - 220.dp.toPx()).toInt() }
                Modifier
                    .width(280.dp)  // Fixed width for square shape
                    .offset { IntOffset(0, offsetY) }
            } ?: Modifier
                .width(280.dp)
                .align(Alignment.TopCenter)
        }
        BubblePosition.BELOW -> {
            spotlightBounds?.let {
                val offsetY = with(density) { (it.bottom + 20.dp.toPx()).toInt() }
                Modifier
                    .width(280.dp)  // Fixed width for square shape
                    .offset { IntOffset(0, offsetY) }
            } ?: Modifier
                .width(280.dp)
                .align(Alignment.BottomCenter)
        }
        BubblePosition.CENTER -> {
            Modifier
                .width(280.dp)  // Fixed width for square shape
                .align(Alignment.Center)
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),  // Reduced from 24dp to 20dp
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)  // Reduced from 16dp to 12dp
        ) {
            // Progress indicator
            Text(
                text = "Step ${step + 1} of $totalSteps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back or Skip button
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text("← Back")
                    }
                } else {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
                
                // Next or Finish button
                if (onFinish != null) {
                    Button(onClick = onFinish) {
                        Text("Finish 🎉")
                    }
                } else if (onNext != null) {
                    Button(onClick = onNext) {
                        Text("Next →")
                    }
                }
            }
        }
    }
}