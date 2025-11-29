package com.mobprog.lokalert

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    recentSearches: List<String>,
    onViewOnMap: () -> Unit,
    viewModel: MapsViewModel
) {
    val savedLocations by viewModel.savedLocations.collectAsState()


    var showFavoritesOnly by remember { mutableStateOf(false) }

    val displayedLocations = remember(savedLocations, showFavoritesOnly) {
        if (showFavoritesOnly) {
            savedLocations.filter { it.isFavorite }
        } else {
            savedLocations
        }
    }

    // Bottom Sheet State
    var locationToEdit by remember { mutableStateOf<LocationAlarm?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp), // Increased side padding
    ) {
        // --- HEADER ---
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "My Locations",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 1: RECENT SEARCH HISTORY ---
        // Visual Style: Low emphasis (Flat list, smaller text)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "RECENT SEARCHES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recentSearches.isEmpty()) {
            Text(
                "No recent history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 140.dp), // Limit height
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(recentSearches) { _, location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: SAVED LOCATIONS ---
        // Visual Style: High Emphasis (Cards, Icons, Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Saved Locations",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            // 3. NEW: The Toggle Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Favorites Only",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = showFavoritesOnly,
                    onCheckedChange = { showFavoritesOnly = it },
                    modifier = Modifier.scale(0.8f) // Make it slightly smaller
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (displayedLocations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Changing text based on filter context
                val emptyText = if (showFavoritesOnly && displayedLocations.isNotEmpty()) {
                    "No favorites found."
                } else {
                    "No pins set yet. Go to Map to add one!"
                }
                Text(emptyText, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Gap between cards
            ) {
                items(displayedLocations, key = { it.id }) { alarm ->
                    SavedLocationCard(
                        alarm = alarm,
                        onToggleFavorite = { viewModel.toggleFavorite(alarm) },
                        onDelete = { viewModel.deleteLocation(alarm) },
                        onEdit = { locationToEdit = alarm }
                    )
                }
            }
        }
    }

    // --- BOTTOM SHEET ---
    if (locationToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = { locationToEdit = null },
            sheetState = sheetState
        ) {
            EditLocationSheet(
                alarm = locationToEdit!!,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { locationToEdit = null }
                },
                onSave = { name, radius ->
                    viewModel.updateLocationDetails(locationToEdit!!, name, radius)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { locationToEdit = null }
                },
                onViewOnMap = {
                    viewModel.locationToFocus = LatLng(locationToEdit!!.latitude, locationToEdit!!.longitude)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        locationToEdit = null
                        onViewOnMap()
                    }
                }
            )
        }
    }
}

// --- SUB-COMPONENTS ---



@Composable
fun SavedLocationCard(
    alarm: LocationAlarm,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val isFav = alarm.isFavorite
    val cardColor = MaterialTheme.colorScheme.surface
    val borderColor = if (isFav) Color(0xFFFFD700) else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isFav) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon Bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFav) Color(0xFFFFD700).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.Place,
                    contentDescription = null,
                    tint = if (isFav) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map, // Or a radius icon
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Radius: ${alarm.radius.toInt()}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Reuse EditLocationSheet from previous answer, or paste it here if needed
@Composable
fun EditLocationSheet(
    alarm: LocationAlarm,
    onDismiss: () -> Unit,
    onSave: (String, Float) -> Unit,
    onViewOnMap: () -> Unit
) {
    var name by remember { mutableStateOf(alarm.name) }
    var radius by remember { mutableFloatStateOf(alarm.radius) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("Edit Location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Location Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Alert Radius", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${radius.toInt()}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = radius, onValueChange = { radius = it }, valueRange = 100f..2000f)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        OutlinedButton(
            onClick = onViewOnMap,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("View on Map")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(name, radius) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }
    }
}