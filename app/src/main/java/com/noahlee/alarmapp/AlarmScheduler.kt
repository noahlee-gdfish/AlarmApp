package com.noahlee.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        if (alarm.triggerAtMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(context, alarm)
        val show = PendingIntent.getActivity(
            context,
            showRequestCode(alarm.id),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(alarm.triggerAtMillis, show), pi)
    }

    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            broadcastRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
    }

    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        val now = System.currentTimeMillis()
        alarms.filter { it.triggerAtMillis > now }.forEach { schedule(context, it) }
    }

    private fun alarmPendingIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, alarm.title)
        }
        return PendingIntent.getBroadcast(
            context,
            broadcastRequestCode(alarm.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun broadcastRequestCode(id: Long): Int = (id xor (id ushr 32)).toInt() and 0x7FFFFFFF

    private fun showRequestCode(id: Long): Int = (broadcastRequestCode(id) + 1) and 0x7FFFFFFF
}
