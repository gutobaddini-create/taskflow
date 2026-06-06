package com.taskflow

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.taskflow.core.notifications.ReminderReceiver
import com.taskflow.core.notifications.ReminderScheduler
import com.taskflow.data.local.TaskFlowDao
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.mapper.toEntity
import com.taskflow.data.mapper.toDomain
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.ReminderEndType
import com.taskflow.domain.model.ReminderType
import com.taskflow.domain.model.Space
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskList
import com.taskflow.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ReminderNotificationInstrumentedTest {
    @get:Rule
    val notificationPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

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
        val fixture = dao.createReminderFixture("QA_notification_alarm")
        val reminder = fixture.reminder
        val triggerAt = LocalDateTime.now().plusSeconds(5)

        dao.upsertReminders(listOf(reminder.copy(nextTriggerAt = triggerAt).toEntity()))

        ReminderScheduler(context).schedule(reminder, triggerAt)

        val deadline = System.currentTimeMillis() + 20_000
        var posted = false
        while (System.currentTimeMillis() < deadline && !posted) {
            Thread.sleep(1_000)
            posted = notificationManager.activeNotifications.any {
                it.id == reminder.id.hashCode() && it.notification.extras.getString("android.title") == fixture.task.title
            }
        }

        assertTrue("Expected TaskFlow reminder notification to be posted by AlarmManager", posted)
    }

    @Test
    fun completeNotificationActionMarksTaskDone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = TaskFlowDatabase.get(context).dao()
        val fixture = dao.createReminderFixture("QA_notification_complete")

        context.sendBroadcast(ReminderReceiver.intent(context, ReminderReceiver.ACTION_COMPLETE, fixture.reminder.id, fixture.task.id))

        assertEventuallyTrue("Expected notification complete action to mark the task as done") {
            dao.taskById(fixture.task.id)?.toDomain()?.isCompleted == true
        }
    }

    @Test
    fun snoozeNotificationActionStoresNextTrigger() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = TaskFlowDatabase.get(context).dao()
        val fixture = dao.createReminderFixture("QA_notification_snooze", reminderEndType = ReminderEndType.Never)

        context.sendBroadcast(ReminderReceiver.intent(context, ReminderReceiver.ACTION_SNOOZE, fixture.reminder.id, fixture.task.id))

        assertEventuallyTrue("Expected notification snooze action to persist a future trigger") {
            dao.reminderById(fixture.reminder.id)?.toDomain()?.nextTriggerAt?.isAfter(LocalDateTime.now()) == true
        }
    }

    private suspend fun TaskFlowDao.createReminderFixture(
        title: String,
        reminderEndType: ReminderEndType = ReminderEndType.OnTaskDone
    ): ReminderFixture {
        val user = User(name = "QA Notification", email = "${title.lowercase()}@taskflow.local")
        val space = Space(name = "QA Notifications", ownerId = user.id, members = listOf(user.id))
        val list = TaskList(spaceId = space.id, name = "Alarmes")
        val task = Task(spaceId = space.id, listId = list.id, title = title, createdBy = user.id, assignedTo = user.id)
        val reminder = Reminder(taskId = task.id, userId = user.id, type = ReminderType.OneTime, endType = reminderEndType)

        upsertUsers(listOf(user.toEntity()))
        upsertSpaces(listOf(space.toEntity()))
        upsertLists(listOf(list.toEntity()))
        upsertTasks(listOf(task.toEntity()))
        upsertReminders(listOf(reminder.toEntity()))
        return ReminderFixture(task, reminder)
    }

    private suspend fun assertEventuallyTrue(message: String, condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        var matched = condition()
        while (System.currentTimeMillis() < deadline && !matched) {
            Thread.sleep(250)
            matched = condition()
        }
        assertTrue(message, matched)
    }

    private data class ReminderFixture(val task: Task, val reminder: Reminder)
}
