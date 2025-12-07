package com.mobprog.lokalert

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutUsScreen(onDismiss: () -> Unit = {}, isEmbedded: Boolean = false) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600
    
    val content = @Composable {
        if (isWideScreen || isEmbedded) {
            // Wide screen layout (side-by-side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left side: App icon and title
                Column(
                    modifier = Modifier.weight(0.35f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "LokAlert Icon",
                        modifier = Modifier.size(if (isEmbedded) 120.dp else 100.dp)
                    )
                    Text(
                        "LokAlert",
                        fontSize = if (isEmbedded) 22.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                
                // Right side: Message and names
                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "LokAlert was crafted with care and dedication by a team of students who poured their hearts into creating a reliable location-based alarm app. We hope it serves you well on your journeys.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Developed by:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val names = listOf(
                        "Aenard James P. Barcenal",
                        "Julianz Amiel G. Bacani",
                        "Moises Jr. F. Fernandez",
                        "Myles Brylle A. Peralta",
                        "Kurt Daniel F. Quintos",
                        "Xander Zyrel A. Sinco"
                    )
                    
                    names.forEach { name ->
                        Text(name, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        } else {
            // Narrow screen layout (stacked)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "LokAlert Icon",
                    modifier = Modifier.size(80.dp)
                )
                Text(
                    "LokAlert",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text(
                    "LokAlert was crafted with care and dedication by a team of students who poured their hearts into creating a reliable location-based alarm app. We hope it serves you well on your journeys.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Developed by:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                val names = listOf(
                    "Aenard James P. Barcenal",
                    "Julianz Amiel G. Bacani",
                    "Moises Jr. F. Fernandez",
                    "Myles Brylle A. Peralta",
                    "Kurt Daniel F. Quintos",
                    "Xander Zyrel A. Sinco"
                )
                
                names.forEach { name ->
                    Text(name, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
    
    if (isEmbedded) {
        // Embedded in two-pane layout - no dialog
        Column {
            Text("About LokAlert", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    } else {
        // Dialog mode
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("About LokAlert")
                }
            },
            text = { content() },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}
