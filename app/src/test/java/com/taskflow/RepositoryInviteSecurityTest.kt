package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.UserPermission
import com.taskflow.domain.model.now
import org.junit.Assert.assertFalse
import org.junit.Test

class RepositoryInviteSecurityTest {
    @Test
    fun expiredInviteCannotBeAccepted() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val user = repo.users.value.last()
        val invite = Invite(taskId = task.id, createdBy = task.createdBy, permission = UserPermission.Owner, expiresAt = now() - 1)

        repo.createInvite(invite)
        repo.acceptInvite(invite.token, user.id)

        assertFalse(repo.invites.value.first { it.token == invite.token }.acceptedBy == user.id)
        assertFalse(repo.tasks.value.first { it.id == task.id }.participants.contains(user.id))
    }
}
