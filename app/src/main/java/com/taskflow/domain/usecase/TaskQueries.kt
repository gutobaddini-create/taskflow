package com.taskflow.domain.usecase

import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.now
import java.time.LocalDate

object TaskQueries {
    fun visibleForUser(tasks: List<Task>, userId: String, invites: List<Invite> = emptyList(), referenceTime: Long = now()): List<Task> {
        val invitedTaskIds = invites
            .asSequence()
            .filter { it.acceptedBy == userId && (it.expiresAt == null || it.expiresAt > referenceTime) }
            .map { it.taskId }
            .toSet()
        return tasks.filter { task ->
            task.createdBy == userId ||
                task.assignedTo == userId ||
                userId in task.participants ||
                task.id in invitedTaskIds
        }
    }

    fun today(tasks: List<Task>, date: LocalDate = LocalDate.now()): List<Task> =
        tasks.filter { task ->
            !task.isCompleted && task.dueDate?.toLocalDate() == date
        }

    fun todayOrUnscheduled(tasks: List<Task>, date: LocalDate = LocalDate.now()): List<Task> =
        tasks.filter { task ->
            !task.isCompleted && (task.dueDate == null || task.dueDate.toLocalDate() == date)
        }

    fun upcoming(tasks: List<Task>, date: LocalDate = LocalDate.now()): List<Task> =
        tasks.filter { task ->
            val dueDate = task.dueDate?.toLocalDate()
            !task.isCompleted && dueDate != null && dueDate.isAfter(date)
        }

    fun overdue(tasks: List<Task>, date: LocalDate = LocalDate.now()): List<Task> =
        tasks.filter { task ->
            val dueDate = task.dueDate?.toLocalDate()
            !task.isCompleted && dueDate != null && dueDate.isBefore(date)
        }

    fun completed(tasks: List<Task>): List<Task> =
        tasks.filter { it.isCompleted || it.status == TaskStatus.Done }

    fun inList(tasks: List<Task>, listId: String): List<Task> =
        tasks.filter { it.listId == listId }

    fun inSpace(tasks: List<Task>, spaceId: String): List<Task> =
        tasks.filter { it.spaceId == spaceId }
}
