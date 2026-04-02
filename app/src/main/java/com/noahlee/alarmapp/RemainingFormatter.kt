package com.noahlee.alarmapp

import android.content.Context

object RemainingFormatter {

    fun format(context: Context, triggerAtMillis: Long): String {
        val delta = triggerAtMillis - System.currentTimeMillis()
        if (delta <= 0L) return context.getString(R.string.remaining_done)
        if (delta < 15_000L) return context.getString(R.string.remaining_soon)
        if (delta < 60_000L) {
            val sec = delta / 1000L
            return "${sec}초 남음"
        }
        val minutes = delta / 60_000L
        val days = minutes / (24 * 60)
        val hours = (minutes / 60) % 24
        val mins = minutes % 60
        val parts = buildList {
            if (days > 0) add("${days}일")
            if (hours > 0) add("${hours}시간")
            if (mins > 0) add("${mins}분")
        }
        val body = if (parts.isEmpty()) "${mins}분" else parts.joinToString(" ")
        return "$body 남음"
    }
}
