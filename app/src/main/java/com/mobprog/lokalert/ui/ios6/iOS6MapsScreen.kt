package com.mobprog.lokalert.ui.ios6

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import com.mobprog.lokalert.MapsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================================
// iOS 6 MAPS SCREEN - Classic iPhone Maps Style
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iOS6MapsScreen(
    onNewSearch: (String) -> Unit,
    onDone: () -> Unit,
    viewModel: MapsViewModel,
    darkMode: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Location permission
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(1.35, 103.87), 10f)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPermission = it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    // Move to user location on load
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(it.latitude, it.longitude), 16f
                        )
                    }
                }
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    
    // Map properties
    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission
        )
    }
    
    // Saved locations for markers
    val savedLocations by viewModel.savedLocations.collectAsState()
    
    // Keep map centered on marker when editing
    LaunchedEffect(viewModel.showBottomSheet, viewModel.markerPosition) {
        if (viewModel.showBottomSheet && viewModel.markerPosition != null) {
            // Center on marker with some offset for the sheet
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(viewModel.markerPosition!!)
            )
        }
    }
    
    // Update radius circle in real-time
    LaunchedEffect(viewModel.radius) {
        // Radius changes are reflected automatically through recomposition
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                // Keep gestures enabled even when editing - allow map movement
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            ),
            onMapClick = { latLng ->
                // Only allow new marker placement when sheet is not showing
                if (!viewModel.showBottomSheet) {
                    viewModel.markerPosition = latLng
                    viewModel.showBottomSheet = true
                    reverseGeocode(context, latLng) { address ->
                        viewModel.alarmName = address
                    }
                    // Center on the new marker
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                        )
                    }
                }
            }
        ) {
            // Current marker
            viewModel.markerPosition?.let { position ->
                Marker(
                    state = MarkerState(position = position),
                    title = viewModel.alarmName.ifEmpty { "New Alarm" }
                )
                
                Circle(
                    center = position,
                    radius = viewModel.radius.toDouble(),
                    fillColor = Color(0x30007AFF),
                    strokeColor = Color(0xFF007AFF),
                    strokeWidth = 2f
                )
            }
            
            // Saved location markers
            savedLocations.forEach { location ->
                Marker(
                    state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                    title = location.name,
                    alpha = if (location.isEnabled) 1f else 0.5f
                )
                
                Circle(
                    center = LatLng(location.latitude, location.longitude),
                    radius = location.radius.toDouble(),
                    fillColor = Color(0x204CD964),
                    strokeColor = if (location.isEnabled) Color(0xFF4CD964) else Color(0xFF8E8E93),
                    strokeWidth = 2f
                )
            }
        }
        
        // Removed touch blocker - map should remain interactive when editing
        
        // iOS 6 styled overlay UI
        Column(modifier = Modifier.fillMaxSize()) {
            // Places API client for autocomplete
            val localPlacesClient = remember {
                try {
                    if (!Places.isInitialized()) {
                        val packageInfo = context.packageManager.getApplicationInfo(
                            context.packageName,
                            PackageManager.GET_META_DATA
                        )
                        val apiKey = packageInfo.metaData?.getString("com.google.android.geo.API_KEY")
                        if (apiKey != null) {
                            Places.initialize(context, apiKey)
                        }
                    }
                    if (Places.isInitialized()) {
                        Places.createClient(context)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            val localAutocompleteToken = remember { AutocompleteSessionToken.newInstance() }
            
            // Search state
            var suggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
            var showSuggestions by remember { mutableStateOf(false) }
            var userSelectedSuggestion by remember { mutableStateOf(false) }
            val focusManager = LocalFocusManager.current
            
            // Autocomplete logic
            LaunchedEffect(searchQuery) {
                if (userSelectedSuggestion) {
                    userSelectedSuggestion = false
                    return@LaunchedEffect
                }
                if (searchQuery.length >= 2 && localPlacesClient != null) {
                    try {
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setSessionToken(localAutocompleteToken)
                            .setQuery(searchQuery)
                            .build()
                        
                        localPlacesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                suggestions = response.autocompletePredictions.map { prediction ->
                                    prediction.placeId to prediction.getFullText(null).toString()
                                }
                                showSuggestions = suggestions.isNotEmpty()
                            }
                            .addOnFailureListener {
                                suggestions = emptyList()
                                showSuggestions = false
                            }
                    } catch (e: Exception) {
                        suggestions = emptyList()
                        showSuggestions = false
                    }
                } else {
                    suggestions = emptyList()
                    showSuggestions = false
                }
            }
            
            // Top search bar area with suggestions
            iOS6MapSearchBarWithSuggestions(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                suggestions = suggestions,
                showSuggestions = showSuggestions,
                onSuggestionClick = { placeId, placeName ->
                    userSelectedSuggestion = true
                    searchQuery = placeName
                    showSuggestions = false
                    focusManager.clearFocus()
                    
                    // Fetch place details and navigate
                    localPlacesClient?.let { client ->
                        val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                        val fetchRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
                        client.fetchPlace(fetchRequest)
                            .addOnSuccessListener { fetchResponse ->
                                fetchResponse.place.latLng?.let { location ->
                                    viewModel.markerPosition = location
                                    viewModel.alarmName = placeName
                                    viewModel.showBottomSheet = true
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(location, 16f)
                                        )
                                    }
                                }
                            }
                    }
                    onNewSearch(placeName)
                },
                onDismissSuggestions = { showSuggestions = false },
                modifier = Modifier.statusBarsPadding()
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Floating action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // My location button
                    iOS6MapButton(
                        icon = Icons.Default.MyLocation,
                        onClick = {
                            if (hasLocationPermission) {
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                        loc?.let {
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(it.latitude, it.longitude), 16f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    // Handle permission error
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }
                
                // Quick alarm button
                iOS6QuickAlarmButton(
                    onClick = {
                        // Create alarm at center of map
                        val center = cameraPositionState.position.target
                        viewModel.markerPosition = center
                        viewModel.showBottomSheet = true
                        reverseGeocode(context, center) { address ->
                            viewModel.alarmName = address
                        }
                    }
                )
            }
        }
        
        // Bottom sheet for alarm creation
        if (viewModel.showBottomSheet) {
            iOS6AlarmCreationSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showBottomSheet = false },
                onSave = {
                    viewModel.saveAlarm {
                        onDone()
                    }
                }
            )
        }
    }
}

// ============================================================================
// iOS 6 MAP SEARCH BAR
// ============================================================================

@Composable
fun iOS6MapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
                .drawBehind {
                    // Inner shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x15000000), Color.Transparent),
                            startY = 0f,
                            endY = 4.dp.toPx()
                        )
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search or tap map to set alarm",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFFC7C7CC)
                        )
                    )
                }
                
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC7C7CC))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// iOS 6 MAP SEARCH BAR WITH SUGGESTIONS
// ============================================================================

@Composable
fun iOS6MapSearchBarWithSuggestions(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<Pair<String, String>>,  // placeId to placeName
    showSuggestions: Boolean,
    onSuggestionClick: (String, String) -> Unit,
    onDismissSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) Color(0xFF007AFF) else Color(0xFFB4B4B6),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x15000000), Color.Transparent),
                                startY = 0f,
                                endY = 4.dp.toPx()
                            )
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search places...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFFC7C7CC)
                            )
                        )
                    }
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true
                    )
                }
                
                if (query.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC7C7CC))
                            .clickable { 
                                onQueryChange("")
                                onDismissSuggestions()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        
        // Suggestions dropdown
        AnimatedVisibility(
            visible = showSuggestions && suggestions.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                ) {
                    items(suggestions.take(5)) { (placeId, placeName) ->
                        iOS6SuggestionItem(
                            text = placeName,
                            onClick = { onSuggestionClick(placeId, placeName) }
                        )
                        if (suggestions.indexOf(placeId to placeName) < suggestions.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 44.dp)
                                    .height(1.dp)
                                    .background(Color(0xFFE5E5E5))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun iOS6SuggestionItem(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPressed) Color(0xFF007AFF).copy(alpha = 0.2f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = TextStyle(
                fontSize = 15.sp,
                color = Color.Black
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================================
// iOS 6 MAP BUTTON
// ============================================================================

@Composable
fun iOS6MapButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFFCCCCCC), Color(0xFFCCCCCC))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFE5E5E5))
                    }
                )
            )
            .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ============================================================================
// iOS 6 QUICK ALARM BUTTON
// ============================================================================

@Composable
fun iOS6QuickAlarmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF3CB84C), Color(0xFF3CB84C))
                    } else {
                        listOf(Color(0xFF7CC576), Color(0xFF3F9F3A))
                    }
                )
            )
            .border(1.dp, Color(0xFF2E8B2E), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Set Alarm Here",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color(0x60000000),
                        offset = Offset(0f, -1f),
                        blurRadius = 0f
                    )
                )
            )
        }
    }
}

// ============================================================================
// iOS 6 ALARM CREATION SHEET - Classic iOS 6 Dark Popup Style
// ============================================================================

@Composable
fun iOS6AlarmCreationSheet(
    viewModel: MapsViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }
    val dismissThreshold = 150f
    
    // Animate the offset smoothly - only for drag-to-dismiss, not slider
    val animatedOffset by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sheetOffset"
    )
    
    // Backdrop alpha based on offset
    val backdropAlpha = (0.5f - (animatedOffset / 600f)).coerceIn(0.05f, 0.5f)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Semi-transparent backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        
        // iOS 6 Style Sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = animatedOffset.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        offsetY = (offsetY + delta * 0.8f).coerceAtLeast(0f)
                    },
                    onDragStopped = { velocity ->
                        if (offsetY > dismissThreshold || velocity > 1000f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    }
                )
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle area - collapses when slider is dragging
            AnimatedVisibility(
                visible = !isSliderDragging,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }
            }
            
            // Main iOS 6 styled card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF5A6570), RoundedCornerShape(10.dp))
            ) {
                // iOS 6 Blue gradient header - collapses when slider is dragging
                AnimatedVisibility(
                    visible = !isSliderDragging,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF6D9FCF),
                                        Color(0xFF4A7BAA),
                                        Color(0xFF3A6897)
                                    )
                                )
                            )
                            .drawBehind {
                                drawLine(
                                    Color.White.copy(alpha = 0.4f),
                                    Offset(0f, 1f),
                                    Offset(size.width, 1f),
                                    1f
                                )
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New Alarm",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    offset = Offset(0f, -1f),
                                    blurRadius = 0f
                                )
                            )
                        )
                    }
                }
                
                // Content area with iOS 6 linen-style background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE8EBF0),
                                    Color(0xFFD8DBE0),
                                    Color(0xFFCDD0D5)
                                )
                            )
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Name field - collapses when slider is dragging
                    AnimatedVisibility(
                        visible = !isSliderDragging,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        iOS6InsetField(
                            label = "Name",
                            value = viewModel.alarmName,
                            onValueChange = { viewModel.alarmName = it },
                            placeholder = "Enter alarm name"
                        )
                    }
                    
                    // Days selector - collapses when slider is dragging
                    AnimatedVisibility(
                        visible = !isSliderDragging,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        iOS6DaysSection(
                            selectedDays = viewModel.selectedDays,
                            onDaysChanged = { viewModel.selectedDays = it }
                        )
                    }
                    
                    // Radius slider - STAYS VISIBLE (at the bottom of content)
                    iOS6InsetSlider(
                        label = "Radius",
                        value = viewModel.radius,
                        onValueChange = { viewModel.radius = it },
                        valueRange = 50f..1000f,
                        valueDisplay = "${viewModel.radius.toInt()}m",
                        onDragStart = { isSliderDragging = true },
                        onDragEnd = { isSliderDragging = false }
                    )
                }
                
                // Bottom buttons area - collapses when slider is dragging
                AnimatedVisibility(
                    visible = !isSliderDragging,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFBFC3C8))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF8A9099))
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            iOS6SheetButton(
                                text = "Cancel",
                                modifier = Modifier.weight(1f),
                                onClick = onDismiss
                            )
                            
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF8A9099))
                            )
                            
                            iOS6SheetButton(
                                text = "Save",
                                modifier = Modifier.weight(1f),
                                isPrimary = true,
                                onClick = onSave
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun iOS6InsetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFB0B5BA), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6D7278)
            ),
            modifier = Modifier.padding(start = 12.dp, top = 8.dp)
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 10.dp, top = 4.dp),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, style = TextStyle(fontSize = 16.sp, color = Color(0xFFAAAAAA)))
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun iOS6InsetSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueDisplay: String,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFB0B5BA), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6D7278))
            )
            Text(
                text = valueDisplay,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A6897))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // iOS 6 style slider using standard Slider
        Slider(
            value = value,
            onValueChange = { 
                if (!isDragging) {
                    isDragging = true
                    onDragStart()
                }
                onValueChange(it) 
            },
            valueRange = valueRange,
            onValueChangeFinished = {
                isDragging = false
                onDragEnd()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFEEEEEE),
                activeTrackColor = Color(0xFF3A7AB0),
                inactiveTrackColor = Color(0xFFD0D4DA)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("50m", style = TextStyle(fontSize = 11.sp, color = Color(0xFF8A8A8E)))
            Text("1000m", style = TextStyle(fontSize = 11.sp, color = Color(0xFF8A8A8E)))
        }
    }
}

@Composable
private fun iOS6DaysSection(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFB0B5BA), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Active Days",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6D7278))
        )
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { i, d ->
                val sel = selectedDays.contains(i)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onDaysChanged(if (sel) selectedDays - i else selectedDays + i) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(if (sel) 2.dp else 1.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = if (sel) {
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF5A9AD0), Color(0xFF3A7AB0), Color(0xFF2A6090))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFF8F8F8), Color(0xFFE8E8E8), Color(0xFFD8D8D8))
                                    )
                                }
                            )
                            .border(1.dp, if (sel) Color(0xFF2A5070) else Color(0xFFB0B5BA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            d,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else Color(0xFF5A5A5E),
                                shadow = if (sel) Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, -1f), 0f) else null
                            )
                        )
                    }
                    Text(
                        dayNames[i],
                        style = TextStyle(fontSize = 9.sp, color = Color(0xFF8A8A8E)),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun iOS6SheetButton(
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonGradient = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF8A9099), Color(0xFF9AA3AA))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E3E8), Color(0xFFCDD0D5))
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(brush = buttonGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (!isPressed) {
                    drawLine(
                        Color.White.copy(alpha = 0.8f),
                        Offset(0f, 1f),
                        Offset(size.width, 1f),
                        1f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isPressed) Color.White else Color(0xFF3A6897),
                shadow = if (isPressed) {
                    Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, -1f), 0f)
                } else {
                    Shadow(Color.White, Offset(0f, 1f), 0f)
                }
            )
        )
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

private fun reverseGeocode(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val locationName = when {
                !address.featureName.isNullOrEmpty() && 
                        address.featureName != address.subThoroughfare -> address.featureName
                !address.thoroughfare.isNullOrEmpty() -> address.thoroughfare
                !address.subLocality.isNullOrEmpty() -> address.subLocality
                !address.locality.isNullOrEmpty() -> address.locality
                else -> "Location ${latLng.latitude.toString().take(6)}, ${latLng.longitude.toString().take(6)}"
            }
            onResult(locationName)
        } else {
            onResult("Location")
        }
    } catch (e: Exception) {
        onResult("Location")
    }
}
