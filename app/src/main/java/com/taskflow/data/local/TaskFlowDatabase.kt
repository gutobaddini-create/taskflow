package com.taskflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        InviteEntity::class
    ],
    version = 1,
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
            ).build().also { instance = it }
        }
    }
}
