package com.mobprog.lokalert

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import kotlin.system.exitProcess

/**
 * App restart mechanism using the SIMPLEST and MOST RELIABLE pattern:
 * Start the new activity BEFORE killing the process.
 * 
 * This works because:
 * 1. We start the new activity first (it begins launching)
 * 2. Then we kill ourselves
 * 3. The new activity is already in the launch queue, so it opens
 * 
 * NO AlarmManager, NO BroadcastReceiver, NO complexity.
 */
object AppRestarter {
    
    private const val TAG = "AppRestarter"
    
    /**
     * Restarts the app immediately using the start-then-kill pattern.
     * This is the SIMPLEST and MOST RELIABLE way to restart an Android app.
     */
    fun restartApp(context: Context, targetDesignLanguage: Int) {
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "SIMPLE RESTART - Start activity then kill process")
        Log.d(TAG, "Target theme: ${if (targetDesignLanguage == 1) "iOS6" else "Material3"}")
        Log.d(TAG, "═══════════════════════════════════════════════")
        
        Toast.makeText(context, "Restarting app...", Toast.LENGTH_SHORT).show()
        
        try {
            // Get the launch intent for this package
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            
            if (intent != null) {
                // Configure the intent to clear and restart
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                
                Log.d(TAG, "Starting new instance of app...")
                
                // START THE NEW ACTIVITY FIRST (this is key!)
                context.startActivity(intent)
                
                Log.d(TAG, "New activity started, now killing current process...")
                
                // Then kill the current process
                (context as? Activity)?.finishAffinity()
                
                // Small delay to ensure the new activity has started
                Thread.sleep(150)
                
                // Kill the process - the new activity is already launching
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(0)
            } else {
                Log.e(TAG, "getLaunchIntentForPackage returned null!")
                Toast.makeText(context, "Restart failed - please restart manually", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during restart", e)
            Toast.makeText(context, "Restart failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    fun restartApp(context: Context) {
        restartApp(context, 0)
    }
}