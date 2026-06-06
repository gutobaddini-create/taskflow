package com.taskflow.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

enum class TaskStatus(val label: String) { Todo("A fazer"), InProgress("Em andamento"), Waiting("Aguardando"), Done("Concluida") }
enum class TaskPriority(val label: String) { Low("Baixa"), Medium("Media"), High("Alta") }
enum class ReminderType { OneTime, Recurring }
enum class RecurrenceType { None, Daily, Weekly, Monthly, Yearly, Custom }
enum class RecurrenceUnit { Days, Weeks, Months, Years }
enum class WeekDay(val value: Int, val short: String) { Monday(1, "seg"), Tuesday(2, "ter"), Wednesday(3, "qua"), Thursday(4, "qui"), Friday(5, "sex"), Saturday(6, "sab"), Sunday(7, "dom") }
enum class MonthlyRule(val label: String) { None("Sem regra"), FirstBusinessDay("Primeiro dia util"), LastBusinessDay("Ultimo dia util"), LastDay("Ultimo dia"), FirstMonday("Primeira segunda"), LastFriday("Ultima sexta") }
enum class ReminderEndType { Never, OnDate, AfterOccurrences, OnTaskDone }
enum class AttachmentSource { Camera, Gallery, FilePicker, Shared }
enum class AttachmentType { Image, Pdf, Document, Spreadsheet, Text, Other }
enum class CustomFieldType { Text, Number, Money, Date, Phone, Email, Url, Location, ProcessNumber, Document }
enum class UserPermission(val label: String) { Owner("Editar"), Responsible("Comentar"), Participant("Comentar"), Viewer("Ver") }
enum class PendingOperationType { Create, Update, Delete }
enum class PendingEntityType { Space, List, Task, Reminder, Attachment, Link, CustomField, Checklist, Comment, Invite }

data class User(val id: String = uuid(), val name: String, val email: String, val photoUrl: String? = null, val createdAt: Long = now(), val notificationPermissionStatus: String = "unknown")
data class Space(val id: String = uuid(), val name: String, val ownerId: String, val members: List<String> = emptyList(), val createdAt: Long = now(), val updatedAt: Long = now())
data class TaskList(val id: String = uuid(), val spaceId: String, val name: String, val order: Int = 0, val createdAt: Long = now(), val updatedAt: Long = now())

data class Task(
    val id: String = uuid(),
    val spaceId: String,
    val listId: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.Todo,
    val priority: TaskPriority = TaskPriority.Medium,
    val createdBy: String,
    val assignedTo: String? = null,
    val participants: List<String> = emptyList(),
    val dueDate: LocalDateTime? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = now(),
    val updatedAt: Long = now(),
    val shareToken: String = uuid()
)

data class Reminder(
    val id: String = uuid(),
    val taskId: String,
    val userId: String,
    val type: ReminderType = ReminderType.OneTime,
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val recurrenceType: RecurrenceType = RecurrenceType.None,
    val recurrenceInterval: Int = 1,
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.Weeks,
    val selectedWeekDays: List<WeekDay> = emptyList(),
    val selectedMonthDay: Int? = null,
    val monthlyRule: MonthlyRule = MonthlyRule.None,
    val endType: ReminderEndType = ReminderEndType.Never,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null,
    val occurrencesCompleted: Int = 0,
    val nextTriggerAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val createdAt: Long = now(),
    val updatedAt: Long = now()
)

data class Attachment(val id: String = uuid(), val taskId: String, val uploadedBy: String, val fileName: String, val originalFileName: String, val fileType: AttachmentType, val mimeType: String, val fileSize: Long, val storagePath: String, val secureUrl: String? = null, val thumbnailUrl: String? = null, val description: String = "", val source: AttachmentSource = AttachmentSource.FilePicker, val createdAt: Long = now(), val updatedAt: Long = now(), val isDeleted: Boolean = false)
data class TaskLink(val id: String = uuid(), val taskId: String, val createdBy: String, val title: String, val url: String, val description: String = "", val category: String = "", val isImportant: Boolean = false, val createdAt: Long = now(), val updatedAt: Long = now())
data class CustomField(val id: String = uuid(), val taskId: String, val fieldName: String, val fieldType: CustomFieldType, val fieldValue: String, val createdBy: String, val createdAt: Long = now(), val updatedAt: Long = now())
data class ChecklistItem(val id: String = uuid(), val taskId: String, val title: String, val isDone: Boolean = false)
data class Comment(val id: String = uuid(), val taskId: String, val authorId: String, val text: String, val createdAt: Long = now(), val updatedAt: Long = now())
data class ActivityLog(val id: String = uuid(), val taskId: String, val userId: String, val action: String, val createdAt: Long = now())
data class Invite(val id: String = uuid(), val taskId: String, val createdBy: String, val permission: UserPermission, val token: String = uuid(), val acceptedBy: String? = null, val createdAt: Long = now(), val expiresAt: Long? = null)
data class PendingOperation(val id: String = uuid(), val entity: PendingEntityType, val entityId: String, val operation: PendingOperationType, val createdAt: Long = now(), val attempts: Int = 0, val lastError: String? = null)

fun uuid(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()
