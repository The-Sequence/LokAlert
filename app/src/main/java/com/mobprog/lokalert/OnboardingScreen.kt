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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Track permissions for each page
    var page1PermissionsGranted by remember { mutableStateOf(true) }
    var page2PermissionsGranted by remember { mutableStateOf(false) }
    var page3PermissionsGranted by remember { mutableStateOf(false) }
    var page4PermissionsGranted by remember { mutableStateOf(false) }
    var page5PermissionsGranted by remember { mutableStateOf(false) }

    // Mutable state for permission status that updates immediately
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    var batteryOptimizationIgnored by remember { 
        mutableStateOf(checkBatteryOptimization(context)) 
    }
    
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Update notification state immediately
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Location permissions launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Update location state immediately
        locationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Listen to lifecycle changes to refresh permission status
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAlarmPermission = checkAlarmPermission(context)
                batteryOptimizationIgnored = checkBatteryOptimization(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
                // Also refresh other permissions on resume
                notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                locationGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Update page permission status
    DisposableEffect(notificationGranted, hasAlarmPermission) {
        page2PermissionsGranted = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> notificationGranted && hasAlarmPermission
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> notificationGranted
            else -> true
        }
        onDispose {}
    }

    DisposableEffect(locationGranted) {
        page3PermissionsGranted = locationGranted
        onDispose {}
    }

    DisposableEffect(hasOverlayPermission) {
        page4PermissionsGranted = hasOverlayPermission
        onDispose {}
    }

    DisposableEffect(batteryOptimizationIgnored) {
        page5PermissionsGranted = batteryOptimizationIgnored
        onDispose {}
    }

    // Determine if Next button should be enabled
    val isNextButtonEnabled = when (pagerState.currentPage) {
        0 -> page1PermissionsGranted
        1 -> page2PermissionsGranted
        2 -> page3PermissionsGranted
        3 -> page4PermissionsGranted
        4 -> page5PermissionsGranted
        else -> false
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pager Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "dot_width")
                        val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Next / Finish Button
                Button(
                    enabled = isNextButtonEnabled,
                    onClick = {
                        if (pagerState.currentPage < pageCount - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinished()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text(if (pagerState.currentPage == pageCount - 1) "Get Started" else "Next")
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Welcome to LokAlert",
                    description = "Your intelligent companion for location-based alerts and timing.",
                    icon = Icons.Filled.Home,
                    content = {}
                )
                1 -> NotificationsAndAlarmsPage(
                    notificationGranted = notificationGranted,
                    hasAlarmPermission = hasAlarmPermission,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestAlarmPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                2 -> LocationPage(
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
                3 -> OverlayPermissionPage(
                    hasOverlayPermission = hasOverlayPermission,
                    onRequestOverlayPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
                4 -> BatteryOptimizationPage(
                    isBatteryOptimizationIgnored = batteryOptimizationIgnored,
                    onRequestBatteryOptimization = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        content()
    }
}

@Composable
fun NotificationsAndAlarmsPage(
    notificationGranted: Boolean,
    hasAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestAlarmPermission: () -> Unit
) {
    OnboardingPage(
        title = "Stay on Track",
        description = "We need permissions to ring alarms and send you important notifications.",
        icon = Icons.Rounded.Notifications
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRequestCard(
                title = "Notifications",
                isGranted = notificationGranted,
                onGrantClick = onRequestNotificationPermission
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionRequestCard(
                title = "Exact Alarms",
                isGranted = hasAlarmPermission,
                onGrantClick = onRequestAlarmPermission
            )
        }
    }
}

@Composable
fun LocationPage(
    locationGranted: Boolean,
    onRequestLocationPermission: () -> Unit
) {
    OnboardingPage(
        title = "Enable Location",
        description = "To show local alerts and map features, we need access to your location.",
        icon = Icons.Rounded.LocationOn
    ) {
        PermissionRequestCard(
            title = "Location Access",
            isGranted = locationGranted,
            onGrantClick = onRequestLocationPermission
        )
    }
}

@Composable
fun OverlayPermissionPage(
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    val deviceManufacturer = Build.MANUFACTURER.lowercase()
    val isOnePlus = deviceManufacturer.contains("oneplus")
    
    OnboardingPage(
        title = "Display Over Other Apps",
        description = "Allow LokAlert to show alarm overlay on your screen, including lock screen.",
        icon = Icons.Rounded.Notifications
    ) {
        PermissionRequestCard(
            title = "Overlay Permission",
            isGranted = hasOverlayPermission,
            onGrantClick = onRequestOverlayPermission
        )
        
        if (isOnePlus) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "OnePlus/ColorOS Users:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You may need to enable 'Display on lockscreen' permission in Settings > Apps > LokAlert > Permissions for alarms to work on the lock screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationPage(
    isBatteryOptimizationIgnored: Boolean,
    onRequestBatteryOptimization: () -> Unit
) {
    val deviceManufacturer = Build.MANUFACTURER.lowercase()
    val isXiaomi = deviceManufacturer.contains("xiaomi") || deviceManufacturer.contains("redmi")
    
    OnboardingPage(
        title = "Battery Optimization",
        description = "Disable battery optimization to ensure LokAlert runs reliably in the background.",
        icon = Icons.Filled.FavoriteBorder
    ) {
        PermissionRequestCard(
            title = "Disable Battery Optimization",
            isGranted = isBatteryOptimizationIgnored,
            onGrantClick = onRequestBatteryOptimization
        )
        
        if (isXiaomi) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Xiaomi/MIUI Users:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "For reliable alarms, please also:\n" +
                               "1. Go to Settings > Apps > Manage apps > LokAlert\n" +
                               "2. Enable 'Autostart'\n" +
                               "3. Set Battery saver to 'No restrictions'\n" +
                               "4. Lock the app in recent apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestCard(
    title: String,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!isGranted) {
                FilledTonalButton(onClick = onGrantClick) {
                    Text("Allow")
                }
            }
        }
    }
}

fun checkAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

fun checkBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}

fun getDeviceManufacturer(): String {
    return Build.MANUFACTURER
}
