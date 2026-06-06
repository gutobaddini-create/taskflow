package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.ReminderEndType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryReminderTest {
    @Test
    fun completingTaskDisablesFinishOnCompleteReminders() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val reminder = Reminder(taskId = task.id, userId = repo.users.value.first().id, endType = ReminderEndType.OnTaskDone)

        repo.saveReminder(reminder)
        repo.completeTask(task.id)

        assertTrue(repo.tasks.value.first { it.id == task.id }.isCompleted)
        assertFalse(repo.reminders.value.first { it.id == reminder.id }.isActive)
    }
}
