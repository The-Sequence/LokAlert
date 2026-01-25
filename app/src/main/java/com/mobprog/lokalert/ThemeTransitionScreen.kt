package com.mobprog.lokalert

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Theme transition state manager
 */
enum class ThemeTransitionState {
    IDLE,           // No transition happening
    LOADING,        // Showing loading screen
    COMPLETE        // Transition complete - show restart button
}

/**
 * Data class to hold theme transition info
 */
data class ThemeTransitionInfo(
    val state: ThemeTransitionState = ThemeTransitionState.IDLE,
    val targetDesignLanguage: Int = 0, // 0 = Material 3, 1 = iOS 6
    val isTransitioningToiOS: Boolean = false
)

/**
 * Main theme transition screen with fade in/fade out animation
 * Shows a loading animation then a restart button
 * Theme is ONLY applied when user presses Restart to avoid crash
 */
@Composable
fun ThemeTransitionScreen(
    isTransitioningToiOS: Boolean,
    targetDesignLanguage: Int,
    onApplyThemeAndRestart: () -> Unit
) {
    var loadingPhase by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    
    // Fade in animation
    var fadeIn by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (fadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "screen_fade"
    )
    
    // Start fade in immediately
    LaunchedEffect(Unit) {
        fadeIn = true
    }
    
    // Loading messages based on direction
    val loadingMessages = if (isTransitioningToiOS) {
        listOf(
            "Preparing iOS 6 experience...",
            "Loading classic textures...",
            "Applying skeuomorphic design...",
            "Ready to switch!"
        )
    } else {
        listOf(
            "Preparing Material 3 experience...",
            "Loading modern components...",
            "Applying dynamic colors...",
            "Ready to switch!"
        )
    }
    
    // Progress animation
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val loadingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_rotation"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Progress bar animation
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress_bar"
    )
    
    // Loading phase progression - NO theme change here
    LaunchedEffect(Unit) {
        delay(500)
        loadingPhase = 1
        progress = 0.33f
        
        delay(600)
        loadingPhase = 2
        progress = 0.66f
        
        delay(700)
        loadingPhase = 3
        progress = 1f
        
        delay(300)
        isComplete = true
    }
    
    // Full screen with fade
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha }
            .background(
                if (isTransitioningToiOS) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C3E50),
                            Color(0xFF1A252F)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E),
                            Color(0xFF0D47A1)
                        )
                    )
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated icon (hide when complete)
            if (!isComplete) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .rotate(loadingRotation)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = if (isTransitioningToiOS) {
                                        listOf(
                                            Color(0xFF5C9CE5),
                                            Color(0xFF2C6DB4),
                                            Color(0xFF5C9CE5).copy(alpha = 0.3f),
                                            Color(0xFF5C9CE5)
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFF6200EE),
                                            Color(0xFF03DAC5),
                                            Color(0xFF6200EE).copy(alpha = 0.3f),
                                            Color(0xFF6200EE)
                                        )
                                    }
                                )
                            )
                    )
                    
                    // Inner icon container
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTransitioningToiOS) {
                            Text(
                                text = "📍",
                                fontSize = 48.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                // Show checkmark when complete
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 60.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = if (isComplete) {
                    "Ready to Switch!"
                } else {
                    if (isTransitioningToiOS) "Switching to iOS 6" else "Switching to Material 3"
                },
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Loading message or completion message
            Text(
                text = if (isComplete) {
                    "Press the button below to apply the new theme"
                } else {
                    loadingMessages.getOrElse(loadingPhase) { loadingMessages.last() }
                },
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress bar (hide when complete)
            if (!isComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isTransitioningToiOS) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF5C9CE5),
                                            Color(0xFF4CD964)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF6200EE),
                                            Color(0xFF03DAC5)
                                        )
                                    )
                                }
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Show restart button when complete
            if (isComplete) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        // Apply theme and restart
                        onApplyThemeAndRestart()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (isTransitioningToiOS) Color(0xFF2C6DB4) else Color(0xFF6200EE)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply & Restart",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTransitioningToiOS) Color(0xFF2C6DB4) else Color(0xFF6200EE)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Info message
            if (!isComplete) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "The app icon will also change to match your selected theme",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS 6 styled theme transition screen
 * Theme is ONLY applied when user presses Restart to avoid crash
 */
@Composable
fun iOS6ThemeTransitionScreen(
    isTransitioningToMaterial: Boolean,
    targetDesignLanguage: Int,
    onApplyThemeAndRestart: () -> Unit
) {
    var loadingPhase by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    
    // Fade in animation
    var fadeIn by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (fadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "ios6_screen_fade"
    )
    
    LaunchedEffect(Unit) {
        fadeIn = true
    }
    
    // Loading messages
    val loadingMessages = if (isTransitioningToMaterial) {
        listOf(
            "Preparing Material 3...",
            "Loading modern UI...",
            "Applying dynamic colors...",
            "Ready to switch!"
        )
    } else {
        listOf(
            "Preparing Classic theme...",
            "Loading textures...",
            "Applying glossy effects...",
            "Ready to switch!"
        )
    }
    
    // Spinner animation
    val infiniteTransition = rememberInfiniteTransition(label = "ios6_loading")
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner"
    )
    
    // Progress
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "ios6_progress"
    )
    
    // Loading phase progression - NO theme change here
    LaunchedEffect(Unit) {
        delay(500)
        loadingPhase = 1
        progress = 0.33f
        
        delay(600)
        loadingPhase = 2
        progress = 0.66f
        
        delay(700)
        loadingPhase = 3
        progress = 1f
        
        delay(300)
        isComplete = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha }
            .background(Color(0xFFC5C6C8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // iOS 6 style spinner or checkmark
            if (!isComplete) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier
                        .size(50.dp)
                        .rotate(spinnerRotation)
                    ) {
                        val segments = 12
                        val segmentAngle = 360f / segments
                        
                        for (i in 0 until segments) {
                            val alpha = 1f - (i.toFloat() / segments) * 0.7f
                            drawLine(
                                color = Color(0xFF6D6D72).copy(alpha = alpha),
                                start = Offset(
                                    size.width / 2 + (size.width * 0.25f) * kotlin.math.cos(Math.toRadians((i * segmentAngle).toDouble())).toFloat(),
                                    size.height / 2 + (size.height * 0.25f) * kotlin.math.sin(Math.toRadians((i * segmentAngle).toDouble())).toFloat()
                                ),
                                end = Offset(
                                    size.width / 2 + (size.width * 0.45f) * kotlin.math.cos(Math.toRadians((i * segmentAngle).toDouble())).toFloat(),
                                    size.height / 2 + (size.height * 0.45f) * kotlin.math.sin(Math.toRadians((i * segmentAngle).toDouble())).toFloat()
                                ),
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            } else {
                // Checkmark
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CD964)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 48.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title with iOS 6 text shadow
            Text(
                text = if (isComplete) "Ready to Switch!" else "Switching Theme",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A4A4A),
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 1f),
                        blurRadius = 0f
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isComplete) "Press the button below to apply the new theme" else loadingMessages.getOrElse(loadingPhase) { loadingMessages.last() },
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF6D6D72),
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 1f),
                        blurRadius = 0f
                    )
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // iOS 6 style progress bar (hide when complete)
            if (!isComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFD1D1D6),
                                    Color(0xFFE5E5EA)
                                )
                            )
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF5C9CE5),
                                        Color(0xFF2C6DB4)
                                    )
                                )
                            )
                    )
                }
            }
            
            // Show restart button when complete
            if (isComplete) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // iOS 6 style blue button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF5C9CE5),
                                    Color(0xFF2C6DB4)
                                )
                            )
                        )
                        .clickable {
                            // Apply theme and restart
                            onApplyThemeAndRestart()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply & Restart",
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
    }
}
