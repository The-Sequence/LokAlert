package com.mobprog.lokalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var ringtone: Ringtone? = null
    private val channelId = "ALARM_CHANNEL_HIGH_PRIORITY"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmName = intent?.getStringExtra("ALARM_NAME") ?: "Alarm"
        val soundUriString = intent?.getStringExtra("ALARM_SOUND_URI")

        // 1. Create Notification Channel (Required)
        createNotificationChannel()

        // 2. Create Full Screen Intent (To wake up MainActivity)
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("TRIGGERED_BY_ALARM", true)
            putExtra("ALARM_NAME", alarmName)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build the Notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(alarmName)
            .setContentText("Swipe or Tap to dismiss")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this icon exists in drawable
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Wakes the screen
            .build()

        // 4. START FOREGROUND (Crucial Fix for Crash)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 14 requires specifying the type
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, notification)
        }

        // 5. Play Ringtone
        playRingtone(soundUriString)

        return START_STICKY
    }

    private fun playRingtone(uriString: String?) {
        try {
            val soundUri = if (uriString != null) Uri.parse(uriString) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            ringtone?.stop()
            ringtone = RingtoneManager.getRingtone(applicationContext, soundUri).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        ringtone = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "High Priority Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows full screen alarms"
                setSound(null, null) // We play sound manually via RingtoneManager
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}