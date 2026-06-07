package com.taskflow.data.repository

import android.content.Context
import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.core.notifications.ReminderScheduler
import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.core.security.AttachmentSecurity
import com.taskflow.data.local.TaskFlowDao
import com.taskflow.data.mapper.*
import com.taskflow.domain.model.*
import com.taskflow.domain.repository.TaskFlowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class LocalTaskFlowRepository(
    private val dao: TaskFlowDao,
    context: Context,
    private val scope: CoroutineScope
) : TaskFlowRepository {
    private val reminderScheduler = ReminderScheduler(context.applicationContext)
    private suspend fun logActivity(taskId: String, userId: String, action: String) {
        dao.upsertActivity(listOf(ActivityLog(taskId = taskId, userId = userId, action = action).toEntity()))
    }

    override val users = MutableStateFlow(emptyList<User>())
    override val spaces = MutableStateFlow(emptyList<Space>())
    override val lists = MutableStateFlow(emptyList<TaskList>())
    override val tasks = MutableStateFlow(emptyList<Task>())
    override val reminders = MutableStateFlow(emptyList<Reminder>())
    override val attachments = MutableStateFlow(emptyList<Attachment>())
    override val links = MutableStateFlow(emptyList<TaskLink>())
    override val customFields = MutableStateFlow(emptyList<CustomField>())
    override val checklist = MutableStateFlow(emptyList<ChecklistItem>())
    override val comments = MutableStateFlow(emptyList<Comment>())
    override val invites = MutableStateFlow(emptyList<Invite>())
    override val activity = MutableStateFlow(emptyList<ActivityLog>())
    override val pendingOperations = MutableStateFlow(emptyList<PendingOperation>())

    init {
        collectRoomFlows()
    }

    private fun collectRoomFlows() {
        scope.launch(Dispatchers.IO) { dao.users().map { rows -> rows.map { it.toDomain() } }.collect(users::emit) }
        scope.launch(Dispatchers.IO) { dao.spaces().map { rows -> rows.map { it.toDomain() } }.collect(spaces::emit) }
        scope.launch(Dispatchers.IO) { dao.lists().map { rows -> rows.map { it.toDomain() } }.collect(lists::emit) }
        scope.launch(Dispatchers.IO) { dao.tasks().map { rows -> rows.map { it.toDomain() } }.collect(tasks::emit) }
        scope.launch(Dispatchers.IO) { dao.reminders().map { rows -> rows.map { it.toDomain() } }.collect(reminders::emit) }
        scope.launch(Dispatchers.IO) { dao.attachments().map { rows -> rows.map { it.toDomain() } }.collect(attachments::emit) }
        scope.launch(Dispatchers.IO) { dao.links().map { rows -> rows.map { it.toDomain() } }.collect(links::emit) }
        scope.launch(Dispatchers.IO) { dao.customFields().map { rows -> rows.map { it.toDomain() } }.collect(customFields::emit) }
        scope.launch(Dispatchers.IO) { dao.checklist().map { rows -> rows.map { it.toDomain() } }.collect(checklist::emit) }
        scope.launch(Dispatchers.IO) { dao.comments().map { rows -> rows.map { it.toDomain() } }.collect(comments::emit) }
        scope.launch(Dispatchers.IO) { dao.invites().map { rows -> rows.map { it.toDomain() } }.collect(invites::emit) }
        scope.launch(Dispatchers.IO) { dao.activity().map { rows -> rows.map { it.toDomain() } }.collect(activity::emit) }
        scope.launch(Dispatchers.IO) { dao.pendingOperations().map { rows -> rows.map { it.toDomain() } }.collect(pendingOperations::emit) }
    }

    private suspend fun enqueue(entity: PendingEntityType, entityId: String, operation: PendingOperationType) {
        dao.upsertPendingOperations(listOf(PendingOperation(entity = entity, entityId = entityId, operation = operation).toEntity()))
    }

    override fun saveUser(user: User) {
        scope.launch(Dispatchers.IO) {
            dao.upsertUsers(listOf(user.toEntity()))
            ensureUserWorkspace(user)
        }
    }

    private suspend fun ensureUserWorkspace(user: User) {
        if (dao.ownedSpaceCount(user.id) > 0) return
        val space = Space(name = "Meu espaco", ownerId = user.id, members = listOf(user.id))
        val inbox = TaskList(spaceId = space.id, name = "Minhas tarefas", order = 0)
        dao.upsertSpaces(listOf(space.toEntity()))
        dao.upsertLists(listOf(inbox.toEntity()))
    }

    override fun createTask(task: Task) {
        scope.launch(Dispatchers.IO) {
            val validList = lists.value.any { it.id == task.listId && it.spaceId == task.spaceId }
            if (!validList) return@launch
            dao.upsertTasks(listOf(task.toEntity()))
            enqueue(PendingEntityType.Task, task.id, PendingOperationType.Create)
            dao.upsertActivity(listOf(ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa criada").toEntity()))
        }
    }

    override fun updateTask(task: Task) {
        scope.launch(Dispatchers.IO) {
            val previous = dao.taskById(task.id)?.toDomain()
            dao.upsertTasks(listOf(task.copy(updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.Task, task.id, PendingOperationType.Update)
            val events = mutableListOf(ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa atualizada"))
            if (previous != null) {
                if (previous.status != task.status) events += ActivityLog(taskId = task.id, userId = task.createdBy, action = "Status alterado: ${task.status.label}")
                if (previous.dueDate != task.dueDate) events += ActivityLog(taskId = task.id, userId = task.createdBy, action = "Prazo alterado")
                if (previous.assignedTo != task.assignedTo) events += ActivityLog(taskId = task.id, userId = task.createdBy, action = "Responsavel alterado")
            }
            dao.upsertActivity(events.map { it.toEntity() })
        }
    }

    override fun completeTask(taskId: String) {
        scope.launch(Dispatchers.IO) {
            val task = dao.taskById(taskId)?.toDomain() ?: return@launch
            dao.upsertTasks(listOf(task.copy(status = TaskStatus.Done, isCompleted = true, completedAt = now(), updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.Task, task.id, PendingOperationType.Update)
            dao.remindersByTaskId(taskId)
                .map { it.toDomain() }
                .forEach { reminder ->
                    reminderScheduler.cancel(reminder.id, reminder.taskId)
                    dao.upsertReminders(listOf(reminder.copy(isActive = false, updatedAt = now()).toEntity()))
                }
            dao.upsertActivity(listOf(ActivityLog(taskId = taskId, userId = task.createdBy, action = "Tarefa concluida").toEntity()))
        }
    }

    override fun deleteTask(taskId: String) {
        scope.launch(Dispatchers.IO) {
            val task = dao.taskById(taskId)?.toDomain() ?: return@launch
            dao.deleteTask(taskId)
            enqueue(PendingEntityType.Task, task.id, PendingOperationType.Delete)
            dao.upsertActivity(listOf(ActivityLog(taskId = taskId, userId = task.createdBy, action = "Tarefa excluida").toEntity()))
        }
    }

    override fun createSpace(name: String, ownerId: String?) {
        scope.launch(Dispatchers.IO) {
            val owner = ownerId ?: users.value.firstOrNull()?.id ?: "local"
            val space = Space(name = name, ownerId = owner, members = listOf(owner))
            dao.upsertSpaces(listOf(space.toEntity()))
            enqueue(PendingEntityType.Space, space.id, PendingOperationType.Create)
        }
    }

    override fun updateSpace(space: Space) {
        scope.launch(Dispatchers.IO) {
            dao.upsertSpaces(listOf(space.copy(updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.Space, space.id, PendingOperationType.Update)
        }
    }

    override fun deleteSpace(spaceId: String) {
        scope.launch(Dispatchers.IO) {
            if (dao.taskCountBySpace(spaceId) == 0) {
                dao.deleteSpace(spaceId)
                enqueue(PendingEntityType.Space, spaceId, PendingOperationType.Delete)
            }
        }
    }

    override fun createList(spaceId: String, name: String) {
        scope.launch(Dispatchers.IO) {
            val nextOrder = (lists.value.filter { it.spaceId == spaceId }.maxOfOrNull { it.order } ?: -1) + 1
            val list = TaskList(spaceId = spaceId, name = name, order = nextOrder)
            dao.upsertLists(listOf(list.toEntity()))
            enqueue(PendingEntityType.List, list.id, PendingOperationType.Create)
        }
    }

    override fun updateList(list: TaskList) {
        scope.launch(Dispatchers.IO) {
            dao.upsertLists(listOf(list.copy(updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.List, list.id, PendingOperationType.Update)
        }
    }

    override fun deleteList(listId: String) {
        scope.launch(Dispatchers.IO) {
            if (dao.taskCountByList(listId) == 0) {
                dao.deleteList(listId)
                enqueue(PendingEntityType.List, listId, PendingOperationType.Delete)
            }
        }
    }

    override fun saveReminder(reminder: Reminder) {
        scope.launch(Dispatchers.IO) {
            val operation = if (dao.reminderById(reminder.id) == null) PendingOperationType.Create else PendingOperationType.Update
            val nextTriggerAt = ReminderEngine.nextOccurrence(reminder)
            val value = reminder.copy(nextTriggerAt = nextTriggerAt, updatedAt = now())
            dao.upsertReminders(listOf(value.toEntity()))
            enqueue(PendingEntityType.Reminder, value.id, operation)
            if (value.isActive && nextTriggerAt != null) {
                reminderScheduler.schedule(value, nextTriggerAt)
            } else {
                reminderScheduler.cancel(value.id, value.taskId)
            }
        }
    }

    override fun addAttachment(attachment: Attachment) {
        scope.launch(Dispatchers.IO) {
            val safeAttachment = AttachmentSecurity.withoutPublicPermanentUrls(attachment)
            dao.upsertAttachments(listOf(safeAttachment.toEntity()))
            enqueue(PendingEntityType.Attachment, safeAttachment.id, PendingOperationType.Create)
            logActivity(safeAttachment.taskId, safeAttachment.uploadedBy, "Anexo adicionado: ${safeAttachment.fileName}")
        }
    }

    override fun deleteAttachment(attachmentId: String) {
        scope.launch(Dispatchers.IO) {
            val attachment = dao.attachmentById(attachmentId)?.toDomain() ?: return@launch
            dao.upsertAttachments(listOf(attachment.copy(isDeleted = true, updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.Attachment, attachment.id, PendingOperationType.Delete)
            logActivity(attachment.taskId, users.value.firstOrNull()?.id ?: attachment.uploadedBy, "Anexo removido: ${attachment.fileName}")
        }
    }

    override fun addLink(link: TaskLink) {
        scope.launch(Dispatchers.IO) {
            dao.upsertLinks(listOf(link.toEntity()))
            enqueue(PendingEntityType.Link, link.id, PendingOperationType.Create)
            logActivity(link.taskId, link.createdBy, "Link adicionado: ${link.title}")
        }
    }

    override fun updateLink(link: TaskLink) {
        scope.launch(Dispatchers.IO) {
            dao.upsertLinks(listOf(link.copy(updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.Link, link.id, PendingOperationType.Update)
            logActivity(link.taskId, link.createdBy, "Link atualizado: ${link.title}")
        }
    }

    override fun deleteLink(linkId: String) {
        scope.launch(Dispatchers.IO) {
            val link = dao.linkById(linkId)?.toDomain() ?: return@launch
            dao.deleteLink(linkId)
            enqueue(PendingEntityType.Link, link.id, PendingOperationType.Delete)
            logActivity(link.taskId, users.value.firstOrNull()?.id ?: link.createdBy, "Link removido: ${link.title}")
        }
    }

    override fun addCustomField(field: CustomField) {
        scope.launch(Dispatchers.IO) {
            dao.upsertCustomFields(listOf(field.toEntity()))
            enqueue(PendingEntityType.CustomField, field.id, PendingOperationType.Create)
            logActivity(field.taskId, field.createdBy, "Campo alterado: ${field.fieldName}")
        }
    }

    override fun updateCustomField(field: CustomField) {
        scope.launch(Dispatchers.IO) {
            dao.upsertCustomFields(listOf(field.copy(updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.CustomField, field.id, PendingOperationType.Update)
            logActivity(field.taskId, field.createdBy, "Campo alterado: ${field.fieldName}")
        }
    }

    override fun deleteCustomField(fieldId: String) {
        scope.launch(Dispatchers.IO) {
            val field = dao.customFieldById(fieldId)?.toDomain() ?: return@launch
            dao.deleteCustomField(fieldId)
            enqueue(PendingEntityType.CustomField, field.id, PendingOperationType.Delete)
            logActivity(field.taskId, users.value.firstOrNull()?.id ?: field.createdBy, "Campo removido: ${field.fieldName}")
        }
    }

    override fun addChecklistItem(item: ChecklistItem) {
        scope.launch(Dispatchers.IO) {
            dao.upsertChecklistItems(listOf(item.toEntity()))
            enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Create)
            logActivity(item.taskId, users.value.firstOrNull()?.id ?: "local", "Checklist adicionado: ${item.title}")
        }
    }

    override fun updateChecklistItem(item: ChecklistItem) {
        scope.launch(Dispatchers.IO) {
            dao.upsertChecklistItems(listOf(item.toEntity()))
            enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Update)
            logActivity(item.taskId, users.value.firstOrNull()?.id ?: "local", "Checklist atualizado: ${item.title}")
        }
    }

    override fun toggleChecklistItem(itemId: String) {
        scope.launch(Dispatchers.IO) {
            val item = dao.checklistItemById(itemId)?.toDomain() ?: return@launch
            val updated = item.copy(isDone = !item.isDone)
            dao.upsertChecklistItems(listOf(updated.toEntity()))
            enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Update)
            logActivity(item.taskId, users.value.firstOrNull()?.id ?: "local", if (updated.isDone) "Checklist concluido: ${item.title}" else "Checklist reaberto: ${item.title}")
        }
    }

    override fun deleteChecklistItem(itemId: String) {
        scope.launch(Dispatchers.IO) {
            val item = dao.checklistItemById(itemId)?.toDomain() ?: return@launch
            dao.deleteChecklistItem(itemId)
            enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Delete)
            logActivity(item.taskId, users.value.firstOrNull()?.id ?: "local", "Checklist removido: ${item.title}")
        }
    }

    override fun addComment(comment: Comment) {
        scope.launch(Dispatchers.IO) {
            dao.upsertComments(listOf(comment.toEntity()))
            enqueue(PendingEntityType.Comment, comment.id, PendingOperationType.Create)
            logActivity(comment.taskId, comment.authorId, "Comentario adicionado")
        }
    }

    override fun updateComment(comment: Comment) {
        scope.launch(Dispatchers.IO) {
            val previous = dao.commentById(comment.id)?.toDomain() ?: return@launch
            dao.upsertComments(listOf(comment.copy(updatedAt = now()).toEntity()))
            enqueue(PendingEntityType.Comment, comment.id, PendingOperationType.Update)
            logActivity(previous.taskId, previous.authorId, "Comentario editado")
        }
    }

    override fun deleteComment(commentId: String) {
        scope.launch(Dispatchers.IO) {
            val comment = dao.commentById(commentId)?.toDomain() ?: return@launch
            dao.deleteComment(commentId)
            enqueue(PendingEntityType.Comment, comment.id, PendingOperationType.Delete)
            logActivity(comment.taskId, users.value.firstOrNull()?.id ?: comment.authorId, "Comentario removido")
        }
    }

    override fun createInvite(invite: Invite) {
        scope.launch(Dispatchers.IO) {
            dao.upsertInvites(listOf(invite.toEntity()))
            enqueue(PendingEntityType.Invite, invite.id, PendingOperationType.Create)
            logActivity(invite.taskId, invite.createdBy, "Convite criado: ${invite.permission.label}")
        }
    }

    override fun acceptInvite(token: String, userId: String) {
        scope.launch(Dispatchers.IO) {
            val invite = dao.inviteByToken(token)?.toDomain() ?: return@launch
            if (!PermissionPolicy.isInviteActive(invite)) return@launch
            val task = dao.taskById(invite.taskId)?.toDomain() ?: return@launch
            dao.upsertInvites(listOf(invite.copy(acceptedBy = userId).toEntity()))
            enqueue(PendingEntityType.Invite, invite.id, PendingOperationType.Update)
            if (userId !in task.participants) {
                dao.updateTask(task.copy(participants = task.participants + userId, updatedAt = now()).toEntity())
                enqueue(PendingEntityType.Task, task.id, PendingOperationType.Update)
            }
            logActivity(invite.taskId, userId, "Convite aceito: ${invite.permission.label}")
        }
    }

    override fun declineInvite(token: String) {
        scope.launch(Dispatchers.IO) {
            val invite = dao.inviteByToken(token)?.toDomain()
            dao.deleteInviteByToken(token)
            invite?.let { enqueue(PendingEntityType.Invite, it.id, PendingOperationType.Delete) }
            invite?.let { logActivity(it.taskId, users.value.firstOrNull()?.id ?: "local", "Convite recusado") }
        }
    }
}
