package com.thestudytimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "THE STUDY TIMER"
        val id = intent.getIntExtra("alarm_id", intent.getIntExtra("timer_id", 1))
        val tone = intent.getStringExtra("tone")
        val channelId = "study_timer_alarms"
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Study timer alarm notifications"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    tone?.let { android.net.Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
            })
        }
        val fullScreen = PendingIntent.getActivity(
            context, id, Intent(context, RingingActivity::class.java).apply {
                putExtra("label", label)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("It's time.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreen, true)
            .build()
        nm.notify(id, notification)
        if (intent.getBooleanExtra("repeat", false)) {
            val alarm = AlarmStore(context).load().firstOrNull { it.id == id }
            if (alarm != null) AlarmScheduler(context).schedule(alarm)
        }
    }
}
