package com.taskflow.data.repository

import com.taskflow.domain.model.*
import com.taskflow.domain.repository.TaskFlowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

class InMemoryTaskFlowRepository : TaskFlowRepository {
    private val manuel = User(name = "Manuel", email = "manuel@taskflow.local")
    private val trabalho = Space(name = "Trabalho", ownerId = manuel.id, members = listOf(manuel.id))
    private val pessoal = Space(name = "Pessoal", ownerId = manuel.id, members = listOf(manuel.id))
    private val prazos = TaskList(spaceId = trabalho.id, name = "Prazos", order = 0)
    private val compras = TaskList(spaceId = pessoal.id, name = "Compras", order = 1)

    private val seedTasks = listOf(
        Task(spaceId = trabalho.id, listId = prazos.id, title = "Enviar proposta", description = "Revisar anexos e enviar proposta ao cliente.", priority = TaskPriority.High, status = TaskStatus.InProgress, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(9).withMinute(0)),
        Task(spaceId = pessoal.id, listId = compras.id, title = "Comprar material", description = "Separar lista e comprovante.", priority = TaskPriority.Medium, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(11).withMinute(0)),
        Task(spaceId = trabalho.id, listId = prazos.id, title = "Preparar apresentacao", description = "Levar documentos e revisar roteiro.", priority = TaskPriority.Medium, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(14).withMinute(0)),
        Task(spaceId = pessoal.id, listId = compras.id, title = "Ler relatorio", description = "Anotar pontos principais.", priority = TaskPriority.Low, createdBy = manuel.id, assignedTo = manuel.id, dueDate = LocalDateTime.now().withHour(16).withMinute(30))
    )

    override val users = MutableStateFlow(listOf(manuel))
    override val spaces = MutableStateFlow(listOf(trabalho, pessoal))
    override val lists = MutableStateFlow(listOf(prazos, compras))
    override val tasks = MutableStateFlow(seedTasks)
    override val reminders = MutableStateFlow(listOf(
        Reminder(taskId = seedTasks.first().id, userId = manuel.id, type = ReminderType.Recurring, recurrenceType = RecurrenceType.Custom, recurrenceInterval = 2, recurrenceUnit = RecurrenceUnit.Weeks, selectedWeekDays = listOf(WeekDay.Monday, WeekDay.Thursday), endType = ReminderEndType.OnDate, endDate = java.time.LocalDate.of(2026, 12, 31), nextTriggerAt = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))
    ))
    override val attachments = MutableStateFlow(listOf(
        Attachment(taskId = seedTasks.first().id, uploadedBy = manuel.id, fileName = "Proposta_Projeto.pdf", originalFileName = "Proposta_Projeto.pdf", fileType = AttachmentType.Pdf, mimeType = "application/pdf", fileSize = 1_200_000, storagePath = "local/proposta.pdf"),
        Attachment(taskId = seedTasks.first().id, uploadedBy = manuel.id, fileName = "Briefing_cliente.jpg", originalFileName = "Briefing_cliente.jpg", fileType = AttachmentType.Image, mimeType = "image/jpeg", fileSize = 820_000, storagePath = "local/briefing.jpg")
    ))
    override val links = MutableStateFlow(listOf(TaskLink(taskId = seedTasks.first().id, createdBy = manuel.id, title = "Briefing do projeto", url = "https://docs.google.com/document/d/abc123", description = "Documento com contexto do cliente", category = "Cliente", isImportant = true)))
    override val customFields = MutableStateFlow(listOf(
        CustomField(taskId = seedTasks.first().id, fieldName = "Cliente", fieldType = CustomFieldType.Text, fieldValue = "Empresa ABC", createdBy = manuel.id),
        CustomField(taskId = seedTasks.first().id, fieldName = "Valor", fieldType = CustomFieldType.Money, fieldValue = "R$ 15.000,00", createdBy = manuel.id),
        CustomField(taskId = seedTasks.first().id, fieldName = "Numero do contrato", fieldType = CustomFieldType.ProcessNumber, fieldValue = "CT-2025-0031", createdBy = manuel.id)
    ))
    override val checklist = MutableStateFlow(listOf(ChecklistItem(taskId = seedTasks.first().id, title = "Revisar PDF"), ChecklistItem(taskId = seedTasks.first().id, title = "Confirmar prazo")))
    override val comments = MutableStateFlow(listOf(Comment(taskId = seedTasks.first().id, authorId = manuel.id, text = "Cliente pediu envio ate 14h.")))
    override val invites = MutableStateFlow(emptyList<Invite>())
    override val activity = MutableStateFlow(seedTasks.map { ActivityLog(taskId = it.id, userId = manuel.id, action = "Tarefa criada") })

    override fun createTask(task: Task) {
        tasks.value = tasks.value + task
        activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa criada")
    }

    override fun updateTask(task: Task) {
        tasks.value = tasks.value.map { if (it.id == task.id) task.copy(updatedAt = now()) else it }
        activity.value = activity.value + ActivityLog(taskId = task.id, userId = task.createdBy, action = "Tarefa atualizada")
    }

    override fun completeTask(taskId: String) {
        tasks.value = tasks.value.map { if (it.id == taskId) it.copy(status = TaskStatus.Done, isCompleted = true, completedAt = now(), updatedAt = now()) else it }
        reminders.value = reminders.value.map { if (it.taskId == taskId && it.endType == ReminderEndType.OnTaskDone) it.copy(isActive = false) else it }
        activity.value = activity.value + ActivityLog(taskId = taskId, userId = users.value.first().id, action = "Tarefa concluida")
    }

    override fun createSpace(name: String) {
        spaces.value = spaces.value + Space(name = name, ownerId = manuel.id, members = listOf(manuel.id))
    }

    override fun createList(spaceId: String, name: String) {
        lists.value = lists.value + TaskList(spaceId = spaceId, name = name, order = lists.value.size)
    }

    override fun saveReminder(reminder: Reminder) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id } + reminder
    }

    override fun addAttachment(attachment: Attachment) { attachments.value = attachments.value + attachment }
    override fun addLink(link: TaskLink) { links.value = links.value + link }
    override fun addCustomField(field: CustomField) { customFields.value = customFields.value + field }
    override fun addChecklistItem(item: ChecklistItem) { checklist.value = checklist.value + item }
    override fun toggleChecklistItem(itemId: String) { checklist.value = checklist.value.map { if (it.id == itemId) it.copy(isDone = !it.isDone) else it } }
    override fun addComment(comment: Comment) { comments.value = comments.value + comment }
    override fun createInvite(invite: Invite) { invites.value = invites.value + invite }
}
