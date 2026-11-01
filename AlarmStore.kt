package com.thestudytimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AlarmStore(context: Context) {
    private val prefs = context.getSharedPreferences("study_timer", Context.MODE_PRIVATE)

    fun load(): List<AlarmModel> {
        val raw = prefs.getString("alarms", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val days = o.optJSONArray("days") ?: JSONArray()
                AlarmModel(
                    id = o.getInt("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    label = o.optString("label", "Study alarm"),
                    enabled = o.optBoolean("enabled", true),
                    repeatDays = (0 until days.length()).map { days.getInt(it) }.toSet(),
                    toneUri = o.optString("tone", "").ifBlank { null }
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(alarms: List<AlarmModel>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            val days = JSONArray()
            alarm.repeatDays.sorted().forEach(days::put)
            array.put(JSONObject().apply {
                put("id", alarm.id); put("hour", alarm.hour); put("minute", alarm.minute)
                put("label", alarm.label); put("enabled", alarm.enabled)
                put("days", days); put("tone", alarm.toneUri ?: "")
            })
        }
        prefs.edit().putString("alarms", array.toString()).apply()
    }

    fun nextId(): Int = (load().maxOfOrNull { it.id } ?: 0) + 1
}
