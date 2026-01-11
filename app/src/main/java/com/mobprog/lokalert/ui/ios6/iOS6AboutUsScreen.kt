package com.mobprog.lokalert.ui.ios6

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobprog.lokalert.R

// ============================================================================
// iOS 6 ABOUT US SCREEN - Skeuomorphic Style
// ============================================================================

@Composable
fun iOS6AboutUsScreen(
    onDismiss: () -> Unit = {},
    isDialog: Boolean = true
) {
    if (isDialog) {
        iOS6AboutDialog(onDismiss = onDismiss)
    } else {
        iOS6AboutContent(onNavigateBack = onDismiss)
    }
}

@Composable
private fun iOS6AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F7F7),
        shape = RoundedCornerShape(14.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                iOS6AboutDialogContent()
            }
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
                
                // OK Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OK",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    )
                }
            }
        }
    )
}

@Composable
private fun iOS6AboutDialogContent() {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon with iOS 6 style
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))
                    )
                )
                .border(1.dp, Color(0xFF2A5A8F), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📍",
                fontSize = 40.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "LokAlert",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Text(
            text = "Version 1.0.0",
            style = TextStyle(
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFCED1D6))
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "LokAlert was crafted with care and dedication. We hope it serves you well on your journeys.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Developed by:",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6D6D72)
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Developer names in iOS 6 style grouped list
        iOS6DeveloperList()
    }
}

@Composable
private fun iOS6DeveloperList() {
    val developers = listOf(
        "Adamos, Eurika",
        "Alemaña, Onyx Herod",
        "Billones, Gerald",
        "Crisologo, Terence Joefrey",
        "Mabahin, Ryan",
        "Royo, Aenard Ollyer"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCED1D6), RoundedCornerShape(8.dp))
    ) {
        developers.forEachIndexed { index, name ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF007AFF), Color(0xFF0056B3))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.first().toString(),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = name,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                )
            }
            
            if (index < developers.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp)
                        .height(1.dp)
                        .background(Color(0xFFCED1D6))
                )
            }
        }
    }
}

// ============================================================================
// iOS 6 ABOUT CONTENT (Full Screen Version)
// ============================================================================

@Composable
private fun iOS6AboutContent(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFC5C6C8))
    ) {
        // Navigation Bar
        iOS6NavBar(
            title = "About",
            leftAction = {
                iOS6BackButton(onClick = onNavigateBack)
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(6.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4C98D9), Color(0xFF1E62A7))
                        )
                    )
                    .border(1.dp, Color(0xFF2A5A8F), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📍",
                    fontSize = 50.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "LokAlert",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            
            Text(
                text = "Version 1.0.0",
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // About section
            iOS6GroupedSection(header = "ABOUT") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LokAlert is your smart location companion that helps you never miss your destination.",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color.Black,
                            lineHeight = 22.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Set location-based alarms and get notified when you arrive — perfect for bus rides, train commutes, or any journey where you might doze off!",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color.Black,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
            
            // Features section
            iOS6GroupedSection(header = "FEATURES") {
                iOS6FeatureItem(emoji = "🗺️", title = "Smart Maps", description = "Tap anywhere to set an alarm")
                iOS6Separator(startIndent = 52.dp)
                iOS6FeatureItem(emoji = "🔔", title = "Loud Alerts", description = "Wake up even on silent mode")
                iOS6Separator(startIndent = 52.dp)
                iOS6FeatureItem(emoji = "⭐", title = "Favorites", description = "Save your frequent destinations")
                iOS6Separator(startIndent = 52.dp)
                iOS6FeatureItem(emoji = "🎨", title = "Customizable", description = "Choose your alarm style")
            }
            
            // Team section
            iOS6GroupedSection(header = "DEVELOPMENT TEAM") {
                val developers = listOf(
                    "Adamos, Eurika",
                    "Alemaña, Onyx Herod",
                    "Billones, Gerald",
                    "Crisologo, Terence Joefrey",
                    "Mabahin, Ryan",
                    "Royo, Aenard Ollyer"
                )
                
                developers.forEachIndexed { index, name ->
                    iOS6TeamMemberRow(name = name)
                    if (index < developers.size - 1) {
                        iOS6Separator(startIndent = 56.dp)
                    }
                }
            }
            
            // Copyright
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "© 2024 LokAlert Team\nAll rights reserved.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun iOS6FeatureItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )
            )
        }
    }
}

@Composable
private fun iOS6TeamMemberRow(
    name: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with initial
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8E8E93), Color(0xFF6D6D72))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = name,
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.Black
            )
        )
    }
}
