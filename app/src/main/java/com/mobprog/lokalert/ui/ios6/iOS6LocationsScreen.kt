package com.mobprog.lokalert.ui.ios6

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.LocationAlarm
import com.mobprog.lokalert.MapsViewModel
import kotlinx.coroutines.launch

// ============================================================================
// iOS 6 LOCATIONS SCREEN - Classic iPhone Style
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iOS6LocationsScreen(
    recentSearches: List<String>,
    onViewOnMap: () -> Unit,
    viewModel: MapsViewModel,
    onRecentSearchClick: (String) -> Unit = {},
    isEmbedded: Boolean = false,  // For tablet split-view
    onNavigateToTrash: () -> Unit = {},
    onUseForNewAlarm: (LocationAlarm) -> Unit = {}
) {
    val savedLocations by viewModel.savedLocations.collectAsState()
    var showFavoritesOnly by remember { mutableStateOf(false) }
    val isSelectionMode = viewModel.isSelectionMode
    val selectedAlarmIds = viewModel.selectedAlarmIds
    
    val displayedLocations = remember(savedLocations, showFavoritesOnly) {
        if (showFavoritesOnly) {
            savedLocations.filter { it.isFavorite }
        } else {
            savedLocations
        }
    }
    
    // Edit sheet state
    var locationToEdit by remember { mutableStateOf<LocationAlarm?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(iOS6LinenBackground)
        ) {
            // iOS 6 Navigation Bar - only show if not embedded
            if (!isEmbedded) {
                if (isSelectionMode) {
                    // Selection mode nav bar
                    iOS6NavBar(
                        title = "${selectedAlarmIds.size} Selected",
                        leftAction = {
                            iOS6NavBarButton(
                                text = "Cancel",
                                onClick = { viewModel.clearSelection() }
                            )
                        },
                        rightAction = {
                            iOS6NavBarButton(
                                text = "Select All",
                                onClick = { viewModel.selectAllAlarms() }
                            )
                        }
                    )
                } else {
                    iOS6NavBar(
                        title = "My Alarms",
                        leftAction = {
                            iOS6NavBarButton(
                                text = "Trash",
                                onClick = onNavigateToTrash
                            )
                        },
                        rightAction = {
                            iOS6NavBarButton(
                                text = "Edit",
                                onClick = { viewModel.toggleSelectionMode() }
                            )
                        }
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp, 
                    bottom = if (isSelectionMode && selectedAlarmIds.isNotEmpty()) 100.dp else 16.dp
                )
            ) {
                // For embedded mode (tablet), show a trash button at top
                if (isEmbedded) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MY ALARMS",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D6D72),
                                    shadow = Shadow(
                                        color = Color.White,
                                        offset = Offset(0f, 1f),
                                        blurRadius = 0f
                                    )
                                )
                            )
                            iOS6NavBarButton(
                                text = "Trash",
                                onClick = onNavigateToTrash
                            )
                        }
                    }
                }
                
                // Recent Searches Section (hide in selection mode)
                if (recentSearches.isNotEmpty() && !isSelectionMode) {
                    item {
                        iOS6GroupedSection(header = "RECENT SEARCHES") {
                            recentSearches.forEachIndexed { index, search ->
                                iOS6SearchHistoryRow(
                                    searchText = search,
                                    onClick = { onRecentSearchClick(search) }
                                )
                                if (index < recentSearches.size - 1) {
                                    iOS6Separator(startIndent = 44.dp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Filter buttons (hide in selection mode)
                if (!isSelectionMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            iOS6FilterButton(
                                text = "All Alarms",
                                isSelected = !showFavoritesOnly,
                                onClick = { showFavoritesOnly = false }
                            )
                            iOS6FilterButton(
                                text = "Favorites",
                                isSelected = showFavoritesOnly,
                                onClick = { showFavoritesOnly = true }
                            )
                        }
                    }
                }
                
                // Saved Alarms Section
                item {
                    if (displayedLocations.isEmpty()) {
                        iOS6EmptyStateCard()
                    }
                }
                
                if (displayedLocations.isNotEmpty()) {
                    item {
                        // Section header outside the grouped content
                        Text(
                            text = "SAVED ALARMS",
                            modifier = Modifier.padding(start = 30.dp, top = 16.dp, bottom = 6.dp),
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D6D72),
                                shadow = Shadow(
                                    color = Color.White,
                                    offset = Offset(0f, 1f),
                                    blurRadius = 0f
                                )
                            )
                        )
                    }
                    
                    // Grouped card for all alarms
                    item {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .shadow(2.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
                        ) {
                            displayedLocations.forEachIndexed { index, location ->
                                iOS6AlarmRow(
                                    alarm = location,
                                    onEdit = { locationToEdit = it },
                                    onToggle = { viewModel.toggleAlarmEnabled(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteLocation(it) },
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedAlarmIds.contains(location.id),
                                    onLongPress = {
                                        if (!isSelectionMode) {
                                            viewModel.toggleSelectionMode()
                                        }
                                        viewModel.toggleAlarmSelection(location.id)
                                    },
                                    onSelect = { viewModel.toggleAlarmSelection(location.id) }
                                )
                                if (index < displayedLocations.size - 1) {
                                    iOS6Separator(startIndent = 72.dp)
                                }
                            }
                        }
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        
        // iOS 6 styled floating delete button when in selection mode
        if (isSelectionMode && selectedAlarmIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                iOS6DeleteSelectedButton(
                    count = selectedAlarmIds.size,
                    onClick = { viewModel.deleteSelectedAlarms() }
                )
            }
        }
    }
    
    // Bottom sheet for editing
    if (locationToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = { locationToEdit = null },
            sheetState = sheetState,
            containerColor = iOS6LinenBackground
        ) {
            iOS6EditLocationSheet(
                alarm = locationToEdit!!,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { locationToEdit = null }
                },
                onSave = { name, activeDays, soundUri, isGradualVolume ->
                    viewModel.updateAlarmAllDetails(locationToEdit!!, name, activeDays, soundUri, isGradualVolume)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { locationToEdit = null }
                },
                onUseForNewAlarm = {
                    // Use this saved location as a starting point for a NEW alarm
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        val alarm = locationToEdit!!
                        locationToEdit = null
                        onUseForNewAlarm(alarm)
                    }
                }
            )
        }
    }
}

// ============================================================================
// iOS 6 COLOR CONSTANTS
// ============================================================================

private val iOS6LinenBackground = Color(0xFFC5C6C8)
private val iOS6BlueButtonTop = Color(0xFF4C98D9)
private val iOS6BlueButtonBottom = Color(0xFF1E62A7)

// ============================================================================
// iOS 6 SEARCH HISTORY ROW
// ============================================================================

@Composable
private fun iOS6SearchHistoryRow(
    searchText: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPressed) Color(0xFF007AFF).copy(alpha = 0.5f) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = if (isPressed) Color.White else Color(0xFF8E8E93),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = searchText,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontSize = 17.sp,
                color = if (isPressed) Color.White else Color.Black
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "❯",
            style = TextStyle(
                fontSize = 18.sp,
                color = if (isPressed) Color.White else Color(0xFFC7C7CC)
            )
        )
    }
}

// ============================================================================
// iOS 6 ALARM ROW
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun iOS6AlarmRow(
    alarm: LocationAlarm,
    onEdit: (LocationAlarm) -> Unit,
    onToggle: (LocationAlarm) -> Unit,
    onToggleFavorite: (LocationAlarm) -> Unit,
    onDelete: (LocationAlarm) -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onSelect: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF007AFF).copy(alpha = 0.3f)
            isPressed -> Color(0xFF007AFF).copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "row_bg"
    )
    
    // Show iOS 6 styled delete confirmation
    if (showDeleteDialog) {
        iOS6AlertDialog(
            title = "Delete Alarm",
            message = "Are you sure you want to delete \"${alarm.name}\"? It will be moved to trash.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = { onDelete(alarm) },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isSelectionMode) {
                        onSelect()
                    } else {
                        onEdit(alarm)
                    }
                },
                onLongClick = onLongPress
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            // Selection checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF007AFF), Color(0xFF0056B3))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF0056B3) else Color(0xFFB4B4B6),
                        CircleShape
                    )
                    .clickable { onSelect() },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            // Delete button (red minus)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
        }
        
        // Location pin icon with colored background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (alarm.isEnabled) {
                            listOf(Color(0xFF4CD964), Color(0xFF3CB84C))
                        } else {
                            listOf(Color(0xFF8E8E93), Color(0xFF6D6D72))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Alarm details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = alarm.name,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPressed || isSelected) Color.Black else Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (alarm.isFavorite) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${alarm.radius.toInt()}m radius • ${formatActiveDays(alarm.activeDays)}",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )
            )
        }
        
        // Toggle switch (hide in selection mode)
        if (!isSelectionMode) {
            iOS6Toggle(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle(alarm) }
            )
        }
    }
}

private fun formatActiveDays(activeDays: Set<Int>): String {
    if (activeDays.size == 7) return "Every day"
    if (activeDays.isEmpty()) return "No days"
    
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return activeDays.sorted().map { dayNames[it] }.joinToString(", ")
}

// ============================================================================
// iOS 6 DELETE SELECTED BUTTON
// ============================================================================

@Composable
fun iOS6DeleteSelectedButton(
    count: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFFC62828), Color(0xFFC62828))
                    } else {
                        listOf(Color(0xFFFF3B30), Color(0xFFD32F2F))
                    }
                )
            )
            .border(1.dp, Color(0xFFB71C1C), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete $count alarm${if (count > 1) "s" else ""}",
                style = TextStyle(
                    fontSize = 17.sp,
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
// iOS 6 FILTER BUTTON
// ============================================================================

@Composable
fun iOS6FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = when {
                        isSelected -> listOf(iOS6BlueButtonTop, iOS6BlueButtonBottom)
                        isPressed -> listOf(Color(0xFFCCCCCC), Color(0xFFCCCCCC))
                        else -> listOf(Color(0xFFFFFFFF), Color(0xFFE5E5E5))
                    }
                )
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF2A5A8F) else Color(0xFFB4B4B6),
                RoundedCornerShape(5.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color.Black,
                shadow = if (isSelected) Shadow(
                    color = Color(0x60000000),
                    offset = Offset(0f, -1f),
                    blurRadius = 0f
                ) else null
            )
        )
    }
}

// ============================================================================
// iOS 6 EMPTY STATE CARD
// ============================================================================

@Composable
fun iOS6EmptyStateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 16.dp)
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with linen-style background
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Alarms Set",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap on the map to set your first location alarm",
            style = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                lineHeight = 20.sp
            )
        )
    }
}

// ============================================================================
// iOS 6 BLUE BUTTON
// ============================================================================

@Composable
fun iOS6BlueButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF194F87), Color(0xFF194F87))
                    } else {
                        listOf(iOS6BlueButtonTop, iOS6BlueButtonBottom)
                    }
                )
            )
            .border(1.dp, Color(0xFF2A5A8F), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
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

// ============================================================================
// iOS 6 NAV BAR BUTTON
// ============================================================================

@Composable
fun iOS6NavBarButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF194F87), Color(0xFF194F87))
                    } else {
                        listOf(Color(0xFF5A90C8), Color(0xFF3D6A9F))
                    }
                )
            )
            .border(1.dp, Color(0xFF2A5A8F), RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
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

// ============================================================================
// iOS 6 EDIT LOCATION SHEET
// ============================================================================

@Composable
fun iOS6EditLocationSheet(
    alarm: LocationAlarm,
    onDismiss: () -> Unit,
    onSave: (String, Set<Int>, String, Boolean) -> Unit,
    onUseForNewAlarm: () -> Unit = {}
) {
    var name by remember { mutableStateOf(alarm.name) }
    var activeDays by remember { mutableStateOf(alarm.activeDays) }
    var soundUri by remember { mutableStateOf(alarm.soundUri) }
    var isGradualVolume by remember { mutableStateOf(alarm.isGradualVolume) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            iOS6NavBarButton(text = "Cancel", onClick = onDismiss)
            
            Text(
                text = "Edit Alarm",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            
            iOS6NavBarButton(
                text = "Save",
                onClick = { onSave(name, activeDays, soundUri, isGradualVolume) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Name field
        iOS6GroupedSection(header = "NAME") {
            iOS6TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Alarm name"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Active days
        iOS6GroupedSection(header = "ACTIVE DAYS") {
            iOS6DaySelector(
                selectedDays = activeDays,
                onDaysChanged = { activeDays = it }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Options
        iOS6GroupedSection(header = "OPTIONS") {
            iOS6SettingsRow(
                title = "Gradual Volume",
                trailing = {
                    iOS6Toggle(
                        checked = isGradualVolume,
                        onCheckedChange = { isGradualVolume = it }
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Use for New Alarm button
        iOS6GroupedSection(header = "ACTIONS") {
            iOS6SettingsRow(
                title = "Use Location for New Alarm",
                trailing = {
                    Text(
                        text = "❯",
                        style = TextStyle(
                            fontSize = 18.sp,
                            color = Color(0xFFC7C7CC)
                        )
                    )
                },
                onClick = onUseForNewAlarm
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// iOS 6 TEXT FIELD
// ============================================================================

@Composable
fun iOS6TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .drawBehind {
                // Inner shadow at top
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x15000000), Color.Transparent),
                        startY = 0f,
                        endY = 4.dp.toPx()
                    )
                )
            }
            .padding(horizontal = 15.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = TextStyle(
                    fontSize = 17.sp,
                    color = Color(0xFFC7C7CC)
                )
            )
        }
        
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 17.sp,
                color = Color.Black
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// ============================================================================
// iOS 6 DAY SELECTOR
// ============================================================================

@Composable
fun iOS6DaySelector(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayNames.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index)
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(iOS6BlueButtonTop, iOS6BlueButtonBottom)
                            } else {
                                listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                            }
                        )
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF2A5A8F) else Color(0xFFB4B4B6),
                        CircleShape
                    )
                    .clickable {
                        onDaysChanged(
                            if (isSelected) selectedDays - index else selectedDays + index
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        shadow = if (isSelected) Shadow(
                            color = Color(0x60000000),
                            offset = Offset(0f, -1f),
                            blurRadius = 0f
                        ) else null
                    )
                )
            }
        }
    }
}
