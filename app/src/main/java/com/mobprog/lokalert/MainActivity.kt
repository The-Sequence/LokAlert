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
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
            
            // Use system theme on first launch, then user preference
            val darkMode = savedDarkMode ?: if (isSystemInDarkTheme) 1 else 0
            
            // Save the initial theme based on system if not set
            LaunchedEffect(savedDarkMode) {
                if (savedDarkMode == null) {
                    appPreferences.setDarkMode(if (isSystemInDarkTheme) 1 else 0)
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
            
            // Start foreground location tracking service
            LaunchedEffect(Unit) {
                LocationTrackingService.startService(context)
            }
            
            LokAlertApp()
        }
    }
}

@Composable
fun LokAlertApp() {
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
    
    // Tour Prompt and Help Icon Spotlight State
    val userPreferences = remember { Onboarding(context) }
    val tourPromptShown by userPreferences.isTourPromptShown.collectAsState(initial = null)
    val helpIconSpotlightShown by userPreferences.isHelpIconSpotlightShown.collectAsState(initial = null)
    var showTourPrompt by remember { mutableStateOf(false) }
    var showHelpIconSpotlight by remember { mutableStateOf(false) }
    
    // Get dark mode preference
    val appPreferences = remember { AppPreferences(context) }
    val darkMode by appPreferences.darkMode.collectAsState(initial = 0)
    
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
    
    // Show tour prompt after onboarding if not shown yet
    LaunchedEffect(tourPromptShown) {
        if (tourPromptShown == false) {
            showTourPrompt = true
        }
    }
    
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
                        viewModel = mapsViewModel,
                        darkMode = darkMode
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
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Welcome to LokAlert!") },
        text = {
            Column {
                Text("Would you like a quick guided tour to learn how to use the app?")
                Spacer(modifier = Modifier.height(8.dp))
                Text("It only takes a minute and will help you get started.")
            }
        },
        confirmButton = {
            Button(onClick = onStartTour) {
                Text("Start Tour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Skip")
            }
        }
    )
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
    ABOVE, BELOW, CENTER
}

data class InteractiveTourStep(
    val title: String,
    val message: String,
    val spotlightBounds: Rect?,
    val secondarySpotlightBounds: Rect? = null,
    val bubblePosition: BubblePosition,
    val useCircularSpotlight: Boolean = false
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
    val tourSteps = listOf(
        InteractiveTourStep(
            title = "Welcome to LokAlert! 👋",
            message = "Thanks for choosing us! Tap the Help icon (?) anytime to restart this tour. The Settings icon (⚙️) lets you customize your experience.",
            spotlightBounds = helpIconBounds,
            secondarySpotlightBounds = settingsIconBounds,
            bubblePosition = BubblePosition.BELOW,
            useCircularSpotlight = true
        ),
        InteractiveTourStep(
            title = "Your Navigation Hub",
            message = "Switch between Map and Alarms screens using these tabs. Everything you need is just a tap away!",
            spotlightBounds = bottomNavBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = false
        ),
        InteractiveTourStep(
            title = "Map Screen 🗺️",
            message = "This is your main workspace for creating location alarms. Pin destinations and set up alerts with ease!",
            spotlightBounds = mapTabBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = false
        ),
        InteractiveTourStep(
            title = "Find Your Destination 🔍",
            message = "Search for any place or long-press anywhere on the map to drop a pin. Finding locations has never been easier!",
            spotlightBounds = searchBarBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.BELOW,
            useCircularSpotlight = false
        ),
        InteractiveTourStep(
            title = "Interactive Map View",
            message = "Long-press to place a pin, then drag it to fine-tune your location. The blue circle shows your alert radius!",
            spotlightBounds = mapAreaBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.CENTER,
            useCircularSpotlight = false
        ),
        InteractiveTourStep(
            title = "Manage Your Alarms 🔔",
            message = "Access all your location alarms here. View, edit, enable, disable, or delete alarms with just a few taps!",
            spotlightBounds = locationsTabBounds,
            secondarySpotlightBounds = null,
            bubblePosition = BubblePosition.ABOVE,
            useCircularSpotlight = false
        ),
        InteractiveTourStep(
            title = "Ready to Go! 🎉",
            message = "You're all set to create your first location alarm. Never miss your destination again with LokAlert!",
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
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
            }
            
            currentTourStep.spotlightBounds?.let { bounds ->
                if (currentTourStep.useCircularSpotlight) {
                    val centerX = bounds.left + (bounds.right - bounds.left) / 2
                    val centerY = bounds.top + (bounds.bottom - bounds.top) / 2
                    val radius = maxOf(bounds.width, bounds.height) / 2 + 8.dp.toPx()
                    
                    overlayPath.addOval(
                        Rect(
                            center = Offset(centerX, centerY),
                            radius = radius
                        )
                    )
                } else {
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
            
            currentTourStep.secondarySpotlightBounds?.let { bounds ->
                if (currentTourStep.useCircularSpotlight) {
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
            
            drawPath(
                path = overlayPath,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }
        
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
    
    val modifier = when (position) {
        BubblePosition.ABOVE -> {
            spotlightBounds?.let {
                val offsetY = with(density) { (it.top - 220.dp.toPx()).toInt() }
                Modifier
                    .width(280.dp)
                    .offset { IntOffset(0, offsetY) }
            } ?: Modifier
                .width(280.dp)
                .align(Alignment.TopCenter)
        }
        BubblePosition.BELOW -> {
            spotlightBounds?.let {
                val offsetY = with(density) { (it.bottom + 20.dp.toPx()).toInt() }
                Modifier
                    .width(280.dp)
                    .offset { IntOffset(0, offsetY) }
            } ?: Modifier
                .width(280.dp)
                .align(Alignment.BottomCenter)
        }
        BubblePosition.CENTER -> {
            Modifier
                .width(280.dp)
                .align(Alignment.Center)
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
                    Button(onClick = onNext) {
                        Text("Next →")
                    }
                }
            }
        }
    }
}