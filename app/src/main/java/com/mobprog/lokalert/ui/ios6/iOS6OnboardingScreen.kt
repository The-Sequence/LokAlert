package com.mobprog.lokalert.ui.ios6

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

// ============================================================================
// iOS 6 ONBOARDING SCREEN - Complete Skeuomorphic Onboarding
// ============================================================================

@Composable
fun iOS6OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 7 // Privacy, Welcome, Notifications, Location, Overlay, Battery, Celebration
    
    // Permission states
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasLocationPermission by remember { mutableStateOf(checkLocationPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasBatteryOptimization by remember { mutableStateOf(checkBatteryOptimization(context)) }
    var privacyAccepted by remember { mutableStateOf(false) }
    
    // Lifecycle observer to refresh permissions when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                hasLocationPermission = checkLocationPermission(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasBatteryOptimization = checkBatteryOptimization(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Permission launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }
    
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOS6OnboardingColors.linenBackground)
            .statusBarsPadding()
    ) {
        // iOS 6 Navigation Bar
        iOS6OnboardingNavBar(
            title = getPageTitle(currentPage),
            showSkip = currentPage > 0 && currentPage < totalPages - 1,
            onSkip = onComplete
        )
        
        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentPage) {
                0 -> iOS6PrivacyPage(
                    accepted = privacyAccepted,
                    onAcceptChange = { privacyAccepted = it }
                )
                1 -> iOS6WelcomePage()
                2 -> iOS6PermissionPage(
                    title = "Stay Alerted",
                    description = "Enable notifications to receive location alerts even when the app is in the background.",
                    icon = Icons.Default.Notifications,
                    iconEmoji = "🔔",
                    isGranted = hasNotificationPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            hasNotificationPermission = true
                        }
                    }
                )
                3 -> iOS6PermissionPage(
                    title = "Know Your Location",
                    description = "LokAlert needs location access to alert you when you arrive at your destinations.",
                    icon = Icons.Default.LocationOn,
                    iconEmoji = "📍",
                    isGranted = hasLocationPermission,
                    onRequestPermission = {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
                4 -> iOS6PermissionPage(
                    title = "Never Miss Your Stop",
                    description = "Allow overlay permission to show alerts over other apps, even when your screen is locked.",
                    icon = Icons.Default.Layers,
                    iconEmoji = "⚡",
                    isGranted = hasOverlayPermission,
                    onRequestPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
                5 -> iOS6PermissionPage(
                    title = "Reliable Tracking",
                    description = "Disable battery optimization to ensure accurate location tracking in the background.",
                    icon = Icons.Default.BatteryChargingFull,
                    iconEmoji = "🔋",
                    isGranted = hasBatteryOptimization,
                    onRequestPermission = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                )
                6 -> iOS6CelebrationPage()
            }
        }
        
        // Bottom navigation
        iOS6OnboardingBottomBar(
            currentPage = currentPage,
            totalPages = totalPages,
            canProceed = when (currentPage) {
                0 -> privacyAccepted
                else -> true
            },
            onBack = { if (currentPage > 0) currentPage-- },
            onNext = {
                if (currentPage < totalPages - 1) {
                    currentPage++
                } else {
                    onComplete()
                }
            },
            isLastPage = currentPage == totalPages - 1
        )
    }
}

// ============================================================================
// iOS 6 ONBOARDING COLORS
// ============================================================================

private object iOS6OnboardingColors {
    val linenBackground = Color(0xFFC5C6C8)
    val navBarTop = Color(0xFF5C9CE5)
    val navBarBottom = Color(0xFF2C6DB4)
    val cardBackground = Color(0xFFFFFFFF)
    val primaryText = Color(0xFF000000)
    val secondaryText = Color(0xFF8E8E93)
    val greenIcon = Color(0xFF34C759)
    val blueIcon = Color(0xFF007AFF)
}

// ============================================================================
// iOS 6 ONBOARDING NAV BAR
// ============================================================================

@Composable
private fun iOS6OnboardingNavBar(
    title: String,
    showSkip: Boolean,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        iOS6OnboardingColors.navBarTop,
                        iOS6OnboardingColors.navBarBottom
                    )
                )
            )
            .drawBehind {
                // Bottom border
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
            }
    ) {
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
        
        if (showSkip) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                iOS6NavBarTextButton(
                    text = "Skip",
                    onClick = onSkip
                )
            }
        }
    }
}

@Composable
private fun iOS6NavBarTextButton(
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
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
// iOS 6 PRIVACY PAGE
// ============================================================================

@Composable
private fun iOS6PrivacyPage(
    accepted: Boolean,
    onAcceptChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF5AC8FA), Color(0xFF007AFF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📋",
                fontSize = 40.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Before We Start",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iOS6OnboardingColors.primaryText
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Quick Privacy Note",
            style = TextStyle(
                fontSize = 17.sp,
                color = iOS6OnboardingColors.secondaryText
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Privacy card
        iOS6OnboardingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LokAlert processes all location data directly on your device. Nothing is sent to external servers — your privacy is our priority.",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = iOS6OnboardingColors.primaryText,
                        lineHeight = 22.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy points
                listOf(
                    "All data stays on your device",
                    "No external servers or tracking",
                    "You control your data completely"
                ).forEach { point ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iOS6OnboardingColors.greenIcon),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = point,
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = iOS6OnboardingColors.primaryText
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Acceptance checkbox
        iOS6OnboardingCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAcceptChange(!accepted) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iOS6Checkbox(
                    checked = accepted,
                    onCheckedChange = onAcceptChange
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "I accept the privacy terms and conditions",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = iOS6OnboardingColors.primaryText
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// iOS 6 WELCOME PAGE
// ============================================================================

@Composable
private fun iOS6WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))
                    )
                )
                .border(1.dp, Color(0xFF2A5A8F), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📍",
                fontSize = 64.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to LokAlert",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iOS6OnboardingColors.primaryText
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Never miss your stop again. Set location-based alarms and get notified when you arrive at your destination.",
            style = TextStyle(
                fontSize = 17.sp,
                color = iOS6OnboardingColors.secondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Feature highlights
        iOS6OnboardingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                iOS6FeatureRow(
                    emoji = "🗺️",
                    title = "Set Alarms Anywhere",
                    description = "Tap on any location on the map"
                )
                iOS6FeatureDivider()
                iOS6FeatureRow(
                    emoji = "🔔",
                    title = "Smart Alerts",
                    description = "Get notified when you're nearby"
                )
                iOS6FeatureDivider()
                iOS6FeatureRow(
                    emoji = "😴",
                    title = "Peace of Mind",
                    description = "Perfect for commutes when you might doze off"
                )
            }
        }
    }
}

@Composable
private fun iOS6FeatureRow(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOS6OnboardingColors.primaryText
                )
            )
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = iOS6OnboardingColors.secondaryText
                )
            )
        }
    }
}

@Composable
private fun iOS6FeatureDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp)
            .height(1.dp)
            .background(Color(0xFFCED1D6))
    )
}

// ============================================================================
// iOS 6 PERMISSION PAGE
// ============================================================================

@Composable
private fun iOS6PermissionPage(
    title: String,
    description: String,
    icon: ImageVector,
    iconEmoji: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isGranted) {
                            listOf(Color(0xFF7CC576), Color(0xFF3F9F3A))
                        } else {
                            listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                        }
                    )
                )
                .border(
                    1.dp,
                    if (isGranted) Color(0xFF2E8B2E) else Color(0xFFB4B4B6),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Text(
                    text = iconEmoji,
                    fontSize = 48.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = title,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = iOS6OnboardingColors.primaryText
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = TextStyle(
                fontSize = 17.sp,
                color = iOS6OnboardingColors.secondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Permission button
        if (isGranted) {
            iOS6OnboardingCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = iOS6OnboardingColors.greenIcon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Permission Granted",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = iOS6OnboardingColors.greenIcon
                        )
                    )
                }
            }
        } else {
            iOS6PrimaryButton(
                text = "Enable",
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }
    }
}

// ============================================================================
// iOS 6 CELEBRATION PAGE
// ============================================================================

@Composable
private fun iOS6CelebrationPage() {
    // Confetti animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration icon
        Text(
            text = "",
            fontSize = (80 * bounceScale).sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "You're All Set!",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iOS6OnboardingColors.primaryText
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "LokAlert is ready to help you never miss your stop. Start by setting your first location alarm!",
            style = TextStyle(
                fontSize = 17.sp,
                color = iOS6OnboardingColors.secondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Quick tips card
        iOS6OnboardingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Tips",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOS6OnboardingColors.primaryText
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                listOf(
                    "Tap anywhere on the map to set an alarm",
                    "Adjust the radius to control when you're alerted",
                    "Favorite locations you visit often"
                ).forEachIndexed { index, tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iOS6OnboardingColors.blueIcon),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tip,
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = iOS6OnboardingColors.primaryText,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// iOS 6 BOTTOM BAR
// ============================================================================

@Composable
private fun iOS6OnboardingBottomBar(
    currentPage: Int,
    totalPages: Int,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isLastPage: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                )
            )
            .drawBehind {
                // Top border
                drawLine(
                    color = Color(0xFFB4B4B6),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page dots
        iOS6PageDots(
            pageCount = totalPages,
            currentPage = currentPage
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentPage > 0) {
                iOS6SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
            }
            
            iOS6PrimaryButton(
                text = if (isLastPage) "Get Started" else "Continue",
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================================
// iOS 6 REUSABLE COMPONENTS
// ============================================================================

@Composable
private fun iOS6OnboardingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(iOS6OnboardingColors.cardBackground)
            .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
            .drawBehind {
                // Inner shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x10000000), Color.Transparent),
                        startY = 0f,
                        endY = 4.dp.toPx()
                    )
                )
            }
    ) {
        content()
    }
}

@Composable
private fun iOS6Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (checked) {
                        listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))
                    } else {
                        listOf(Color.White, Color(0xFFF0F0F0))
                    }
                )
            )
            .border(
                1.dp,
                if (checked) Color(0xFF2A5A8F) else Color(0xFFB4B4B6),
                RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun iOS6PageDots(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            Color(0xFF007AFF)
                        } else {
                            Color(0xFFB4B4B6)
                        }
                    )
                    .then(
                        if (index == currentPage) {
                            Modifier.border(1.dp, Color(0xFF005ECB), CircleShape)
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun iOS6PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .shadow(if (enabled) 3.dp else 0.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = when {
                        !enabled -> listOf(Color(0xFFCCCCCC), Color(0xFFBBBBBB))
                        isPressed -> listOf(Color(0xFF194F87), Color(0xFF194F87))
                        else -> listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))
                    }
                )
            )
            .border(
                1.dp,
                if (enabled) Color(0xFF2A5A8F) else Color(0xFFAAAAAA),
                RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
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
                shadow = if (enabled) Shadow(
                    color = Color(0x60000000),
                    offset = Offset(0f, -1f),
                    blurRadius = 0f
                ) else null
            )
        )
    }
}

@Composable
fun iOS6SecondaryButton(
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
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF000000)
            )
        )
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

private fun getPageTitle(page: Int): String {
    return when (page) {
        0 -> "Privacy"
        1 -> "Welcome"
        2 -> "Notifications"
        3 -> "Location"
        4 -> "Overlay"
        5 -> "Battery"
        6 -> "Ready!"
        else -> "Setup"
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
