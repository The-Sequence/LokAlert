package com.mobprog.lokalert.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.ui.theme.DesignLanguage
import com.mobprog.lokalert.ui.theme.LocalDesignLanguage

// ============================================================================
// iOS 6 COLOR CONSTANTS
// ============================================================================

object iOS6Style {
    // Navigation bar gradient
    val NavBarTop = Color(0xFF5C9CE5)
    val NavBarBottom = Color(0xFF2C6DB4)
    
    // Button gradients
    val BlueButtonTop = Color(0xFF4C98D9)
    val BlueButtonBottom = Color(0xFF1E62A7)
    val BlueButtonPressed = Color(0xFF194F87)
    
    val GrayButtonTop = Color(0xFFFFFFFF)
    val GrayButtonBottom = Color(0xFFD5D5D5)
    val GrayButtonPressed = Color(0xFFBBBBBB)
    
    val RedButtonTop = Color(0xFFE57373)
    val RedButtonBottom = Color(0xFFC62828)
    
    val GreenButtonTop = Color(0xFF7CC576)
    val GreenButtonBottom = Color(0xFF3F9F3A)
    
    // Toggle switch
    val SwitchOn = Color(0xFF4CD964)
    val SwitchOff = Color(0xFFE5E5EA)
    
    // Table/List
    val TableBackground = Color(0xFFF2F2F7)
    val CellBackground = Color(0xFFFFFFFF)
    val Separator = Color(0xFFC6C6C8)
    val HeaderText = Color(0xFF6D6D72)
    
    // Text
    val PrimaryText = Color(0xFF000000)
    val SecondaryText = Color(0xFF8E8E93)
    val LinkBlue = Color(0xFF007AFF)
    val DestructiveRed = Color(0xFFFF3B30)
    
    // Borders
    val BorderLight = Color(0xFFCCCCCC)
    val BorderDark = Color(0xFF999999)
    
    // Tab bar
    val TabBarTop = Color(0xFF898989)
    val TabBarBottom = Color(0xFF4B4B4B)
}

// ============================================================================
// ADAPTIVE BUTTON - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            isPrimary = isPrimary,
            isDestructive = isDestructive,
            content = content
        )
    } else {
        if (isDestructive) {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                content()
            }
        } else if (isPrimary) {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                content()
            }
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                content()
            }
        }
    }
}

@Composable
private fun iOS6StyledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val gradientColors = when {
        isDestructive -> listOf(iOS6Style.RedButtonTop, iOS6Style.RedButtonBottom)
        isPrimary -> if (isPressed) {
            listOf(iOS6Style.BlueButtonPressed, iOS6Style.BlueButtonPressed)
        } else {
            listOf(iOS6Style.BlueButtonTop, iOS6Style.BlueButtonBottom)
        }
        else -> if (isPressed) {
            listOf(iOS6Style.GrayButtonPressed, iOS6Style.GrayButtonPressed)
        } else {
            listOf(iOS6Style.GrayButtonTop, iOS6Style.GrayButtonBottom)
        }
    }
    
    val textColor = if (isPrimary || isDestructive) Color.White else iOS6Style.PrimaryText
    
    Box(
        modifier = modifier
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(brush = Brush.verticalGradient(gradientColors))
            .border(1.dp, iOS6Style.BorderDark, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
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

// ============================================================================
// ADAPTIVE SWITCH - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
    }
}

@Composable
private fun iOS6StyledSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Animated track color
    val trackColor by animateColorAsState(
        targetValue = if (checked) iOS6Style.SwitchOn else iOS6Style.SwitchOff,
        animationSpec = tween(200),
        label = "switchTrack"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF3CB84C) else Color(0xFFDDDDDD),
        animationSpec = tween(200),
        label = "switchBorder"
    )
    
    // Animated thumb position with spring for bounce effect like iOS 6
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbPosition"
    )
    
    // Animated thumb scale for press feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "thumbScale"
    )
    
    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onCheckedChange(!checked) }
    ) {
        // Inner shadow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(15.dp))
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x20000000), Color.Transparent),
                            startY = 0f,
                            endY = 8.dp.toPx()
                        )
                    )
                }
        )
        
        // ON/OFF labels (iOS 6 style)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ON label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "ON",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = if (checked) 1f else 0f)
                    )
                )
            }
            
            // OFF label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "OFF",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A9A9A).copy(alpha = if (checked) 0f else 1f)
                    )
                )
            }
        }
        
        // Thumb with animation
        Box(
            modifier = Modifier
                .padding(2.dp)
                .offset(x = thumbOffset)
                .size(27.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFEEEEEE))
                    )
                )
                .border(0.5.dp, Color(0xFFCCCCCC), CircleShape)
        )
    }
}

// ============================================================================
// ADAPTIVE CARD - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledCard(
            modifier = modifier,
            onClick = onClick,
            content = content
        )
    } else {
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = modifier
            ) {
                Column(modifier = Modifier.padding(16.dp), content = content)
            }
        } else {
            Card(modifier = modifier) {
                Column(modifier = Modifier.padding(16.dp), content = content)
            }
        }
    }
}

@Composable
private fun iOS6StyledCard(
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
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8F8F8))
                )
            )
            .border(1.dp, iOS6Style.BorderLight, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

// ============================================================================
// ADAPTIVE TOP BAR - Material 3 or iOS 6 Style
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledTopBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions
        )
    } else {
            CenterAlignedTopAppBar(
            title = { Text(title) },
            modifier = modifier,
            navigationIcon = { navigationIcon?.invoke() },
            actions = actions
        )
    }
}

@Composable
private fun iOS6StyledTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(iOS6Style.NavBarTop, iOS6Style.NavBarBottom)
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
        // Navigation icon
        navigationIcon?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                it()
            }
        }
        
        // Title with embossed text effect
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
        
        // Actions
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            content = actions
        )
    }
}

// ============================================================================
// ADAPTIVE NAVIGATION BAR (Bottom) - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledNavigationBar(
            modifier = modifier,
            content = content
        )
    } else {
        NavigationBar(modifier = modifier, content = content)
    }
}

@Composable
private fun iOS6StyledNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(iOS6Style.TabBarTop, iOS6Style.TabBarBottom)
                )
            )
            .drawBehind {
                // Top highlight
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
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ============================================================================
// ADAPTIVE NAVIGATION ITEM - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun RowScope.AdaptiveNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledNavigationItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
            modifier = modifier
        )
    } else {
        NavigationBarItem(
            selected = selected,
            onClick = onClick,
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            modifier = modifier
        )
    }
}

@Composable
private fun iOS6StyledNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glow effect when selected
        Box(
            modifier = if (selected) {
                Modifier
                    .size(28.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                iOS6Style.LinkBlue.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            } else {
                Modifier.size(28.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) iOS6Style.LinkBlue else Color(0xFFCCCCCC),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) iOS6Style.LinkBlue else Color(0xFFCCCCCC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// ADAPTIVE LIST ITEM / TABLE CELL - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledListItem(
            headlineContent = headlineContent,
            modifier = modifier,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            onClick = onClick
        )
    } else {
        ListItem(
            headlineContent = headlineContent,
            modifier = modifier.then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent
        )
    }
}

@Composable
private fun iOS6StyledListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isPressed && onClick != null) 
                    iOS6Style.LinkBlue.copy(alpha = 0.2f) 
                else 
                    iOS6Style.CellBackground
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.let {
            it()
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            headlineContent()
            supportingContent?.let {
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(
                    LocalContentColor provides iOS6Style.SecondaryText
                ) {
                    it()
                }
            }
        }
        
        trailingContent?.let {
            Spacer(modifier = Modifier.width(16.dp))
            it()
        }
        
        // Disclosure indicator for clickable items
        if (onClick != null && trailingContent == null) {
            Text(
                text = "❯",
                color = iOS6Style.SecondaryText,
                fontSize = 16.sp
            )
        }
    }
}

// ============================================================================
// ADAPTIVE DIVIDER - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = startIndent)
                .height(1.dp)
                .background(iOS6Style.Separator)
        )
    } else {
        HorizontalDivider(modifier = modifier.padding(start = startIndent))
    }
}

// ============================================================================
// ADAPTIVE SLIDER - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        // iOS 6 style slider with custom colors
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = iOS6Style.LinkBlue,
                inactiveTrackColor = iOS6Style.SwitchOff
            )
        )
    } else {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps
        )
    }
}

// ============================================================================
// ADAPTIVE TEXT FIELD - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        iOS6StyledTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            placeholder = placeholder,
            enabled = enabled
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = if (label.isNotEmpty()) {{ Text(label) }} else null,
            placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder) }} else null,
            enabled = enabled,
            singleLine = singleLine
        )
    }
}

@Composable
private fun iOS6StyledTextField(
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
            .border(1.dp, iOS6Style.BorderLight, RoundedCornerShape(8.dp))
            .drawBehind {
                // Inner shadow at top
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x20000000), Color.Transparent),
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
                color = iOS6Style.SecondaryText,
                fontSize = 16.sp
            )
        }
        
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = iOS6Style.PrimaryText
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// ============================================================================
// ADAPTIVE ICON BUTTON - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        Box(
            modifier = modifier
                .size(44.dp)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                content()
            }
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            content()
        }
    }
}

// ============================================================================
// ADAPTIVE BACKGROUND - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val designLanguage = LocalDesignLanguage.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
                    iOS6Style.TableBackground
                } else {
                    MaterialTheme.colorScheme.background
                }
            ),
        content = content
    )
}

// ============================================================================
// ADAPTIVE SECTION HEADER - Material 3 or iOS 6 Style
// ============================================================================

@Composable
fun AdaptiveSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val designLanguage = LocalDesignLanguage.current
    
    if (designLanguage == DesignLanguage.IOS6_SKEUOMORPHIC) {
        Text(
            text = title.uppercase(),
            modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = iOS6Style.HeaderText,
                shadow = Shadow(
                    color = Color.White,
                    offset = Offset(0f, 1f),
                    blurRadius = 0f
                )
            )
        )
    } else {
        Text(
            text = title,
            modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
