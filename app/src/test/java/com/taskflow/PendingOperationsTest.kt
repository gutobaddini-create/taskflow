package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentType
import com.taskflow.domain.model.CustomField
import com.taskflow.domain.model.CustomFieldType
import com.taskflow.domain.model.ChecklistItem
import com.taskflow.domain.model.Comment
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperationType
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskLink
import com.taskflow.domain.model.UserPermission
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingOperationsTest {
    @Test
    fun taskCreateAndUpdateAreQueued() {
        val repo = InMemoryTaskFlowRepository()
        val list = repo.lists.value.first()
        val user = repo.users.value.first()
        val task = Task(spaceId = list.spaceId, listId = list.id, title = "Offline", createdBy = user.id)

        repo.createTask(task)
        repo.updateTask(task.copy(description = "Editada offline"))

        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Task && it.entityId == task.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Task && it.entityId == task.id && it.operation == PendingOperationType.Update })
    }

    @Test
    fun materialsAreQueuedForFutureSync() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val user = repo.users.value.first()
        val attachment = Attachment(taskId = task.id, uploadedBy = user.id, fileName = "local.pdf", originalFileName = "local.pdf", fileType = AttachmentType.Pdf, mimeType = "application/pdf", fileSize = 42, storagePath = "local/local.pdf")
        val link = TaskLink(taskId = task.id, createdBy = user.id, title = "Referencia", url = "https://taskflow.local/referencia")
        val field = CustomField(taskId = task.id, fieldName = "Contato", fieldType = CustomFieldType.Phone, fieldValue = "123", createdBy = user.id)
        val checklist = ChecklistItem(taskId = task.id, title = "Conferir")

        repo.addAttachment(attachment)
        repo.addLink(link)
        repo.addCustomField(field)
        repo.addChecklistItem(checklist)

        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Attachment && it.entityId == attachment.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Link && it.entityId == link.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.CustomField && it.entityId == field.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Checklist && it.entityId == checklist.id && it.operation == PendingOperationType.Create })
    }

    @Test
    fun attachmentDeleteIsQueuedForFutureSync() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val user = repo.users.value.first()
        val attachment = Attachment(taskId = task.id, uploadedBy = user.id, fileName = "delete.pdf", originalFileName = "delete.pdf", fileType = AttachmentType.Pdf, mimeType = "application/pdf", fileSize = 42, storagePath = "local/delete.pdf")

        repo.addAttachment(attachment)
        repo.deleteAttachment(attachment.id)

        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Attachment && it.entityId == attachment.id && it.operation == PendingOperationType.Delete })
    }

    @Test
    fun collaborationEntitiesAreQueuedForFutureSync() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val user = repo.users.value.first()
        val reminder = Reminder(taskId = task.id, userId = user.id)
        val comment = Comment(taskId = task.id, authorId = user.id, text = "Confirmado")
        val invite = Invite(taskId = task.id, createdBy = user.id, permission = UserPermission.Viewer)

        repo.saveReminder(reminder)
        repo.addComment(comment)
        repo.createInvite(invite)
        repo.acceptInvite(invite.token, repo.users.value.last().id)

        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Reminder && it.entityId == reminder.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Comment && it.entityId == comment.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Invite && it.entityId == invite.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Invite && it.entityId == invite.id && it.operation == PendingOperationType.Update })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Task && it.entityId == task.id && it.operation == PendingOperationType.Update })
    }

    @Test
    fun spacesAndListsAreQueuedForFutureSync() {
        val repo = InMemoryTaskFlowRepository()
        val originalSpaceIds = repo.spaces.value.map { it.id }.toSet()

        repo.createSpace("Financeiro")
        val createdSpace = repo.spaces.value.first { it.id !in originalSpaceIds }
        repo.createList(createdSpace.id, "Notas")
        val createdList = repo.lists.value.first { it.spaceId == createdSpace.id }

        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Space && it.entityId == createdSpace.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.List && it.entityId == createdList.id && it.operation == PendingOperationType.Create })
    }
}
