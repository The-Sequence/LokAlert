package com.mobprog.lokalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private var ringtone: Ringtone? = null
        const val ACTION_DISMISS = "com.mobprog.lokalert.DISMISS_ALARM"

        fun stopRingtone() {
            ringtone?.stop()
            ringtone = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmId = intent.getLongExtra("ALARM_ID", -1)

        if (intent.action == ACTION_DISMISS) {
            stopRingtone()
            if (alarmId != -1L) {
                notificationManager.cancel(alarmId.toInt())
            }
            return
        }

        // Stop any previously playing ringtone before starting a new one
        stopRingtone()

        val soundUriString = intent.getStringExtra("ALARM_SOUND_URI")
        val soundUri = soundUriString?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Create notification channel if it doesn't exist
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("lok_alert_channel", "LokAlert Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for LokAlert Alarms"
                setSound(null, null) // Sound is handled manually
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create dismiss intent
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("ALARM_ID", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, alarmId.toInt(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, "lok_alert_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("LokAlert")
            .setContentText("Your alarm is going off!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .setAutoCancel(true)
            .build()

        // Notify
        notificationManager.notify(alarmId.toInt(), notification)

        // Play ringtone
        try {
            ringtone = RingtoneManager.getRingtone(context, soundUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
