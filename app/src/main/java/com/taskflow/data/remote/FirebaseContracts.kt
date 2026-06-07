package com.taskflow.data.remote

import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.UserPermission

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
    const val InviteLinks = "inviteLinks"
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
        InviteLinks,
        PendingOperations
    )
}

object FirebaseStoragePaths {
    fun attachment(userId: String, taskId: String, attachment: Attachment): String {
        return "users/$userId/tasks/$taskId/attachments/${attachment.id}/${attachment.fileName}"
    }

    fun thumbnail(userId: String, taskId: String, attachmentId: String): String {
        return "users/$userId/tasks/$taskId/thumbnails/$attachmentId.jpg"
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
    suspend fun createInviteLink(invite: RemoteInviteLink): RemoteInviteLink
    suspend fun resolveInviteLink(token: String): RemoteInviteLink?
    suspend fun acceptInviteLink(token: String, userId: String): RemoteInviteLink
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

data class RemoteInviteTaskSnapshot(
    val id: String,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val createdBy: String,
    val assignedTo: String?,
    val dueDateEpochMillis: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class RemoteInviteLink(
    val token: String,
    val task: RemoteInviteTaskSnapshot,
    val permission: UserPermission,
    val createdBy: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val acceptedBy: String? = null,
    val acceptedAt: Long? = null
)
