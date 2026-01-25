package com.mobprog.lokalert

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that handles app restart after theme change.
 * 
 * SIMPLIFIED APPROACH:
 * - Launches MainActivity directly (always available as launcher)
 * - No alias detection or complex logic
 * - Reliable restart every time
 */
class AppRestartReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AppRestartReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "AppRestartReceiver triggered!")
        Log.d(TAG, "Launching MainActivity directly...")
        Log.d(TAG, "═══════════════════════════════════════════════")
        
        try {
            // Launch MainActivity directly - it's always available as a launcher
            val restartIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(context.packageName, "com.mobprog.lokalert.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            context.startActivity(restartIntent)
            Log.d(TAG, "✓ MainActivity launched successfully!")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error launching MainActivity", e)
            
            // Fallback to getLaunchIntentForPackage
            try {
                val fallbackIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(fallbackIntent)
                    Log.d(TAG, "✓ Fallback launch successful")
                } else {
                    Log.e(TAG, "✗ Fallback intent is null")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "✗ Fallback also failed", e2)
            }
        }
    }
}
