package com.taskflow

import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.UserPermission
import com.taskflow.domain.usecase.TaskQueries
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskQueriesTest {
    private val today = LocalDate.of(2026, 6, 6)
    private val visibleTask = task("visible", createdBy = "ana")
    private val assignedTask = task("assigned", assignedTo = "ana")
    private val participantTask = task("participant", participants = listOf("ana"))
    private val invitedTask = task("invited", createdBy = "manuel")
    private val hiddenTask = task("hidden", createdBy = "manuel")

    @Test
    fun visibleForUserIncludesOwnershipAssignmentParticipantsAndAcceptedInvites() {
        val result = TaskQueries.visibleForUser(
            tasks = listOf(visibleTask, assignedTask, participantTask, invitedTask, hiddenTask),
            userId = "ana",
            invites = listOf(Invite(taskId = invitedTask.id, createdBy = "manuel", permission = UserPermission.Viewer, acceptedBy = "ana")),
            referenceTime = 1_000
        )

        assertEquals(listOf("visible", "assigned", "participant", "invited"), result.map { it.id })
    }

    @Test
    fun visibleForUserIgnoresExpiredInvites() {
        val result = TaskQueries.visibleForUser(
            tasks = listOf(invitedTask),
            userId = "ana",
            invites = listOf(Invite(taskId = invitedTask.id, createdBy = "manuel", permission = UserPermission.Viewer, acceptedBy = "ana", expiresAt = 999)),
            referenceTime = 1_000
        )

        assertEquals(emptyList<String>(), result.map { it.id })
    }

    @Test
    fun dateBucketsIgnoreCompletedTasks() {
        val tasks = listOf(
            task("today", dueDate = today.atTime(9, 0)),
            task("tomorrow", dueDate = today.plusDays(1).atTime(9, 0)),
            task("yesterday", dueDate = today.minusDays(1).atTime(9, 0)),
            task("done-today", status = TaskStatus.Done, isCompleted = true, dueDate = today.atTime(9, 0))
        )

        assertEquals(listOf("today"), TaskQueries.today(tasks, today).map { it.id })
        assertEquals(listOf("today"), TaskQueries.todayOrUnscheduled(tasks, today).map { it.id })
        assertEquals(listOf("tomorrow"), TaskQueries.upcoming(tasks, today).map { it.id })
        assertEquals(listOf("yesterday"), TaskQueries.overdue(tasks, today).map { it.id })
        assertEquals(listOf("done-today"), TaskQueries.completed(tasks).map { it.id })
    }

    @Test
    fun todayOrUnscheduledIncludesTasksWithoutDueDate() {
        val tasks = listOf(task("today", dueDate = today.atTime(9, 0)), task("no-date", dueDate = null))

        assertEquals(listOf("today", "no-date"), TaskQueries.todayOrUnscheduled(tasks, today).map { it.id })
    }

    @Test
    fun inListKeepsOnlyRequestedList() {
        val tasks = listOf(task("a", listId = "prazos"), task("b", listId = "compras"))

        assertEquals(listOf("a"), TaskQueries.inList(tasks, "prazos").map { it.id })
    }

    @Test
    fun inSpaceKeepsOnlyRequestedSpace() {
        val tasks = listOf(task("a"), task("b").copy(spaceId = "other-space"))

        assertEquals(listOf("a"), TaskQueries.inSpace(tasks, "space").map { it.id })
    }

    private fun task(
        id: String,
        createdBy: String = "ana",
        assignedTo: String? = null,
        participants: List<String> = emptyList(),
        status: TaskStatus = TaskStatus.Todo,
        isCompleted: Boolean = false,
        dueDate: LocalDateTime? = null,
        listId: String = "prazos"
    ) = Task(
        id = id,
        spaceId = "space",
        listId = listId,
        title = id,
        createdBy = createdBy,
        assignedTo = assignedTo,
        participants = participants,
        status = status,
        isCompleted = isCompleted,
        dueDate = dueDate
    )
}
