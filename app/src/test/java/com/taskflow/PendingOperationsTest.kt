package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentType
import com.taskflow.domain.model.CustomField
import com.taskflow.domain.model.CustomFieldType
import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperationType
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskLink
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

        repo.addAttachment(attachment)
        repo.addLink(link)
        repo.addCustomField(field)

        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Attachment && it.entityId == attachment.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.Link && it.entityId == link.id && it.operation == PendingOperationType.Create })
        assertTrue(repo.pendingOperations.value.any { it.entity == PendingEntityType.CustomField && it.entityId == field.id && it.operation == PendingOperationType.Create })
    }
}
