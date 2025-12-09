package com.mobprog.lokalert

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Performance utilities for optimizing the app on both slow and fast devices.
 */
object PerformanceUtils {
    
    // Cache device performance level to avoid repeated checks
    private var cachedPerformanceLevel: PerformanceLevel? = null
    
    enum class PerformanceLevel {
        LOW,      // Low-end devices: minimal animations, basic effects
        MEDIUM,   // Mid-range: standard animations, moderate effects
        HIGH      // High-end: full animations, rich effects
    }
    
    /**
     * Determines the device's performance level based on RAM and other factors.
     */
    fun getPerformanceLevel(context: Context): PerformanceLevel {
        cachedPerformanceLevel?.let { return it }
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        
        val level = when {
            activityManager.isLowRamDevice || totalRamGb < 3.0 -> PerformanceLevel.LOW
            totalRamGb < 6.0 -> PerformanceLevel.MEDIUM
            else -> PerformanceLevel.HIGH
        }
        
        cachedPerformanceLevel = level
        return level
    }
    
    /**
     * Determines if the device is a low-end device based on RAM and API level.
     */
    fun isLowEndDevice(context: Context): Boolean {
        return getPerformanceLevel(context) == PerformanceLevel.LOW
    }
    
    /**
     * Determines if the device is a high-end device.
     */
    fun isHighEndDevice(context: Context): Boolean {
        return getPerformanceLevel(context) == PerformanceLevel.HIGH
    }
    
    /**
     * Gets the optimal animation duration based on device performance.
     * Slower devices get faster (shorter) animations to feel more responsive.
     * High-end devices get slightly longer animations for smoother appearance.
     */
    fun getOptimalAnimationDuration(context: Context, baseDuration: Int): Int {
        return when (getPerformanceLevel(context)) {
            PerformanceLevel.LOW -> (baseDuration * 0.5).toInt().coerceAtLeast(100)
            PerformanceLevel.MEDIUM -> baseDuration
            PerformanceLevel.HIGH -> (baseDuration * 1.1).toInt()
        }
    }
    
    /**
     * Gets the optimal spring stiffness for animations.
     */
    fun getOptimalSpringStiffness(context: Context): Float {
        return when (getPerformanceLevel(context)) {
            PerformanceLevel.LOW -> Spring.StiffnessHigh
            PerformanceLevel.MEDIUM -> Spring.StiffnessMedium
            PerformanceLevel.HIGH -> Spring.StiffnessMediumLow
        }
    }
    
    /**
     * Gets the optimal spring damping ratio.
     */
    fun getOptimalSpringDamping(context: Context): Float {
        return when (getPerformanceLevel(context)) {
            PerformanceLevel.LOW -> Spring.DampingRatioNoBouncy
            PerformanceLevel.MEDIUM -> Spring.DampingRatioLowBouncy
            PerformanceLevel.HIGH -> Spring.DampingRatioMediumBouncy
        }
    }
    
    /**
     * Determines if complex animations should be enabled.
     */
    fun shouldEnableComplexAnimations(context: Context): Boolean {
        return getPerformanceLevel(context) != PerformanceLevel.LOW
    }
    
    /**
     * Determines if blur effects should be enabled.
     */
    fun shouldEnableBlurEffects(context: Context): Boolean {
        // Blur requires Android 12+ and high-end device for best performance
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
               getPerformanceLevel(context) == PerformanceLevel.HIGH
    }
    
    /**
     * Gets the optimal frame rate for infinite animations.
     * Lower frame rate for slow devices to reduce CPU usage.
     */
    fun getOptimalInfiniteAnimationDuration(context: Context, baseDuration: Int): Int {
        return when (getPerformanceLevel(context)) {
            PerformanceLevel.LOW -> (baseDuration * 2.0).toInt() // Much slower = less CPU
            PerformanceLevel.MEDIUM -> (baseDuration * 1.3).toInt()
            PerformanceLevel.HIGH -> baseDuration
        }
    }
    
    /**
     * Gets the optimal count for list items to render at once.
     */
    fun getOptimalBatchSize(context: Context): Int {
        return when (getPerformanceLevel(context)) {
            PerformanceLevel.LOW -> 5
            PerformanceLevel.MEDIUM -> 10
            PerformanceLevel.HIGH -> 20
        }
    }
}

/**
 * Composable to remember if device is low-end.
 */
@Composable
fun rememberIsLowEndDevice(): Boolean {
    val context = LocalContext.current
    return remember { PerformanceUtils.isLowEndDevice(context) }
}

/**
 * Composable to remember if device is high-end.
 */
@Composable
fun rememberIsHighEndDevice(): Boolean {
    val context = LocalContext.current
    return remember { PerformanceUtils.isHighEndDevice(context) }
}

/**
 * Composable to remember the device's performance level.
 */
@Composable
fun rememberPerformanceLevel(): PerformanceUtils.PerformanceLevel {
    val context = LocalContext.current
    return remember { PerformanceUtils.getPerformanceLevel(context) }
}

/**
 * Composable to get optimal animation duration.
 */
@Composable
fun rememberOptimalAnimationDuration(baseDuration: Int): Int {
    val context = LocalContext.current
    return remember(baseDuration) { 
        PerformanceUtils.getOptimalAnimationDuration(context, baseDuration) 
    }
}

/**
 * Composable to check if complex animations should be enabled.
 */
@Composable
fun rememberShouldEnableComplexAnimations(): Boolean {
    val context = LocalContext.current
    return remember { PerformanceUtils.shouldEnableComplexAnimations(context) }
}

/**
 * Composable to get optimal spring animation spec.
 */
@Composable
fun <T> rememberOptimalSpringSpec(): AnimationSpec<T> {
    val context = LocalContext.current
    return remember {
        spring(
            dampingRatio = PerformanceUtils.getOptimalSpringDamping(context),
            stiffness = PerformanceUtils.getOptimalSpringStiffness(context)
        )
    }
}

/**
 * Composable to get optimal tween animation spec.
 */
@Composable
fun <T> rememberOptimalTweenSpec(baseDuration: Int = 300): AnimationSpec<T> {
    val context = LocalContext.current
    return remember(baseDuration) {
        tween(
            durationMillis = PerformanceUtils.getOptimalAnimationDuration(context, baseDuration),
            easing = FastOutSlowInEasing
        )
    }
}
