package com.mobprog.lokalert.ui.ios6

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.MapsViewModel

// ============================================================================
// iOS 6 MAIN APP - Complete Alternative UI
// ============================================================================

/**
 * Main iOS 6 styled app that replaces the Material 3 version
 * when design language is set to iOS 6 Skeuomorphic
 */
@Composable
fun iOS6MainApp(
    mapsViewModel: MapsViewModel,
    recentSearches: List<String>,
    onNewSearch: (String) -> Unit,
    darkMode: Int = 0,
    onColorChange: (Color) -> Unit = {},
    isRainbowEnabled: Boolean = false,
    onRainbowToggle: (Boolean) -> Unit = {},
    onThemeTransitionRequest: (Int) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var previousNonSettingsTab by remember { mutableIntStateOf(0) }
    var showTrash by remember { mutableStateOf(false) }
    
    // Track if we're showing settings for the 3D cube effect
    val isShowingSettings = selectedTab == 2
    
    // Animate rotation for OS X Snow Leopard style 3D cube transition
    val cubeAngle by animateFloatAsState(
        targetValue = if (isShowingSettings) 90f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "cubeRotation"
    )
    
    // Check if we're on a tablet/large screen
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    // Dark background for status bar area - consistent across all screens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // Dark status bar spacer
        Spacer(modifier = Modifier.statusBarsPadding())
        
        // Main content area with 3D cube effect
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (isShowingSettings || cubeAngle > 0f) Color(0xFF1A1A1A) else Color(0xFFC5C6C8))
        ) {
            if (isTablet) {
                // iPad-style split view for tablets WITH cube animation for settings
                // No tab bar needed - sidebar has all navigation
                iOS6TabletLayout(
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        // When switching to settings, remember current non-settings tab
                        if (newTab == 2 && selectedTab != 2) {
                            previousNonSettingsTab = selectedTab
                        }
                        // When switching to a non-settings tab, update previousNonSettingsTab
                        if (newTab != 2) {
                            previousNonSettingsTab = newTab
                        }
                        selectedTab = newTab
                    },
                    mapsViewModel = mapsViewModel,
                    recentSearches = recentSearches,
                    onNewSearch = onNewSearch,
                    darkMode = darkMode,
                    onColorChange = onColorChange,
                    isRainbowEnabled = isRainbowEnabled,
                    onRainbowToggle = onRainbowToggle,
                    isShowingSettings = isShowingSettings,
                    cubeAngle = cubeAngle,
                    previousNonSettingsTab = previousNonSettingsTab,
                    onThemeTransitionRequest = onThemeTransitionRequest
                )
            } else {
                // Phone layout with 3D cube transition for settings
                
                // When NOT in settings transition (cubeAngle == 0), show normal content
                if (!isShowingSettings && cubeAngle == 0f) {
                    // Show trash screen if needed
                    if (showTrash) {
                        iOS6TrashScreen(
                            viewModel = mapsViewModel,
                            onBack = { showTrash = false }
                        )
                    } else {
                        when (selectedTab) {
                            0 -> {
                                iOS6MapsScreen(
                                    onNewSearch = onNewSearch,
                                    onDone = { selectedTab = 1 },
                                    viewModel = mapsViewModel,
                                    darkMode = darkMode
                                )
                            }
                            1 -> {
                                iOS6LocationsScreen(
                                    recentSearches = recentSearches,
                                    onViewOnMap = { selectedTab = 0 },
                                    viewModel = mapsViewModel,
                                    onRecentSearchClick = { query ->
                                        // Set the pending search query
                                        mapsViewModel.pendingSearchQuery = query
                                        // Navigate to Maps screen
                                        selectedTab = 0
                                    },
                                    onNavigateToTrash = { showTrash = true },
                                    onUseForNewAlarm = { alarm ->
                                        // Use saved location to create a NEW alarm
                                        mapsViewModel.useLocationForNewAlarm(alarm)
                                        // Navigate to Maps screen
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // During animation or when showing settings
                    // Keep map in background to prevent flickering
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0f }
                    ) {
                        iOS6MapsScreen(
                            onNewSearch = onNewSearch,
                            onDone = { selectedTab = 1 },
                            viewModel = mapsViewModel,
                            darkMode = darkMode
                        )
                    }
                    
                    // Front face (Map or Alarms) - rotates away
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = -cubeAngle
                                cameraDistance = 12f * density
                                transformOrigin = TransformOrigin(1f, 0.5f)
                                alpha = (1f - cubeAngle / 90f).coerceIn(0f, 1f)
                            }
                    ) {
                        when (previousNonSettingsTab) {
                            0 -> {
                                iOS6MapsScreen(
                                    onNewSearch = onNewSearch,
                                    onDone = { selectedTab = 1 },
                                    viewModel = mapsViewModel,
                                    darkMode = darkMode
                                )
                            }
                            1 -> {
                                iOS6LocationsScreen(
                                    recentSearches = recentSearches,
                                    onViewOnMap = { selectedTab = 0 },
                                    viewModel = mapsViewModel,
                                    onRecentSearchClick = { query ->
                                        mapsViewModel.pendingSearchQuery = query
                                        selectedTab = 0
                                    },
                                    onNavigateToTrash = { showTrash = true },
                                    onUseForNewAlarm = { alarm ->
                                        mapsViewModel.useLocationForNewAlarm(alarm)
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }
                    
                    // Back face (Settings) - rotates in
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = 90f - cubeAngle
                                cameraDistance = 12f * density
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                alpha = (cubeAngle / 90f).coerceIn(0f, 1f)
                            }
                    ) {
                        iOS6SettingsScreen(
                            onColorChange = onColorChange,
                            isRainbowEnabled = isRainbowEnabled,
                            onRainbowToggle = onRainbowToggle,
                            mapsViewModel = mapsViewModel,
                            onNavigateBack = { 
                                selectedTab = previousNonSettingsTab 
                            },
                            onThemeTransitionRequest = onThemeTransitionRequest
                        )
                    }
                }
            }
        }
        
        // iOS 6 Tab Bar - only show on phones, not tablets
        if (!isTablet) {
            iOS6TabBar(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    // When switching to settings, remember current non-settings tab
                    if (newTab == 2 && selectedTab != 2) {
                        previousNonSettingsTab = selectedTab
                    }
                    // When switching to a non-settings tab, update previousNonSettingsTab
                    if (newTab != 2) {
                        previousNonSettingsTab = newTab
                    }
                    selectedTab = newTab
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

// ============================================================================
// iOS 6 TABLET LAYOUT - iPad-style Split View with 3D Animation
// ============================================================================

@Composable
private fun iOS6TabletLayout(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    mapsViewModel: MapsViewModel,
    recentSearches: List<String>,
    onNewSearch: (String) -> Unit,
    darkMode: Int,
    onColorChange: (Color) -> Unit,
    isRainbowEnabled: Boolean,
    onRainbowToggle: (Boolean) -> Unit,
    isShowingSettings: Boolean,
    cubeAngle: Float,
    previousNonSettingsTab: Int,
    onThemeTransitionRequest: (Int) -> Unit = {}
) {
    // Track if we have a selected detail item
    var selectedDetailItem by remember { mutableStateOf<String?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    
    // When NOT in settings transition, show normal content
    if (!isShowingSettings && cubeAngle == 0f) {
        // Show trash screen if needed (full screen overlay on tablet)
        if (showTrash) {
            iOS6TrashScreen(
                viewModel = mapsViewModel,
                onBack = { showTrash = false }
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar - Locations list
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFC5C6C8))
            ) {
                // Sidebar navigation tabs
                iOS6SidebarTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { 
                        onTabSelected(it)
                        selectedDetailItem = null
                    }
                )
                
                // Sidebar content - always show alarms list
                Box(modifier = Modifier.weight(1f)) {
                    iOS6LocationsScreen(
                        recentSearches = recentSearches,
                        onViewOnMap = { },
                        viewModel = mapsViewModel,
                        onRecentSearchClick = { query ->
                            mapsViewModel.pendingSearchQuery = query
                            onTabSelected(0)
                        },
                        isEmbedded = true,
                        onNavigateToTrash = { showTrash = true },
                        onUseForNewAlarm = { alarm ->
                            mapsViewModel.useLocationForNewAlarm(alarm)
                            // On tablet, switch to Maps tab in detail view
                            onTabSelected(0)
                        }
                    )
                }
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF8E8E93))
            )
            
            // Right side - Map
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFC5C6C8))
            ) {
                iOS6MapsScreen(
                    onNewSearch = onNewSearch,
                    onDone = { onTabSelected(1) },
                    viewModel = mapsViewModel,
                    darkMode = darkMode
                )
            }
        }
        }
    } else {
        // During animation or when showing settings - apply 3D cube effect
        // Keep map in the background to prevent flickering
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
        ) {
            // Hidden map layer - keeps map service alive in background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0f }
            ) {
                iOS6MapsScreen(
                    onNewSearch = onNewSearch,
                    onDone = { onTabSelected(1) },
                    viewModel = mapsViewModel,
                    darkMode = darkMode
                )
            }
            
            // Front face (Map + Alarms split view) - rotates away
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = -cubeAngle
                        cameraDistance = 12f * density
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        alpha = (1f - cubeAngle / 90f).coerceIn(0f, 1f)
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left sidebar
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFC5C6C8))
                    ) {
                        iOS6SidebarTabs(
                            selectedTab = previousNonSettingsTab,
                            onTabSelected = { onTabSelected(it) }
                        )
                        
                        Box(modifier = Modifier.weight(1f)) {
                            iOS6LocationsScreen(
                                recentSearches = recentSearches,
                                onViewOnMap = { },
                                viewModel = mapsViewModel,
                                onRecentSearchClick = { query ->
                                    mapsViewModel.pendingSearchQuery = query
                                    onTabSelected(0)
                                },
                                isEmbedded = true,
                                onNavigateToTrash = { showTrash = true },
                                onUseForNewAlarm = { alarm ->
                                    mapsViewModel.useLocationForNewAlarm(alarm)
                                    onTabSelected(0)
                                }
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF8E8E93))
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFFC5C6C8))
                    ) {
                        iOS6MapsScreen(
                            onNewSearch = onNewSearch,
                            onDone = { onTabSelected(1) },
                            viewModel = mapsViewModel,
                            darkMode = darkMode
                        )
                    }
                }
            }
            
            // Back face (Settings) - rotates in
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 90f - cubeAngle
                        cameraDistance = 12f * density
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        alpha = (cubeAngle / 90f).coerceIn(0f, 1f)
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left sidebar - Settings list
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFC5C6C8))
                    ) {
                        iOS6SidebarTabs(
                            selectedTab = 2,
                            onTabSelected = { onTabSelected(it) }
                        )
                        
                        Box(modifier = Modifier.weight(1f)) {
                            iOS6SettingsScreen(
                                onColorChange = onColorChange,
                                isRainbowEnabled = isRainbowEnabled,
                                onRainbowToggle = onRainbowToggle,
                                mapsViewModel = mapsViewModel,
                                onNavigateBack = { onTabSelected(previousNonSettingsTab) },
                                isEmbedded = true,
                                onDetailSelected = { selectedDetailItem = it },
                                selectedDetail = selectedDetailItem,
                                onThemeTransitionRequest = onThemeTransitionRequest
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF8E8E93))
                    )
                    
                    // Right side - Detail pane based on selection
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFFC5C6C8))
                    ) {
                        when (selectedDetailItem) {
                            "alarms" -> iOS6AlarmsDetailPane()
                            else -> iOS6EmptyDetailPane()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun iOS6EmptyDetailPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFD4D4D4), Color(0xFFC5C6C8))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // iOS 6 style icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFB0B0B0), Color(0xFF8E8E93))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙️",
                    style = TextStyle(fontSize = 40.sp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Select an option",
                style = TextStyle(
                    fontSize = 17.sp,
                    color = Color(0xFF8E8E93)
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Choose a setting from the left to view details",
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF6D6D72),
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 1f),
                        blurRadius = 0f
                    )
                )
            )
        }
    }
}

@Composable
private fun iOS6SidebarTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF5C9CE5), Color(0xFF2C6DB4))
                )
            )
            .drawBehind {
                drawLine(
                    color = Color(0xFF1A4A7A),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            "Alarms" to 1,
            "Settings" to 2
        ).forEach { (title, index) ->
            val isSelected = selectedTab == index || (selectedTab == 0 && index == 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(index) }
                    .background(
                        if (isSelected) Color(0x30000000) else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                )
            }
        }
    }
}

object iOS6Theme {
    // Navigation Bar
    val navBarGradientTop = Color(0xFF5C9CE5)
    val navBarGradientBottom = Color(0xFF2C6DB4)
    val navBarBorder = Color(0xFF1A4A7A)
    
    // Tab Bar
    val tabBarGradientTop = Color(0xFF6D6D72)
    val tabBarGradientBottom = Color(0xFF353537)
    
    // Buttons
    val blueButtonTop = Color(0xFF4C98D9)
    val blueButtonBottom = Color(0xFF1E62A7)
    val blueButtonPressed = Color(0xFF194F87)
    
    val grayButtonTop = Color(0xFFFFFFFF)
    val grayButtonBottom = Color(0xFFE5E5E5)
    val grayButtonPressed = Color(0xFFCCCCCC)
    
    val greenButtonTop = Color(0xFF7CC576)
    val greenButtonBottom = Color(0xFF3F9F3A)
    
    val redButtonTop = Color(0xFFE57373)
    val redButtonBottom = Color(0xFFC62828)
    
    // Toggle Switch
    val switchOn = Color(0xFF4CD964)
    val switchOff = Color(0xFFE5E5EA)
    
    // Backgrounds
    val linenBackground = Color(0xFFC5C6C8)
    val tableBackground = Color(0xFFF2F2F7)
    val cellBackground = Color(0xFFFFFFFF)
    
    // Text Colors
    val primaryText = Color(0xFF000000)
    val secondaryText = Color(0xFF8E8E93)
    val headerText = Color(0xFF6D6D72)
    val linkBlue = Color(0xFF007AFF)
    val destructiveRed = Color(0xFFFF3B30)
    
    // Borders & Separators
    val separator = Color(0xFFCED1D6)
    val borderLight = Color(0xFFB4B4B6)
    val borderDark = Color(0xFF8E8E93)
    val disclosureArrow = Color(0xFFC7C7CC)
    
    // Icon Colors (App icon style backgrounds)
    val iconBlue = Color(0xFF007AFF)
    val iconGreen = Color(0xFF34C759)
    val iconOrange = Color(0xFFFF9500)
    val iconRed = Color(0xFFFF3B30)
    val iconPurple = Color(0xFFAF52DE)
    val iconGray = Color(0xFF8E8E93)
    val iconTeal = Color(0xFF5AC8FA)
    val iconPink = Color(0xFFFF2D55)
    val iconYellow = Color(0xFFFFCC00)
}

// ============================================================================
// ADDITIONAL BUTTON STYLES
// ============================================================================

@Composable
fun iOS6GreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .shadow(3.dp, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF3CB84C), Color(0xFF3CB84C))
                    } else {
                        listOf(iOS6Theme.greenButtonTop, iOS6Theme.greenButtonBottom)
                    }
                )
            )
            .border(1.dp, Color(0xFF2E8B2E), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Color.White,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0x60000000),
                    offset = androidx.compose.ui.geometry.Offset(0f, -1f),
                    blurRadius = 0f
                )
            )
        )
    }
}

@Composable
fun iOS6RedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .shadow(3.dp, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFFC62828), Color(0xFFC62828))
                    } else {
                        listOf(iOS6Theme.redButtonTop, iOS6Theme.redButtonBottom)
                    }
                )
            )
            .border(1.dp, Color(0xFFB71C1C), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Color.White,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0x60000000),
                    offset = androidx.compose.ui.geometry.Offset(0f, -1f),
                    blurRadius = 0f
                )
            )
        )
    }
}

@Composable
fun iOS6GrayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .shadow(3.dp, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(iOS6Theme.grayButtonPressed, iOS6Theme.grayButtonPressed)
                    } else {
                        listOf(iOS6Theme.grayButtonTop, iOS6Theme.grayButtonBottom)
                    }
                )
            )
            .border(1.dp, Color(0xFFB4B4B6), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = iOS6Theme.primaryText
            )
        )
    }
}
