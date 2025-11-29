package com.mobprog.lokalert


import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import com.google.android.libraries.places.api.Places
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.launch


data class Alarm(
    val locationName: String,
    val radius: Float,
    val sound: String,
    val isEnabled: Boolean
)


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
                    scope.launch {
                        userPreferences.saveOnboardingCompleted()
                    }
                }
            )
        }
        true -> {
            LokAlertApp()
        }
    }
}


@Composable
fun LokAlertApp() {
    var currentScreen by remember { mutableStateOf("Maps") }
    var titleColor by remember { mutableStateOf(Color(0xFF006DFF)) }
    var isRainbowEffectEnabled by remember { mutableStateOf(false) }
    var recentSearches by remember {
        mutableStateOf(
            listOf(
                "Manila, PH",
                "Cebu, PH",
                "Davao, PH"
            )
        )
    } // Initialize state
    var favoriteLocations by remember {
        mutableStateOf(
            listOf<String>(
                "Home",
                "Work"
            )
        )
    }

    val animatedTitleColor = remember {
        Animatable(
            titleColor,
            TwoWayConverter(
                convertToVector = { color: Color ->
                    AnimationVector4D(
                        color.red,
                        color.green,
                        color.blue,
                        color.alpha
                    )
                },
                convertFromVector = { vector -> Color(vector.v1, vector.v2, vector.v3, vector.v4) }
            )
        )
    }

    LaunchedEffect(isRainbowEffectEnabled, titleColor) {
        if (isRainbowEffectEnabled) {
            launch {
                val rainbowColors = listOf(
                    Color.Red,
                    Color.Green,
                    Color.Blue,
                    Color.Magenta,
                    Color.Yellow,
                    Color.Red
                )
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

    fun addRecentSearch(location: String) {
        val MAX_HISTORY = 5
        if (!recentSearches.contains(location)) {
            recentSearches = listOf(location) + recentSearches
            if (recentSearches.size > MAX_HISTORY) {
                recentSearches = recentSearches.take(MAX_HISTORY)
            }
        }
    }

    fun toggleFavorite(location: String) { // NEW FUNCTION
        favoriteLocations = if (favoriteLocations.contains(location)) {
            favoriteLocations.filter { it != location }
        } else {
            favoriteLocations + location
        }
    }

    Scaffold(
        topBar = { TopBar(animatedTitleColor.value) },
        bottomBar = { BottomNavBar(currentScreen) { currentScreen = it } }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                "Favorites" -> FavoritesScreen(
                    recentSearches = recentSearches,
                    favoriteLocations = favoriteLocations,
                    onToggleFavorite = ::toggleFavorite
                ) // Pass state and updater
                "Maps" -> MapsScreen(onNewSearch = ::addRecentSearch) // Pass updater
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

// Add this to the bottom of your MainActivity.kt file, outside any class
fun Modifier.bypassing(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(pass = PointerEventPass.Initial)
        }
    }
}


@Composable
fun TopBar(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "LokAlert",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Options",
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun SearchHistoryItem(location: String, isFavorite: Boolean, onToggleFavorite: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Implement navigation/search on click */ }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Location",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(location, fontSize = 16.sp)
        }

        IconButton(onClick = { onToggleFavorite(location) }) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (isFavorite) Color.Red else Color.Gray
            )
        }
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
        "Mono" to if (isSystemInDarkTheme()) {
            Color.White
        } else {
            Color.Black
        }

    )
    val colorOptionKeys = colorOptions.keys.toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SingleChoiceSegmentedButtonRow {
                        colorOptionKeys.forEachIndexed { index, name ->
                            val colorValue = colorOptions[name]!!
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = colorOptionKeys.size
                                ),
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
            selected = currentScreen == "Favorites",
            onClick = { onScreenSelected("Favorites") },
            icon = { Icon(Icons.Default.Place, contentDescription = null) },
            label = { Text("Locations") }
        )

        NavigationBarItem(
            selected = currentScreen == "Settings",
            onClick = { onScreenSelected("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}