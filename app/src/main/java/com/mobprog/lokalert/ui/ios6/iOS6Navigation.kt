package com.mobprog.lokalert.ui.ios6

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// iOS 6 TAB BAR - Classic iPhone Bottom Navigation
// ============================================================================

/**
 * iOS 6 styled bottom tab bar with classic dark gradient and glowing icons
 */
@Composable
fun iOS6TabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabItem("Map", Icons.Default.Map),
        TabItem("Alarms", Icons.Default.Alarm),
        TabItem("Settings", Icons.Default.Settings)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6D6D72),
                        Color(0xFF353537)
                    )
                )
            )
            .drawBehind {
                // Top highlight line
                drawLine(
                    color = Color(0xFF8E8E93),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                // Top inner shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x30000000), Color.Transparent),
                        startY = 1.dp.toPx(),
                        endY = 5.dp.toPx()
                    )
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                iOS6TabItem(
                    icon = tab.icon,
                    label = tab.label,
                    isSelected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun iOS6TabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E8E93)
    val labelColor = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E8E93)
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glow effect when selected
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect for selected state
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF007AFF).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = labelColor
            )
        )
    }
}

// ============================================================================
// iOS 6 MAIN APP SCAFFOLD
// ============================================================================

/**
 * Main scaffold that wraps the iOS 6 themed app with tab bar navigation
 */
@Composable
fun iOS6AppScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content(PaddingValues(bottom = 49.dp))
        }
        
        iOS6TabBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}

// ============================================================================
// iOS 6 TOOLBAR BUTTON
// ============================================================================

/**
 * iOS 6 styled toolbar button (for use in navigation bars)
 */
@Composable
fun iOS6ToolbarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: iOS6ToolbarButtonStyle = iOS6ToolbarButtonStyle.PLAIN
) {
    val textColor = when (style) {
        iOS6ToolbarButtonStyle.PLAIN -> Color.White
        iOS6ToolbarButtonStyle.DONE -> Color.White
        iOS6ToolbarButtonStyle.DESTRUCTIVE -> Color(0xFFFF453A)
    }
    
    val fontWeight = when (style) {
        iOS6ToolbarButtonStyle.DONE -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    
    Text(
        text = text,
        modifier = modifier.clickable(onClick = onClick),
        style = TextStyle(
            fontSize = 17.sp,
            fontWeight = fontWeight,
            color = textColor
        )
    )
}

enum class iOS6ToolbarButtonStyle {
    PLAIN,
    DONE,
    DESTRUCTIVE
}

// ============================================================================
// iOS 6 PAGE CONTROL (Dots indicator)
// ============================================================================

/**
 * iOS 6 styled page control dots
 */
@Composable
fun iOS6PageControl(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 8.dp else 6.dp)
                    .background(
                        color = if (index == currentPage) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.5f)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

// ============================================================================
// iOS 6 SEGMENTED CONTROL
// ============================================================================

/**
 * iOS 6 styled segmented control
 */
@Composable
fun iOS6SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp)
            )
            .drawBehind {
                // Border
                drawRoundRect(
                    color = Color(0xFF007AFF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
            }
            .padding(1.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        brush = if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectionChange(index) }
                    .drawBehind {
                        // Divider between segments
                        if (index > 0) {
                            drawLine(
                                color = Color(0xFF007AFF),
                                start = Offset(0f, 2.dp.toPx()),
                                end = Offset(0f, size.height - 2.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF007AFF)
                    )
                )
            }
        }
    }
}

// ============================================================================
// iOS 6 ALERT VIEW
// ============================================================================

/**
 * iOS 6 styled alert dialog
 */
@Composable
fun iOS6AlertView(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmButton: String = "OK",
    onConfirm: () -> Unit = onDismiss,
    cancelButton: String? = null,
    onCancel: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFAFFFFFF),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        title = {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFCED1D6))
                )
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    cancelButton?.let {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCancel?.invoke() ?: onDismiss() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = it,
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF)
                                )
                            )
                        }
                        
                        // Vertical separator
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(44.dp)
                                .background(Color(0xFFCED1D6))
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onConfirm() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmButton,
                            style = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = if (cancelButton == null) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDestructive) Color(0xFFFF3B30) else Color(0xFF007AFF)
                            )
                        )
                    }
                }
            }
        }
    )
}

// ============================================================================
// iOS 6 ACTION SHEET
// ============================================================================

/**
 * iOS 6 styled action sheet
 */
@Composable
fun iOS6ActionSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    actions: List<iOS6ActionSheetAction>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0F7F7F7),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        title = title?.let {
            {
                Text(
                    text = it,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E8E93)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                message?.let {
                    Text(
                        text = it,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                actions.forEach { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFCED1D6))
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                action.onClick()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = action.title,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = if (action.style == iOS6ActionStyle.CANCEL) FontWeight.Bold else FontWeight.Normal,
                                color = when (action.style) {
                                    iOS6ActionStyle.DESTRUCTIVE -> Color(0xFFFF3B30)
                                    else -> Color(0xFF007AFF)
                                }
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

data class iOS6ActionSheetAction(
    val title: String,
    val style: iOS6ActionStyle = iOS6ActionStyle.DEFAULT,
    val onClick: () -> Unit
)

enum class iOS6ActionStyle {
    DEFAULT,
    DESTRUCTIVE,
    CANCEL
}
