package com.mobprog.lokalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Selection mode for Demo Setup
 */
enum class DemoLocationMode {
    START,
    DESTINATION
}

/**
 * Full-screen Demo Setup Screen with interactive map
 * Allows users to set mock start location and destination by tapping on the map
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DemoSetupScreen(
    onDismiss: () -> Unit,
    darkMode: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Demo Mode Manager
    val demoModeManager = remember { DemoModeManager.getInstance(context) }
    val mockLocation by demoModeManager.mockLocation.collectAsState()
    val destination by demoModeManager.destination.collectAsState()
    val destinationRadius by demoModeManager.destinationRadius.collectAsState()
    val speedMps by demoModeManager.speedMps.collectAsState()
    
    // Selection mode state
    var selectionMode by remember { mutableStateOf(DemoLocationMode.START) }
    
    // Location names (geocoded)
    var startLocationName by remember { mutableStateOf("Tap map to set start") }
    var destinationName by remember { mutableStateOf("Tap map to set destination") }
    
    // Map state
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    // Initialize camera at mock location or default
    val initialPosition = mockLocation ?: LatLng(14.7012, 121.0764) // NU Fairview default
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 14f)
    }
    
    // Map styling
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveMapDarkMode = when (darkMode) {
        3 -> if (isSystemDark) 1 else 0
        2 -> 1
        else -> darkMode
    }
    val mapProperties = remember(hasLocationPermission, effectiveMapDarkMode) {
        MapProperties(
            isMyLocationEnabled = false, // We show our own markers
            mapStyleOptions = if (effectiveMapDarkMode > 0) {
                try {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                } catch (e: Exception) {
                    null
                }
            } else null
        )
    }
    
    // Geocode location names
    LaunchedEffect(mockLocation) {
        mockLocation?.let { loc ->
            try {
                val geocoder = Geocoder(context)
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                }
                startLocationName = results?.firstOrNull()?.let { addr ->
                    addr.thoroughfare ?: addr.locality ?: addr.subAdminArea ?: "Start Location"
                } ?: "Start: %.4f, %.4f".format(loc.latitude, loc.longitude)
            } catch (e: Exception) {
                startLocationName = "Start: %.4f, %.4f".format(loc.latitude, loc.longitude)
            }
        }
    }
    
    LaunchedEffect(destination) {
        destination?.let { loc ->
            try {
                val geocoder = Geocoder(context)
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                }
                destinationName = results?.firstOrNull()?.let { addr ->
                    addr.thoroughfare ?: addr.locality ?: addr.subAdminArea ?: "Destination"
                } ?: "Dest: %.4f, %.4f".format(loc.latitude, loc.longitude)
            } catch (e: Exception) {
                destinationName = "Dest: %.4f, %.4f".format(loc.latitude, loc.longitude)
            }
        }
    }
    
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
                        when (selectionMode) {
                            DemoLocationMode.START -> {
                                demoModeManager.setMockLocation(latLng)
                            }
                            DemoLocationMode.DESTINATION -> {
                                demoModeManager.setDestination(latLng)
                            }
                        }
                    }
                ) {
                    // Start location marker (blue)
                    mockLocation?.let { start ->
                        val startMarkerState = rememberMarkerState(key = "demo_start", position = start)
                        
                        LaunchedEffect(start) {
                            startMarkerState.position = start
                        }
                        
                        Marker(
                            state = startMarkerState,
                            title = "Start Location",
                            snippet = startLocationName,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            draggable = true,
                            zIndex = 100f
                        )
                        
                        // Handle drag
                        LaunchedEffect(startMarkerState.position) {
                            if (startMarkerState.position != start) {
                                demoModeManager.setMockLocation(startMarkerState.position)
                            }
                        }
                    }
                    
                    // Destination marker (green) with radius circle
                    destination?.let { dest ->
                        val destMarkerState = rememberMarkerState(key = "demo_dest", position = dest)
                        
                        LaunchedEffect(dest) {
                            destMarkerState.position = dest
                        }
                        
                        Marker(
                            state = destMarkerState,
                            title = "Destination",
                            snippet = destinationName,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            draggable = true,
                            zIndex = 99f
                        )
                        
                        // Handle drag
                        LaunchedEffect(destMarkerState.position) {
                            if (destMarkerState.position != dest) {
                                demoModeManager.setDestination(destMarkerState.position)
                            }
                        }
                        
                        // Destination radius circle
                        Circle(
                            center = dest,
                            radius = destinationRadius.toDouble(),
                            strokeColor = Color(0xFF4CAF50),
                            fillColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            strokeWidth = 4f
                        )
                    }
                    
                    // Path line between start and destination
                    if (mockLocation != null && destination != null) {
                        Polyline(
                            points = listOf(mockLocation!!, destination!!),
                            color = Color(0xFF2196F3).copy(alpha = 0.7f),
                            width = 10f
                        )
                    }
                }
                
                // Top Bar with title and close button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📍 Demo Setup",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap map to set locations",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        FilledTonalButton(onClick = onDismiss) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Done")
                        }
                    }
                }
                
                // Mode Selection Toggle (below top bar)
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Select what to set:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Location Button
                            FilterChip(
                                selected = selectionMode == DemoLocationMode.START,
                                onClick = { selectionMode = DemoLocationMode.START },
                                label = {
                                    Column {
                                        Text("🔵 Start Location", fontWeight = FontWeight.Medium)
                                        Text(
                                            text = startLocationName,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Destination Button
                            FilterChip(
                                selected = selectionMode == DemoLocationMode.DESTINATION,
                                onClick = { selectionMode = DemoLocationMode.DESTINATION },
                                label = {
                                    Column {
                                        Text("🟢 Destination", fontWeight = FontWeight.Medium)
                                        Text(
                                            text = destinationName,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Bottom Controls Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {
                        // Destination Radius Slider
                        Text(
                            text = "Destination Radius: ${destinationRadius}m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = destinationRadius.toFloat(),
                            onValueChange = { demoModeManager.setDestinationRadius(it.toInt()) },
                            valueRange = 10f..500f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Speed Slider
                        Text(
                            text = "Movement Speed: $speedMps m/s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when {
                                speedMps <= 2 -> "🚶 Walking slowly"
                                speedMps <= 5 -> "🚶 Walking"
                                speedMps <= 10 -> "🚴 Cycling"
                                speedMps <= 20 -> "🚗 Driving city"
                                speedMps <= 35 -> "🚗 Driving highway"
                                else -> "🚀 High speed"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = speedMps.toFloat(),
                            onValueChange = { demoModeManager.setSpeed(it.toInt()) },
                            valueRange = 1f..50f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Center on Start
                            OutlinedButton(
                                onClick = {
                                    mockLocation?.let { loc ->
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(loc, 15f),
                                                durationMs = 500
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = mockLocation != null
                            ) {
                                Text("🔵 Go to Start")
                            }
                            
                            // Center on Destination
                            OutlinedButton(
                                onClick = {
                                    destination?.let { loc ->
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(loc, 15f),
                                                durationMs = 500
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = destination != null
                            ) {
                                Text("🟢 Go to Dest")
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Fit both markers in view
                        Button(
                            onClick = {
                                if (mockLocation != null && destination != null) {
                                    scope.launch {
                                        val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                                            .include(mockLocation!!)
                                            .include(destination!!)
                                            .build()
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngBounds(bounds, 100),
                                            durationMs = 500
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = mockLocation != null && destination != null
                        ) {
                            Text("📍 Fit Both Locations")
                        }
                    }
                }
                
                // Instructions overlay when nothing is set
                if (mockLocation == null && destination == null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👆", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Tap anywhere on the map",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "to set your ${if (selectionMode == DemoLocationMode.START) "start" else "destination"} location",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Current selection mode indicator (floating)
                Card(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectionMode == DemoLocationMode.START) {
                            Color(0xFF2196F3).copy(alpha = 0.9f)
                        } else {
                            Color(0xFF4CAF50).copy(alpha = 0.9f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (selectionMode == DemoLocationMode.START) "Setting: START" else "Setting: DEST",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
