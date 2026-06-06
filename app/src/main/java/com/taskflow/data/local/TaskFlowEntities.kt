package com.taskflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val createdAt: Long,
    val notificationPermissionStatus: String
)

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String,
    val members: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val name: String,
    val listOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val listId: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdBy: String,
    val assignedTo: String?,
    val participants: String,
    val dueDateEpochMillis: Long?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val shareToken: String
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val userId: String,
    val type: String,
    val startDateEpochDay: Long,
    val startMinuteOfDay: Int,
    val recurrenceType: String,
    val recurrenceInterval: Int,
    val recurrenceUnit: String,
    val selectedWeekDays: String,
    val selectedMonthDay: Int?,
    val monthlyRule: String,
    val endType: String,
    val endDateEpochDay: Long?,
    val maxOccurrences: Int?,
    val occurrencesCompleted: Int,
    val nextTriggerAtEpochMillis: Long?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val uploadedBy: String,
    val fileName: String,
    val originalFileName: String,
    val fileType: String,
    val mimeType: String,
    val fileSize: Long,
    val storagePath: String,
    val secureUrl: String?,
    val thumbnailUrl: String?,
    val description: String,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@Entity(tableName = "task_links")
data class TaskLinkEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val createdBy: String,
    val title: String,
    val url: String,
    val description: String,
    val category: String,
    val isImportant: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "custom_fields")
data class CustomFieldEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val fieldName: String,
    val fieldType: String,
    val fieldValue: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "activity_log")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val userId: String,
    val action: String,
    val createdAt: Long
)

@Entity(tableName = "invites")
data class InviteEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val createdBy: String,
    val permission: String,
    val token: String,
    val acceptedBy: String?,
    val createdAt: Long,
    val expiresAt: Long?
)

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val entity: String,
    val entityId: String,
    val operation: String,
    val createdAt: Long,
    val attempts: Int,
    val lastError: String?
)
