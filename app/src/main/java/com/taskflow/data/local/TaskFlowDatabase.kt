package com.taskflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        SpaceEntity::class,
        TaskListEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        AttachmentEntity::class,
        TaskLinkEntity::class,
        CustomFieldEntity::class,
        ChecklistItemEntity::class,
        CommentEntity::class,
        ActivityLogEntity::class,
        InviteEntity::class,
        PendingOperationEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TaskFlowDatabase : RoomDatabase() {
    abstract fun dao(): TaskFlowDao

    companion object {
        @Volatile private var instance: TaskFlowDatabase? = null

        fun get(context: Context): TaskFlowDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TaskFlowDatabase::class.java,
                LocalPersistencePlan.databaseName
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_operations` (
                        `id` TEXT NOT NULL,
                        `entity` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM reminders WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM attachments WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM task_links WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM custom_fields WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM checklist_items WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM comments WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM activity_log WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM invites WHERE taskId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM pending_operations WHERE entityId IN (SELECT id FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM tasks WHERE createdBy IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local'))")
                db.execSQL("DELETE FROM task_lists WHERE spaceId IN (SELECT id FROM spaces WHERE ownerId IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')))")
                db.execSQL("DELETE FROM spaces WHERE ownerId IN (SELECT id FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local'))")
                db.execSQL("DELETE FROM users WHERE email IN ('manuel@taskflow.local', 'ana@taskflow.local')")
            }
        }
    }
}
