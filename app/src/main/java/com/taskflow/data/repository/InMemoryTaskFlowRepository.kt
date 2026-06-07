package com.taskflow.data.repository

import com.taskflow.domain.model.*
import com.taskflow.domain.repository.TaskFlowRepository
import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.core.security.AttachmentSecurity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryTaskFlowRepository : TaskFlowRepository {
    private val localUser = User(name = "Usuario local", email = "")
    private val collaborator = User(name = "Colaborador", email = "")
    private val localSpace = Space(name = "Meu espaco", ownerId = localUser.id, members = listOf(localUser.id, collaborator.id))
    private val secondarySpace = Space(name = "Compartilhado", ownerId = localUser.id, members = listOf(localUser.id, collaborator.id))
    private val localList = TaskList(spaceId = localSpace.id, name = "Minhas tarefas", order = 0)
    private val secondaryList = TaskList(spaceId = secondarySpace.id, name = "Geral", order = 0)
    private val neutralTask = Task(
        spaceId = localSpace.id,
        listId = localList.id,
        title = "Primeira tarefa",
        description = "",
        createdBy = localUser.id,
        assignedTo = localUser.id
    )

    override val users = MutableStateFlow(listOf(localUser, collaborator))
    override val spaces = MutableStateFlow(listOf(localSpace, secondarySpace))
    override val lists = MutableStateFlow(listOf(localList, secondaryList))
    override val tasks = MutableStateFlow(listOf(neutralTask))
    override val reminders = MutableStateFlow(emptyList<Reminder>())
    override val attachments = MutableStateFlow(emptyList<Attachment>())
    override val links = MutableStateFlow(emptyList<TaskLink>())
    override val customFields = MutableStateFlow(emptyList<CustomField>())
    override val checklist = MutableStateFlow(emptyList<ChecklistItem>())
    override val comments = MutableStateFlow(emptyList<Comment>())
    override val invites = MutableStateFlow(emptyList<Invite>())
    override val activity = MutableStateFlow(emptyList<ActivityLog>())
    override val pendingOperations = MutableStateFlow(emptyList<PendingOperation>())

    private fun enqueue(entity: PendingEntityType, entityId: String, operation: PendingOperationType) {
        pendingOperations.value = pendingOperations.value + PendingOperation(entity = entity, entityId = entityId, operation = operation)
    }

    override fun saveUser(user: User) {
        users.value = users.value.filterNot { it.id == user.id || it.email.equals(user.email, ignoreCase = true) } + user
    }

    override fun createTask(task: Task) {
        if (lists.value.none { it.id == task.listId && it.spaceId == task.spaceId }) return
        tasks.value = tasks.value + task
        activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa criada")
        enqueue(PendingEntityType.Task, task.id, PendingOperationType.Create)
    }

    override fun updateTask(task: Task) {
        val previous = tasks.value.firstOrNull { it.id == task.id }
        tasks.value = tasks.value.map { if (it.id == task.id) task.copy(updatedAt = now()) else it }
        activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa atualizada")
        enqueue(PendingEntityType.Task, task.id, PendingOperationType.Update)
        if (previous != null) {
            if (previous.status != task.status) activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Status alterado: ${task.status.label}")
            if (previous.dueDate != task.dueDate) activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Prazo alterado")
            if (previous.assignedTo != task.assignedTo) activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Responsavel alterado")
        }
    }

    override fun completeTask(taskId: String) {
        tasks.value = tasks.value.map { if (it.id == taskId) it.copy(status = TaskStatus.Done, isCompleted = true, completedAt = now(), updatedAt = now()) else it }
        reminders.value = reminders.value.map { if (it.taskId == taskId) it.copy(isActive = false) else it }
        activity.value = activity.value + ActivityLog(taskId = taskId, userId = users.value.first().id, action = "Tarefa concluida")
        enqueue(PendingEntityType.Task, taskId, PendingOperationType.Update)
    }

    override fun deleteTask(taskId: String) {
        tasks.value = tasks.value.filterNot { it.id == taskId }
        activity.value = activity.value + ActivityLog(taskId = taskId, userId = users.value.first().id, action = "Tarefa excluida")
        enqueue(PendingEntityType.Task, taskId, PendingOperationType.Delete)
    }

    override fun createSpace(name: String, ownerId: String?) {
        val owner = ownerId ?: localUser.id
        val space = Space(name = name, ownerId = owner, members = listOf(owner))
        spaces.value = spaces.value + space
        enqueue(PendingEntityType.Space, space.id, PendingOperationType.Create)
    }

    override fun updateSpace(space: Space) {
        spaces.value = spaces.value.map { if (it.id == space.id) space.copy(updatedAt = now()) else it }
        enqueue(PendingEntityType.Space, space.id, PendingOperationType.Update)
    }

    override fun deleteSpace(spaceId: String) {
        if (tasks.value.none { it.spaceId == spaceId }) {
            spaces.value = spaces.value.filterNot { it.id == spaceId }
            enqueue(PendingEntityType.Space, spaceId, PendingOperationType.Delete)
        }
    }

    override fun createList(spaceId: String, name: String) {
        val nextOrder = (lists.value.filter { it.spaceId == spaceId }.maxOfOrNull { it.order } ?: -1) + 1
        val list = TaskList(spaceId = spaceId, name = name, order = nextOrder)
        lists.value = lists.value + list
        enqueue(PendingEntityType.List, list.id, PendingOperationType.Create)
    }

    override fun updateList(list: TaskList) {
        lists.value = lists.value.map { if (it.id == list.id) list.copy(updatedAt = now()) else it }
        enqueue(PendingEntityType.List, list.id, PendingOperationType.Update)
    }

    override fun deleteList(listId: String) {
        if (tasks.value.none { it.listId == listId }) {
            lists.value = lists.value.filterNot { it.id == listId }
            enqueue(PendingEntityType.List, listId, PendingOperationType.Delete)
        }
    }

    override fun saveReminder(reminder: Reminder) {
        val operation = if (reminders.value.any { it.id == reminder.id }) PendingOperationType.Update else PendingOperationType.Create
        reminders.value = reminders.value.filterNot { it.id == reminder.id } + reminder
        enqueue(PendingEntityType.Reminder, reminder.id, operation)
    }

    override fun addAttachment(attachment: Attachment) {
        val safeAttachment = AttachmentSecurity.withoutPublicPermanentUrls(attachment)
        attachments.value = attachments.value + safeAttachment
        activity.value = activity.value + ActivityLog(taskId = safeAttachment.taskId, userId = safeAttachment.uploadedBy, action = "Anexo adicionado: ${safeAttachment.fileName}")
        enqueue(PendingEntityType.Attachment, safeAttachment.id, PendingOperationType.Create)
    }
    override fun deleteAttachment(attachmentId: String) {
        val attachment = attachments.value.firstOrNull { it.id == attachmentId } ?: return
        attachments.value = attachments.value.filterNot { it.id == attachmentId }
        activity.value = activity.value + ActivityLog(taskId = attachment.taskId, userId = users.value.first().id, action = "Anexo removido: ${attachment.fileName}")
        enqueue(PendingEntityType.Attachment, attachment.id, PendingOperationType.Delete)
    }
    override fun addLink(link: TaskLink) {
        links.value = links.value + link
        activity.value = activity.value + ActivityLog(taskId = link.taskId, userId = link.createdBy, action = "Link adicionado: ${link.title}")
        enqueue(PendingEntityType.Link, link.id, PendingOperationType.Create)
    }
    override fun updateLink(link: TaskLink) {
        links.value = links.value.map { if (it.id == link.id) link.copy(updatedAt = now()) else it }
        activity.value = activity.value + ActivityLog(taskId = link.taskId, userId = link.createdBy, action = "Link atualizado: ${link.title}")
        enqueue(PendingEntityType.Link, link.id, PendingOperationType.Update)
    }
    override fun deleteLink(linkId: String) {
        val link = links.value.firstOrNull { it.id == linkId } ?: return
        links.value = links.value.filterNot { it.id == linkId }
        activity.value = activity.value + ActivityLog(taskId = link.taskId, userId = users.value.first().id, action = "Link removido: ${link.title}")
        enqueue(PendingEntityType.Link, link.id, PendingOperationType.Delete)
    }
    override fun addCustomField(field: CustomField) {
        customFields.value = customFields.value + field
        activity.value = activity.value + ActivityLog(taskId = field.taskId, userId = field.createdBy, action = "Campo alterado: ${field.fieldName}")
        enqueue(PendingEntityType.CustomField, field.id, PendingOperationType.Create)
    }
    override fun updateCustomField(field: CustomField) {
        customFields.value = customFields.value.map { if (it.id == field.id) field.copy(updatedAt = now()) else it }
        activity.value = activity.value + ActivityLog(taskId = field.taskId, userId = field.createdBy, action = "Campo alterado: ${field.fieldName}")
        enqueue(PendingEntityType.CustomField, field.id, PendingOperationType.Update)
    }
    override fun deleteCustomField(fieldId: String) {
        val field = customFields.value.firstOrNull { it.id == fieldId } ?: return
        customFields.value = customFields.value.filterNot { it.id == fieldId }
        activity.value = activity.value + ActivityLog(taskId = field.taskId, userId = users.value.first().id, action = "Campo removido: ${field.fieldName}")
        enqueue(PendingEntityType.CustomField, field.id, PendingOperationType.Delete)
    }
    override fun addChecklistItem(item: ChecklistItem) {
        checklist.value = checklist.value + item
        activity.value = activity.value + ActivityLog(taskId = item.taskId, userId = users.value.first().id, action = "Checklist adicionado: ${item.title}")
        enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Create)
    }
    override fun updateChecklistItem(item: ChecklistItem) {
        checklist.value = checklist.value.map { if (it.id == item.id) item else it }
        activity.value = activity.value + ActivityLog(taskId = item.taskId, userId = users.value.first().id, action = "Checklist atualizado: ${item.title}")
        enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Update)
    }
    override fun toggleChecklistItem(itemId: String) {
        checklist.value = checklist.value.map { if (it.id == itemId) it.copy(isDone = !it.isDone) else it }
        checklist.value.firstOrNull { it.id == itemId }?.let { activity.value = activity.value + ActivityLog(taskId = it.taskId, userId = users.value.first().id, action = if (it.isDone) "Checklist concluido: ${it.title}" else "Checklist reaberto: ${it.title}") }
        enqueue(PendingEntityType.Checklist, itemId, PendingOperationType.Update)
    }
    override fun deleteChecklistItem(itemId: String) {
        val item = checklist.value.firstOrNull { it.id == itemId } ?: return
        checklist.value = checklist.value.filterNot { it.id == itemId }
        activity.value = activity.value + ActivityLog(taskId = item.taskId, userId = users.value.first().id, action = "Checklist removido: ${item.title}")
        enqueue(PendingEntityType.Checklist, item.id, PendingOperationType.Delete)
    }
    override fun addComment(comment: Comment) {
        comments.value = comments.value + comment
        activity.value = activity.value + ActivityLog(taskId = comment.taskId, userId = comment.authorId, action = "Comentario adicionado")
        enqueue(PendingEntityType.Comment, comment.id, PendingOperationType.Create)
    }
    override fun updateComment(comment: Comment) {
        val previous = comments.value.firstOrNull { it.id == comment.id } ?: return
        comments.value = comments.value.map { if (it.id == comment.id) comment.copy(updatedAt = now()) else it }
        activity.value = activity.value + ActivityLog(taskId = previous.taskId, userId = previous.authorId, action = "Comentario editado")
        enqueue(PendingEntityType.Comment, comment.id, PendingOperationType.Update)
    }
    override fun deleteComment(commentId: String) {
        val comment = comments.value.firstOrNull { it.id == commentId } ?: return
        comments.value = comments.value.filterNot { it.id == commentId }
        activity.value = activity.value + ActivityLog(taskId = comment.taskId, userId = users.value.first().id, action = "Comentario removido")
        enqueue(PendingEntityType.Comment, comment.id, PendingOperationType.Delete)
    }
    override fun createInvite(invite: Invite) {
        invites.value = invites.value + invite
        activity.value = activity.value + ActivityLog(taskId = invite.taskId, userId = invite.createdBy, action = "Convite criado: ${invite.permission.label}")
        enqueue(PendingEntityType.Invite, invite.id, PendingOperationType.Create)
    }
    override fun acceptInvite(token: String, userId: String) {
        val invite = invites.value.firstOrNull { it.token == token } ?: return
        if (!PermissionPolicy.isInviteActive(invite)) return
        invites.value = invites.value.map { if (it.token == token) it.copy(acceptedBy = userId) else it }
        tasks.value = tasks.value.map { if (it.id == invite.taskId && userId !in it.participants) it.copy(participants = it.participants + userId, updatedAt = now()) else it }
        activity.value = activity.value + ActivityLog(taskId = invite.taskId, userId = userId, action = "Convite aceito: ${invite.permission.label}")
        enqueue(PendingEntityType.Invite, invite.id, PendingOperationType.Update)
        enqueue(PendingEntityType.Task, invite.taskId, PendingOperationType.Update)
    }
    override fun declineInvite(token: String) {
        val invite = invites.value.firstOrNull { it.token == token }
        invites.value = invites.value.filterNot { it.token == token }
        invite?.let { activity.value = activity.value + ActivityLog(taskId = it.taskId, userId = users.value.first().id, action = "Convite recusado") }
        invite?.let { enqueue(PendingEntityType.Invite, it.id, PendingOperationType.Delete) }
    }
}
