package com.mobprog.lokalert.ui.ios6

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.MapsViewModel
import com.mobprog.lokalert.TrashedAlarm
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// iOS 6 TRASH SCREEN
// ============================================================================

private val iOS6LinenBackground = Color(0xFFC5C6C8)

@Composable
fun iOS6TrashScreen(
    viewModel: MapsViewModel,
    onBack: () -> Unit
) {
    val trashedAlarms by viewModel.trashedAlarms.collectAsState()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOS6LinenBackground)
    ) {
        // iOS 6 Navigation Bar
        iOS6NavBar(
            title = "Trash",
            leftAction = {
                iOS6NavBarButton(
                    text = "Back",
                    onClick = onBack
                )
            },
            rightAction = {
                if (trashedAlarms.isNotEmpty()) {
                    iOS6NavBarButton(
                        text = "Empty",
                        onClick = { showEmptyTrashDialog = true }
                    )
                }
            }
        )
        
        if (trashedAlarms.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                iOS6EmptyTrashCard()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Info text
                item {
                    Text(
                        text = "${trashedAlarms.size} ITEM${if (trashedAlarms.size > 1) "S" else ""} IN TRASH",
                        modifier = Modifier.padding(start = 30.dp, top = 8.dp, bottom = 6.dp),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D6D72),
                            shadow = Shadow(
                                color = Color.White,
                                offset = Offset(0f, 1f),
                                blurRadius = 0f
                            )
                        )
                    )
                }
                
                // Restore all button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        iOS6GreenButton(
                            text = "Restore All",
                            onClick = { showRestoreAllDialog = true }
                        )
                    }
                }
                
                // Trashed items
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .shadow(2.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
                    ) {
                        trashedAlarms.forEachIndexed { index, trashedAlarm ->
                            iOS6TrashedAlarmRow(
                                trashedAlarm = trashedAlarm,
                                onRestore = { viewModel.restoreFromTrash(trashedAlarm) },
                                onPermanentDelete = { viewModel.permanentlyDelete(trashedAlarm) }
                            )
                            if (index < trashedAlarms.size - 1) {
                                iOS6Separator(startIndent = 72.dp)
                            }
                        }
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Empty trash confirmation dialog
    if (showEmptyTrashDialog) {
        iOS6AlertDialog(
            title = "Empty Trash?",
            message = "All ${trashedAlarms.size} alarms will be permanently deleted. This action cannot be undone.",
            confirmText = "Empty Trash",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }
    
    // Restore all confirmation dialog
    if (showRestoreAllDialog) {
        iOS6AlertDialog(
            title = "Restore All?",
            message = "All ${trashedAlarms.size} alarms will be restored to your saved locations.",
            confirmText = "Restore All",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.restoreAllFromTrash()
                showRestoreAllDialog = false
            },
            onDismiss = { showRestoreAllDialog = false }
        )
    }
}

// ============================================================================
// iOS 6 TRASHED ALARM ROW
// ============================================================================

@Composable
fun iOS6TrashedAlarmRow(
    trashedAlarm: TrashedAlarm,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val deletedDate = remember(trashedAlarm.deletedAt) {
        dateFormat.format(Date(trashedAlarm.deletedAt))
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        iOS6AlertDialog(
            title = "Delete Forever?",
            message = "\"${trashedAlarm.name}\" will be permanently deleted. This action cannot be undone.",
            confirmText = "Delete Forever",
            dismissText = "Cancel",
            onConfirm = {
                onPermanentDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPressed) Color(0xFF007AFF).copy(alpha = 0.5f) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Permanent delete button (red X)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30))
                .clickable { showDeleteDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete permanently",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Location pin icon with gray background (indicating deleted)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8E8E93), Color(0xFF6D6D72))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Alarm details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trashedAlarm.name,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPressed) Color.White else Color.Black
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Deleted: $deletedDate",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = if (isPressed) Color.White.copy(alpha = 0.8f) else Color(0xFF8E8E93)
                )
            )
        }
        
        // Restore button
        iOS6SmallGreenButton(
            text = "Restore",
            onClick = onRestore
        )
    }
}

// ============================================================================
// iOS 6 GREEN BUTTON (for restore)
// ============================================================================

@Composable
fun iOS6GreenButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF2E7D32), Color(0xFF2E7D32))
                    } else {
                        listOf(Color(0xFF4CD964), Color(0xFF3CB84C))
                    }
                )
            )
            .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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

@Composable
fun iOS6SmallGreenButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF2E7D32), Color(0xFF2E7D32))
                    } else {
                        listOf(Color(0xFF4CD964), Color(0xFF3CB84C))
                    }
                )
            )
            .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
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

// ============================================================================
// iOS 6 EMPTY TRASH CARD
// ============================================================================

@Composable
fun iOS6EmptyTrashCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 16.dp)
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFB4B4B6), RoundedCornerShape(10.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with linen-style background
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Trash is Empty",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Deleted alarms will appear here",
            style = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                lineHeight = 20.sp
            )
        )
    }
}
