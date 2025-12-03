package com.mobprog.lokalert

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.places.api.Places

@Composable
fun MapDiagnosticsScreen() {
    val context = LocalContext.current
    val diagnostics = remember { runDiagnostics(context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Map Diagnostics",
            style = MaterialTheme.typography.headlineMedium
        )
        
        diagnostics.forEach { (key, value) ->
            DiagnosticItem(key, value)
        }
    }
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (value.contains("✓") || value.contains("OK")) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (value.contains("✗") || value.contains("ERROR")) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun runDiagnostics(context: Context): Map<String, String> {
    val results = mutableMapOf<String, String>()
    
    // Check Google Play Services
    val availability = GoogleApiAvailability.getInstance()
    val playServicesStatus = availability.isGooglePlayServicesAvailable(context)
    results["Google Play Services"] = when (playServicesStatus) {
        ConnectionResult.SUCCESS -> "✓ Available"
        ConnectionResult.SERVICE_MISSING -> "✗ Not installed"
        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "⚠ Update required"
        ConnectionResult.SERVICE_DISABLED -> "✗ Disabled"
        else -> "✗ Error code: $playServicesStatus"
    }
    
    // Check Places API
    results["Places API"] = if (Places.isInitialized()) {
        "✓ Initialized"
    } else {
        "✗ Not initialized"
    }
    
    // Check API Key
    try {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        results["API Key"] = if (apiKey != null) {
            "✓ Found (${apiKey.take(20)}...)"
        } else {
            "✗ Not found in manifest"
        }
    } catch (e: Exception) {
        results["API Key"] = "✗ Error reading: ${e.message}"
    }
    
    // Check Internet
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as 
        android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork
    results["Internet"] = if (network != null) {
        "✓ Connected"
    } else {
        "✗ No connection"
    }
    
    // Check Package Name
    results["Package Name"] = context.packageName
    
    return results
}
