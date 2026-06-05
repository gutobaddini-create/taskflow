package com.taskflow.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskflow.domain.model.Reminder
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder, triggerAt: LocalDateTime? = ReminderEngine.nextOccurrence(reminder)) {
        if (triggerAt == null) return
        val intent = ReminderReceiver.intent(context, ReminderReceiver.ACTION_SHOW, reminder.id, reminder.taskId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun snooze(reminderId: String, taskId: String, minutes: Long = 10) {
        val intent = ReminderReceiver.intent(context, ReminderReceiver.ACTION_SHOW, reminderId, taskId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${reminderId}:snooze".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = System.currentTimeMillis() + minutes * 60_000
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancel(reminderId: String, taskId: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            ReminderReceiver.intent(context, ReminderReceiver.ACTION_SHOW, reminderId, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val CHANNEL_ID = "taskflow_reminders"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "Lembretes TaskFlow", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alertas locais de tarefas e recorrencias"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
