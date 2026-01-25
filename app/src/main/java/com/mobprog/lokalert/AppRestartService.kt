package com.mobprog.lokalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * A background service that restarts the app.
 * This service runs in a separate process so it survives the main app closing.
 * It also handles icon switching safely while the main app is closed.
 * 
 * On Android 12+, this must run as a foreground service.
 */
class AppRestartService : Service() {
    
    companion object {
        private const val TAG = "AppRestartService"
        private const val EXTRA_RESTART_DELAY = "restart_delay"
        private const val EXTRA_TARGET_DESIGN_LANGUAGE = "target_design_language"
        private const val EXTRA_SHOULD_CHANGE_ICON = "should_change_icon"
        private const val DEFAULT_RESTART_DELAY = 1000L
        
        private const val NOTIFICATION_CHANNEL_ID = "app_restart_channel"
        private const val NOTIFICATION_ID = 9999
        
        /**
         * Start the restart service which will restart the app after a delay
         * @param context The context
         * @param delayMs Delay before restarting (default 1000ms)
         * @param targetDesignLanguage The target design language (0 = Material, 1 = iOS6), or -1 to skip icon change
         */
        fun startRestart(
            context: Context, 
            delayMs: Long = DEFAULT_RESTART_DELAY,
            targetDesignLanguage: Int = -1
        ) {
            val intent = Intent(context, AppRestartService::class.java).apply {
                putExtra(EXTRA_RESTART_DELAY, delayMs)
                putExtra(EXTRA_TARGET_DESIGN_LANGUAGE, targetDesignLanguage)
                putExtra(EXTRA_SHOULD_CHANGE_ICON, targetDesignLanguage >= 0)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Restart service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start restart service", e)
            }
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate - process: ${android.os.Process.myPid()}")
        
        // For Android 8.0+, we must start as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        
        val delay = intent?.getLongExtra(EXTRA_RESTART_DELAY, DEFAULT_RESTART_DELAY) ?: DEFAULT_RESTART_DELAY
        val targetDesignLanguage = intent?.getIntExtra(EXTRA_TARGET_DESIGN_LANGUAGE, -1) ?: -1
        val shouldChangeIcon = intent?.getBooleanExtra(EXTRA_SHOULD_CHANGE_ICON, false) ?: false
        
        handler.postDelayed({
            Log.d(TAG, "Restart delay elapsed, proceeding with restart")
            
            // Change icon FIRST while main app is closed - this is safe
            if (shouldChangeIcon && targetDesignLanguage >= 0) {
                try {
                    Log.d(TAG, "Changing icon to: ${if (targetDesignLanguage == 1) "iOS6" else "Material3"}")
                    IconManager.setAppIcon(applicationContext, targetDesignLanguage)
                    // Give system time to register the icon change
                    Thread.sleep(300)
                    Log.d(TAG, "Icon change completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error changing icon", e)
                }
            }
            
            // Then restart the app
            restartMainApp(targetDesignLanguage)
            
            // Stop the service (minSdk is 24 / Android 7.0)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d(TAG, "Service stopped")
        }, delay)
        
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Restart",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Handles app restart after theme change"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Switching theme...")
            .setContentText("Restarting app with new theme")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun restartMainApp(targetDesignLanguage: Int) {
        try {
            // Determine which activity alias to launch based on target design language
            val aliasName = if (targetDesignLanguage == 1) {
                "com.mobprog.lokalert.MainActivityiOS6"
            } else {
                "com.mobprog.lokalert.MainActivityMaterial"
            }
            
            Log.d(TAG, "Attempting to restart via alias: $aliasName")
            
            // Create intent directly to the activity alias
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, aliasName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            startActivity(intent)
            Log.d(TAG, "App restart initiated successfully via $aliasName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart via alias, trying getLaunchIntentForPackage", e)
            
            // Fallback: try getLaunchIntentForPackage
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(launchIntent)
                    Log.d(TAG, "App restart initiated via getLaunchIntentForPackage")
                } else {
                    Log.e(TAG, "getLaunchIntentForPackage returned null")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "All restart methods failed", e2)
            }
        }
    }
}
