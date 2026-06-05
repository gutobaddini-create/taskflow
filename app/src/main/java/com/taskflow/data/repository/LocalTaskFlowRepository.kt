package com.taskflow.data.repository

import android.content.Context
import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.core.notifications.ReminderScheduler
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

    init {
        collectRoomFlows()
        scope.launch(Dispatchers.IO) { seedIfNeeded() }
    }

    private fun collectRoomFlows() {
        scope.launch { dao.users().map { rows -> rows.map { it.toDomain() } }.collect(users::emit) }
        scope.launch { dao.spaces().map { rows -> rows.map { it.toDomain() } }.collect(spaces::emit) }
        scope.launch { dao.lists().map { rows -> rows.map { it.toDomain() } }.collect(lists::emit) }
        scope.launch { dao.tasks().map { rows -> rows.map { it.toDomain() } }.collect(tasks::emit) }
        scope.launch { dao.reminders().map { rows -> rows.map { it.toDomain() } }.collect(reminders::emit) }
        scope.launch { dao.attachments().map { rows -> rows.map { it.toDomain() } }.collect(attachments::emit) }
        scope.launch { dao.links().map { rows -> rows.map { it.toDomain() } }.collect(links::emit) }
        scope.launch { dao.customFields().map { rows -> rows.map { it.toDomain() } }.collect(customFields::emit) }
        scope.launch { dao.checklist().map { rows -> rows.map { it.toDomain() } }.collect(checklist::emit) }
        scope.launch { dao.comments().map { rows -> rows.map { it.toDomain() } }.collect(comments::emit) }
        scope.launch { dao.invites().map { rows -> rows.map { it.toDomain() } }.collect(invites::emit) }
        scope.launch { dao.activity().map { rows -> rows.map { it.toDomain() } }.collect(activity::emit) }
    }

    private suspend fun seedIfNeeded() {
        if (dao.taskCount() > 0) return

        val manuel = User(name = "Manuel", email = "manuel@taskflow.local")
        val trabalho = Space(name = "Trabalho", ownerId = manuel.id, members = listOf(manuel.id))
        val pessoal = Space(name = "Pessoal", ownerId = manuel.id, members = listOf(manuel.id))
        val prazos = TaskList(spaceId = trabalho.id, name = "Prazos", order = 0)
        val compras = TaskList(spaceId = pessoal.id, name = "Compras", order = 1)
        val seedTasks = listOf(
            Task(spaceId = trabalho.id, listId = prazos.id, title = "Enviar proposta", description = "Revisar anexos e enviar proposta ao cliente.", priority = TaskPriority.High, status = TaskStatus.InProgress, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(9).withMinute(0)),
            Task(spaceId = pessoal.id, listId = compras.id, title = "Comprar material", description = "Separar lista e comprovante.", priority = TaskPriority.Medium, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(11).withMinute(0)),
            Task(spaceId = trabalho.id, listId = prazos.id, title = "Preparar apresentacao", description = "Levar documentos e revisar roteiro.", priority = TaskPriority.Medium, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(14).withMinute(0)),
            Task(spaceId = pessoal.id, listId = compras.id, title = "Ler relatorio", description = "Anotar pontos principais.", priority = TaskPriority.Low, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(16).withMinute(30))
        )
        val firstTask = seedTasks.first()
        dao.upsertUsers(listOf(manuel.toEntity()))
        dao.upsertSpaces(listOf(trabalho, pessoal).map { it.toEntity() })
        dao.upsertLists(listOf(prazos, compras).map { it.toEntity() })
        dao.upsertTasks(seedTasks.map { it.toEntity() })
        dao.upsertReminders(listOf(Reminder(taskId = firstTask.id, userId = manuel.id, type = ReminderType.Recurring, recurrenceType = RecurrenceType.Custom, recurrenceInterval = 2, recurrenceUnit = RecurrenceUnit.Weeks, selectedWeekDays = listOf(WeekDay.Monday, WeekDay.Thursday), endType = ReminderEndType.OnDate, endDate = LocalDate.of(2026, 12, 31), nextTriggerAt = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))).map { it.toEntity() })
        dao.upsertAttachments(listOf(
            Attachment(taskId = firstTask.id, uploadedBy = manuel.id, fileName = "Proposta_Projeto.pdf", originalFileName = "Proposta_Projeto.pdf", fileType = AttachmentType.Pdf, mimeType = "application/pdf", fileSize = 1_200_000, storagePath = "local/proposta.pdf"),
            Attachment(taskId = firstTask.id, uploadedBy = manuel.id, fileName = "Briefing_cliente.jpg", originalFileName = "Briefing_cliente.jpg", fileType = AttachmentType.Image, mimeType = "image/jpeg", fileSize = 820_000, storagePath = "local/briefing.jpg")
        ).map { it.toEntity() })
        dao.upsertLinks(listOf(TaskLink(taskId = firstTask.id, createdBy = manuel.id, title = "Briefing do projeto", url = "https://docs.google.com/document/d/abc123", description = "Documento com contexto do cliente", category = "Cliente", isImportant = true)).map { it.toEntity() })
        dao.upsertCustomFields(listOf(
            CustomField(taskId = firstTask.id, fieldName = "Cliente", fieldType = CustomFieldType.Text, fieldValue = "Empresa ABC", createdBy = manuel.id),
            CustomField(taskId = firstTask.id, fieldName = "Valor", fieldType = CustomFieldType.Money, fieldValue = "R$ 15.000,00", createdBy = manuel.id),
            CustomField(taskId = firstTask.id, fieldName = "Numero do contrato", fieldType = CustomFieldType.ProcessNumber, fieldValue = "CT-2025-0031", createdBy = manuel.id)
        ).map { it.toEntity() })
        dao.upsertChecklistItems(listOf(ChecklistItem(taskId = firstTask.id, title = "Revisar PDF"), ChecklistItem(taskId = firstTask.id, title = "Confirmar prazo")).map { it.toEntity() })
        dao.upsertComments(listOf(Comment(taskId = firstTask.id, authorId = manuel.id, text = "Cliente pediu envio ate 14h.")).map { it.toEntity() })
        dao.upsertActivity(seedTasks.map { ActivityLog(taskId = it.id, userId = manuel.id, action = "Tarefa criada").toEntity() })
    }

    override fun createTask(task: Task) {
        scope.launch(Dispatchers.IO) {
            dao.upsertTasks(listOf(task.toEntity()))
            dao.upsertActivity(listOf(ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa criada").toEntity()))
        }
    }

    override fun updateTask(task: Task) {
        scope.launch(Dispatchers.IO) {
            dao.upsertTasks(listOf(task.copy(updatedAt = now()).toEntity()))
            dao.upsertActivity(listOf(ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa atualizada").toEntity()))
        }
    }

    override fun completeTask(taskId: String) {
        scope.launch(Dispatchers.IO) {
            val task = dao.taskById(taskId)?.toDomain() ?: return@launch
            dao.upsertTasks(listOf(task.copy(status = TaskStatus.Done, isCompleted = true, completedAt = now(), updatedAt = now()).toEntity()))
            dao.remindersByTaskId(taskId)
                .map { it.toDomain() }
                .filter { it.endType == ReminderEndType.OnTaskDone }
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
            dao.upsertActivity(listOf(ActivityLog(taskId = taskId, userId = task.createdBy, action = "Tarefa excluida").toEntity()))
        }
    }

    override fun createSpace(name: String) {
        scope.launch(Dispatchers.IO) {
            val owner = users.value.firstOrNull()?.id ?: "local"
            dao.upsertSpaces(listOf(Space(name = name, ownerId = owner, members = listOf(owner)).toEntity()))
        }
    }

    override fun updateSpace(space: Space) {
        scope.launch(Dispatchers.IO) {
            dao.upsertSpaces(listOf(space.copy(updatedAt = now()).toEntity()))
        }
    }

    override fun deleteSpace(spaceId: String) {
        scope.launch(Dispatchers.IO) {
            if (dao.taskCountBySpace(spaceId) == 0) dao.deleteSpace(spaceId)
        }
    }

    override fun createList(spaceId: String, name: String) {
        scope.launch(Dispatchers.IO) {
            dao.upsertLists(listOf(TaskList(spaceId = spaceId, name = name, order = lists.value.size).toEntity()))
        }
    }

    override fun updateList(list: TaskList) {
        scope.launch(Dispatchers.IO) {
            dao.upsertLists(listOf(list.copy(updatedAt = now()).toEntity()))
        }
    }

    override fun deleteList(listId: String) {
        scope.launch(Dispatchers.IO) {
            if (dao.taskCountByList(listId) == 0) dao.deleteList(listId)
        }
    }

    override fun saveReminder(reminder: Reminder) {
        scope.launch(Dispatchers.IO) {
            val nextTriggerAt = ReminderEngine.nextOccurrence(reminder)
            val value = reminder.copy(nextTriggerAt = nextTriggerAt, updatedAt = now())
            dao.upsertReminders(listOf(value.toEntity()))
            reminderScheduler.schedule(value, nextTriggerAt)
        }
    }

    override fun addAttachment(attachment: Attachment) {
        scope.launch(Dispatchers.IO) { dao.upsertAttachments(listOf(attachment.toEntity())) }
    }

    override fun addLink(link: TaskLink) {
        scope.launch(Dispatchers.IO) { dao.upsertLinks(listOf(link.toEntity())) }
    }

    override fun addCustomField(field: CustomField) {
        scope.launch(Dispatchers.IO) { dao.upsertCustomFields(listOf(field.toEntity())) }
    }

    override fun addChecklistItem(item: ChecklistItem) {
        scope.launch(Dispatchers.IO) { dao.upsertChecklistItems(listOf(item.toEntity())) }
    }

    override fun toggleChecklistItem(itemId: String) {
        scope.launch(Dispatchers.IO) {
            val item = dao.checklistItemById(itemId)?.toDomain() ?: return@launch
            dao.upsertChecklistItems(listOf(item.copy(isDone = !item.isDone).toEntity()))
        }
    }

    override fun addComment(comment: Comment) {
        scope.launch(Dispatchers.IO) { dao.upsertComments(listOf(comment.toEntity())) }
    }

    override fun createInvite(invite: Invite) {
        scope.launch(Dispatchers.IO) { dao.upsertInvites(listOf(invite.toEntity())) }
    }
}
