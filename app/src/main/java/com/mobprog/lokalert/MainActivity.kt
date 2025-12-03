package com.mobprog.lokalert

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

    when (isOnboardingCompleted) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        false -> {
            OnboardingScreen(
                onFinished = {
                    scope.launch { userPreferences.saveOnboardingCompleted() }
                }
            )
        }
        true -> {
            LokAlertApp()
        }
    }
}

@Composable
fun  LokAlertApp() {
    var currentScreen by remember { mutableStateOf("Maps") }
    var titleColor by remember { mutableStateOf(Color(0xFF006DFF)) }
    var isRainbowEffectEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val mapsViewModel: MapsViewModel = viewModel()

    // State for Search History
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }

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
    Scaffold(
        topBar = { TopBar(
            color = animatedTitleColor.value,
            onSettingsClick = { currentScreen = "Settings" }) },
        bottomBar = {
            if (currentScreen != "Settings") {
                BottomNavBar(currentScreen) { currentScreen = it }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
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
                        currentScreen = "Maps"
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
fun TopBar(color: Color, onSettingsClick: () -> Unit) {
    var showHelpDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // App Title
        Text(
            text = "LokAlert",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        // Action Buttons (Aligned Right)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Help Icon
            IconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = "Help Guide",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Settings Icon
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
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
fun BottomNavBar(currentScreen: String, onScreenSelected: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentScreen == "Maps",
            onClick = { onScreenSelected("Maps") },
            icon = { Icon(Icons.Default.Map, contentDescription = null) },
            label = { Text("Map") }
        )
        NavigationBarItem(
            selected = currentScreen == "Locations",
            onClick = { onScreenSelected("Locations") },
            icon = { Icon(Icons.Default.Place, contentDescription = null) },
            label = { Text("Locations") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("How to Use LokAlert", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview
                HelpSection(
                    title = "📍 What is LokAlert?",
                    description = "LokAlert is a location-based alarm app that notifies you when you're near a specific place. Perfect for never missing your stop on public transport or remembering tasks at certain locations!"
                )
                
                HorizontalDivider()
                
                // Map Screen Guide
                HelpSection(
                    title = "🗺️ Map Screen",
                    description = "Set up location-based alarms:"
                )
                HelpItem("• Search for a location using the search bar at the top")
                HelpItem("• Long-press anywhere on the map to pin a location")
                HelpItem("• Drag the pin to adjust the exact position")
                HelpItem("• Tap the floating 'Set Pin' or 'Edit Pin' button to configure your alarm")
                HelpItem("• Use the 'My Location' button to quickly jump to your current position")
                
                HorizontalDivider()
                
                // Setting Up Alarms
                HelpSection(
                    title = "⚙️ Configuring Your Alarm",
                    description = "When you tap 'Set Pin' or 'Edit Pin':"
                )
                HelpItem("• Name: Give your alarm a memorable name (e.g., 'Office', 'Home')")
                HelpItem("• Active Days: Choose which days of the week this alarm should be active")
                HelpItem("• Sound: Select your preferred alarm ringtone")
                HelpItem("• Gradual Volume: Enable to start quietly and gradually increase volume")
                HelpItem("• Radius: Adjust the detection radius (50m-5000m) using the slider. The circle on the map shows your radius")
                HelpItem("• Tap 'Clear' below the name field to quickly clear it")
                
                HorizontalDivider()
                
                // Locations Screen Guide
                HelpSection(
                    title = "�� Locations Screen",
                    description = "Manage your saved alarms:"
                )
                HelpItem("• Recent Searches: Quick access to your search history")
                HelpItem("• Saved Locations: All your configured alarms appear here")
                HelpItem("• Heart Icon: Tap to favorite important locations")
                HelpItem("• Filter: Show only favorites using the toggle")
                HelpItem("• Tap any location card to:")
                HelpItem("  - Toggle alarm on/off with the switch")
                HelpItem("  - 'View on Map' to see the location and edit settings")
                HelpItem("  - 'Delete' to remove the alarm")
                
                HorizontalDivider()
                
                // Tips & Tricks
                HelpSection(
                    title = "💡 Tips & Tricks",
                    description = ""
                )
                HelpItem("• Pin Replacement: If you search for a new location while a pin is set, you'll be asked if you want to replace it")
                HelpItem("• Quick Edit: Tap the info bar showing your pinned location to adjust settings")
                HelpItem("• Permissions: Allow location access for the app to work properly")
                HelpItem("• Dark Mode: The map automatically switches to dark mode based on your system settings")
                
                HorizontalDivider()
                
                // Settings
                HelpSection(
                    title = "🎨 Settings",
                    description = "Customize your experience:"
                )
                HelpItem("• Rainbow Title: Animated rainbow effect for the app title")
                HelpItem("• Title Color: Choose from Red, Green, Blue, or Mono (system theme)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

@Composable
fun HelpSection(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HelpItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp)
    )
}