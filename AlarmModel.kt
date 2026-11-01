package com.thestudytimer

import java.util.Calendar

data class AlarmModel(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String = "Study alarm",
    val enabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(), // Calendar.SUNDAY .. SATURDAY
    val toneUri: String? = null
)

object AlarmTime {
    fun nextTriggerMillis(alarm: AlarmModel, nowMillis: Long): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.repeatDays.isEmpty()) {
            if (!candidate.after(now)) candidate.add(Calendar.DAY_OF_YEAR, 1)
            return candidate.timeInMillis
        }
        for (offset in 0..7) {
            val day = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (alarm.repeatDays.contains(day.get(Calendar.DAY_OF_WEEK)) && day.after(now)) {
                return day.timeInMillis
            }
        }
        return candidate.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
    }
}
