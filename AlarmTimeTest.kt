package com.thestudytimer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AlarmTimeTest {
    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun oneTimeAlarmMovesToTomorrowWhenTimeHasPassed() {
        val now = millis(2026, Calendar.JANUARY, 5, 8, 0)
        val alarm = AlarmModel(1, 7, 30)
        val expected = millis(2026, Calendar.JANUARY, 6, 7, 30)
        assertEquals(expected, AlarmTime.nextTriggerMillis(alarm, now))
    }

    @Test fun oneTimeAlarmCanFireLaterToday() {
        val now = millis(2026, Calendar.JANUARY, 5, 6, 0)
        val alarm = AlarmModel(1, 7, 30)
        val expected = millis(2026, Calendar.JANUARY, 5, 7, 30)
        assertEquals(expected, AlarmTime.nextTriggerMillis(alarm, now))
    }

    @Test fun repeatingAlarmFindsNextSelectedDay() {
        val now = millis(2026, Calendar.JANUARY, 5, 8, 0) // Monday
        val alarm = AlarmModel(1, 7, 30, repeatDays = setOf(Calendar.WEDNESDAY))
        val expected = millis(2026, Calendar.JANUARY, 7, 7, 30)
        assertEquals(expected, AlarmTime.nextTriggerMillis(alarm, now))
    }
}
