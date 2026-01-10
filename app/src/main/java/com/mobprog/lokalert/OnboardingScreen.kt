package com.mobprog.lokalert

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

// ============================================================================
// RESPONSIVE SIZING UTILITIES
// ============================================================================

enum class ScreenSizeClass {
    COMPACT,    // Small phones (< 360dp width)
    MEDIUM,     // Regular phones (360-400dp width)
    EXPANDED,   // Large phones / small tablets (400-600dp width)
    LARGE       // Tablets / foldables (600dp+ width)
}

@Composable
fun rememberScreenSizeClass(): ScreenSizeClass {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 360 -> ScreenSizeClass.COMPACT
        configuration.screenWidthDp < 400 -> ScreenSizeClass.MEDIUM
        configuration.screenWidthDp < 600 -> ScreenSizeClass.EXPANDED
        else -> ScreenSizeClass.LARGE
    }
}

// Responsive dimension helper
@Composable
fun responsivePadding(
    compact: Int = 12,
    medium: Int = 16,
    expanded: Int = 20,
    large: Int = 24
): Int {
    val screenSize = rememberScreenSizeClass()
    return when (screenSize) {
        ScreenSizeClass.COMPACT -> compact
        ScreenSizeClass.MEDIUM -> medium
        ScreenSizeClass.EXPANDED -> expanded
        ScreenSizeClass.LARGE -> large
    }
}

// Responsive font size multiplier
@Composable
fun fontSizeMultiplier(): Float {
    val screenSize = rememberScreenSizeClass()
    return when (screenSize) {
        ScreenSizeClass.COMPACT -> 0.85f
        ScreenSizeClass.MEDIUM -> 0.95f
        ScreenSizeClass.EXPANDED -> 1f
        ScreenSizeClass.LARGE -> 1.1f
    }
}

// ============================================================================
// DATA CLASSES FOR ONBOARDING CONTENT
// ============================================================================

data class OnboardingContent(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val tips: List<String> = emptyList(),
    val whyNeeded: String = ""
)

// Enhanced content for each onboarding page
fun getOnboardingContent(page: Int): OnboardingContent {
    return when (page) {
        0 -> OnboardingContent(
            title = "Before We Start",
            subtitle = "Quick Privacy Note",
            description = "LokAlert processes all location data directly on your device. " +
                    "Nothing is sent to external servers — your privacy is our priority.",
            icon = Icons.Filled.Description,
            tips = listOf(
                "All data stays on your device",
                "No external servers or tracking",
                "You control your data completely"
            ),
            whyNeeded = "Please accept to continue using LokAlert."
        )
        1 -> OnboardingContent(
            title = "Welcome to LokAlert",
            subtitle = "Your Smart Location Companion",
            description = "LokAlert is a geofence app that helps you never miss your destination. " +
                    "Simply set a location on the map, and we'll alert you when you're nearby — " +
                    "perfect for bus rides, train commutes, or any journey where you might doze off!",
            icon = Icons.Filled.Home,
            tips = listOf(
                "Set alarms for any location on the map",
                "Get loud alerts even when your screen is off",
                "Customize alarm sounds and vibration intensity"
            ),
            whyNeeded = "No permissions needed for this step. Let's explore what LokAlert can do for you!"
        )
        2 -> OnboardingContent(
            title = "Stay Notified",
            subtitle = "Notifications & Alarm Permissions",
            description = "To wake you up when you're near your destination, LokAlert needs permission to " +
                    "send notifications and schedule alarms. Without these, we can't alert you!",
            icon = Icons.Filled.Notifications,
            tips = listOf(
                "Notifications let us show alerts on your screen",
                "Alarm permission ensures precise timing",
                "Your alarms will ring even in silent mode",
                "You control which sounds and vibrations to use"
            ),
            whyNeeded = "These permissions are essential — without them, LokAlert cannot alert you when you arrive at your destination."
        )
        3 -> OnboardingContent(
            title = "Know Your Location",
            subtitle = "Location Access Permission",
            description = "LokAlert uses your GPS location to detect when you're approaching your saved destinations. " +
                    "We only check your location when you have active alarms — your privacy matters to us!",
            icon = Icons.Filled.Place,
            tips = listOf(
                "We only track location when alarms are active",
                "Your location data stays on your device",
                "No data is ever sent to external servers",
                "Battery-efficient location monitoring"
            ),
            whyNeeded = "Location access is required for geofencing to work. Without it, we cannot detect when you're near your destination."
        )
        4 -> OnboardingContent(
            title = "Display Alarms Anywhere",
            subtitle = "Overlay Permission",
            description = "This permission allows LokAlert to show a full-screen alarm even when your phone is locked " +
                    "or you're using another app. It's like having a smart alarm clock that works everywhere!",
            icon = Icons.Filled.Lock,
            tips = listOf(
                "Alarms appear over any app you're using",
                "Works even when your phone is locked",
                "Perfect for napping during commutes",
                "Swipe to dismiss when you're ready"
            ),
            whyNeeded = "Without this permission, alarms may not show up on your lock screen, and you might miss your stop!"
        )
        5 -> OnboardingContent(
            title = "Keep Running Smoothly",
            subtitle = "Background Activity Permission",
            description = "Android sometimes stops apps to save battery. This permission ensures LokAlert stays active " +
                    "in the background so your alarms always trigger on time, even after hours of inactivity.",
            icon = Icons.Filled.Settings,
            tips = listOf(
                "Prevents Android from stopping LokAlert",
                "Ensures alarms work after long periods",
                "Uses minimal battery — we're efficient!",
                "Critical for reliable location monitoring"
            ),
            whyNeeded = "Without this, Android may stop LokAlert in the background, causing your alarms to not trigger."
        )
        6 -> OnboardingContent(
            title = "One More Step",
            subtitle = "Lock Screen Display",
            description = "Your device needs an extra setting to show alarms on the lock screen. " +
                    "This is a device-specific feature that requires manual enabling in your settings.",
            icon = Icons.Rounded.Notifications,
            tips = listOf(
                "Required for lock screen alarms",
                "One-time setup in device settings",
                "Follow the simple steps below",
                "Essential for full functionality"
            ),
            whyNeeded = "This device-specific setting ensures alarms can wake you up even when your phone is locked."
        )
        else -> OnboardingContent(
            title = "Welcome",
            subtitle = "",
            description = "",
            icon = Icons.Filled.Home
        )
    }
}

// ============================================================================
// ANIMATED ICON COMPONENT - SUBTLE CONTINUOUS ANIMATIONS
// ============================================================================

@Composable
fun AnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    // Determine animation type based on icon
    val animationType = when (imageVector) {
        Icons.Filled.Notifications, Icons.Rounded.Notifications -> "bell" // Bell gentle swing
        Icons.Filled.Place, Icons.Rounded.LocationOn -> "bounce" // Location pin gentle bounce
        Icons.Filled.Lock -> "pulse" // Lock subtle pulse (no rotation!)
        Icons.Filled.Settings -> "rotate" // Settings slow rotation
        else -> "pulse" // Default subtle pulse
    }
    
    // Create infinite transition for continuous but subtle animation
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
    
    when (animationType) {
        "bell" -> {
            // Bell gentle swing (much smaller range, slower)
            val rotation by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bell_swing"
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = modifier.graphicsLayer {
                    rotationZ = rotation
                },
                tint = tint
            )
        }
        "bounce" -> {
            // Location pin gentle bounce (smaller distance, slower)
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pin_bounce"
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = modifier.graphicsLayer {
                    translationY = offsetY
                },
                tint = tint
            )
        }
        "rotate" -> {
            // Settings very slow rotation (much slower than before)
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "settings_rotation"
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = modifier.graphicsLayer {
                    rotationZ = rotation
                },
                tint = tint
            )
        }
        "pulse" -> {
            // Very subtle pulse (smaller scale change, slower)
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_pulse"
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                tint = tint
            )
        }
    }
}

// ============================================================================
// MAIN ONBOARDING SCREEN
// ============================================================================

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onPreloadMap: () -> Unit = {},
    backgroundContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Detect foldable/wide screen (600dp or wider)
    val isWideScreen = configuration.screenWidthDp >= 600
    
    // ========================================================================
    // SPLASH SCREEN STATE AND ANIMATIONS
    // ========================================================================
    var showSplash by remember { mutableStateOf(true) }
    var splashPhase by remember { mutableIntStateOf(0) } // 0=initial, 1=fade in, 2=show content, 3=transition out
    
    // Screen size for responsive splash
    val screenSize = rememberScreenSizeClass()
    val fontMultiplier = fontSizeMultiplier()
    
    // Splash animation values - Logo fade in with subtle pulse
    val splashLogoAlpha by animateFloatAsState(
        targetValue = when (splashPhase) {
            0 -> 0f
            1, 2 -> 1f
            3 -> 0f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splash_logo_alpha"
    )
    
    // Logo scale with subtle breathing effect
    val splashLogoScale by animateFloatAsState(
        targetValue = when (splashPhase) {
            0 -> 0.8f
            1, 2 -> 1f
            3 -> 0.95f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splash_logo_scale"
    )
    
    val splashTextAlpha by animateFloatAsState(
        targetValue = when (splashPhase) {
            0, 1 -> 0f
            2 -> 1f
            3 -> 0f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splash_text_alpha"
    )
    
    // Simple fade out transition
    val splashFadeOut by animateFloatAsState(
        targetValue = if (splashPhase >= 3) 0f else 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splash_fade_out",
        finishedListener = {
            if (splashPhase == 3) {
                showSplash = false
            }
        }
    )
    
    // Calculate logo size for splash screen
    val splashLogoSize = when (screenSize) {
        ScreenSizeClass.COMPACT -> 120.dp
        ScreenSizeClass.MEDIUM -> 140.dp
        ScreenSizeClass.EXPANDED -> 160.dp
        ScreenSizeClass.LARGE -> 180.dp
    }
    
    // Splash screen animation sequence
    LaunchedEffect(Unit) {
        delay(100) // Small delay before starting
        // Phase 1: Fade in logo with scale
        splashPhase = 1
        delay(700)
        
        // Phase 2: Show tagline
        splashPhase = 2
        delay(1400)
        
        // Phase 3: Curtain transition out
        splashPhase = 3
    }
    
    // ========================================================================
    // SPLASH SCREEN DISPLAY - SIMPLE FADE TRANSITION
    // ========================================================================
    if (showSplash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .graphicsLayer { alpha = splashFadeOut },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated logo with subtle pulse
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "LokAlert Logo",
                    modifier = Modifier
                        .size(splashLogoSize)
                        .graphicsLayer { 
                            alpha = splashLogoAlpha
                            scaleX = splashLogoScale
                            scaleY = splashLogoScale
                        }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // App title
                Text(
                    text = "LokAlert",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = when (screenSize) {
                        ScreenSizeClass.COMPACT -> 36.sp
                        ScreenSizeClass.MEDIUM -> 40.sp
                        ScreenSizeClass.EXPANDED -> 44.sp
                        ScreenSizeClass.LARGE -> 48.sp
                    },
                    modifier = Modifier.graphicsLayer { alpha = splashLogoAlpha }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tagline with fade animation
                Text(
                    text = "Location-based Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (18 * fontMultiplier).sp,
                    modifier = Modifier.graphicsLayer { alpha = splashTextAlpha }
                )
            }
        }
        return
    }
    
    // ========================================================================
    // REST OF ONBOARDING (after splash)
    // ========================================================================
    
    val isChineseRom = remember {
        try {
            val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
            val brand = Build.BRAND?.lowercase() ?: ""
            val fingerprint = Build.FINGERPRINT?.lowercase() ?: ""
            val display = Build.DISPLAY?.lowercase() ?: ""
        
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ||
            fingerprint.contains("miui") || display.contains("miui") ||
            fingerprint.contains("hyperos") || display.contains("hyperos") ||
            manufacturer.contains("oppo") || manufacturer.contains("realme") ||
            brand.contains("oppo") || brand.contains("realme") ||
            fingerprint.contains("coloros") || display.contains("coloros") ||
            manufacturer.contains("oneplus") || brand.contains("oneplus") ||
            fingerprint.contains("oxygenos") || display.contains("oxygenos") ||
            manufacturer.contains("vivo") || brand.contains("vivo") ||
            fingerprint.contains("originos") || display.contains("originos") ||
            fingerprint.contains("funtouch") || display.contains("funtouch") ||
            manufacturer.contains("huawei") || manufacturer.contains("honor") ||
            brand.contains("huawei") || brand.contains("honor") ||
            fingerprint.contains("emui") || display.contains("emui") ||
            fingerprint.contains("harmonyos") || display.contains("harmonyos") ||
            manufacturer.contains("meizu") || brand.contains("meizu") ||
            fingerprint.contains("flyme") || display.contains("flyme") ||
            manufacturer.contains("zte") || manufacturer.contains("nubia") ||
            brand.contains("zte") || brand.contains("nubia") ||
            manufacturer.contains("lenovo") || brand.contains("lenovo") ||
            fingerprint.contains("zui") || display.contains("zui") ||
            manufacturer.contains("samsung") || brand.contains("samsung")
        } catch (e: Exception) {
            false
        }
    }
    
    val pageCount = if (isChineseRom) 7 else 6  // Added Terms & Conditions page
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    // Terms and Conditions acceptance state
    var termsAccepted by remember { mutableStateOf(false) }
    
    var page1PermissionsGranted by remember { mutableStateOf(true) }  // Welcome page - always true
    var page2PermissionsGranted by remember { mutableStateOf(false) }
    var page3PermissionsGranted by remember { mutableStateOf(false) }
    var page4PermissionsGranted by remember { mutableStateOf(false) }
    var page5PermissionsGranted by remember { mutableStateOf(false) }
    var page6PermissionsGranted by remember { mutableStateOf(false) }
    var page7PermissionsGranted by remember { mutableStateOf(!isChineseRom) }
    
    var lockscreenPermissionAcknowledged by remember { mutableStateOf(false) }

    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }
    var batteryOptimizationIgnored by remember { mutableStateOf(checkBatteryOptimization(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAlarmPermission = checkAlarmPermission(context)
                batteryOptimizationIgnored = checkBatteryOptimization(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
                notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
                locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(notificationGranted, hasAlarmPermission) {
        page3PermissionsGranted = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> notificationGranted && hasAlarmPermission
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> notificationGranted
            else -> true
        }
        onDispose {}
    }

    DisposableEffect(locationGranted) { page4PermissionsGranted = locationGranted; onDispose {} }
    DisposableEffect(hasOverlayPermission) { page5PermissionsGranted = hasOverlayPermission; onDispose {} }
    DisposableEffect(batteryOptimizationIgnored) { page6PermissionsGranted = batteryOptimizationIgnored; onDispose {} }
    DisposableEffect(lockscreenPermissionAcknowledged, isChineseRom) {
        page7PermissionsGranted = if (isChineseRom) lockscreenPermissionAcknowledged else true
        onDispose {}
    }

    val isNextButtonEnabled = when (pagerState.currentPage) {
        0 -> termsAccepted  // Terms & Conditions - must accept
        1 -> page1PermissionsGranted  // Welcome
        2 -> page3PermissionsGranted  // Notifications
        3 -> page4PermissionsGranted  // Location
        4 -> page5PermissionsGranted  // Overlay
        5 -> page6PermissionsGranted  // Battery
        6 -> page7PermissionsGranted  // Lockscreen (Chinese ROM only)
        else -> false
    }
    
    // Fade transition state
    var isTransitioning by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isTransitioning) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "fade_transition"
    )
    
    // Celebration screen state
    var showCelebration by remember { mutableStateOf(false) }
    var celebrationVisible by remember { mutableStateOf(false) }
    var startCircularReveal by remember { mutableStateOf(false) }
    
    // Celebration fade-in animation
    val celebrationAlpha by animateFloatAsState(
        targetValue = if (celebrationVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "celebration_fade"
    )
    
    // Preload map when celebration screen is about to show
    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            onPreloadMap()
            // Small delay then fade in the celebration
            delay(100)
            celebrationVisible = true
        }
    }
    
    // Show celebration screen with confetti and circular reveal
    if (showCelebration) {
        Box(modifier = Modifier.fillMaxSize()) {
            CelebrationScreen(
                onContinue = {
                    startCircularReveal = true
                },
                startReveal = startCircularReveal,
                onRevealComplete = onFinished,
                backgroundContent = backgroundContent,
                modifier = Modifier.graphicsLayer { alpha = celebrationAlpha }
            )
        }
        return
    }
    
    // Responsive bottom bar padding (screenSize already declared in splash section)
    val bottomBarHorizontalPadding = responsivePadding(compact = 12, medium = 16, expanded = 20, large = 24)
    val bottomBarVerticalPadding = responsivePadding(compact = 10, medium = 12, expanded = 14, large = 16)

    // Simple fade-in animation for onboarding content
    // Syncs with splash screen fade out (500ms splash fade out + 100ms delay)
    val onboardingFadeIn by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "onboarding_fade_in"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { 
                alpha = onboardingFadeIn
            }
    ) {
    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = bottomBarHorizontalPadding.dp, vertical = bottomBarVerticalPadding.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button (hidden on first page)
                    if (pagerState.currentPage > 0) {
                        FilledTonalButton(
                            enabled = !isTransitioning,
                            onClick = {
                                scope.launch {
                                    isTransitioning = true
                                    delay(250) // Wait for fade out
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    delay(100)
                                    isTransitioning = false
                                }
                            },
                            contentPadding = PaddingValues(
                                horizontal = if (screenSize == ScreenSizeClass.COMPACT) 14.dp else 20.dp,
                                vertical = if (screenSize == ScreenSizeClass.COMPACT) 8.dp else 12.dp
                            )
                        ) {
                            Text(
                                text = "Back",
                                fontWeight = FontWeight.Medium,
                                fontSize = if (screenSize == ScreenSizeClass.COMPACT) 13.sp else 14.sp
                            )
                        }
                    } else {
                        // Spacer to maintain layout when Back button is hidden
                        Spacer(modifier = Modifier.width(if (screenSize == ScreenSizeClass.COMPACT) 60.dp else 80.dp))
                    }
                    
                    // Page indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (screenSize == ScreenSizeClass.COMPACT) 4.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotSize = if (screenSize == ScreenSizeClass.COMPACT) 5.dp else 6.dp
                        val expandedDotWidth = if (screenSize == ScreenSizeClass.COMPACT) 16.dp else 20.dp
                        repeat(pageCount) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val width by animateDpAsState(if (isSelected) expandedDotWidth else dotSize, label = "dot_width")
                            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .height(dotSize)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }

                    // Next/Finish button
                    Button(
                        enabled = isNextButtonEnabled && !isTransitioning,
                        onClick = {
                            if (pagerState.currentPage < pageCount - 1) {
                                scope.launch {
                                    isTransitioning = true
                                    delay(250) // Wait for fade out
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    delay(100)
                                    isTransitioning = false
                                }
                            } else {
                                // Fade out then show celebration screen
                                scope.launch {
                                    isTransitioning = true
                                    delay(300) // Wait for fade out
                                    showCelebration = true
                                }
                            }
                        },
                        contentPadding = PaddingValues(
                            horizontal = if (screenSize == ScreenSizeClass.COMPACT) 16.dp else 24.dp,
                            vertical = if (screenSize == ScreenSizeClass.COMPACT) 8.dp else 12.dp
                        )
                    ) {
                        Text(
                            text = if (pagerState.currentPage == pageCount - 1) "Get Started" else "Continue",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (screenSize == ScreenSizeClass.COMPACT) 13.sp else 14.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha },
                // Add smooth page transitions
                pageSpacing = 16.dp,
                // Disable swiping on ALL pages - user must use Continue button
                userScrollEnabled = false
            ) { page ->
                val content = getOnboardingContent(page)
            
            // Permission action content for each page
            val permissionContent: @Composable ColumnScope.() -> Unit = {
                when (page) {
                    0 -> TermsAndConditionsContent(
                        isAccepted = termsAccepted,
                        onAcceptChanged = { termsAccepted = it }
                    )
                    1 -> WelcomePermissionContent()
                    2 -> NotificationPermissionContent(
                        notificationGranted = notificationGranted,
                        hasAlarmPermission = hasAlarmPermission,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onRequestAlarmPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = "package:${context.packageName}".toUri()
                                })
                            }
                        }
                    )
                    3 -> LocationPermissionContent(
                        locationGranted = locationGranted,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                    4 -> OverlayPermissionContent(
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlayPermission = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    )
                    5 -> BatteryPermissionContent(
                        isBatteryOptimizationIgnored = batteryOptimizationIgnored,
                        onRequestBatteryOptimization = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                    6 -> if (isChineseRom) {
                        LockscreenPermissionContent(
                            isAcknowledged = lockscreenPermissionAcknowledged,
                            onAcknowledge = { lockscreenPermissionAcknowledged = true },
                            onOpenSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        )
                    }
                }
            }
            
            if (isWideScreen) {
                // Foldable/Wide screen layout: Left-Right partition
                FoldableOnboardingPage(
                    content = content,
                    permissionContent = permissionContent
                )
            } else {
                // Regular phone layout: Stacked
                RegularOnboardingPage(
                    content = content,
                    permissionContent = permissionContent
                )
            }
        }
        }
    }
    } // End of entrance animation Box
}

// ============================================================================
// FOLDABLE DEVICE LAYOUT (Left-Right Partition)
// ============================================================================

@Composable
fun FoldableOnboardingPage(
    content: OnboardingContent,
    permissionContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // LEFT SIDE: Logo, App Name, Text Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header: Logo and App Name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "LokAlert Logo",
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LokAlert",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Location-Based Alarms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Page Title
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Subtitle (if present)
            if (content.subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content.subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 26.sp
            )
            
            // Tips Card (if present)
            if (content.tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "What you should know:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        content.tips.forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Why Needed Info Card (if present)
            if (content.whyNeeded.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "ℹ️",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = content.whyNeeded,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Push content to top, allow scrolling if needed
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Vertical Divider between columns
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        
        // RIGHT SIDE: Illustration and Permission Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Page Icon/Illustration
            // Animated Icon Container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedIcon(
                    imageVector = content.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Permission Actions Content
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                permissionContent()
            }
        }
    }
}

// ============================================================================
// REGULAR PHONE LAYOUT (Stacked) - Responsive
// ============================================================================

@Composable
fun RegularOnboardingPage(
    content: OnboardingContent,
    permissionContent: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val screenSize = rememberScreenSizeClass()
    val fontMultiplier = fontSizeMultiplier()
    
    // Responsive dimensions
    val horizontalPadding = responsivePadding(compact = 12, medium = 16, expanded = 20, large = 24)
    val verticalSpacing = responsivePadding(compact = 8, medium = 12, expanded = 16, large = 20)
    val iconSize = when (screenSize) {
        ScreenSizeClass.COMPACT -> 70.dp
        ScreenSizeClass.MEDIUM -> 80.dp
        ScreenSizeClass.EXPANDED -> 90.dp
        ScreenSizeClass.LARGE -> 100.dp
    }
    val innerIconSize = when (screenSize) {
        ScreenSizeClass.COMPACT -> 36.dp
        ScreenSizeClass.MEDIUM -> 42.dp
        ScreenSizeClass.EXPANDED -> 48.dp
        ScreenSizeClass.LARGE -> 52.dp
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = horizontalPadding.dp, vertical = 8.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo and App Name at top - compact on small screens
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (verticalSpacing / 2).dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "LokAlert Logo",
                modifier = Modifier.size(if (screenSize == ScreenSizeClass.COMPACT) 40.dp else 48.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LokAlert",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = (16 * fontMultiplier).sp
            )
        }
        
        Spacer(modifier = Modifier.height(verticalSpacing.dp))
        
        // Illustration/Icon
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedIcon(
                imageVector = content.icon,
                contentDescription = null,
                modifier = Modifier.size(innerIconSize),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(verticalSpacing.dp))
        
        // Title
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * fontMultiplier).sp
        )
        
        if (content.subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content.subtitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontSize = (14 * fontMultiplier).sp
            )
        }
        
        Spacer(modifier = Modifier.height((verticalSpacing / 2).dp))
        
        // Description
        Text(
            text = content.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = (20 * fontMultiplier).sp,
            fontSize = (14 * fontMultiplier).sp
        )
        
        // Tips section - more compact
        if (content.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(verticalSpacing.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "What you should know:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (13 * fontMultiplier).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    content.tips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 1.dp),
                            lineHeight = (16 * fontMultiplier).sp,
                            fontSize = (12 * fontMultiplier).sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(verticalSpacing.dp))
        
        // Permission content
        permissionContent()
        
        Spacer(modifier = Modifier.height(verticalSpacing.dp))
    }
}

// ============================================================================
// PERMISSION CONTENT COMPOSABLES
// ============================================================================

@Composable
fun ColumnScope.TermsAndConditionsContent(
    isAccepted: Boolean,
    onAcceptChanged: (Boolean) -> Unit
) {
    val screenSize = rememberScreenSizeClass()
    val fontMultiplier = fontSizeMultiplier()
    
    // Privacy highlights card
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (screenSize == ScreenSizeClass.COMPACT) 12.dp else 16.dp,
                vertical = if (screenSize == ScreenSizeClass.COMPACT) 10.dp else 14.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔒", fontSize = (18 * fontMultiplier).sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Privacy Matters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (14 * fontMultiplier).sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "• All location data is processed on your device only\n" +
                       "• We never send your data to external servers\n" +
                       "• No tracking, no analytics, no data collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = (18 * fontMultiplier).sp,
                fontSize = (12 * fontMultiplier).sp
            )
        }
    }
    
    Spacer(modifier = Modifier.height(if (screenSize == ScreenSizeClass.COMPACT) 8.dp else 12.dp))
    
    // Disclaimer card
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (screenSize == ScreenSizeClass.COMPACT) 12.dp else 16.dp,
                vertical = if (screenSize == ScreenSizeClass.COMPACT) 10.dp else 14.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = (16 * fontMultiplier).sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Please Note",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (13 * fontMultiplier).sp
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "LokAlert is a student project. While we've done our best to make it reliable, " +
                       "we cannot guarantee the alarm will always trigger perfectly. " +
                       "Please don't rely solely on this app for critical situations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = (16 * fontMultiplier).sp,
                fontSize = (11 * fontMultiplier).sp
            )
        }
    }
    
    Spacer(modifier = Modifier.height(if (screenSize == ScreenSizeClass.COMPACT) 10.dp else 14.dp))
    
    // Acceptance checkbox - more compact
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAccepted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAccepted,
                onCheckedChange = onAcceptChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "I understand and agree to continue",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isAccepted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isAccepted) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (13 * fontMultiplier).sp
            )
        }
    }
}

@Composable
fun ColumnScope.WelcomePermissionContent() {
    Text(
        text = "Ready to get started? Let's set up a few permissions to make LokAlert work perfectly for you.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ColumnScope.NotificationPermissionContent(
    notificationGranted: Boolean,
    hasAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestAlarmPermission: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionRequestCard(
            title = "Notifications",
            description = "Receive alerts when you're near your destination",
            isGranted = notificationGranted,
            onGrantClick = onRequestNotificationPermission
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PermissionRequestCard(
            title = "Exact Alarms",
            description = "Schedule precise location-based alarms",
            isGranted = hasAlarmPermission,
            onGrantClick = onRequestAlarmPermission
        )
    }
}

@Composable
fun ColumnScope.LocationPermissionContent(
    locationGranted: Boolean,
    onRequestLocationPermission: () -> Unit
) {
    PermissionRequestCard(
        title = "Location Access",
        description = "Required to detect when you're near saved places",
        isGranted = locationGranted,
        onGrantClick = onRequestLocationPermission
    )
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔒", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Your privacy is protected. Location data never leaves your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun ColumnScope.OverlayPermissionContent(
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    val isOnePlus = Build.MANUFACTURER.lowercase().contains("oneplus")
    
    PermissionRequestCard(
        title = "Display Over Other Apps",
        description = "Show alarms on top of any screen",
        isGranted = hasOverlayPermission,
        onGrantClick = onRequestOverlayPermission
    )
    
    if (isOnePlus) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "OnePlus Users",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Also enable 'Display on lockscreen' in Settings > Apps > LokAlert > Permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ColumnScope.BatteryPermissionContent(
    isBatteryOptimizationIgnored: Boolean,
    onRequestBatteryOptimization: () -> Unit
) {
    val isXiaomi = Build.MANUFACTURER.lowercase().let { 
        it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") 
    }
    
    PermissionRequestCard(
        title = "Unrestricted Battery",
        description = "Keep LokAlert running in the background",
        isGranted = isBatteryOptimizationIgnored,
        onGrantClick = onRequestBatteryOptimization
    )
    
    if (isXiaomi) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "MIUI/HyperOS Users",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Also enable 'Autostart' and set Battery saver to 'No restrictions' for best results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// Helper function to get Chinese ROM-specific settings intent
fun getChineseRomSettingsIntent(context: Context, packageName: String): Intent {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    
    return when {
        // Xiaomi/Redmi/POCO - MIUI/HyperOS
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
        brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
            try {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", packageName)
                }
            } catch (e: Exception) {
                // Fallback to app details
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // OPPO/Realme - ColorOS
        manufacturer.contains("oppo") || manufacturer.contains("realme") ||
        brand.contains("oppo") || brand.contains("realme") -> {
            try {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.floatwindow.FloatWindowListActivity"
                    )
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // OnePlus - OxygenOS
        manufacturer.contains("oneplus") || brand.contains("oneplus") -> {
            try {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // Vivo - OriginOS/FuntouchOS
        manufacturer.contains("vivo") || brand.contains("vivo") -> {
            try {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // Huawei/Honor - EMUI/HarmonyOS
        manufacturer.contains("huawei") || manufacturer.contains("honor") ||
        brand.contains("huawei") || brand.contains("honor") -> {
            try {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.permissionmanager.ui.MainActivity"
                    )
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // Samsung - One UI
        manufacturer.contains("samsung") || brand.contains("samsung") -> {
            try {
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // Meizu - Flyme
        manufacturer.contains("meizu") || brand.contains("meizu") -> {
            try {
                Intent("com.meizu.safe.security.SHOW_APPSEC").apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra("packageName", packageName)
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
        }
        
        // Default fallback
        else -> {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
    }
}

@Composable
fun ColumnScope.LockscreenPermissionContent(
    isAcknowledged: Boolean,
    onAcknowledge: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showNotEnabledError by remember { mutableStateOf(false) }
    var hasAttemptedValidation by remember { mutableStateOf(false) }
    
    val deviceName = when {
        Build.MANUFACTURER.lowercase().let { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") } -> "MIUI/HyperOS"
        Build.MANUFACTURER.lowercase().let { it.contains("oppo") || it.contains("realme") } -> "ColorOS/Realme UI"
        Build.MANUFACTURER.lowercase().contains("oneplus") -> "OxygenOS"
        Build.MANUFACTURER.lowercase().contains("vivo") -> "OriginOS"
        Build.MANUFACTURER.lowercase().let { it.contains("huawei") || it.contains("honor") } -> "EMUI/HarmonyOS"
        Build.MANUFACTURER.lowercase().contains("samsung") -> "One UI"
        else -> "your device"
    }
    
    // Get ROM-specific steps
    val steps = when {
        Build.MANUFACTURER.lowercase().let { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") } -> listOf(
            "Tap 'Open Settings' below",
            "Find and tap 'Other permissions'",
            "Enable 'Display on lock screen'",
            "Also enable 'Autostart' in MIUI settings",
            "Return here and tap 'I've Enabled It'"
        )
        Build.MANUFACTURER.lowercase().let { it.contains("oppo") || it.contains("realme") } -> listOf(
            "Tap 'Open Settings' below",
            "Go to 'Permission management'",
            "Enable 'Display on lock screen'",
            "Enable 'Auto-start' in App management",
            "Return here and tap 'I've Enabled It'"
        )
        Build.MANUFACTURER.lowercase().contains("oneplus") -> listOf(
            "Tap 'Open Settings' below",
            "Go to App info > Permissions",
            "Enable 'Display on lock screen'",
            "Also disable 'Background restrictions'",
            "Return here and tap 'I've Enabled It'"
        )
        Build.MANUFACTURER.lowercase().contains("vivo") -> listOf(
            "Tap 'Open Settings' below",
            "Go to 'Permissions' > 'System permissions'",
            "Enable 'Lock screen display'",
            "Enable 'Background pop-up'",
            "Return here and tap 'I've Enabled It'"
        )
        Build.MANUFACTURER.lowercase().let { it.contains("huawei") || it.contains("honor") } -> listOf(
            "Tap 'Open Settings' below",
            "Go to 'Permissions' > 'Special access'",
            "Enable 'Lock screen notifications'",
            "Also check 'App launch' settings",
            "Return here and tap 'I've Enabled It'"
        )
        Build.MANUFACTURER.lowercase().contains("samsung") -> listOf(
            "Tap 'Open Settings' below",
            "Go to 'Battery and device care'",
            "Tap 'Battery' > 'Background usage limits'",
            "Add LokAlert to 'Never sleeping apps'",
            "Return here and tap 'I've Enabled It'"
        )
        else -> listOf(
            "Tap 'Open Settings' below",
            "Find and tap 'Permissions'",
            "Enable 'Display on lock screen'",
            "Return here and tap 'I've Enabled It'"
        )
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Steps for $deviceName:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Error message if user claims enabled but hasn't actually done it
    if (showNotEnabledError) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Please complete the steps above first. If you've already done this, tap the button again to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    FilledTonalButton(
        onClick = {
            // Try ROM-specific settings first, fallback to app details
            try {
                val romIntent = getChineseRomSettingsIntent(context, context.packageName)
                romIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(romIntent)
            } catch (e: Exception) {
                // Fallback to standard app details
                onOpenSettings()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Settings")
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Button(
        onClick = {
            if (!hasAttemptedValidation) {
                // First attempt - show warning that they should have enabled it
                hasAttemptedValidation = true
                showNotEnabledError = true
            } else {
                // Second attempt - trust the user and proceed
                showNotEnabledError = false
                onAcknowledge()
            }
        },
        enabled = !isAcknowledged,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isAcknowledged) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isAcknowledged) "Done" else "I've Enabled It")
    }
}

// ============================================================================
// PERMISSION REQUEST CARD
// ============================================================================

@Composable
fun PermissionRequestCard(
    title: String,
    description: String = "",
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onGrantClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Allow")
                }
            }
        }
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

fun checkAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else true
}

fun checkBatteryOptimization(context: Context): Boolean {
    return (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .isIgnoringBatteryOptimizations(context.packageName)
}

// ============================================================================
// CELEBRATION SCREEN WITH CONFETTI AND CIRCULAR REVEAL
// ============================================================================

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float,
    val shape: Int // 0 = rectangle, 1 = circle, 2 = triangle
)

@Composable
fun CelebrationScreen(
    onContinue: () -> Unit,
    startReveal: Boolean,
    onRevealComplete: () -> Unit,
    backgroundContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Confetti particles
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var showButton by remember { mutableStateOf(false) }
    
    // Confetti colors
    val confettiColors = listOf(
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFFFFE66D), // Yellow
        Color(0xFF95E1D3), // Mint
        Color(0xFFF38181), // Coral
        Color(0xFFAA96DA), // Purple
        Color(0xFF7BD3EA), // Sky blue
        Color(0xFFFFB347), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Cyan
        Color(0xFFCDDC39)  // Lime
    )
    
    // Initialize confetti particles
    LaunchedEffect(Unit) {
        // Create confetti particles from multiple points (like party poppers)
        val newParticles = mutableListOf<ConfettiParticle>()
        
        // Left party popper (top-left)
        repeat(50) {
            newParticles.add(
                ConfettiParticle(
                    x = screenWidth * 0.15f,
                    y = screenHeight * 0.25f,
                    velocityX = Random.nextFloat() * 10f + 3f,
                    velocityY = Random.nextFloat() * -15f - 5f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 12f - 6f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 14f + 6f,
                    shape = Random.nextInt(3)
                )
            )
        }
        
        // Right party popper (top-right)
        repeat(50) {
            newParticles.add(
                ConfettiParticle(
                    x = screenWidth * 0.85f,
                    y = screenHeight * 0.25f,
                    velocityX = Random.nextFloat() * -10f - 3f,
                    velocityY = Random.nextFloat() * -15f - 5f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 12f - 6f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 14f + 6f,
                    shape = Random.nextInt(3)
                )
            )
        }
        
        // Center top burst
        repeat(40) {
            newParticles.add(
                ConfettiParticle(
                    x = screenWidth * 0.5f,
                    y = screenHeight * 0.1f,
                    velocityX = Random.nextFloat() * 16f - 8f,
                    velocityY = Random.nextFloat() * 8f + 2f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 15f - 7.5f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 12f + 5f,
                    shape = Random.nextInt(3)
                )
            )
        }
        
        // Left side cascade
        repeat(30) {
            newParticles.add(
                ConfettiParticle(
                    x = screenWidth * 0.05f,
                    y = screenHeight * Random.nextFloat() * 0.5f,
                    velocityX = Random.nextFloat() * 6f + 2f,
                    velocityY = Random.nextFloat() * 4f - 2f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 8f - 4f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 10f + 4f,
                    shape = Random.nextInt(3)
                )
            )
        }
        
        // Right side cascade
        repeat(30) {
            newParticles.add(
                ConfettiParticle(
                    x = screenWidth * 0.95f,
                    y = screenHeight * Random.nextFloat() * 0.5f,
                    velocityX = Random.nextFloat() * -6f - 2f,
                    velocityY = Random.nextFloat() * 4f - 2f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 8f - 4f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 10f + 4f,
                    shape = Random.nextInt(3)
                )
            )
        }
        
        particles = newParticles
        
        // Show button after a short delay
        delay(800)
        showButton = true
    }
    
    // Animate confetti
    LaunchedEffect(particles) {
        while (particles.isNotEmpty()) {
            delay(16) // ~60 FPS
            particles = particles.map { particle ->
                particle.copy(
                    x = particle.x + particle.velocityX,
                    y = particle.y + particle.velocityY + 2f, // gravity
                    rotation = particle.rotation + particle.rotationSpeed
                )
            }.filter { it.y < screenHeight + 50 } // Remove particles that fall off screen
        }
    }
    
    // Circular reveal animation
    val revealProgress = remember { Animatable(0f) }
    
    LaunchedEffect(startReveal) {
        if (startReveal) {
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                )
            )
            // Call onRevealComplete after animation
            onRevealComplete()
        }
    }
    
    // Calculate max radius for circular reveal (diagonal of screen from center to corner)
    // Multiply by 1.1 to ensure it fully covers the corners
    val maxRadius = sqrt(screenWidth * screenWidth + screenHeight * screenHeight) / 2 * 1.1f
    
    // Fade alpha for background content that appears behind the reveal
    // Only fade in when the reveal animation actually starts (not during celebration fade-in)
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (startReveal && revealProgress.value > 0.05f) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "background_fade"
    )
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background content (main app) - rendered behind the celebration screen
        // This is what gets revealed by the circular animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backgroundAlpha }
        ) {
            backgroundContent()
        }
        
        // Celebration screen content with circular reveal effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // Draw a circle that "erases" the celebration content to reveal background
                    if (startReveal) {
                        drawCircle(
                            color = Color.Transparent,
                            radius = revealProgress.value * maxRadius,
                            center = Offset(size.width / 2, size.height / 2),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
        ) {
            // White background for celebration screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
            
            // Confetti canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { particle ->
                    drawContext.canvas.save()
                    drawContext.canvas.translate(particle.x, particle.y)
                    drawContext.canvas.rotate(particle.rotation)
                    
                    when (particle.shape) {
                        0 -> { // Rectangle
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(-particle.size / 2, -particle.size / 4),
                                size = Size(particle.size, particle.size / 2)
                            )
                        }
                        1 -> { // Circle
                            drawCircle(
                                color = particle.color,
                                radius = particle.size / 2,
                                center = Offset.Zero
                            )
                        }
                        2 -> { // Small square
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(-particle.size / 3, -particle.size / 3),
                                size = Size(particle.size / 1.5f, particle.size / 1.5f)
                            )
                        }
                    }
                    
                    drawContext.canvas.restore()
                }
            }
            
            // Main content - Responsive
            val screenSize = rememberScreenSizeClass()
            val fontMultiplier = fontSizeMultiplier()
            val contentPadding = responsivePadding(compact = 16, medium = 20, expanded = 28, large = 32)
            val iconSize = when (screenSize) {
                ScreenSizeClass.COMPACT -> 70.dp
                ScreenSizeClass.MEDIUM -> 80.dp
                ScreenSizeClass.EXPANDED -> 90.dp
                ScreenSizeClass.LARGE -> 100.dp
            }
            val innerIconSize = when (screenSize) {
                ScreenSizeClass.COMPACT -> 42.dp
                ScreenSizeClass.MEDIUM -> 48.dp
                ScreenSizeClass.EXPANDED -> 54.dp
                ScreenSizeClass.LARGE -> 60.dp
            }
            val emojiSize = when (screenSize) {
                ScreenSizeClass.COMPACT -> 44.sp
                ScreenSizeClass.MEDIUM -> 52.sp
                ScreenSizeClass.EXPANDED -> 58.sp
                ScreenSizeClass.LARGE -> 64.sp
            }
            val verticalSpacing = when (screenSize) {
                ScreenSizeClass.COMPACT -> 12.dp
                ScreenSizeClass.MEDIUM -> 16.dp
                ScreenSizeClass.EXPANDED -> 20.dp
                ScreenSizeClass.LARGE -> 24.dp
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Party popper emojis
                Text(
                    text = "🎉",
                    fontSize = emojiSize
                )
                
                Spacer(modifier = Modifier.height(verticalSpacing))
                
                // Checkmark icon
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .background(
                            Color(0xFF4CAF50),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(innerIconSize),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height((verticalSpacing.value * 1.3f).dp))
                
                // Title
                Text(
                    text = "You're All Set!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF333333),
                    fontSize = (26 * fontMultiplier).sp
                )
                
                Spacer(modifier = Modifier.height((verticalSpacing.value * 0.5f).dp))
                
                // Subtitle
                Text(
                    text = "All permissions granted!\nLokAlert is ready to go.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF666666),
                    lineHeight = (22 * fontMultiplier).sp,
                    fontSize = (15 * fontMultiplier).sp
                )
                
                Spacer(modifier = Modifier.height((verticalSpacing.value * 2f).dp))
                
                // Continue button with fade-in animation
                androidx.compose.animation.AnimatedVisibility(
                    visible = showButton,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = tween(500)
                    )
                ) {
                    val buttonHeight = when (screenSize) {
                        ScreenSizeClass.COMPACT -> 48.dp
                        ScreenSizeClass.MEDIUM -> 52.dp
                        else -> 56.dp
                    }
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        shape = RoundedCornerShape(buttonHeight / 2),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Let's Go! 🚀",
                            fontSize = (16 * fontMultiplier).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
