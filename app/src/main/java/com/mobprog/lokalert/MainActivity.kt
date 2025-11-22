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
// UPDATED: Alarm is now location-based
data class Alarm(val locationName: String, val radius: Float, val sound: String, val isEnabled: Boolean)



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Places API
        if (!Places.isInitialized()) {
            val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData?.getString("com.google.android.geo.API_KEY")
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
    var recentSearches by remember { mutableStateOf(listOf("Manila, PH", "Cebu, PH", "Davao, PH")) } // Initialize state
    var favoriteLocations by remember { mutableStateOf(listOf<String>("Home", "Work")) } // NEW STATE - Initialized with some data

    val animatedTitleColor = remember {
        Animatable(
            titleColor,
            TwoWayConverter(
                convertToVector = { color: Color -> AnimationVector4D(color.red, color.green, color.blue, color.alpha) },
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
fun SearchScreen() {
    Column {
        SearchSection()
        MapSection()
        RecentSearchSection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSection(
    onPlacePinClick: (() -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Only create the client if Places is initialized
    val placesClient = remember {
        if (Places.isInitialized()) Places.createClient(context) else null
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && placesClient != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries("PH")
                .setSessionToken(token)
                .setQuery(searchText)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                    expanded = suggestions.isNotEmpty()
                }
                .addOnFailureListener { 
                    suggestions = emptyList()
                    expanded = false
                }
        } else {
            suggestions = emptyList()
            expanded = false
        }
    }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = { Text("Search locations...", fontSize = 14.sp) },
            trailingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    modifier = Modifier.clickable { onSearch?.invoke(searchText) }
                ) 
            },
            shape = RoundedCornerShape(30.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch?.invoke(searchText)
                    expanded = false
                }
            )
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f) // Adjust width as needed
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(text = suggestion) },
                    onClick = {
                        searchText = suggestion
                        expanded = false
                        onSuggestionClick?.invoke(suggestion)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "OR",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { onPlacePinClick?.invoke() },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(40.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Click to Place Pin on Map")
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun MapSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF1C2A38), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Map Placeholder",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun RecentSearchSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Recent Searches", fontWeight = FontWeight.Bold)
        }
        Text(
            text = "No recent searches",
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapsScreen(onNewSearch: (String) -> Unit) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableFloatStateOf(100f) } // Default radius 100 meters
    var showRadiusAdjustment by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f)
    }
    val coroutineScope = rememberCoroutineScope()

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            onMapClick = {
                 // Optional: Click map to set pin as well
                 // markerPosition = latLng
            }
        ) {
            markerPosition?.let { position ->
                val markerState = rememberMarkerState(position = position)

                // Sync changes from marker drag back to our state
                if (markerState.dragState == com.google.maps.android.compose.DragState.END) {
                    markerPosition = markerState.position
                }

                Marker(
                    state = markerState,
                    title = "Selected Location",
                    draggable = true, // Make the marker draggable
                    onClick = {
                        // Keep default behavior (show info window) but we can add custom logic here
                        false
                    }
                )
                Circle(
                    center = markerState.position, // Use markerState position to follow drag
                    radius = radius.toDouble(),
                    strokeColor = Color(0xFF006DFF),
                    strokeWidth = 2f,
                    fillColor = Color(0x22006DFF)
                )
            }
        }

        // Floating Action Button to set pin at center of screen (current camera target)
        Box(
             modifier = Modifier
                 .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                 .align(Alignment.BottomEnd)
                 .padding(16.dp)
                 .padding(bottom = 80.dp) // Add padding to avoid overlap with BottomNavBar
                 .pointerInput(Unit) {
                     detectDragGestures { change, dragAmount ->
                         change.consume()
                         offsetX += dragAmount.x
                         offsetY += dragAmount.y
                     }
                 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showRadiusAdjustment) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                         Column {
                             Text("Adjust Radius: ${radius.toInt()}m", fontSize = 12.sp)
                             Slider(
                                 value = radius,
                                 onValueChange = { radius = it },
                                 valueRange = 50f..2000f,
                                 steps = 19
                             )
                         }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (markerPosition != null) {
                        Button(
                            onClick = {
                                 showRadiusAdjustment = !showRadiusAdjustment
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(56.dp)
                        ) {
                             Icon(Icons.Default.Edit, contentDescription = "Adjust Radius")
                             Spacer(Modifier.width(8.dp))
                             Text("Adjust Radius")
                        }
                    }

                    Button(
                        onClick = {
                            if (markerPosition == null) {
                                markerPosition = cameraPositionState.position.target
                                showRadiusAdjustment = false
                                Toast.makeText(context, "Location set!", Toast.LENGTH_SHORT).show()
                            } else {
                                markerPosition = null
                                showRadiusAdjustment = false
                                Toast.makeText(context, "Location removed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(56.dp)
                    ) {
                         Icon(if (markerPosition == null) Icons.Default.LocationOn else Icons.Default.Delete, contentDescription = if (markerPosition == null) "Set Location" else "Remove Location")
                         Spacer(Modifier.width(8.dp))
                         Text(if (markerPosition == null) "Set Location" else "Remove Location")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color(0xFFF0F0F0))
                .fillMaxWidth()
        ) {
            SearchSection(
                onPlacePinClick = {
                    if (hasLocationPermission) {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            location?.let {
                                val latLng = LatLng(it.latitude, it.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                    )
                                }
                            } ?: run {
                                Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        onNewSearch(query) // Record search on manual search
                        coroutineScope.launch {
                            try {
                                val geocoder = Geocoder(context)
                                val addresses = withContext(Dispatchers.IO) {
                                    @Suppress("DEPRECATION")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        geocoder.getFromLocationName(query, 1)
                                    } else {
                                        geocoder.getFromLocationName(query, 1)
                                    }
                                }

                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val latLng = LatLng(address.latitude, address.longitude)
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                    )
                                } else {
                                    Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onSuggestionClick = { suggestion ->
                    onNewSearch(suggestion) // Record search on suggestion click
                    coroutineScope.launch {
                        try {
                            val geocoder = Geocoder(context)
                            val addresses = withContext(Dispatchers.IO) {
                                @Suppress("DEPRECATION")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    geocoder.getFromLocationName(suggestion, 1)
                                } else {
                                    geocoder.getFromLocationName(suggestion, 1)
                                }
                            }

                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val latLng = LatLng(address.latitude, address.longitude)
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                )
                            } else {
                                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
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
            Icon(Icons.Default.Place, contentDescription = "Location", modifier = Modifier.size(24.dp))
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
        Text("My Locations", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        // First Section/Row: Search History
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Recent Search History", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.Gray)
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
            Text("Favorite Locations", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(favoriteLocations: List<String>) {
    val context = LocalContext.current
    var alarms by remember {
        mutableStateOf(
            listOf(
                // UPDATED: Initial alarms use new location/radius structure
                Alarm(locationName = "Home", radius = 100f, sound = "Chimes", isEnabled = true),
                Alarm(locationName = "Work", radius = 500f, sound = "Radar", isEnabled = false),
            )
        )
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    var alarmIndexToEdit by remember { mutableIntStateOf(-1) }
    var isNewAlarm by remember { mutableStateOf(false) } // Track if we are creating a new alarm

    if (showEditDialog && alarmToEdit != null) {
        EditAlarmDialog(
            alarm = alarmToEdit!!,
            favoriteLocations = favoriteLocations,
            onDismiss = { showEditDialog = false },
            onSave = { updatedAlarm ->
                val newList = alarms.toMutableList()
                if (isNewAlarm) {
                    newList.add(updatedAlarm)
                } else {
                    newList[alarmIndexToEdit] = updatedAlarm
                }
                alarms = newList
                showEditDialog = false
                isNewAlarm = false

                if (updatedAlarm.isEnabled) {
                    updateAlarmSchedule(context, updatedAlarm)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Location Alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            // PLUS Icon for adding a new alarm
            IconButton(
                onClick = {
                    if (favoriteLocations.isEmpty()) {
                        Toast.makeText(context, "Please add a favorite location first!", Toast.LENGTH_SHORT).show()
                    } else {
                        isNewAlarm = true
                        // Create a default alarm using the first favorite location
                        alarmToEdit = Alarm(
                            locationName = favoriteLocations.first(),
                            radius = 100f,
                            sound = "Chimes",
                            isEnabled = true
                        )
                        alarmIndexToEdit = -1 // Indicates a new alarm
                        showEditDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = Color(0xFF006DFF))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            itemsIndexed(alarms) { index, alarm ->
                AlarmItem(
                    alarm = alarm,
                    onToggle = { isEnabled ->
                        val newList = alarms.toMutableList()
                        val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                        newList[index] = updatedAlarm
                        alarms = newList

                        if (isEnabled) {
                            updateAlarmSchedule(context, updatedAlarm)
                        } else {
                            // Cancel the alarm if it was disabled
                            Toast.makeText(context, "Alarm for ${updatedAlarm.locationName} cancelled!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = {
                        val newList = alarms.toMutableList()
                        newList.removeAt(index)
                        alarms = newList
                    },
                    onClick = {
                        isNewAlarm = false
                        alarmToEdit = alarm
                        alarmIndexToEdit = index
                        showEditDialog = true
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(
    alarm: Alarm,
    favoriteLocations: List<String>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    var selectedLocation by remember(alarm, favoriteLocations) {
        val initialLocation = alarm.locationName
        if (favoriteLocations.contains(initialLocation)) {
            mutableStateOf(initialLocation)
        } else if (favoriteLocations.isNotEmpty()) {
            mutableStateOf(favoriteLocations.first())
        } else {
            mutableStateOf(initialLocation)
        }
    }
    
    var radius by remember(alarm) { mutableFloatStateOf(alarm.radius) }
    var sound by remember(alarm) { mutableStateOf(alarm.sound) }

    // Determine if the Save button should be enabled
    val saveEnabled = favoriteLocations.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Location Alarm") },
        text = {
            Column {
                Text(
                    text = "Select Location:",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (favoriteLocations.isEmpty()) {
                    Text("No favorite locations added. Add some in Favorites tab!", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) { // Limit height of the list
                        items(favoriteLocations) { location ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = saveEnabled) { selectedLocation = location }
                                    .background(if (selectedLocation == location) Color.LightGray else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = "Location", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(location, fontSize = 16.sp, fontWeight = if (selectedLocation == location) FontWeight.Bold else FontWeight.Normal)
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Radius Slider
                Text("Radius: ${radius.toInt()}m", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..2000f,
                    steps = 19, // Steps for granular control (50m increments)
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Sound Input (keeping existing logic)
                OutlinedTextField(
                    value = sound,
                    onValueChange = { sound = it },
                    label = { Text("Sound") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(alarm.copy(locationName = selectedLocation, radius = radius, sound = sound)) 
                },
                enabled = saveEnabled && selectedLocation.isNotEmpty() && selectedLocation != "No favorite locations added"
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// UPDATED: Renamed function and changed signature to location-based alarm.
fun updateAlarmSchedule(context: Context, alarm: Alarm) {
    // This function would typically implement Geofencing APIs (e.e., Google Location Services)
    // to trigger the alarm when the user enters/exits the specified radius around the location.
    // For this task, we treat it as a UI/placeholder implementation.
    
    if (alarm.isEnabled) {
        // Placeholder for Geofence registration
        Toast.makeText(
            context,
            "Location Alarm set for ${alarm.locationName} (Radius: ${alarm.radius.toInt()}m)!",
            Toast.LENGTH_LONG
        ).show()
    }
}


@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // UPDATED: Display Location Name and Radius
            Text(text = "Location: ${alarm.locationName}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Radius: ${alarm.radius.toInt()}m | Sound: ${alarm.sound}", fontSize = 14.sp, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Alarm")
            }
        }
    }
}

@Composable
fun SettingsScreen(onColorChange: (Color) -> Unit, isRainbowEnabled: Boolean, onRainbowToggle: (Boolean) -> Unit) {
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

        Column(modifier = Modifier.clickable(enabled = !isRainbowEnabled) { showColorOptions = !showColorOptions }) {
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
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
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