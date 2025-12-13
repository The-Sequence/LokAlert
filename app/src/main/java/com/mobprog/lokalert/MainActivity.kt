package com.mobprog.lokalert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.Places
import com.mobprog.lokalert.ui.theme.LokAlertTheme
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
            val appPreferences = remember { AppPreferences(applicationContext) }
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val savedDarkMode by appPreferences.darkMode.collectAsState(initial = null)
            
            // Use Auto (3) on first launch which follows system theme
            val darkMode = savedDarkMode ?: 3
            
            // Save the initial theme as Auto if not set
            LaunchedEffect(savedDarkMode) {
                if (savedDarkMode == null) {
                    appPreferences.setDarkMode(3) // Auto mode
                }
            }
            
            LokAlertTheme(darkMode = darkMode) {
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
    
    // State to track if we should preload the main app
    var shouldPreloadMainApp by remember { mutableStateOf(false) }
    
    // Track if reveal animation completed - removes onboarding overlay
    var revealCompleted by remember { mutableStateOf(false) }
    
    // Control when tour prompt should show (after reveal completes)
    var enableTourPrompt by remember { mutableStateOf(false) }

    when {
        isOnboardingCompleted == null -> {
            // Loading state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        revealCompleted -> {
            // After reveal: Show the app directly (same instance that was preloaded)
            LaunchedEffect(Unit) {
                LocationTrackingService.startService(context)
            }
            LokAlertApp(enableTourPromptImmediately = true)
        }
        isOnboardingCompleted == false || !hasLocationPermission -> {
            // During onboarding: Show OnboardingScreen which handles preloading in backgroundContent
            OnboardingScreen(
                onFinished = {
                    scope.launch { 
                        userPreferences.saveOnboardingCompleted()
                        hasLocationPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        
                        // Mark reveal as completed - switches to showing app directly
                        revealCompleted = true
                        enableTourPrompt = true
                    }
                },
                onPreloadMap = {
                    shouldPreloadMainApp = true
                },
                backgroundContent = {
                    // Preload the app INSIDE CelebrationScreen's backgroundContent
                    // This is only visible through the circular reveal
                    if (shouldPreloadMainApp) {
                        LaunchedEffect(Unit) {
                            LocationTrackingService.startService(context)
                        }
                        LokAlertApp(enableTourPromptImmediately = false)
                    }
                }
            )
        }
        else -> {
            // Fresh launch with onboarding already completed
            LaunchedEffect(Unit) {
                LocationTrackingService.startService(context)
            }
            LokAlertApp()
        }
    }
}

@Composable
fun LokAlertApp(enableTourPromptImmediately: Boolean = true) {
    var currentScreen by remember { mutableStateOf("Maps") }
    var titleColor by remember { mutableStateOf(Color(0xFF006DFF)) }
    var isRainbowEffectEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val mapsViewModel: MapsViewModel = viewModel()

    // State for Search History
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }
    
    // Guided Tour State
    var showGuidedTour by remember { mutableStateOf(false) }
    var tourStep by remember { mutableIntStateOf(0) }
    
    // Interactive tour action tracking
    var tourMapInteracted by remember { mutableStateOf(false) }
    var tourSearchInteracted by remember { mutableStateOf(false) }
    var tourAlarmCreated by remember { mutableStateOf(false) }
    var allowMapInteraction by remember { mutableStateOf(false) }
    var allowSearchInteraction by remember { mutableStateOf(false) }
    
    // Tour Prompt and Help Icon Spotlight State
    val userPreferences = remember { Onboarding(context) }
    val tourPromptShown by userPreferences.isTourPromptShown.collectAsState(initial = null)
    val helpIconSpotlightShown by userPreferences.isHelpIconSpotlightShown.collectAsState(initial = null)
    var showTourPrompt by remember { mutableStateOf(false) }
    var showHelpIconSpotlight by remember { mutableStateOf(false) }
    
    // Get dark mode preference
    val appPreferences = remember { AppPreferences(context) }
    val darkMode by appPreferences.darkMode.collectAsState(initial = 0)
    
    // Get offline mode state
    val isOfflineModeEnabled by appPreferences.offlineModeEnabled.collectAsState(initial = false)
    
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
    var quickAlarmButtonBounds by remember { mutableStateOf<Rect?>(null) }
    
    // Show tour prompt after onboarding if not shown yet
    // BUT only if enableTourPromptImmediately is true
    LaunchedEffect(tourPromptShown, enableTourPromptImmediately) {
        if (tourPromptShown == false && enableTourPromptImmediately) {
            showTourPrompt = true
        }
    }

    // Tour-controlled screen (overrides user selection during tour)
    // Most steps should stay on current screen - only force screen for specific viewing steps
    val effectiveScreen = if (showGuidedTour) {
        when (tourStep) {
            0 -> currentScreen // Welcome - stay on current screen
            1 -> currentScreen // User needs to TAP Alarms tab - stay on current screen
            2 -> currentScreen // User needs to TAP Map tab - stay on current screen (should be on Locations)
            3, 4, 5, 6 -> "Maps" // Interactive Map, Search Bar, Set Pin, Quick Alarm - force Maps screen
            7 -> currentScreen // Final step - stay on current screen
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
                    onBackClick = { currentScreen = "Maps" },
                    isOfflineMode = isOfflineModeEnabled
                ) 
            },
            bottomBar = {
                if (effectiveScreen != "Settings") {
                    BottomNavBar(
                        currentScreen = effectiveScreen,
                        onScreenSelected = { screen ->
                            // During tour, track navigation and allow it
                            if (showGuidedTour) {
                                currentScreen = screen
                            } else {
                                currentScreen = screen
                            }
                        },
                        onBottomNavPositioned = { bottomNavBounds = it },
                        onMapTabPositioned = { mapTabBounds = it },
                        onLocationsTabPositioned = { locationsTabBounds = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                // Use smooth animated screen transitions
                AnimatedScreenTransition(targetState = effectiveScreen) { screen ->
                    when (screen) {
                        "Maps" -> MapsScreen(
                            onNewSearch = { query -> addRecentSearch(query) },
                            onDone = { 
                                // Smooth transition with slight delay for visual feedback
                                scope.launch {
                                    kotlinx.coroutines.delay(200) // Reduced delay for snappier feel
                                    currentScreen = "Locations"
                                }
                            },
                            viewModel = mapsViewModel,
                            darkMode = darkMode,
                            onSetPinButtonPositioned = { setPinButtonBounds = it },
                            onQuickAlarmButtonPositioned = { quickAlarmButtonBounds = it },
                            onSearchBarPositioned = { searchBarBounds = it },
                            onMapAreaPositioned = { mapAreaBounds = it },
                            onMapMoved = { tourMapInteracted = true },
                            onSearchBarTapped = { tourSearchInteracted = true },
                            onAlarmCreated = { tourAlarmCreated = true }
                        )
                        "Locations" -> LocationsScreen(
                            recentSearches = recentSearches,
                            viewModel = mapsViewModel,
                            onViewOnMap = {
                                // Switch to Map screen when "View on Map" is clicked in the sheet
                                if (!showGuidedTour) currentScreen = "Maps"
                            },
                            onRecentSearchClick = { searchQuery ->
                                // Set the search query in viewModel to trigger search when Maps screen opens
                                mapsViewModel.pendingSearchQuery = searchQuery
                                // Navigate to Maps screen
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
        }
        
        // Tour Prompt Dialog
        if (showTourPrompt && !showGuidedTour && !showHelpIconSpotlight) {
            TourPromptDialog(
                onStartTour = {
                    showTourPrompt = false
                    showGuidedTour = true
                    tourStep = 0
                    scope.launch {
                        userPreferences.saveTourPromptShown()
                    }
                },
                onDecline = {
                    showTourPrompt = false
                    scope.launch {
                        userPreferences.saveTourPromptShown()
                    }
                    // Show help icon spotlight if not shown yet
                    if (helpIconSpotlightShown == false) {
                        showHelpIconSpotlight = true
                    }
                }
            )
        }
        
        // Help Icon Spotlight
        if (showHelpIconSpotlight && helpIconBounds != null) {
            HelpIconSpotlightOverlay(
                helpIconBounds = helpIconBounds,
                onFinish = {
                    showHelpIconSpotlight = false
                    scope.launch {
                        userPreferences.saveHelpIconSpotlightShown()
                    }
                }
            )
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
                quickAlarmButtonBounds = quickAlarmButtonBounds,
                currentScreen = currentScreen,
                hasInteractedWithMap = tourMapInteracted,
                hasInteractedWithSearch = tourSearchInteracted,
                hasCreatedAlarm = tourAlarmCreated,
                onNext = { 
                    tourAlarmCreated = false // Reset for next step
                    tourStep++ 
                },
                onBack = { tourStep-- },
                onFinish = { 
                    showGuidedTour = false
                    tourStep = 0
                    tourMapInteracted = false
                    tourSearchInteracted = false
                    tourAlarmCreated = false
                    allowMapInteraction = false
                    allowSearchInteraction = false
                },
                onAllowMapInteraction = { allowMapInteraction = true },
                onAllowSearchInteraction = { allowSearchInteraction = true },
                onNavigateToScreen = { screen -> currentScreen = screen }
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
    onBackClick: () -> Unit = {},
    isOfflineMode: Boolean = false
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
        
        // App Title with Mode Indicator (Center)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "LokAlert",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            // Mode indicator badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = if (isOfflineMode) 
                            MaterialTheme.colorScheme.tertiaryContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isOfflineMode) "📴" else "🌐",
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isOfflineMode) "Offline" else "Online",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOfflineMode)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

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
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text("Map") },
            selected = currentScreen == "Maps",
            onClick = { onScreenSelected("Maps") },
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
            icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarms") },
            label = { Text("Alarms") },
            selected = currentScreen == "Locations",
            onClick = { onScreenSelected("Locations") },
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
fun HelpDialog(
    onDismiss: () -> Unit,
    onStartTour: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Need Help?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "👋 Welcome to LokAlert!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Would you like a quick guided tour to learn how to use the app?",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Interactive",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "⚡",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Quick",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "🎯",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Easy",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartTour,
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Tour")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text("Not Now")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun TourPromptDialog(
    onStartTour: () -> Unit,
    onDecline: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 360
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "speech_bubble_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "speech_bubble_alpha"
    )
    
    // Trigger animation on appear
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Speech bubble overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * alpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Dismiss on tap outside? Currently no action */ },
        contentAlignment = Alignment.Center
    ) {
        // Speech bubble card
        Card(
            modifier = Modifier
                .padding(if (isCompact) 16.dp else 24.dp)
                .widthIn(max = 340.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
            shape = RoundedCornerShape(if (isCompact) 20.dp else 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(if (isCompact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji icon
                Text(
                    text = "👋",
                    fontSize = if (isCompact) 36.sp else 48.sp
                )
                
                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))
                
                Text(
                    text = "Welcome to LokAlert!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = if (isCompact) 20.sp else 24.sp
                )
                
                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
                
                Text(
                    text = "Would you like a quick guided tour to learn how to use the app?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isCompact) 13.sp else 14.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "It only takes a minute! ⏱️",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isCompact) 11.sp else 12.sp
                )
                
                Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))
                
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 12.dp else 16.dp,
                            vertical = if (isCompact) 8.dp else 10.dp
                        )
                    ) {
                        Text("Skip", fontSize = if (isCompact) 13.sp else 14.sp)
                    }
                    
                    Button(
                        onClick = onStartTour,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 12.dp else 16.dp,
                            vertical = if (isCompact) 8.dp else 10.dp
                        )
                    ) {
                        Text("Start Tour", fontSize = if (isCompact) 13.sp else 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpIconSpotlightOverlay(
    helpIconBounds: Rect?,
    onFinish: () -> Unit
) {
    if (helpIconBounds == null) return
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onFinish() }
    ) {
        // Dark overlay with hole for help icon
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.7f))
        }
        
        // Help text
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Need Help?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the help icon (?) anytime to restart the guided tour.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFinish) {
                        Text("Got It!")
                    }
                }
            }
        }
    }
}

// Enum and data class for guided tour
enum class BubblePosition {
    ABOVE, BELOW, CENTER, BOTTOM
}

data class InteractiveTourStep(
    val title: String,
    val message: String,
    val spotlightBounds: Rect?,
    val secondarySpotlightBounds: Rect? = null,
    val bubblePosition: BubblePosition,
    val useCircularSpotlight: Boolean = false,
    val requiresAction: Boolean = false,
    val actionHint: String = ""
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
    quickAlarmButtonBounds: Rect?,
    currentScreen: String,
    hasInteractedWithMap: Boolean,
    hasInteractedWithSearch: Boolean,
    hasCreatedAlarm: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onAllowMapInteraction: () -> Unit,
    onAllowSearchInteraction: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val tourSteps = listOf(
        InteractiveTourStep(
            title = "Welcome to LokAlert! 👋",
            message = "Let's learn how to use the app together. This interactive tour will guide you step by step.",
            spotlightBounds = helpIconBounds,
            secondarySpotlightBounds = settingsIconBounds,
            bubblePosition = BubblePosition.BELOW,
            useCircularSpotlight = true,
            requiresAction = false,
            actionHint = ""
        ),
        InteractiveTourStep(
            title = "Try It: Go to Alarms 🔔",
            message = "Tap the 'Alarms' tab now to see your saved location alarms!",
            spotlightBounds = locationsTabBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = true,
            requiresAction = true,
            actionHint = "👆 Tap 'Alarms' tab"
        ),
        InteractiveTourStep(
            title = "Alarms Screen ✅",
            message = "Great! This is where all your location alarms are displayed. Now tap 'Map' to go back.",
            spotlightBounds = mapTabBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = true,
            requiresAction = true,
            actionHint = "👆 Tap 'Map' tab"
        ),
        InteractiveTourStep(
            title = "Interactive Map 🗺️",
            message = "Try moving the map around! Pan, zoom, or rotate to explore. This is how you'll navigate to find locations for your alarms.",
            spotlightBounds = mapAreaBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.BOTTOM,
            useCircularSpotlight = false,
            requiresAction = true,
            actionHint = "👆 Move the map to continue"
        ),
        InteractiveTourStep(
            title = "Search Bar 🔍",
            message = "Tap the search bar to find any location. You can search for addresses, places, or landmarks!",
            spotlightBounds = searchBarBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.BELOW,
            useCircularSpotlight = true,
            requiresAction = true,
            actionHint = "👆 Tap the search bar"
        ),
        InteractiveTourStep(
            title = "Edit Pin 📍",
            message = "Tap 'Edit Pin' to place a marker at the map center. You can then adjust the alarm radius and settings!",
            spotlightBounds = setPinButtonBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = true,
            requiresAction = false,
            actionHint = ""
        ),
        InteractiveTourStep(
            title = "Quick Alarm ⚡",
            message = "Tap 'Quick Alarm' to instantly create an alarm at the current location with default settings!",
            spotlightBounds = quickAlarmButtonBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = true,
            requiresAction = false,
            actionHint = ""
        ),
        InteractiveTourStep(
            title = "You're Ready! 🎉",
            message = "Congratulations! You now know how to use LokAlert. Create your first location alarm and never miss your destination again!",
            spotlightBounds = null,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.CENTER,
            useCircularSpotlight = false,
            requiresAction = false,
            actionHint = ""
        )
    )
    
    val currentTourStep = tourSteps.getOrNull(step) ?: return
    
    // Check if current step's action is completed
    val isActionCompleted = when (step) {
        1 -> currentScreen == "Locations" || currentScreen == "Alarms" // User tapped Alarms tab
        2 -> currentScreen == "Maps"   // User tapped Map tab
        3 -> hasInteractedWithMap // User moved the map
        4 -> hasInteractedWithSearch // User tapped the search bar
        else -> true // No action required for other steps
    }
    
    // Pulsing animation for highlight border - using simpler approach
    var pulsePhase by remember { mutableStateOf(0f) }
    LaunchedEffect(currentTourStep.requiresAction && !isActionCompleted) {
        if (currentTourStep.requiresAction && !isActionCompleted) {
            while (true) {
                pulsePhase = (pulsePhase + 0.05f) % (2f * 3.14159f)
                kotlinx.coroutines.delay(30)
            }
        }
    }
    val pulseAlpha = (kotlin.math.sin(pulsePhase.toDouble()) * 0.3 + 0.7).toFloat()
    val pulseScale = (kotlin.math.sin(pulsePhase.toDouble()) * 0.02 + 1.02).toFloat()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Semi-transparent overlay that allows clicking through to navigation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
            }
            
            currentTourStep.spotlightBounds?.let { bounds ->
                if (currentTourStep.useCircularSpotlight) {
                    val centerX = bounds.left + (bounds.right - bounds.left) / 2
                    val centerY = bounds.top + (bounds.bottom - bounds.top) / 2
                    val radius = maxOf(bounds.width, bounds.height) / 2 + 12.dp.toPx()
                    
                    overlayPath.addOval(
                        Rect(
                            center = Offset(centerX, centerY),
                            radius = radius
                        )
                    )
                } else {
                    // Rectangular spotlight with tighter padding
                    val padding = 8.dp.toPx()
                    val spotlightRect = RoundRect(
                        left = bounds.left - padding,
                        top = bounds.top - padding,
                        right = bounds.right + padding,
                        bottom = bounds.bottom + padding,
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                    
                    overlayPath.addRoundRect(spotlightRect)
                }
            }
            
            currentTourStep.secondarySpotlightBounds?.let { bounds ->
                if (currentTourStep.useCircularSpotlight) {
                    val centerX = bounds.left + (bounds.right - bounds.left) / 2
                    val centerY = bounds.top + (bounds.bottom - bounds.top) / 2
                    val radius = maxOf(bounds.width, bounds.height) / 2 + 12.dp.toPx()
                    
                    overlayPath.addOval(
                        Rect(
                            center = Offset(centerX, centerY),
                            radius = radius
                        )
                    )
                }
            }
            
            drawPath(
                path = overlayPath,
                color = Color.Black.copy(alpha = 0.5f)
            )
            
            // Draw pulsing highlight border around spotlighted element
            if (currentTourStep.requiresAction && !isActionCompleted) {
                currentTourStep.spotlightBounds?.let { bounds ->
                    if (currentTourStep.useCircularSpotlight) {
                        val centerX = bounds.left + (bounds.right - bounds.left) / 2
                        val centerY = bounds.top + (bounds.bottom - bounds.top) / 2
                        val radius = (maxOf(bounds.width, bounds.height) / 2 + 16.dp.toPx()) * pulseScale
                        
                        drawCircle(
                            color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    } else {
                        val padding = 12.dp.toPx() * pulseScale
                        drawRoundRect(
                            color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                            topLeft = Offset(bounds.left - padding, bounds.top - padding),
                            size = androidx.compose.ui.geometry.Size(
                                bounds.width + padding * 2,
                                bounds.height + padding * 2
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }
            }
        }
        
        
        SpeechBubble(
            title = currentTourStep.title,
            message = currentTourStep.message,
            actionHint = currentTourStep.actionHint,
            position = currentTourStep.bubblePosition,
            spotlightBounds = currentTourStep.spotlightBounds,
            step = step,
            totalSteps = tourSteps.size,
            requiresAction = currentTourStep.requiresAction,
            isActionCompleted = isActionCompleted,
            onNext = if (step < tourSteps.size - 1 && isActionCompleted) onNext else null,
            onBack = if (step > 0) onBack else null,
            onFinish = if (step == tourSteps.size - 1) onFinish else null,
            onSkip = onFinish
        )
    }
}

@Composable
fun BoxScope.SpeechBubble(
    title: String,
    message: String,
    actionHint: String = "",
    position: BubblePosition,
    spotlightBounds: Rect?,
    step: Int,
    totalSteps: Int,
    requiresAction: Boolean = false,
    isActionCompleted: Boolean = true,
    onNext: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onFinish: (() -> Unit)?,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Calculate bubble height estimate (varies based on content)
    val estimatedBubbleHeight = with(density) { 280.dp.toPx() }
    val bubbleWidth = 300.dp
    val horizontalPadding = with(density) { 16.dp.toPx() }
    
    val modifier = when (position) {
        BubblePosition.ABOVE -> {
            spotlightBounds?.let { bounds ->
                // Position bubble above the spotlight with enough clearance
                val availableSpaceAbove = bounds.top
                val offsetY = if (availableSpaceAbove > estimatedBubbleHeight + 40) {
                    // Enough space above - place near the top with some margin
                    with(density) { (bounds.top - estimatedBubbleHeight - 20.dp.toPx()).toInt().coerceAtLeast(20.dp.toPx().toInt()) }
                } else {
                    // Not enough space above - place at top of screen
                    with(density) { 60.dp.toPx().toInt() }
                }
                
                Modifier
                    .width(bubbleWidth)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, offsetY) }
            } ?: Modifier
                .width(bubbleWidth)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        }
        BubblePosition.BELOW -> {
            spotlightBounds?.let { bounds ->
                // Position bubble below the spotlight with enough clearance
                val availableSpaceBelow = screenHeight - bounds.bottom
                val offsetY = if (availableSpaceBelow > estimatedBubbleHeight + 40) {
                    // Enough space below - place right below spotlight
                    with(density) { (bounds.bottom + 24.dp.toPx()).toInt() }
                } else {
                    // Not enough space below - place at center or adjust
                    with(density) { (screenHeight / 2 - estimatedBubbleHeight / 2).toInt() }
                }
                
                Modifier
                    .width(bubbleWidth)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopStart)
                    .offset { IntOffset(((screenWidth - bubbleWidth.toPx()) / 2).toInt(), offsetY) }
            } ?: Modifier
                .width(bubbleWidth)
                .padding(horizontal = 16.dp)
                .align(Alignment.Center)
        }
        BubblePosition.CENTER -> {
            Modifier
                .width(bubbleWidth)
                .padding(horizontal = 16.dp)
                .align(Alignment.Center)
        }
        BubblePosition.BOTTOM -> {
            // Position bubble at bottom middle of screen, above the navigation bar
            Modifier
                .width(bubbleWidth)
                .padding(horizontal = 16.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)  // Above nav bar
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Step ${step + 1} of $totalSteps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Show action hint if action is required but not completed
            if (requiresAction && !isActionCompleted && actionHint.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = actionHint,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Show completion indicator
            if (requiresAction && isActionCompleted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✅ Great job! Tap Next to continue",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text("← Back")
                    }
                } else {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
                
                if (onFinish != null) {
                    Button(onClick = onFinish) {
                        Text("Finish 🎉")
                    }
                } else if (onNext != null) {
                    Button(
                        onClick = onNext,
                        enabled = !requiresAction || isActionCompleted
                    ) {
                        Text("Next →")
                    }
                } else if (requiresAction && !isActionCompleted) {
                    // Show disabled button when waiting for action
                    Button(
                        onClick = {},
                        enabled = false
                    ) {
                        Text("Next →")
                    }
                }
            }
        }
    }
}
