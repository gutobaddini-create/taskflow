package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.ReminderEndType
import com.taskflow.domain.model.ReminderType
import com.taskflow.domain.model.RecurrenceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryReminderTest {
    @Test
    fun completingTaskDisablesTaskReminders() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val finishOnComplete = Reminder(taskId = task.id, userId = repo.users.value.first().id, endType = ReminderEndType.OnTaskDone)
        val recurring = Reminder(taskId = task.id, userId = repo.users.value.first().id, type = ReminderType.Recurring, recurrenceType = RecurrenceType.Daily)

        repo.saveReminder(finishOnComplete)
        repo.saveReminder(recurring)
        repo.completeTask(task.id)

        assertTrue(repo.tasks.value.first { it.id == task.id }.isCompleted)
        assertFalse(repo.reminders.value.first { it.id == finishOnComplete.id }.isActive)
        assertFalse(repo.reminders.value.first { it.id == recurring.id }.isActive)
    }
}
