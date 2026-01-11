package com.mobprog.lokalert.ui.ios6

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

// ============================================================================
// iOS 6 ALARM OVERLAY SCREEN - Skeuomorphic Alarm Display
// ============================================================================

/**
 * iOS 6 styled alarm overlay screen with classic skeuomorphic design
 */
@Composable
fun iOS6AlarmOverlayScreen(
    alarmName: String,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    dismissStyle: Int = 0, // 0=Slider, 1=SwipeUp, 2=Button
    showEmoji: Boolean = true,
    emoji: String = "🚨",
    onDismiss: () -> Unit,
    onSnooze: () -> Unit = {}
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    // Glowing ring animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        // Radial glow effect behind icon
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Alarm icon with iOS 6 style
            if (showEmoji) {
                Box(
                    modifier = Modifier
                        .size((100 * pulseScale).dp)
                        .alpha(pulseAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B).copy(alpha = glowAlpha * 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // Icon container with iOS 6 gradient
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFE53935)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFFFAB91), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 48.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // "YOU'VE ARRIVED" text with classic iOS style
            Text(
                text = "YOU'VE ARRIVED",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B),
                    letterSpacing = 3.sp
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Location name with embossed style
            Text(
                text = alarmName,
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color(0x80000000),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Coordinates (if available)
            if (latitude != 0.0 || longitude != 0.0) {
                Text(
                    text = "%.4f, %.4f".format(latitude, longitude),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Dismiss control based on style
            when (dismissStyle) {
                0 -> iOS6SliderDismiss(onDismiss = onDismiss)
                1 -> iOS6SwipeUpDismiss(onDismiss = onDismiss)
                else -> iOS6ButtonDismiss(onDismiss = onDismiss)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ============================================================================
// iOS 6 SLIDER DISMISS (Classic "Slide to Unlock" Style)
// ============================================================================

@Composable
private fun iOS6SliderDismiss(
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val trackWidth = with(density) { (configuration.screenWidthDp.dp - 80.dp).toPx() }
    val thumbWidth = with(density) { 60.dp.toPx() }
    val maxOffset = trackWidth - thumbWidth
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    
    // Shimmer animation for hint text
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            delay(200)
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Track background with iOS 6 inset style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(2.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A2A3E),
                            Color(0xFF3A3A4E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF4A4A5E), RoundedCornerShape(30.dp))
                .drawBehind {
                    // Inner shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x40000000), Color.Transparent),
                            startY = 0f,
                            endY = 10.dp.toPx()
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Hint text with shimmer
            Text(
                text = "slide to dismiss",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            )
            
            // Chevron arrows
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 80.dp)
                    .alpha(0.3f)
            ) {
                repeat(3) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Draggable thumb with iOS 6 glossy style
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(60.dp)
                .align(Alignment.CenterStart)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFE0E0E0)
                        )
                    )
                )
                .border(1.dp, Color(0xFFCCCCCC), CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, maxOffset)
                        if (offsetX >= maxOffset * 0.9f) {
                            isDismissed = true
                        }
                    },
                    onDragStopped = {
                        if (offsetX < maxOffset * 0.9f) {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Slide to dismiss",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ============================================================================
// iOS 6 SWIPE UP DISMISS
// ============================================================================

@Composable
private fun iOS6SwipeUpDismiss(
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val maxOffset = with(density) { -150.dp.toPx() }
    
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    
    // Bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_y"
    )
    
    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            delay(200)
            onDismiss()
        }
    }
    
    Column(
        modifier = Modifier
            .offset { IntOffset(0, (offsetY + bounceY).roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    offsetY = (offsetY + delta).coerceIn(maxOffset, 0f)
                    if (offsetY <= maxOffset * 0.7f) {
                        isDismissed = true
                    }
                },
                onDragStopped = {
                    if (offsetY > maxOffset * 0.7f) {
                        offsetY = 0f
                    }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Up arrow indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
        )
        
        // iOS 6 styled dismiss button
        Box(
            modifier = Modifier
                .width(200.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A4A5E),
                            Color(0xFF3A3A4E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF5A5A6E), RoundedCornerShape(12.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Swipe up to dismiss",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

// ============================================================================
// iOS 6 BUTTON DISMISS
// ============================================================================

@Composable
private fun iOS6ButtonDismiss(
    onDismiss: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main dismiss button with iOS 6 glossy red style
        Box(
            modifier = Modifier
                .width(280.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color(0xFFC62828), Color(0xFFC62828))
                        } else {
                            listOf(Color(0xFFFF6B6B), Color(0xFFE53935))
                        }
                    )
                )
                .border(1.dp, Color(0xFFFF8A80), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dismiss Alarm",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Snooze button (secondary)
        Box(
            modifier = Modifier
                .width(200.dp)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A4A5E),
                            Color(0xFF3A3A4E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF5A5A6E), RoundedCornerShape(10.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Snooze,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Snooze 5 min",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}
