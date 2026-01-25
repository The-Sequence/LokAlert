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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ============================================================================
// iOS 6 EXTENDED COMPONENTS - Complete UI Kit
// ============================================================================

// Color definitions for iOS 6 style
object iOS6ExtColors {
    val navBarTop = Color(0xFF5C9CE5)
    val navBarBottom = Color(0xFF2C6DB4)
    val blueButtonTop = Color(0xFF4C98D9)
    val blueButtonBottom = Color(0xFF1E62A7)
    val greenButtonTop = Color(0xFF7CC576)
    val greenButtonBottom = Color(0xFF3F9F3A)
    val redButtonTop = Color(0xFFE57373)
    val redButtonBottom = Color(0xFFC62828)
    val grayButtonTop = Color(0xFFFFFFFF)
    val grayButtonBottom = Color(0xFFE5E5E5)
    val linenBackground = Color(0xFFC5C6C8)
    val tableBackground = Color(0xFFF2F2F7)
    val cellBackground = Color(0xFFFFFFFF)
    val separatorColor = Color(0xFFCED1D6)
    val primaryText = Color(0xFF000000)
    val secondaryText = Color(0xFF8E8E93)
    val linkBlue = Color(0xFF007AFF)
    val destructiveRed = Color(0xFFFF3B30)
    val successGreen = Color(0xFF34C759)
}

// ============================================================================
// iOS 6 SLIDER - Classic Style with Glossy Thumb
// ============================================================================

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun iOS6Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        // Use constraints to satisfy the BoxWithConstraints scope requirement
        val constraintsMaxWidth = constraints.maxWidth
        val trackWidthPx = constraintsMaxWidth.toFloat()
        val thumbRadius = with(density) { 14.dp.toPx() }
        val thumbPosition = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)) * (trackWidthPx - thumbRadius * 2) + thumbRadius
        
        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE5E5EA))
                .border(0.5.dp, Color(0xFFCCCCCC), RoundedCornerShape(2.dp))
                .drawBehind {
                    // Inner shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x20000000), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.5f
                        )
                    )
                }
        )
        
        // Active track
        Box(
            modifier = Modifier
                .width(with(density) { thumbPosition.toDp() })
                .height(4.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF007AFF), Color(0xFF0056B3))
                    )
                )
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset((thumbPosition - thumbRadius).toInt(), 0) }
                .size(28.dp)
                .align(Alignment.CenterStart)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFEEEEEE))
                    )
                )
                .border(0.5.dp, Color(0xFFCCCCCC), CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta ->
                        val newPosition = (thumbPosition + delta).coerceIn(thumbRadius, trackWidthPx - thumbRadius)
                        val newValue = valueRange.start + (newPosition - thumbRadius) / (trackWidthPx - thumbRadius * 2) * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue.coerceIn(valueRange))
                    }
                )
        )
    }
}

// ============================================================================
// iOS 6 STEPPER - Plus/Minus Control
// ============================================================================

@Composable
fun iOS6Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..100,
    step: Int = 1
) {
    Row(
        modifier = modifier
            .height(29.dp)
            .shadow(2.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF007AFF), RoundedCornerShape(5.dp))
    ) {
        // Minus button
        iOS6StepperButton(
            icon = Icons.Default.Remove,
            enabled = value > range.first,
            onClick = { onValueChange((value - step).coerceIn(range)) }
        )
        
        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF007AFF))
        )
        
        // Plus button
        iOS6StepperButton(
            icon = Icons.Default.Add,
            enabled = value < range.last,
            onClick = { onValueChange((value + step).coerceIn(range)) }
        )
    }
}

@Composable
private fun iOS6StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed && enabled) {
                        listOf(Color(0xFF007AFF), Color(0xFF0056B3))
                    } else {
                        listOf(Color.White, Color(0xFFF0F0F0))
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                if (isPressed) Color.White else Color(0xFF007AFF)
            } else {
                Color(0xFFCCCCCC)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================================
// iOS 6 PROGRESS BAR - Classic Style
// ============================================================================

@Composable
fun iOS6ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFE5E5EA),
    progressColor: Color = Color(0xFF007AFF)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(4.5.dp))
            .background(trackColor)
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.5.dp))
            .drawBehind {
                // Inner shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x30000000), Color.Transparent),
                        startY = 0f,
                        endY = 4.dp.toPx()
                    )
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.5.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            progressColor,
                            progressColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .drawBehind {
                    // Glossy highlight
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x40FFFFFF), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.5f
                        )
                    )
                }
        )
    }
}

// ============================================================================
// iOS 6 ACTIVITY INDICATOR (Spinner)
// ============================================================================

@Composable
fun iOS6ActivityIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF8E8E93),
    size: Dp = 20.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .rotate(rotation)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val radius = (this.size.minDimension - strokeWidth) / 2
                val segmentCount = 12
                
                for (i in 0 until segmentCount) {
                    val angle = (i * 360f / segmentCount) - 90
                    val alpha = (i + 1).toFloat() / segmentCount
                    
                    drawArc(
                        color = color.copy(alpha = alpha),
                        startAngle = angle,
                        sweepAngle = 20f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(radius * 2, radius * 2)
                    )
                }
            }
    )
}

// ============================================================================
// iOS 6 BADGE
// ============================================================================

@Composable
fun iOS6Badge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF3B30), Color(0xFFD32F2F))
                    )
                )
                .border(1.dp, Color(0xFFFF6B6B), CircleShape)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

// ============================================================================
// iOS 6 PICKER (Wheel Style)
// ============================================================================

@Composable
fun iOS6WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - visibleItemCount / 2).coerceAtLeast(0)
    )
    val itemHeight = 44.dp
    
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem((selectedIndex - visibleItemCount / 2).coerceAtLeast(0))
    }
    
    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE5E5EA), Color.White, Color.White, Color(0xFFE5E5EA))
                )
            )
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2))
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == selectedIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable { onSelectionChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = TextStyle(
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color(0xFF8E8E93)
                        )
                    )
                }
            }
        }
        
        // Selection indicator overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .drawBehind {
                    // Top line
                    drawLine(
                        color = Color(0xFFCCCCCC),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Bottom line
                    drawLine(
                        color = Color(0xFFCCCCCC),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        )
    }
}

// ============================================================================
// iOS 6 TOOLBAR (Bottom Toolbar)
// ============================================================================

@Composable
fun iOS6Toolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7F7F7),
                        Color(0xFFE5E5E5)
                    )
                )
            )
            .drawBehind {
                // Top border
                drawLine(
                    color = Color(0xFFCCCCCC),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun iOS6ToolbarItem(
    icon: ImageVector,
    label: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) {
                if (isPressed) Color(0xFF0056B3) else Color(0xFF007AFF)
            } else {
                Color(0xFFCCCCCC)
            },
            modifier = Modifier.size(24.dp)
        )
        
        label?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = if (enabled) {
                        if (isPressed) Color(0xFF0056B3) else Color(0xFF007AFF)
                    } else {
                        Color(0xFFCCCCCC)
                    }
                )
            )
        }
    }
}

// ============================================================================
// iOS 6 SEARCH BAR
// ============================================================================

@Composable
fun iOS6SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFBABEC5), Color(0xFF9DA1A8))
                )
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(8.dp))
                .drawBehind {
                    // Inner shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x15000000), Color.Transparent),
                            startY = 0f,
                            endY = 4.dp.toPx()
                        )
                    )
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFFC7C7CC)
                        )
                    )
                }
                
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC7C7CC))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// iOS 6 INFO BUTTON (Classic "i" Button)
// ============================================================================

@Composable
fun iOS6InfoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: iOS6InfoButtonStyle = iOS6InfoButtonStyle.LIGHT
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val (backgroundColor, borderColor, textColor) = when (style) {
        iOS6InfoButtonStyle.LIGHT -> Triple(
            if (isPressed) Color(0xFFE0E0E0) else Color.White,
            Color(0xFF007AFF),
            Color(0xFF007AFF)
        )
        iOS6InfoButtonStyle.DARK -> Triple(
            if (isPressed) Color(0xFF0056B3) else Color(0xFF007AFF),
            Color(0xFF0056B3),
            Color.White
        )
    }
    
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "i",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        )
    }
}

enum class iOS6InfoButtonStyle {
    LIGHT, DARK
}

// ============================================================================
// iOS 6 DETAIL DISCLOSURE BUTTON
// ============================================================================

@Composable
fun iOS6DetailDisclosureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .size(29.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF0056B3), Color(0xFF0056B3))
                    } else {
                        listOf(Color(0xFF007AFF), Color(0xFF0056B3))
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Details",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================================
// iOS 6 PULL TO REFRESH INDICATOR
// ============================================================================

@Composable
fun iOS6RefreshIndicator(
    isRefreshing: Boolean,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else progress * 360f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        },
        label = "refresh_rotation"
    )
    
    Box(
        modifier = modifier
            .size(30.dp)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refreshing",
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ============================================================================
// iOS 6 DELETE BUTTON (For Swipe Actions)
// ============================================================================

@Composable
fun iOS6DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Delete"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFFC62828), Color(0xFFC62828))
                    } else {
                        listOf(Color(0xFFFF3B30), Color(0xFFD32F2F))
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        )
    }
}

// ============================================================================
// iOS 6 EDIT MODE CONTROLS
// ============================================================================

@Composable
fun iOS6ReorderHandle(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(Color(0xFFC7C7CC), RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun iOS6DeleteIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFFC62828), Color(0xFFC62828))
                    } else {
                        listOf(Color(0xFFFF3B30), Color(0xFFD32F2F))
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(2.dp)
                .background(Color.White, RoundedCornerShape(1.dp))
        )
    }
}
