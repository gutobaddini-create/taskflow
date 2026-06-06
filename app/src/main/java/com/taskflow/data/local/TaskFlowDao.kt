package com.taskflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskFlowDao {
    @Query("SELECT * FROM users")
    fun users(): Flow<List<UserEntity>>

    @Query("SELECT * FROM spaces ORDER BY createdAt")
    fun spaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM task_lists ORDER BY listOrder")
    fun lists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM tasks ORDER BY COALESCE(dueDateEpochMillis, createdAt)")
    fun tasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM reminders")
    fun reminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM attachments WHERE isDeleted = 0")
    fun attachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM task_links")
    fun links(): Flow<List<TaskLinkEntity>>

    @Query("SELECT * FROM custom_fields")
    fun customFields(): Flow<List<CustomFieldEntity>>

    @Query("SELECT * FROM checklist_items")
    fun checklist(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM comments ORDER BY createdAt")
    fun comments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM invites ORDER BY createdAt DESC")
    fun invites(): Flow<List<InviteEntity>>

    @Query("SELECT * FROM activity_log ORDER BY createdAt DESC")
    fun activity(): Flow<List<ActivityLogEntity>>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun taskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(values: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpaces(values: List<SpaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLists(values: List<TaskListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(values: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(values: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(values: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinks(values: List<TaskLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomFields(values: List<CustomFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChecklistItems(values: List<ChecklistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(values: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(values: List<ActivityLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvites(values: List<InviteEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun taskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    suspend fun reminderById(reminderId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun remindersByTaskId(taskId: String): List<ReminderEntity>

    @Query("SELECT * FROM attachments WHERE id = :attachmentId LIMIT 1")
    suspend fun attachmentById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM task_links WHERE id = :linkId LIMIT 1")
    suspend fun linkById(linkId: String): TaskLinkEntity?

    @Query("SELECT * FROM custom_fields WHERE id = :fieldId LIMIT 1")
    suspend fun customFieldById(fieldId: String): CustomFieldEntity?

    @Query("SELECT * FROM invites WHERE token = :token LIMIT 1")
    suspend fun inviteByToken(token: String): InviteEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE spaceId = :spaceId")
    suspend fun taskCountBySpace(spaceId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId")
    suspend fun taskCountByList(listId: String): Int

    @Query("SELECT * FROM checklist_items WHERE id = :itemId LIMIT 1")
    suspend fun checklistItemById(itemId: String): ChecklistItemEntity?

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM spaces WHERE id = :spaceId")
    suspend fun deleteSpace(spaceId: String)

    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteList(listId: String)

    @Query("DELETE FROM task_links WHERE id = :linkId")
    suspend fun deleteLink(linkId: String)

    @Query("DELETE FROM custom_fields WHERE id = :fieldId")
    suspend fun deleteCustomField(fieldId: String)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteChecklistItem(itemId: String)

    @Query("DELETE FROM invites WHERE token = :token")
    suspend fun deleteInviteByToken(token: String)
}
