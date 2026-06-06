package com.taskflow

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskflow.core.notifications.ReminderScheduler
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.mapper.toEntity
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.ReminderType
import com.taskflow.domain.model.Space
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskList
import com.taskflow.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ReminderNotificationInstrumentedTest {
    @Test
    fun scheduledReminderPostsNotificationNearTriggerTime() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderScheduler.ensureChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        val dao = TaskFlowDatabase.get(context).dao()
        val user = User(name = "QA Notification", email = "qa-notification@taskflow.local")
        val space = Space(name = "QA Notifications", ownerId = user.id, members = listOf(user.id))
        val list = TaskList(spaceId = space.id, name = "Alarmes")
        val task = Task(spaceId = space.id, listId = list.id, title = "QA_notification_alarm", createdBy = user.id, assignedTo = user.id)
        val reminder = Reminder(taskId = task.id, userId = user.id, type = ReminderType.OneTime)
        val triggerAt = LocalDateTime.now().plusSeconds(5)

        dao.upsertUsers(listOf(user.toEntity()))
        dao.upsertSpaces(listOf(space.toEntity()))
        dao.upsertLists(listOf(list.toEntity()))
        dao.upsertTasks(listOf(task.toEntity()))
        dao.upsertReminders(listOf(reminder.copy(nextTriggerAt = triggerAt).toEntity()))

        ReminderScheduler(context).schedule(reminder, triggerAt)

        val deadline = System.currentTimeMillis() + 20_000
        var posted = false
        while (System.currentTimeMillis() < deadline && !posted) {
            Thread.sleep(1_000)
            posted = notificationManager.activeNotifications.any {
                it.id == reminder.id.hashCode() && it.notification.extras.getString("android.title") == task.title
            }
        }

        assertTrue("Expected TaskFlow reminder notification to be posted by AlarmManager", posted)
    }
}
