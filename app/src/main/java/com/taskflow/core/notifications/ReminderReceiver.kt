package com.taskflow.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.taskflow.MainActivity
import com.taskflow.R
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.mapper.toDomain
import com.taskflow.data.mapper.toEntity
import com.taskflow.domain.model.ActivityLog
import com.taskflow.domain.model.ReminderEndType
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        when (intent.action) {
            ACTION_COMPLETE -> completeTask(context, taskId)
            ACTION_SNOOZE -> snoozeReminder(context, reminderId, taskId)
            else -> showReminder(context, reminderId, taskId, intent.getBooleanExtra(EXTRA_SNOOZED, false))
        }
    }

    private fun showReminder(context: Context, reminderId: String, taskId: String, fromSnooze: Boolean) {
        ReminderScheduler.ensureChannel(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = TaskFlowDatabase.get(context).dao()
            val task = dao.taskById(taskId)?.toDomain()
            val reminder = dao.reminderById(reminderId)?.toDomain()
            if (task != null && reminder != null && reminder.isActive && !task.isCompleted) {
                val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_taskflow)
                    .setContentTitle(task.title)
                    .setContentText("${task.dueDate?.toLocalTime() ?: reminder.startTime} - TaskFlow")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent(context, taskId))
                    .addAction(R.drawable.ic_taskflow, "Abrir", openPendingIntent(context, taskId))
                    .addAction(R.drawable.ic_taskflow, "Concluir", actionPendingIntent(context, ACTION_COMPLETE, reminderId, taskId))
                    .addAction(R.drawable.ic_taskflow, "Adiar", actionPendingIntent(context, ACTION_SNOOZE, reminderId, taskId))
                    .build()
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(reminderId.hashCode(), notification)
                if (!fromSnooze) {
                    val updated = ReminderEngine.afterDelivery(reminder, LocalDateTime.now())
                    dao.upsertReminders(listOf(updated.toEntity()))
                    if (updated.isActive && updated.nextTriggerAt != null) {
                        ReminderScheduler(context).schedule(updated, updated.nextTriggerAt)
                    }
                }
            }
            pendingResult.finish()
        }
    }

    private fun completeTask(context: Context, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = TaskFlowDatabase.get(context).dao()
            val task = dao.taskById(taskId)?.toDomain()
            if (task != null) {
                dao.upsertTasks(listOf(task.copy(status = TaskStatus.Done, isCompleted = true, completedAt = now(), updatedAt = now()).toEntity()))
                dao.upsertActivity(listOf(ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa concluida por notificacao").toEntity()))
                dao.remindersByTaskId(taskId)
                    .map { it.toDomain() }
                    .filter { it.endType == ReminderEndType.OnTaskDone }
                    .forEach { dao.upsertReminders(listOf(it.copy(isActive = false, updatedAt = now()).toEntity())) }
            }
            pendingResult.finish()
        }
    }

    private fun snoozeReminder(context: Context, reminderId: String, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = TaskFlowDatabase.get(context).dao()
            val reminder = dao.reminderById(reminderId)?.toDomain()
            val task = dao.taskById(taskId)?.toDomain()
            if (reminder != null && task != null && reminder.isActive && !task.isCompleted) {
                val updated = ReminderEngine.afterSnooze(reminder, LocalDateTime.now(), SNOOZE_MINUTES)
                dao.upsertReminders(listOf(updated.toEntity()))
                dao.upsertActivity(listOf(ActivityLog(taskId = taskId, userId = task.createdBy, action = "Lembrete adiado por ${SNOOZE_MINUTES} minutos").toEntity()))
                ReminderScheduler(context).snooze(reminderId, taskId, SNOOZE_MINUTES)
            }
            pendingResult.finish()
        }
    }

    private fun openPendingIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionPendingIntent(context: Context, action: String, reminderId: String, taskId: String): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            "$action:$reminderId".hashCode(),
            intent(context, action, reminderId, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_SHOW = "com.taskflow.reminders.SHOW"
        const val ACTION_COMPLETE = "com.taskflow.reminders.COMPLETE"
        const val ACTION_SNOOZE = "com.taskflow.reminders.SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_SNOOZED = "snoozed"
        const val SNOOZE_MINUTES = 10L

        fun intent(context: Context, action: String, reminderId: String, taskId: String): Intent {
            return Intent(context, ReminderReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TASK_ID, taskId)
            }
        }
    }
}
