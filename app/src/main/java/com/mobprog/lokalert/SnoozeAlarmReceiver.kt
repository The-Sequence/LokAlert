package com.mobprog.lokalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that handles snooze alarms.
 * When triggered, it re-opens the AlarmOverlayActivity to show the alarm again.
 */
class SnoozeAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmName = intent.getStringExtra("ALARM_NAME") ?: "Location Alarm"
        val soundUri = intent.getStringExtra("SOUND_URI") ?: ""
        val isGradualVolume = intent.getBooleanExtra("IS_GRADUAL_VOLUME", false)
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        
        // Launch the alarm overlay activity again
        val alarmIntent = Intent(context, AlarmOverlayActivity::class.java).apply {
            putExtra("ALARM_NAME", alarmName)
            putExtra("SOUND_URI", soundUri)
            putExtra("IS_GRADUAL_VOLUME", isGradualVolume)
            putExtra("LATITUDE", latitude)
            putExtra("LONGITUDE", longitude)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        context.startActivity(alarmIntent)
    }
}
