package com.taskflow.data.mapper

import com.taskflow.data.local.*
import com.taskflow.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val separator = "|"

private fun List<String>.pack(): String = joinToString(separator)
private fun String.unpack(): List<String> = if (isBlank()) emptyList() else split(separator)

private fun LocalDateTime?.toEpochMillis(): Long? = this?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
private fun Long?.toLocalDateTime(): LocalDateTime? = this?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
private fun LocalDate?.toEpochDayValue(): Long? = this?.toEpochDay()
private fun Long?.toLocalDateValue(): LocalDate? = this?.let(LocalDate::ofEpochDay)
private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

fun User.toEntity() = UserEntity(id, name, email, photoUrl, createdAt, notificationPermissionStatus)
fun UserEntity.toDomain() = User(id, name, email, photoUrl, createdAt, notificationPermissionStatus)

fun Space.toEntity() = SpaceEntity(id, name, ownerId, members.pack(), createdAt, updatedAt)
fun SpaceEntity.toDomain() = Space(id, name, ownerId, members.unpack(), createdAt, updatedAt)

fun TaskList.toEntity() = TaskListEntity(id, spaceId, name, order, createdAt, updatedAt)
fun TaskListEntity.toDomain() = TaskList(id, spaceId, name, listOrder, createdAt, updatedAt)

fun Task.toEntity() = TaskEntity(
    id = id,
    spaceId = spaceId,
    listId = listId,
    title = title,
    description = description,
    status = status.name,
    priority = priority.name,
    createdBy = createdBy,
    assignedTo = assignedTo,
    participants = participants.pack(),
    dueDateEpochMillis = dueDate.toEpochMillis(),
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    shareToken = shareToken
)

fun TaskEntity.toDomain() = Task(
    id = id,
    spaceId = spaceId,
    listId = listId,
    title = title,
    description = description,
    status = TaskStatus.valueOf(status),
    priority = TaskPriority.valueOf(priority),
    createdBy = createdBy,
    assignedTo = assignedTo,
    participants = participants.unpack(),
    dueDate = dueDateEpochMillis.toLocalDateTime(),
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    shareToken = shareToken
)

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    taskId = taskId,
    userId = userId,
    type = type.name,
    startDateEpochDay = startDate.toEpochDay(),
    startMinuteOfDay = startTime.toMinuteOfDay(),
    recurrenceType = recurrenceType.name,
    recurrenceInterval = recurrenceInterval,
    recurrenceUnit = recurrenceUnit.name,
    selectedWeekDays = selectedWeekDays.map { it.name }.pack(),
    selectedMonthDay = selectedMonthDay,
    monthlyRule = monthlyRule.name,
    endType = endType.name,
    endDateEpochDay = endDate.toEpochDayValue(),
    maxOccurrences = maxOccurrences,
    occurrencesCompleted = occurrencesCompleted,
    nextTriggerAtEpochMillis = nextTriggerAt.toEpochMillis(),
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    taskId = taskId,
    userId = userId,
    type = ReminderType.valueOf(type),
    startDate = LocalDate.ofEpochDay(startDateEpochDay),
    startTime = startMinuteOfDay.toLocalTime(),
    recurrenceType = RecurrenceType.valueOf(recurrenceType),
    recurrenceInterval = recurrenceInterval,
    recurrenceUnit = RecurrenceUnit.valueOf(recurrenceUnit),
    selectedWeekDays = selectedWeekDays.unpack().map { WeekDay.valueOf(it) },
    selectedMonthDay = selectedMonthDay,
    monthlyRule = MonthlyRule.valueOf(monthlyRule),
    endType = ReminderEndType.valueOf(endType),
    endDate = endDateEpochDay.toLocalDateValue(),
    maxOccurrences = maxOccurrences,
    occurrencesCompleted = occurrencesCompleted,
    nextTriggerAt = nextTriggerAtEpochMillis.toLocalDateTime(),
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Attachment.toEntity() = AttachmentEntity(id, taskId, uploadedBy, fileName, originalFileName, fileType.name, mimeType, fileSize, storagePath, secureUrl, thumbnailUrl, description, source.name, createdAt, updatedAt, isDeleted)
fun AttachmentEntity.toDomain() = Attachment(id, taskId, uploadedBy, fileName, originalFileName, AttachmentType.valueOf(fileType), mimeType, fileSize, storagePath, secureUrl, thumbnailUrl, description, AttachmentSource.valueOf(source), createdAt, updatedAt, isDeleted)

fun TaskLink.toEntity() = TaskLinkEntity(id, taskId, createdBy, title, url, description, category, isImportant, createdAt, updatedAt)
fun TaskLinkEntity.toDomain() = TaskLink(id, taskId, createdBy, title, url, description, category, isImportant, createdAt, updatedAt)

fun CustomField.toEntity() = CustomFieldEntity(id, taskId, fieldName, fieldType.name, fieldValue, createdBy, createdAt, updatedAt)
fun CustomFieldEntity.toDomain() = CustomField(id, taskId, fieldName, CustomFieldType.valueOf(fieldType), fieldValue, createdBy, createdAt, updatedAt)

fun ChecklistItem.toEntity() = ChecklistItemEntity(id, taskId, title, isDone)
fun ChecklistItemEntity.toDomain() = ChecklistItem(id, taskId, title, isDone)

fun Comment.toEntity() = CommentEntity(id, taskId, authorId, text, createdAt, updatedAt)
fun CommentEntity.toDomain() = Comment(id, taskId, authorId, text, createdAt, updatedAt)

fun ActivityLog.toEntity() = ActivityLogEntity(id, taskId, userId, action, createdAt)
fun ActivityLogEntity.toDomain() = ActivityLog(id, taskId, userId, action, createdAt)

fun Invite.toEntity() = InviteEntity(id, taskId, createdBy, permission.name, token, acceptedBy, createdAt, expiresAt)
fun InviteEntity.toDomain() = Invite(id, taskId, createdBy, UserPermission.valueOf(permission), token, acceptedBy, createdAt, expiresAt)
