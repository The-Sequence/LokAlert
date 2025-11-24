package com.mobprog.lokalert


import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.mobprog.lokalert.ui.theme.LokAlertTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Notifications


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
                RequestPermissions()
                LokAlertApp()
            }
        }
    }
}

@Composable
fun RequestPermissions() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Intent().also { intent ->
                intent.action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                context.startActivity(intent)
            }
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
        bottomBar = { BottomNavBar(currentScreen) { currentScreen = it } },
        containerColor = Color(0xFFF0F0F0)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                "Search" -> SearchScreen()
                "Favorites" -> FavoritesScreen(
                    recentSearches = recentSearches,
                    favoriteLocations = favoriteLocations,
                    onToggleFavorite = ::toggleFavorite
                ) // Pass state and updater
                "Maps" -> MapsScreen(onNewSearch = ::addRecentSearch) // Pass updater
                "Alarms" -> AlarmsScreen(favoriteLocations = favoriteLocations) // Pass favoriteLocations
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
fun FavoritesScreen(
    recentSearches: List<String>,
    favoriteLocations: List<String>,
    onToggleFavorite: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Adjust padding for a cleaner look
    ) {
        // Main title for the screen
        Text(
            "My Locations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        // First Section/Row: Search History
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                "Recent Search History",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable and constrained history list using LazyColumn
            if (recentSearches.isEmpty()) {
                Text("No recent searches.", color = Color.LightGray)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp), // Limit height to about 5-6 items, enabling scroll if more exist
                    userScrollEnabled = true,
                ) {
                    itemsIndexed(recentSearches) { index, location ->
                        val isFavorite = favoriteLocations.contains(location)
                        SearchHistoryItem(
                            location = location,
                            isFavorite = isFavorite,
                            onToggleFavorite = onToggleFavorite
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Separator space between the two main sections

        // Second Section/Row: Favorite Locations Content
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Align text to start for better layout when list is populated
        ) {
            Text(
                "Favorite Locations",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (favoriteLocations.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("You haven't added any favorite locations yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth() // Use LazyColumn for Favorites List
                ) {
                    items(favoriteLocations) { location ->
                        // Reuse SearchHistoryItem for display, as it now handles the favorite/unfavorite logic
                        SearchHistoryItem(
                            location = location,
                            isFavorite = true, // It is a favorite if it's in this list
                            onToggleFavorite = onToggleFavorite
                        )
                        HorizontalDivider()
                    }
                }
            }
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
    val colorOptions = mapOf(
        "Red" to Color.Red,
        "Green" to Color.Green,
        "Blue" to Color.Blue,
        "Black" to Color.Black
    )
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
                    colorOptions.forEach { (name, colorValue) ->
                        Button(onClick = { onColorChange(colorValue) }) {
                            Text(name)
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
            selected = currentScreen == "Favorites",
            onClick = { onScreenSelected("Favorites") },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            label = { Text("Favorites") }
        )
        NavigationBarItem(
            selected = currentScreen == "Maps",
            onClick = { onScreenSelected("Maps") },
            icon = { Icon(Icons.Default.Place, contentDescription = null) },
            label = { Text("Maps") }
        )
        NavigationBarItem(
            selected = currentScreen == "Alarms",
            onClick = { onScreenSelected("Alarms") },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            label = { Text("Alarms") }
        )
        NavigationBarItem(
            selected = currentScreen == "Settings",
            onClick = { onScreenSelected("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}