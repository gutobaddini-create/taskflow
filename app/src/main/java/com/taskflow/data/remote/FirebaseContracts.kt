package com.taskflow.data.remote

import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.PendingOperation

object FirebaseCollections {
    const val Users = "users"
    const val Spaces = "spaces"
    const val Lists = "lists"
    const val Tasks = "tasks"
    const val Reminders = "reminders"
    const val Attachments = "attachments"
    const val Links = "links"
    const val CustomFields = "customFields"
    const val Checklist = "checklist"
    const val Comments = "comments"
    const val ActivityLog = "activityLog"
    const val Invites = "invites"
    const val PendingOperations = "pendingOperations"

    val all = listOf(
        Users,
        Spaces,
        Lists,
        Tasks,
        Reminders,
        Attachments,
        Links,
        CustomFields,
        Checklist,
        Comments,
        ActivityLog,
        Invites,
        PendingOperations
    )
}

object FirebaseStoragePaths {
    fun attachment(userId: String, taskId: String, attachment: Attachment): String {
        return "users/$userId/tasks/$taskId/attachments/${attachment.id}/${attachment.fileName}"
    }

    fun thumbnail(userId: String, taskId: String, attachmentId: String): String {
        return "users/$userId/tasks/$taskId/attachments/$attachmentId/thumbnail.jpg"
    }
}

sealed class RemoteAuthResult {
    data class SignedIn(val userId: String) : RemoteAuthResult()
    data object SignedOut : RemoteAuthResult()
}

interface RemoteTaskFlowDataSource {
    val isConfigured: Boolean

    suspend fun signIn(email: String, password: String): RemoteAuthResult
    suspend fun signUp(name: String, email: String, password: String): RemoteAuthResult
    suspend fun sendPasswordReset(email: String)
    suspend fun signOut(): RemoteAuthResult
    suspend fun syncPendingOperation(operation: PendingOperation): RemoteSyncResult
    suspend fun uploadAttachment(userId: String, attachment: Attachment): RemoteUploadResult
    suspend fun deleteAttachment(userId: String, attachment: Attachment)
    suspend fun registerFcmToken(userId: String, token: String)
}

sealed class RemoteSyncResult {
    data object Applied : RemoteSyncResult()
    data class Conflict(val remoteUpdatedAt: Long, val message: String) : RemoteSyncResult()
}

data class RemoteUploadResult(
    val storagePath: String,
    val secureUrl: String? = null,
    val thumbnailPath: String? = null
)
