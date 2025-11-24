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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
@Composable
fun MapsScreen(onNewSearch: (String) -> Unit) {
    val context = LocalContext.current
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
                                Toast.makeText(context, "Location removed!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            if (markerPosition == null) Icons.Default.LocationOn else Icons.Default.Delete,
                            contentDescription = if (markerPosition == null) "Set Location" else "Remove Location"
                        )
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
                        val fusedLocationClient =
                            LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            location?.let {
                                val latLng = LatLng(it.latitude, it.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                    )
                                }
                            } ?: run {
                                Toast.makeText(
                                    context,
                                    "Unable to get current location",
                                    Toast.LENGTH_SHORT
                                ).show()
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
}
