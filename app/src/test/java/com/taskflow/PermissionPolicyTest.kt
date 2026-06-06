package com.taskflow

import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.UserPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {
    private val task = Task(
        id = "task-1",
        spaceId = "space-1",
        listId = "list-1",
        title = "Enviar proposta",
        createdBy = "manuel",
        assignedTo = "manuel"
    )

    @Test
    fun strongestAcceptedPermissionWins() {
        val invites = listOf(
            Invite(taskId = task.id, createdBy = "manuel", permission = UserPermission.Viewer, acceptedBy = "ana"),
            Invite(taskId = task.id, createdBy = "manuel", permission = UserPermission.Participant, acceptedBy = "ana")
        )

        val permission = PermissionPolicy.acceptedPermission(task.id, "ana", invites)

        assertEquals(UserPermission.Participant, permission)
    }

    @Test
    fun viewerCannotEditOrComment() {
        assertFalse(PermissionPolicy.canEditTask(task, "ana", UserPermission.Viewer))
        assertFalse(PermissionPolicy.canCommentOnTask(task, "ana", UserPermission.Viewer))
    }

    @Test
    fun participantCanCommentWithoutEditing() {
        assertFalse(PermissionPolicy.canEditTask(task, "ana", UserPermission.Participant))
        assertTrue(PermissionPolicy.canCommentOnTask(task, "ana", UserPermission.Participant))
    }
}
