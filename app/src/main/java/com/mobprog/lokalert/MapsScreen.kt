package com.mobprog.lokalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MapsScreen(onNewSearch: (String) -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var uiSettings by remember {
        mutableStateOf(MapUiSettings(
        zoomControlsEnabled = false,
        myLocationButtonEnabled = false
    ))
    }

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

    val isDarkTheme = isSystemInDarkTheme()

    val mapProperties = remember(isDarkTheme, hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = if (isDarkTheme) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
            } else {
                null // Default Light Mode
            }
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableFloatStateOf(100f) } // Default radius 100 meters
    var showRadiusAdjustment by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // Move the camera instantly to the user's location
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = { if (hasLocationPermission) {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                val latLng = LatLng(location.latitude, location.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                    )
                                }
                            } else {
                                Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
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
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Device location")
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (markerPosition == null) {
                            markerPosition = cameraPositionState.position.target
                            showRadiusAdjustment = false
                            Toast.makeText(context, "Location set!", Toast.LENGTH_SHORT).show()
                        } else {
                            markerPosition?.let { target ->
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLng(target)
                                    )
                                }
                            }
                            showBottomSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Set Pin"
                        )
                    },
                    text = {
                        Text(
                            text = if (markerPosition == null) "Set Pin" else "Edit Pin",
                            fontSize = 16.sp
                        )
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapClick = {
                    // Optional: Click map to set pin as well
                    // markerPosition = latLng
                }
            ) {
                markerPosition?.let { position ->
                    val markerState = rememberMarkerState(position = position)

                    // Sync changes from marker drag back to our state
                    if (markerState.dragState == DragState.END) {
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

            if (markerPosition == null) {
                Icon(
                    imageVector = Icons.Filled.Add, // Or use a custom crosshair icon
                    contentDescription = "Center Target",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp), // Make it large enough to see
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SearchSection(
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
                                    Toast.makeText(
                                        context,
                                        "Location not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "Error searching location",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp), // Add padding for bottom safe area
                verticalArrangement = Arrangement.spacedBy(16.dp) // Spacing between elements
            ) {
                // 1. Header Title
                Text(
                    text = "Edit Location",
                    style = MaterialTheme.typography.headlineSmall
                )

                Column {
                    Text(
                        text = "Alert Radius: ${radius.toInt()} meters",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 100f..1000f

                    )

                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            // Logic to remove the pin
                            markerPosition = null
                            showRadiusAdjustment = false
                            Toast.makeText(context, "Location removed!", Toast.LENGTH_SHORT).show()

                            // Close the sheet
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Delete")
                    }

                    // Done/Close Button
                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        },
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
