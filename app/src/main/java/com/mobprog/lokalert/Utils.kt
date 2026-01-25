package com.mobprog.lokalert

import android.content.Context
import android.location.Address
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Utility functions for the LokAlert app
 */

/**
 * Get a readable location name from an Address object
 */
fun getReadableLocationName(address: Address): String {
    return buildString {
        // Try to get the most specific available name
        val featureName = address.featureName
        val thoroughfare = address.thoroughfare
        val subLocality = address.subLocality
        val locality = address.locality
        val subAdminArea = address.subAdminArea
        val adminArea = address.adminArea
        
        when {
            !featureName.isNullOrBlank() && featureName != thoroughfare -> append(featureName)
            !thoroughfare.isNullOrBlank() -> append(thoroughfare)
            !subLocality.isNullOrBlank() -> append(subLocality)
            !locality.isNullOrBlank() -> append(locality)
            !subAdminArea.isNullOrBlank() -> append(subAdminArea)
            !adminArea.isNullOrBlank() -> append(adminArea)
            else -> append("Unknown Location")
        }
        
        // Add city/locality if available and not already included
        if (!locality.isNullOrBlank() && !toString().contains(locality)) {
            append(", $locality")
        }
    }
}

/**
 * Get a short alarm name from a location name
 */
fun getShortAlarmName(locationName: String): String {
    // Take first part before comma, or first 30 characters
    val parts = locationName.split(",")
    val shortName = parts[0].trim()
    return if (shortName.length > 30) {
        shortName.take(27) + "..."
    } else {
        shortName
    }
}

/**
 * Get ringtone title for display in Maps screen
 */
fun getMapRingtoneTitle(context: Context, soundUri: String): String {
    if (soundUri.isEmpty()) return "Default Alarm"
    
    return try {
        val uri = Uri.parse(soundUri)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.getTitle(context) ?: "Custom Sound"
    } catch (e: Exception) {
        "Custom Sound"
    }
}

/**
 * A clickable row for picking items like ringtones
 */
@Composable
fun MapsPickerRow(
    label: String,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
