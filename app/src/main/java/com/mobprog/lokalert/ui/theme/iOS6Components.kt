package com.mobprog.lokalert.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design Language Options
 * 0 = Material 3 (Modern)
 * 1 = iOS 6 Skeuomorphic (Retro)
 */
enum class DesignLanguage {
    MATERIAL3,
    IOS6_SKEUOMORPHIC
}

/**
 * CompositionLocal for design language
 */
val LocalDesignLanguage = compositionLocalOf { DesignLanguage.MATERIAL3 }

// ============================================================================
// iOS 6 COLOR PALETTE
// ============================================================================

object iOS6Colors {
    // Classic iOS 6 blue gradient for navigation bars
    val NavBarGradientTop = Color(0xFF5C9CE5)
    val NavBarGradientBottom = Color(0xFF2C6DB4)
    
    // Button gradients
    val BlueButtonTop = Color(0xFF4C98D9)
    val BlueButtonBottom = Color(0xFF1E62A7)
    val BlueButtonPressed = Color(0xFF194F87)
    
    val GrayButtonTop = Color(0xFFFFFFFF)
    val GrayButtonBottom = Color(0xFFD5D5D5)
    val GrayButtonPressed = Color(0xFFBBBBBB)
    
    val GreenButtonTop = Color(0xFF7CC576)
    val GreenButtonBottom = Color(0xFF3F9F3A)
    
    val RedButtonTop = Color(0xFFE57373)
    val RedButtonBottom = Color(0xFFC62828)
    
    // Toggle switch colors
    val SwitchOnTrack = Color(0xFF4CD964)
    val SwitchOffTrack = Color(0xFFE5E5EA)
    val SwitchThumb = Color(0xFFFFFFFF)
    
    // Table/List colors
    val TableBackground = Color(0xFFF2F2F7)
    val TableCellBackground = Color(0xFFFFFFFF)
    val TableSeparator = Color(0xFFC6C6C8)
    val TableHeaderText = Color(0xFF6D6D72)
    
    // Text colors
    val PrimaryText = Color(0xFF000000)
    val SecondaryText = Color(0xFF8E8E93)
    val LinkText = Color(0xFF007AFF)
    
    // Other UI elements
    val SelectionBlue = Color(0xFF007AFF)
    val DestructiveRed = Color(0xFFFF3B30)
    val BorderLight = Color(0xFFCCCCCC)
    val BorderDark = Color(0xFF999999)
    val InnerShadow = Color(0x33000000)
    val DropShadow = Color(0x40000000)
    
    // Linen texture background color
    val LinenBackground = Color(0xFFC5C5C5)
}

// ============================================================================
// iOS 6 STYLED COMPONENTS
// ============================================================================

/**
 * iOS 6 style glossy button with gradient and inner shadow
 */
@Composable
fun iOS6Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    isPrimary: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val gradientColors = when {
        isDestructive -> listOf(iOS6Colors.RedButtonTop, iOS6Colors.RedButtonBottom)
        isPrimary -> if (isPressed) {
            listOf(iOS6Colors.BlueButtonPressed, iOS6Colors.BlueButtonPressed)
        } else {
            listOf(iOS6Colors.BlueButtonTop, iOS6Colors.BlueButtonBottom)
        }
        else -> if (isPressed) {
            listOf(iOS6Colors.GrayButtonPressed, iOS6Colors.GrayButtonPressed)
        } else {
            listOf(iOS6Colors.GrayButtonTop, iOS6Colors.GrayButtonBottom)
        }
    }
    
    val textColor = if (isPrimary || isDestructive) Color.White else iOS6Colors.PrimaryText
    
    Box(
        modifier = modifier
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = iOS6Colors.DropShadow,
                spotColor = iOS6Colors.DropShadow
            )
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(gradientColors)
            )
            .border(
                width = 1.dp,
                color = if (isPrimary || isDestructive) iOS6Colors.BorderDark else iOS6Colors.BorderLight,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * iOS 6 style navigation bar with blue gradient
 */
@Composable
fun iOS6NavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    leftAction: (@Composable () -> Unit)? = null,
    rightAction: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        iOS6Colors.NavBarGradientTop,
                        iOS6Colors.NavBarGradientBottom
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
        // Left action
        leftAction?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                it()
            }
        }
        
        // Title
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
        
        // Right action
        rightAction?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                it()
            }
        }
    }
}

/**
 * iOS 6 style toggle switch
 */
@Composable
fun iOS6Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor = if (checked) iOS6Colors.SwitchOnTrack else iOS6Colors.SwitchOffTrack
    
    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .border(
                width = 1.dp,
                color = if (checked) Color(0xFF3CB84C) else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // Inner shadow on track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(15.dp))
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x20000000),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 8.dp.toPx()
                        )
                    )
                }
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFEEEEEE)
                        )
                    )
                )
                .border(0.5.dp, Color(0xFFCCCCCC), CircleShape)
        )
    }
}

/**
 * iOS 6 style table cell / list item
 */
@Composable
fun iOS6TableCell(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDisclosure: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isPressed && onClick != null) iOS6Colors.SelectionBlue.copy(alpha = 0.2f)
                else iOS6Colors.TableCellBackground
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            
            if (showDisclosure) {
                Text(
                    text = "❯",
                    color = iOS6Colors.SecondaryText,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * iOS 6 style grouped table section
 */
@Composable
fun iOS6TableSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Header
        header?.let {
            Text(
                text = it.uppercase(),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = iOS6Colors.TableHeaderText,
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 1f),
                        blurRadius = 0f
                    )
                )
            )
        }
        
        // Content with rounded corners and border
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(iOS6Colors.TableCellBackground)
                .border(
                    width = 1.dp,
                    color = iOS6Colors.BorderLight,
                    shape = RoundedCornerShape(10.dp)
                ),
            content = content
        )
        
        // Footer
        footer?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = iOS6Colors.TableHeaderText
                )
            )
        }
    }
}

/**
 * iOS 6 style divider for table cells
 */
@Composable
fun iOS6TableDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(iOS6Colors.TableSeparator)
    )
}

/**
 * iOS 6 style card with embossed look
 */
@Composable
fun iOS6Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8F8F8)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = iOS6Colors.BorderLight,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

/**
 * iOS 6 style segmented control
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
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, iOS6Colors.SelectionBlue, RoundedCornerShape(5.dp))
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    iOS6Colors.BlueButtonTop,
                                    iOS6Colors.BlueButtonBottom
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFF0F0F0)
                                )
                            )
                        }
                    )
                    .clickable { onSelectionChange(index) }
                    .then(
                        if (index > 0) {
                            Modifier.drawBehind {
                                drawLine(
                                    color = iOS6Colors.SelectionBlue,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else iOS6Colors.SelectionBlue
                    )
                )
            }
        }
    }
}

/**
 * iOS 6 style text field with inset border
 */
@Composable
fun iOS6TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, iOS6Colors.BorderLight, RoundedCornerShape(8.dp))
            .drawBehind {
                // Inner shadow at top
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x20000000),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 4.dp.toPx()
                    )
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                color = iOS6Colors.SecondaryText,
                fontSize = 16.sp
            )
        }
        
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = iOS6Colors.PrimaryText
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * iOS 6 linen texture background
 */
@Composable
fun iOS6LinenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iOS6Colors.TableBackground)
            .drawBehind {
                // Simulate linen texture with subtle pattern
                val patternSize = 4.dp.toPx()
                for (x in 0 until (size.width / patternSize).toInt()) {
                    for (y in 0 until (size.height / patternSize).toInt()) {
                        if ((x + y) % 2 == 0) {
                            drawRect(
                                color = Color(0x08000000),
                                topLeft = Offset(x * patternSize, y * patternSize),
                                size = androidx.compose.ui.geometry.Size(patternSize, patternSize)
                            )
                        }
                    }
                }
            },
        content = content
    )
}

/**
 * iOS 6 style bottom tab bar
 */
@Composable
fun iOS6TabBar(
    items: List<Pair<String, String>>, // Pair of (label, emoji)
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF898989),
                        Color(0xFF4B4B4B)
                    )
                )
            )
            .drawBehind {
                // Top border highlight
                drawLine(
                    color = Color(0xFFB0B0B0),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (label, emoji) ->
                val isSelected = index == selectedIndex
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onItemSelected(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icon with glow effect when selected
                    Box(
                        modifier = if (isSelected) {
                            Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            iOS6Colors.SelectionBlue.copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        } else Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = if (isSelected) iOS6Colors.SelectionBlue else Color(0xFFCCCCCC)
                        )
                    )
                }
            }
        }
    }
}

/**
 * iOS 6 style alert/confirmation dialog
 */
@Composable
fun iOS6AlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF0FFFFFF),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dismissText?.let {
                    iOS6Button(
                        onClick = { onDismiss?.invoke() ?: onDismissRequest() },
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(it)
                    }
                }
                
                iOS6Button(
                    onClick = onConfirm,
                    isDestructive = isDestructive,
                    isPrimary = !isDestructive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(confirmText)
                }
            }
        }
    )
}
