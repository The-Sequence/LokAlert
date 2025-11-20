package com.mobprog.lokalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmName = intent.getStringExtra("ALARM_NAME") ?: "Alarm"

        // Create the full-screen intent
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TRIGGERED_BY_ALARM", true)
            putExtra("ALARM_NAME", alarmName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, alarmId.toInt(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure you have this drawable
            .setContentTitle(alarmName)
            .setContentText("Your alarm is ringing!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        // Create the channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("alarm_channel", "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for alarms"
                setSound(null, null) // Sound is handled by the service
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Post the notification
        notificationManager.notify(alarmId.toInt(), notificationBuilder.build())

        // Start the sound service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_SOUND_URI", intent.getStringExtra("ALARM_SOUND_URI"))
        }
        context.startService(serviceIntent)
    }
}
