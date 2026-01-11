package com.mobprog.lokalert.ui.ios6

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.AppPreferences
import com.mobprog.lokalert.MapsViewModel
import kotlinx.coroutines.launch

// ============================================================================
// iOS 6 SETTINGS SCREEN - Complete Skeuomorphic Design
// ============================================================================

/**
 * iOS 6 styled Settings screen with classic grouped table view
 */
@Composable
fun iOS6SettingsScreen(
    onColorChange: (Color) -> Unit = {},
    isRainbowEnabled: Boolean = false,
    onRainbowToggle: (Boolean) -> Unit = {},
    mapsViewModel: MapsViewModel? = null,
    onNavigateBack: () -> Unit = {},
    isEmbedded: Boolean = false,  // For tablet split-view
    onDetailSelected: (String?) -> Unit = {},  // Callback for tablet detail selection
    selectedDetail: String? = null  // Currently selected detail for tablet highlighting
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context) }
    
    // Collect preferences
    val cooldownEnabled by appPreferences.isCooldownEnabled.collectAsState(initial = false)
    val cooldownMinutes by appPreferences.cooldownMinutes.collectAsState(initial = 5)
    val vibrationIntensity by appPreferences.vibrationIntensity.collectAsState(initial = 2)
    val darkMode by appPreferences.darkMode.collectAsState(initial = 3)
    val appTheme by appPreferences.appTheme.collectAsState(initial = 0)
    val designLanguage by appPreferences.designLanguage.collectAsState(initial = 1)
    val overlayDismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val overlayShowEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val overlayBackgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    
    // Dialog states
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDarkModePicker by remember { mutableStateOf(false) }
    var showDesignLanguagePicker by remember { mutableStateOf(false) }
    var showDismissStylePicker by remember { mutableStateOf(false) }
    var showVibrationPicker by remember { mutableStateOf(false) }
    var showCooldownPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showBackgroundStylePicker by remember { mutableStateOf(false) }
    var showThemeNotAvailableDialog by remember { mutableStateOf(false) }
    
    // Theme not available dialog
    if (showThemeNotAvailableDialog) {
        iOS6AlertDialog(
            title = "Feature Not Available",
            message = "Dark Mode and App Theme settings are only applicable when using the Material 3 design style. Switch to Material 3 in Design Style to use these features.",
            confirmText = "OK",
            onConfirm = { showThemeNotAvailableDialog = false },
            onDismiss = { showThemeNotAvailableDialog = false }
        )
    }
    
    // Dialog Composables
    if (showAboutDialog) {
        iOS6AboutUsScreen(onDismiss = { showAboutDialog = false })
    }
    
    if (showThemePicker) {
        iOS6PickerDialog(
            title = "App Theme",
            options = listOf("Standard", "Expressive", "Ocean", "Sunset", "Forest", "Retro", "Monochrome"),
            selectedIndex = appTheme,
            onSelect = { index ->
                scope.launch { appPreferences.setAppTheme(index) }
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }
    
    if (showDarkModePicker) {
        iOS6PickerDialog(
            title = "Dark Mode",
            options = listOf("Light", "Dark", "Auto"),
            selectedIndex = when(darkMode) { 0 -> 0; 1 -> 1; else -> 2 },
            onSelect = { index ->
                scope.launch { appPreferences.setDarkMode(when(index) { 0 -> 0; 1 -> 1; else -> 3 }) }
                showDarkModePicker = false
            },
            onDismiss = { showDarkModePicker = false }
        )
    }
    
    if (showDesignLanguagePicker) {
        iOS6PickerDialog(
            title = "Design Style",
            options = listOf("Material 3", "iOS 6 Classic"),
            selectedIndex = designLanguage,
            onSelect = { index ->
                scope.launch { appPreferences.setDesignLanguage(index) }
                showDesignLanguagePicker = false
            },
            onDismiss = { showDesignLanguagePicker = false }
        )
    }
    
    if (showDismissStylePicker) {
        iOS6PickerDialog(
            title = "Dismiss Style",
            options = listOf("Slider", "Swipe Up", "Button"),
            selectedIndex = overlayDismissStyle,
            onSelect = { index ->
                scope.launch { appPreferences.setOverlayDismissStyle(index) }
                showDismissStylePicker = false
            },
            onDismiss = { showDismissStylePicker = false }
        )
    }
    
    if (showVibrationPicker) {
        iOS6PickerDialog(
            title = "Vibration Intensity",
            options = listOf("Low", "Medium", "Strong"),
            selectedIndex = vibrationIntensity,
            onSelect = { index ->
                scope.launch { appPreferences.setVibrationIntensity(index) }
                showVibrationPicker = false
            },
            onDismiss = { showVibrationPicker = false }
        )
    }
    
    if (showCooldownPicker) {
        iOS6WheelPickerDialog(
            title = "Cooldown Time",
            values = listOf(1, 2, 3, 5, 10, 15, 30),
            selectedValue = cooldownMinutes,
            suffix = " min",
            onSelect = { minutes ->
                scope.launch { 
                    appPreferences.setCooldownMinutes(minutes) 
                }
                showCooldownPicker = false
            },
            onDismiss = { showCooldownPicker = false }
        )
    }
    
    if (showBackgroundStylePicker) {
        iOS6PickerDialog(
            title = "Overlay Background",
            options = listOf("Gradient", "Solid", "Transparent"),
            selectedIndex = overlayBackgroundStyle,
            onSelect = { index ->
                scope.launch { appPreferences.setOverlayBackgroundStyle(index) }
                showBackgroundStylePicker = false
            },
            onDismiss = { showBackgroundStylePicker = false }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(iOS6TableBackground)
        ) {
            // iOS 6 Navigation Bar - only show if not embedded
            // No back button - navigation is done via tab bar with 3D cube animation
            if (!isEmbedded) {
                iOS6NavBar(
                    title = "Settings"
                    // No left action - back navigation via tab bar
                )
            }
        
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
            // NOTIFICATIONS Section
            iOS6GroupedSection(header = "NOTIFICATIONS") {
                iOS6SettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    iconColor = iOS6OrangeIcon,
                    title = "Alarm Cooldown",
                    trailing = {
                        iOS6Toggle(
                            checked = cooldownEnabled,
                            onCheckedChange = {
                                scope.launch { appPreferences.setCooldownEnabled(it) }
                            }
                        )
                    }
                )
                
                if (cooldownEnabled) {
                    iOS6Separator()
                    iOS6SettingsRow(
                        icon = Icons.Default.Timer,
                        iconColor = iOS6BlueIcon,
                        title = "Cooldown Time",
                        value = "$cooldownMinutes min",
                        showDisclosure = true,
                        onClick = { showCooldownPicker = true }
                    )
                }
            }
            
            // ALARM Section
            iOS6GroupedSection(header = "ALARM") {
                iOS6SettingsRow(
                    icon = Icons.Default.Vibration,
                    iconColor = iOS6PurpleIcon,
                    title = "Vibration Intensity",
                    value = when(vibrationIntensity) {
                        0 -> "Low"
                        1 -> "Medium"
                        else -> "Strong"
                    },
                    showDisclosure = true,
                    onClick = { showVibrationPicker = true }
                )
                
                iOS6Separator()
                
                iOS6SettingsRow(
                    icon = Icons.Default.MusicNote,
                    iconColor = iOS6RedIcon,
                    title = "Default Sound",
                    value = "Default",
                    showDisclosure = true,
                    onClick = { 
                        // Open system ringtone picker
                        val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle if activity not found
                        }
                    }
                )
            }
            
            // APPEARANCE Section
            iOS6GroupedSection(header = "APPEARANCE") {
                iOS6SettingsRow(
                    icon = Icons.Default.DarkMode,
                    iconColor = iOS6GrayIcon,
                    title = "Dark Mode",
                    value = when(darkMode) {
                        0 -> "Light"
                        1 -> "Dark"
                        else -> "Auto"
                    },
                    showDisclosure = true,
                    onClick = { showThemeNotAvailableDialog = true }
                )
                
                iOS6Separator()
                
                iOS6SettingsRow(
                    icon = Icons.Default.Palette,
                    iconColor = iOS6GreenIcon,
                    title = "App Theme",
                    value = when(appTheme) {
                        0 -> "Standard"
                        1 -> "Expressive"
                        2 -> "Ocean"
                        3 -> "Sunset"
                        4 -> "Forest"
                        5 -> "Retro"
                        else -> "Monochrome"
                    },
                    showDisclosure = true,
                    onClick = { showThemeNotAvailableDialog = true }
                )
                
                iOS6Separator()
                
                iOS6SettingsRow(
                    icon = Icons.Default.ScreenRotation,
                    iconColor = iOS6BlueIcon,
                    title = "Design Style",
                    value = if (designLanguage == 1) "iOS 6 Classic" else "Material 3",
                    showDisclosure = true,
                    onClick = { showDesignLanguagePicker = true }
                )
            }
            
            // ALARMS Section - clickable on tablet to show detail pane
            iOS6GroupedSection(
                header = "ALARMS",
                isSelected = isEmbedded && selectedDetail == "alarms",
                onClick = if (isEmbedded) {{ onDetailSelected("alarms") }} else null
            ) {
                if (!isEmbedded) {
                    // Phone mode - show all options inline
                    iOS6SettingsRow(
                        icon = Icons.Default.Layers,
                        iconColor = iOS6TealIcon,
                        title = "Dismiss Style",
                        value = when(overlayDismissStyle) {
                            0 -> "Slider"
                            1 -> "Swipe Up"
                            else -> "Button"
                        },
                        showDisclosure = true,
                        onClick = { showDismissStylePicker = true }
                    )
                    
                    iOS6Separator()
                    
                    iOS6SettingsRow(
                        icon = Icons.Default.Gradient,
                        iconColor = iOS6PinkIcon,
                        title = "Background Style",
                        value = when(overlayBackgroundStyle) {
                            0 -> "Gradient"
                            1 -> "Solid"
                            else -> "Transparent"
                        },
                        showDisclosure = true,
                        onClick = { showBackgroundStylePicker = true }
                    )
                    
                    iOS6Separator()
                    
                    iOS6SettingsRow(
                        icon = Icons.Default.EmojiEmotions,
                        iconColor = iOS6YellowIcon,
                        title = "Show Emoji",
                        trailing = {
                            iOS6Toggle(
                                checked = overlayShowEmoji,
                                onCheckedChange = { 
                                    scope.launch { appPreferences.setOverlayShowEmoji(it) }
                                }
                            )
                        }
                    )
                    
                    iOS6Separator()
                    
                    iOS6SettingsRow(
                        icon = Icons.Default.PlayCircle,
                        iconColor = iOS6BlueIcon,
                        title = "Preview Alarm",
                        showDisclosure = true,
                        onClick = {
                            // Launch alarm overlay activity like Material3 mode does
                            val intent = android.content.Intent(context, com.mobprog.lokalert.AlarmOverlayActivity::class.java).apply {
                                putExtra("ALARM_NAME", "Test Alarm")
                                putExtra("SOUND_URI", "")
                                putExtra("LATITUDE", 0.0)
                                putExtra("LONGITUDE", 0.0)
                                putExtra("IS_GRADUAL_VOLUME", false)
                                putExtra("IS_TEST_MODE", true)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                } else {
                    // Tablet mode - show summary row that opens detail pane
                    iOS6SettingsRow(
                        icon = Icons.Default.Notifications,
                        iconColor = iOS6TealIcon,
                        title = "Alarm Settings",
                        value = "Customize",
                        showDisclosure = true,
                        onClick = { onDetailSelected("alarms") }
                    )
                }
            }
            
            // ABOUT Section
            iOS6GroupedSection(header = "ABOUT") {
                iOS6SettingsRow(
                    icon = Icons.Default.Info,
                    iconColor = iOS6BlueIcon,
                    title = "Version",
                    value = "1.0.0"
                )
                
                iOS6Separator()
                
                iOS6SettingsRow(
                    icon = Icons.Default.People,
                    iconColor = iOS6OrangeIcon,
                    title = "About Us",
                    showDisclosure = true,
                    onClick = { showAboutDialog = true }
                )
            }
            
            // Footer text
            Text(
                text = "LokAlert is designed to help you never miss your stop.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = iOS6FooterText,
                    lineHeight = 18.sp
                )
            )
            }
        }
    }
}

// ============================================================================
// iOS 6 COLOR PALETTE
// ============================================================================

private val iOS6NavBarTop = Color(0xFF5C9CE5)
private val iOS6NavBarBottom = Color(0xFF2C6DB4)
private val iOS6TableBackground = Color(0xFFC5C6C8)
private val iOS6CellBackground = Color(0xFFFFFFFF)
private val iOS6SeparatorColor = Color(0xFFCED1D6)
private val iOS6HeaderText = Color(0xFF6D6D72)
private val iOS6FooterText = Color(0xFF6D6D72)
private val iOS6PrimaryText = Color(0xFF000000)
private val iOS6SecondaryText = Color(0xFF8E8E93)
private val iOS6DisclosureArrow = Color(0xFFC7C7CC)

// iOS 6 Icon Colors (Classic iOS settings style)
private val iOS6BlueIcon = Color(0xFF007AFF)
private val iOS6GreenIcon = Color(0xFF34C759)
private val iOS6OrangeIcon = Color(0xFFFF9500)
private val iOS6RedIcon = Color(0xFFFF3B30)
private val iOS6PurpleIcon = Color(0xFFAF52DE)
private val iOS6GrayIcon = Color(0xFF8E8E93)
private val iOS6TealIcon = Color(0xFF5AC8FA)
private val iOS6PinkIcon = Color(0xFFFF2D55)
private val iOS6YellowIcon = Color(0xFFFFCC00)

// ============================================================================
// iOS 6 NAVIGATION BAR
// ============================================================================

@Composable
fun iOS6NavBar(
    title: String,
    modifier: Modifier = Modifier,
    leftAction: (@Composable () -> Unit)? = null,
    rightAction: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(iOS6NavBarTop, iOS6NavBarBottom)
                )
            )
            .drawBehind {
                // Bottom edge shadow
                drawLine(
                    color = Color(0xFF1A4A7A),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                // Top highlight
                drawLine(
                    color = Color(0x40FFFFFF),
                    start = Offset(0f, 1.dp.toPx()),
                    end = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                // Glossy highlight in upper half
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x30FFFFFF), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.5f
                    )
                )
            }
    ) {
        leftAction?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) { it() }
        }
        
        // Title with embossed shadow
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                shadow = Shadow(
                    color = Color(0x80000000),
                    offset = Offset(0f, -1f),
                    blurRadius = 0f
                )
            )
        )
        
        rightAction?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) { it() }
        }
    }
}

// ============================================================================
// iOS 6 BACK BUTTON
// ============================================================================

@Composable
fun iOS6BackButton(
    onClick: () -> Unit,
    label: String = "Back"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "◀",
            style = TextStyle(
                fontSize = 10.sp,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
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
// iOS 6 GROUPED SECTION
// ============================================================================

@Composable
fun iOS6GroupedSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        // Header
        header?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOS6HeaderText,
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 1f),
                        blurRadius = 0f
                    )
                )
            )
        }
        
        // Content card with inset shadow
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) Color(0xFF007AFF).copy(alpha = 0.15f) else iOS6CellBackground)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF007AFF) else Color(0xFFB4B4B6),
                    shape = RoundedCornerShape(10.dp)
                )
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .drawBehind {
                    // Inner shadow at top
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x15000000), Color.Transparent),
                            startY = 0f,
                            endY = 3.dp.toPx()
                        )
                    )
                },
            content = content
        )
        
        // Footer
        footer?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp, end = 20.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = iOS6FooterText,
                    lineHeight = 17.sp,
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

// ============================================================================
// iOS 6 SETTINGS ROW
// ============================================================================

@Composable
fun iOS6SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = iOS6BlueIcon,
    value: String? = null,
    showDisclosure: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isPressed && onClick != null) Color(0xFF007AFF).copy(alpha = 0.5f)
                else Color.Transparent
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with rounded rect background
        icon?.let {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(iconColor, iconColor.copy(alpha = 0.8f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        // Title
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontSize = 17.sp,
                color = if (isPressed && onClick != null) Color.White else iOS6PrimaryText
            )
        )
        
        // Value or trailing content
        if (trailing != null) {
            trailing()
        } else {
            value?.let {
                Text(
                    text = it,
                    style = TextStyle(
                        fontSize = 17.sp,
                        color = if (isPressed && onClick != null) Color.White else iOS6SecondaryText
                    )
                )
            }
            
            if (showDisclosure) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "❯",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = if (isPressed && onClick != null) Color.White else iOS6DisclosureArrow
                    )
                )
            }
        }
    }
}

// ============================================================================
// iOS 6 SEPARATOR
// ============================================================================

@Composable
fun iOS6Separator(
    startIndent: androidx.compose.ui.unit.Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(iOS6SeparatorColor)
    )
}

// ============================================================================
// iOS 6 TOGGLE SWITCH
// ============================================================================

@Composable
fun iOS6Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Animated colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF4CD964) else Color(0xFFE5E5EA),
        animationSpec = tween(200),
        label = "trackColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF3CB84C) else Color(0xFFDDDDDD),
        animationSpec = tween(200),
        label = "borderColor"
    )
    
    // Animated thumb position with iOS 6 bounce
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbOffset"
    )
    
    // ON/OFF label opacity animations
    val onLabelAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(150),
        label = "onAlpha"
    )
    val offLabelAlpha by animateFloatAsState(
        targetValue = if (checked) 0f else 1f,
        animationSpec = tween(150),
        label = "offAlpha"
    )
    
    // Press feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "thumbScale"
    )
    
    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .drawBehind {
                // Inner shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x20000000), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    )
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onCheckedChange(!checked) }
    ) {
        // ON/OFF labels
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ON label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "ON",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = onLabelAlpha)
                    )
                )
            }
            
            // OFF label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "OFF",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A9A9A).copy(alpha = offLabelAlpha)
                    )
                )
            }
        }
        
        // Animated Thumb
        Box(
            modifier = Modifier
                .padding(2.dp)
                .offset(x = thumbOffset)
                .size(27.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFEEEEEE))
                    )
                )
                .border(0.5.dp, Color(0xFFCCCCCC), CircleShape)
        )
    }
}

// ============================================================================
// iOS 6 PICKER DIALOG - Now uses proper iOS 6 styling
// ============================================================================

@Composable
fun iOS6PickerDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Use the new iOS 6 styled selection dialog
    iOS6SelectionDialog(
        title = title,
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

// ============================================================================
// iOS 6 ALARMS DETAIL PANE - For tablet split view
// ============================================================================

@Composable
fun iOS6AlarmsDetailPane() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context) }
    
    // Collect preferences
    val overlayDismissStyle by appPreferences.overlayDismissStyle.collectAsState(initial = 0)
    val overlayShowEmoji by appPreferences.overlayShowEmoji.collectAsState(initial = true)
    val overlayBackgroundStyle by appPreferences.overlayBackgroundStyle.collectAsState(initial = 0)
    
    // Dialog states
    var showDismissStylePicker by remember { mutableStateOf(false) }
    var showBackgroundStylePicker by remember { mutableStateOf(false) }
    
    // Dialogs
    if (showDismissStylePicker) {
        iOS6PickerDialog(
            title = "Dismiss Style",
            options = listOf("Slider", "Swipe Up", "Button"),
            selectedIndex = overlayDismissStyle,
            onSelect = { index ->
                scope.launch { appPreferences.setOverlayDismissStyle(index) }
                showDismissStylePicker = false
            },
            onDismiss = { showDismissStylePicker = false }
        )
    }
    
    if (showBackgroundStylePicker) {
        iOS6PickerDialog(
            title = "Overlay Background",
            options = listOf("Gradient", "Solid", "Transparent"),
            selectedIndex = overlayBackgroundStyle,
            onSelect = { index ->
                scope.launch { appPreferences.setOverlayBackgroundStyle(index) }
                showBackgroundStylePicker = false
            },
            onDismiss = { showBackgroundStylePicker = false }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOS6TableBackground)
    ) {
        // iOS 6 Navigation Bar for detail pane
        iOS6NavBar(title = "Alarms")
        
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // DISMISS STYLE Section
            iOS6GroupedSection(header = "DISMISS STYLE") {
                iOS6SettingsRow(
                    icon = Icons.Default.Layers,
                    iconColor = iOS6TealIcon,
                    title = "Dismiss Style",
                    value = when(overlayDismissStyle) {
                        0 -> "Slider"
                        1 -> "Swipe Up"
                        else -> "Button"
                    },
                    showDisclosure = true,
                    onClick = { showDismissStylePicker = true }
                )
            }
            
            // APPEARANCE Section
            iOS6GroupedSection(header = "APPEARANCE") {
                iOS6SettingsRow(
                    icon = Icons.Default.Gradient,
                    iconColor = iOS6PinkIcon,
                    title = "Background Style",
                    value = when(overlayBackgroundStyle) {
                        0 -> "Gradient"
                        1 -> "Solid"
                        else -> "Transparent"
                    },
                    showDisclosure = true,
                    onClick = { showBackgroundStylePicker = true }
                )
                
                iOS6Separator()
                
                iOS6SettingsRow(
                    icon = Icons.Default.EmojiEmotions,
                    iconColor = iOS6YellowIcon,
                    title = "Show Emoji",
                    trailing = {
                        iOS6Toggle(
                            checked = overlayShowEmoji,
                            onCheckedChange = { 
                                scope.launch { appPreferences.setOverlayShowEmoji(it) }
                            }
                        )
                    }
                )
            }
            
            // PREVIEW Section
            iOS6GroupedSection(header = "TEST") {
                iOS6SettingsRow(
                    icon = Icons.Default.PlayCircle,
                    iconColor = iOS6BlueIcon,
                    title = "Preview Alarm",
                    showDisclosure = true,
                    onClick = {
                        // Launch alarm overlay activity like Material3 mode does
                        val intent = android.content.Intent(context, com.mobprog.lokalert.AlarmOverlayActivity::class.java).apply {
                            putExtra("ALARM_NAME", "Test Alarm")
                            putExtra("SOUND_URI", "")
                            putExtra("LATITUDE", 0.0)
                            putExtra("LONGITUDE", 0.0)
                            putExtra("IS_GRADUAL_VOLUME", false)
                            putExtra("IS_TEST_MODE", true)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            
            // Footer text
            Text(
                text = "Customize how the alarm appears when you arrive at your destination.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = iOS6FooterText,
                    lineHeight = 18.sp
                )
            )
        }
    }
}