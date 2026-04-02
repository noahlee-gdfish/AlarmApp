package com.noahlee.alarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId != -1L) {
            val store = AlarmStore(appContext)
            val list = store.load()
            if (list.removeAll { it.id == alarmId }) {
                store.save(list)
            }
            AlarmScheduler.cancel(appContext, alarmId)
            appContext.sendBroadcast(
                Intent(ACTION_ALARMS_CHANGED).setPackage(appContext.packageName),
            )
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.app_name)
        createChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.remaining_soon))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .build()
        NotificationManagerCompat.from(context).notify(
            (intent.getLongExtra(EXTRA_ALARM_ID, 0L) % Int.MAX_VALUE).toInt(),
            notification,
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ch = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TITLE = "extra_title"
        const val CHANNEL_ID = "warm_alarm_channel"
        const val ACTION_ALARMS_CHANGED = "com.noahlee.alarmapp.ACTION_ALARMS_CHANGED"
    }
}
