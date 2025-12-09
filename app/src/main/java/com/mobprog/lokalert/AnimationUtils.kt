package com.mobprog.lokalert

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Animation utilities for smooth, performance-optimized animations throughout the app.
 */
object AnimationUtils {
    
    /**
     * Standard animation durations based on Material Design guidelines.
     */
    object Duration {
        const val INSTANT = 100
        const val FAST = 150
        const val STANDARD = 250
        const val MEDIUM = 300
        const val SLOW = 400
        const val EMPHASIS = 500
    }
    
    /**
     * Standard easing curves.
     */
    object Easing {
        val standard = FastOutSlowInEasing
        val decelerate = LinearOutSlowInEasing
        val accelerate = FastOutLinearInEasing
        val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

/**
 * Provides smooth enter/exit animations for content transitions.
 * Automatically adjusts based on device performance.
 */
@Composable
fun AnimatedScreenTransition(
    targetState: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit
) {
    val performanceLevel = rememberPerformanceLevel()
    val duration = when (performanceLevel) {
        PerformanceUtils.PerformanceLevel.LOW -> AnimationUtils.Duration.FAST
        PerformanceUtils.PerformanceLevel.MEDIUM -> AnimationUtils.Duration.STANDARD
        PerformanceUtils.PerformanceLevel.HIGH -> AnimationUtils.Duration.MEDIUM
    }
    
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            when (performanceLevel) {
                PerformanceUtils.PerformanceLevel.LOW -> {
                    // Simple fade for low-end devices
                    fadeIn(animationSpec = tween(duration)) togetherWith
                    fadeOut(animationSpec = tween(duration))
                }
                else -> {
                    // Smooth slide + fade for better devices
                    (fadeIn(animationSpec = tween(duration, easing = AnimationUtils.Easing.standard)) +
                    scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(duration, easing = AnimationUtils.Easing.emphasizedDecelerate)
                    )) togetherWith
                    (fadeOut(animationSpec = tween(duration / 2, easing = AnimationUtils.Easing.accelerate)) +
                    scaleOut(
                        targetScale = 1.02f,
                        animationSpec = tween(duration / 2, easing = AnimationUtils.Easing.accelerate)
                    ))
                }
            }
        },
        label = "ScreenTransition"
    ) { state ->
        content(state)
    }
}

/**
 * Wrapper for smooth visibility animations.
 */
@Composable
fun SmoothAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enterDuration: Int = AnimationUtils.Duration.STANDARD,
    exitDuration: Int = AnimationUtils.Duration.FAST,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val performanceLevel = rememberPerformanceLevel()
    val adjustedEnterDuration = when (performanceLevel) {
        PerformanceUtils.PerformanceLevel.LOW -> enterDuration / 2
        PerformanceUtils.PerformanceLevel.MEDIUM -> enterDuration
        PerformanceUtils.PerformanceLevel.HIGH -> (enterDuration * 1.1).toInt()
    }
    val adjustedExitDuration = when (performanceLevel) {
        PerformanceUtils.PerformanceLevel.LOW -> exitDuration / 2
        PerformanceUtils.PerformanceLevel.MEDIUM -> exitDuration
        PerformanceUtils.PerformanceLevel.HIGH -> (exitDuration * 1.1).toInt()
    }
    
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = when (performanceLevel) {
            PerformanceUtils.PerformanceLevel.LOW -> fadeIn(animationSpec = tween(adjustedEnterDuration))
            else -> fadeIn(animationSpec = tween(adjustedEnterDuration, easing = AnimationUtils.Easing.emphasizedDecelerate)) +
                    expandVertically(animationSpec = tween(adjustedEnterDuration, easing = AnimationUtils.Easing.emphasizedDecelerate)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(adjustedEnterDuration, easing = AnimationUtils.Easing.emphasizedDecelerate))
        },
        exit = when (performanceLevel) {
            PerformanceUtils.PerformanceLevel.LOW -> fadeOut(animationSpec = tween(adjustedExitDuration))
            else -> fadeOut(animationSpec = tween(adjustedExitDuration, easing = AnimationUtils.Easing.accelerate)) +
                    shrinkVertically(animationSpec = tween(adjustedExitDuration, easing = AnimationUtils.Easing.accelerate)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(adjustedExitDuration, easing = AnimationUtils.Easing.accelerate))
        },
        content = content
    )
}

/**
 * Smooth fade animation wrapper.
 */
@Composable
fun SmoothFade(
    visible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = AnimationUtils.Duration.STANDARD,
    content: @Composable () -> Unit
) {
    val performanceLevel = rememberPerformanceLevel()
    val adjustedDuration = when (performanceLevel) {
        PerformanceUtils.PerformanceLevel.LOW -> duration / 2
        PerformanceUtils.PerformanceLevel.MEDIUM -> duration
        PerformanceUtils.PerformanceLevel.HIGH -> (duration * 1.1).toInt()
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(adjustedDuration, easing = AnimationUtils.Easing.standard),
        label = "FadeAlpha"
    )
    
    Box(modifier = modifier.alpha(alpha)) {
        content()
    }
}

/**
 * Provides smooth scale animation for press/release states.
 */
@Composable
fun SmoothPressScale(
    isPressed: Boolean,
    pressedScale: Float = 0.96f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PressScale"
    )
    
    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

/**
 * Creates a smooth content size animation modifier.
 */
@Composable
fun Modifier.smoothAnimateContentSize(): Modifier {
    val performanceLevel = rememberPerformanceLevel()
    
    return this.animateContentSize(
        animationSpec = when (performanceLevel) {
            PerformanceUtils.PerformanceLevel.LOW -> tween(
                durationMillis = AnimationUtils.Duration.FAST,
                easing = AnimationUtils.Easing.standard
            )
            PerformanceUtils.PerformanceLevel.MEDIUM -> spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
            PerformanceUtils.PerformanceLevel.HIGH -> spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        }
    )
}

/**
 * Smooth loading shimmer effect (only for devices that can handle it).
 */
@Composable
fun rememberShimmerAlpha(): State<Float> {
    val performanceLevel = rememberPerformanceLevel()
    val shouldAnimate = performanceLevel != PerformanceUtils.PerformanceLevel.LOW
    
    if (!shouldAnimate) {
        return remember { mutableStateOf(0.5f) }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    return infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (performanceLevel) {
                    PerformanceUtils.PerformanceLevel.HIGH -> 800
                    else -> 1000
                },
                easing = AnimationUtils.Easing.standard
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
}

/**
 * Smooth rotation animation for icons.
 */
@Composable
fun rememberSmoothRotation(
    isRotated: Boolean,
    fromDegrees: Float = 0f,
    toDegrees: Float = 180f
): Float {
    val rotation by animateFloatAsState(
        targetValue = if (isRotated) toDegrees else fromDegrees,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )
    return rotation
}

/**
 * Smooth modifier for entering elements (staggered animation support).
 */
@Composable
fun Modifier.smoothEnterAnimation(
    index: Int = 0,
    visible: Boolean = true,
    delayPerItem: Int = 50
): Modifier {
    val performanceLevel = rememberPerformanceLevel()
    
    // Skip staggered animations on low-end devices
    val effectiveDelay = when (performanceLevel) {
        PerformanceUtils.PerformanceLevel.LOW -> 0
        PerformanceUtils.PerformanceLevel.MEDIUM -> delayPerItem * index.coerceAtMost(3)
        PerformanceUtils.PerformanceLevel.HIGH -> delayPerItem * index.coerceAtMost(5)
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationUtils.Duration.STANDARD,
            delayMillis = effectiveDelay,
            easing = AnimationUtils.Easing.emphasizedDecelerate
        ),
        label = "enterAlpha_$index"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(
            durationMillis = AnimationUtils.Duration.STANDARD,
            delayMillis = effectiveDelay,
            easing = AnimationUtils.Easing.emphasizedDecelerate
        ),
        label = "enterOffset_$index"
    )
    
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}
