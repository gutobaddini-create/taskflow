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
    version = 2,
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
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
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
    }
}
