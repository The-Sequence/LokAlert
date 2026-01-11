package com.mobprog.lokalert.ui.ios6

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ============================================================================
// iOS 6 ALERT DIALOG - Classic Blue Gradient Style
// ============================================================================

/**
 * iOS 6 styled alert dialog with the classic blue gradient header
 * Matches the original iOS 6 UIAlertView design
 */
@Composable
fun iOS6AlertDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        iOS6AlertDialogContent(
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun iOS6AlertDialogContent(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // iOS 6 alert colors
    val alertBlueTop = Color(0xFF5B7BA3)
    val alertBlueBottom = Color(0xFF36547A)
    val alertBodyTop = Color(0xFFE8EDF4)
    val alertBodyBottom = Color(0xFFD5DCE7)
    val buttonTop = Color(0xFFFFFFFF)
    val buttonBottom = Color(0xFFD1D5DB)
    val borderColor = Color(0xFF3B5474)
    val innerBorderLight = Color(0xFF8CA2BD)
    
    Column(
        modifier = Modifier
            .width(270.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        // Title section with blue gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(alertBlueTop, alertBlueBottom)
                    )
                )
                .drawBehind {
                    // Top highlight
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, 1f),
                        end = Offset(size.width, 1f),
                        strokeWidth = 1f
                    )
                    // Inner glow at top
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.5f
                        )
                    )
                }
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, -1f),
                        blurRadius = 0f
                    )
                )
            )
        }
        
        // Message body with light gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(alertBodyTop, alertBodyBottom)
                    )
                )
                .drawBehind {
                    // Top inner border
                    drawLine(
                        color = innerBorderLight,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF3B3B3B),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            )
        }
        
        // Button section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFFCFD5DE))
                .drawBehind {
                    // Top border
                    drawLine(
                        color = Color(0xFF8D9BAB),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
        ) {
            if (dismissText != null) {
                // Left button (Don't Allow / Cancel)
                iOS6AlertButton(
                    text = dismissText,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF8D9BAB))
                )
                
                // Right button (OK / Confirm)
                iOS6AlertButton(
                    text = confirmText,
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Single centered button
                iOS6AlertButton(
                    text = confirmText,
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun iOS6AlertButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonBackground = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF7C8CA1), Color(0xFF5D6D82))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFD1D5DB))
        )
    }
    
    val textColor = if (isPressed) Color.White else Color(0xFF3B5474)
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(brush = buttonBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (!isPressed) {
                    // Top highlight
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, 1f),
                        end = Offset(size.width, 1f),
                        strokeWidth = 1f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                shadow = if (isPressed) {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(0f, -1f),
                        blurRadius = 0f
                    )
                } else {
                    Shadow(
                        color = Color.White,
                        offset = Offset(0f, 1f),
                        blurRadius = 0f
                    )
                }
            )
        )
    }
}

// ============================================================================
// iOS 6 PICKER DIALOG - Classic Style Selection
// ============================================================================

/**
 * iOS 6 styled picker dialog for selecting from a list of options
 */
@Composable
fun iOS6SelectionDialog(
    title: String,
    message: String? = null,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        iOS6SelectionDialogContent(
            title = title,
            message = message,
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun iOS6SelectionDialogContent(
    title: String,
    message: String?,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val alertBlueTop = Color(0xFF5B7BA3)
    val alertBlueBottom = Color(0xFF36547A)
    val alertBodyTop = Color(0xFFE8EDF4)
    val alertBodyBottom = Color(0xFFD5DCE7)
    val borderColor = Color(0xFF3B5474)
    
    Column(
        modifier = Modifier
            .width(270.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        // Title section with blue gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(alertBlueTop, alertBlueBottom)
                    )
                )
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, 1f),
                        end = Offset(size.width, 1f),
                        strokeWidth = 1f
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.5f
                        )
                    )
                }
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, -1f),
                        blurRadius = 0f
                    )
                )
            )
        }
        
        // Message (optional)
        if (message != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(alertBodyTop, alertBodyBottom)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF3B3B3B),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
        
        // Options list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(alertBodyTop, alertBodyBottom)
                    )
                )
        ) {
            options.forEachIndexed { index, option ->
                iOS6SelectionRow(
                    text = option,
                    isSelected = index == selectedIndex,
                    onClick = { 
                        onSelect(index)
                        onDismiss()
                    }
                )
                if (index < options.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(1.dp)
                            .background(Color(0xFFB4B9C1))
                    )
                }
            }
        }
        
        // Cancel button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFFCFD5DE))
                .drawBehind {
                    drawLine(
                        color = Color(0xFF8D9BAB),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
        ) {
            iOS6AlertButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun iOS6SelectionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPressed) Color(0xFF007AFF).copy(alpha = 0.3f) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 16.sp,
                color = Color(0xFF3B3B3B)
            )
        )
        
        if (isSelected) {
            Text(
                text = "✓",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
            )
        }
    }
}

// ============================================================================
// iOS 6 CONFIRMATION DIALOG - For Delete Actions
// ============================================================================

/**
 * iOS 6 styled confirmation dialog, typically used for destructive actions
 */
@Composable
fun iOS6ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    cancelText: String = "Cancel",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    iOS6AlertDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = cancelText,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

// ============================================================================
// iOS 6 WHEEL PICKER DIALOG - Classic Scrollable Style
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun iOS6WheelPickerDialog(
    title: String,
    values: List<Int>,
    selectedValue: Int,
    suffix: String = "",
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialIndex = values.indexOf(values.minByOrNull { kotlin.math.abs(it - selectedValue) } ?: values[0])
    var currentSelectedIndex by remember { mutableStateOf(initialIndex) }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(24.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
        ) {
            // Dark header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(listOf(Color(0xFF4A4A4C), Color(0xFF2C2C2E))))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, -1f), 0f)
                    )
                )
            }
            
            // iOS 6 Wheel Picker
            iOS6WheelPickerContent(
                values = values,
                initialIndex = initialIndex,
                suffix = suffix,
                onIndexChange = { newIndex ->
                    currentSelectedIndex = newIndex
                }
            )
            
            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(listOf(Color(0xFF4A4A4C), Color(0xFF2C2C2E))))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush = Brush.verticalGradient(listOf(Color(0xFF6A6A6C), Color(0xFF4A4A4C))))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush = Brush.verticalGradient(listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))))
                        .clickable { 
                            onSelect(values[currentSelectedIndex])
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun iOS6WheelPickerContent(
    values: List<Int>,
    initialIndex: Int,
    suffix: String,
    onIndexChange: (Int) -> Unit
) {
    val itemHeight = 48.dp
    val visibleItems = 5
    val halfVisible = visibleItems / 2
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    var selectedIndex by remember { mutableStateOf(initialIndex) }
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { centerIndex ->
                val clampedIndex = centerIndex.coerceIn(0, values.size - 1)
                if (clampedIndex != selectedIndex) {
                    selectedIndex = clampedIndex
                    onIndexChange(clampedIndex)
                }
            }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItems)
    ) {
        // ============================================================
        // LIGHT SILVER/WHITE BACKGROUND - The iOS 6 picker drum
        // This is the key - it's a LIGHT surface, not dark!
        // ============================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFFCDD2D8),  // Slightly darker at top
                            0.10f to Color(0xFFD8DDE3),
                            0.20f to Color(0xFFE3E7ED),
                            0.35f to Color(0xFFEBEFF5),
                            0.50f to Color(0xFFF0F4FA),  // Brightest white in center
                            0.65f to Color(0xFFEBEFF5),
                            0.80f to Color(0xFFE3E7ED),
                            0.90f to Color(0xFFD8DDE3),
                            1.00f to Color(0xFFCDD2D8)   // Slightly darker at bottom
                        )
                    )
                )
        )
        
        // ============================================================
        // SCROLLABLE CONTENT
        // ============================================================
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(halfVisible) { Spacer(modifier = Modifier.height(itemHeight)) }
            
            itemsIndexed(values) { index, value ->
                val distance = kotlin.math.abs(index - selectedIndex)
                
                // Text styling based on distance from center
                val textColor = when (distance) {
                    0 -> Color(0xFF000000)      // Selected: pure black
                    1 -> Color(0xFF3C3C43)      // Adjacent: dark gray
                    else -> Color(0xFF7C7C84)  // Far: medium gray
                }
                val fontSize = when (distance) {
                    0 -> 26.sp
                    1 -> 24.sp
                    else -> 22.sp
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$value  $suffix".trim(),
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = textColor
                        )
                    )
                }
            }
            
            items(halfVisible) { Spacer(modifier = Modifier.height(itemHeight)) }
        }
        
        // ============================================================
        // SELECTION BAR - Blue-tinted glass overlay (exact iOS 6 style)
        // ============================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x30A0B0C8),  // Light blue-gray tint
                            Color(0x18A8B8D0),
                            Color(0x18A8B8D0),
                            Color(0x30A0B0C8)
                        )
                    )
                )
                .drawBehind {
                    // Top line
                    drawLine(
                        color = Color(0xFF9AA8B8),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                    // Bottom line
                    drawLine(
                        color = Color(0xFF9AA8B8),
                        start = Offset(0f, size.height - 1f),
                        end = Offset(size.width, size.height - 1f),
                        strokeWidth = 1f
                    )
                }
        )
        
        // ============================================================
        // TOP EDGE DARKENING (subtle 3D curve effect)
        // ============================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x50B0B8C0),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // ============================================================
        // BOTTOM EDGE DARKENING (subtle 3D curve effect)
        // ============================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x50B0B8C0)
                        )
                    )
                )
        )
    }
}