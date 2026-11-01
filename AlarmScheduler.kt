package com.thestudytimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: AlarmModel) {
        if (!alarm.enabled) { cancel(alarm.id); return }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("label", alarm.label)
            putExtra("tone", alarm.toneUri)
            putExtra("repeat", alarm.repeatDays.isNotEmpty())
        }
        val pending = PendingIntent.getBroadcast(
            context, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = AlarmTime.nextTriggerMillis(alarm, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancel(id: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) manager.cancel(pending)
    }

    fun scheduleAll() {
        AlarmStore(context).load().filter { it.enabled }.forEach(::schedule)
    }

    fun scheduleTimer(id: Int, label: String, atMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("timer_id", id); putExtra("label", label)
        }
        val pending = PendingIntent.getBroadcast(
            context, 900000 + id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
    }
}
