package com.noahlee.alarmapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AlarmStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): MutableList<Alarm> {
        val raw = prefs.getString(KEY_ALARMS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Alarm(
                            id = o.getLong("id"),
                            title = o.getString("title"),
                            triggerAtMillis = o.getLong("time"),
                        ),
                    )
                }
            }.sortedBy { it.triggerAtMillis }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun save(alarms: List<Alarm>) {
        val arr = JSONArray()
        alarms.sortedBy { it.triggerAtMillis }.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("time", a.triggerAtMillis)
                },
            )
        }
        prefs.edit().putString(KEY_ALARMS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "alarm_store"
        private const val KEY_ALARMS = "alarms_json"
    }
}
