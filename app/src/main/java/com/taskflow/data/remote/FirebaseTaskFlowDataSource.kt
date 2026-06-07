package com.taskflow.data.remote

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.UserPermission
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseTaskFlowDataSource(
    private val forceConfigured: Boolean? = null,
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val storageProvider: () -> FirebaseStorage = { FirebaseStorage.getInstance() },
    private val messagingProvider: () -> FirebaseMessaging = { FirebaseMessaging.getInstance() }
) : RemoteTaskFlowDataSource {
    override val isConfigured: Boolean
        get() = forceConfigured ?: runCatching { FirebaseApp.getInstance() }.isSuccess

    override suspend fun signIn(email: String, password: String): RemoteAuthResult {
        requireConfigured()
        val result = authProvider().signInWithEmailAndPassword(email.trim(), password).await()
        return result.user?.uid?.let(RemoteAuthResult::SignedIn) ?: RemoteAuthResult.SignedOut
    }

    override suspend fun signUp(name: String, email: String, password: String): RemoteAuthResult {
        requireConfigured()
        val result = authProvider().createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: return RemoteAuthResult.SignedOut
        runCatching {
            firestoreProvider()
                .collection(FirebaseCollections.Users)
                .document(user.uid)
                .set(
                    mapOf(
                        "id" to user.uid,
                        "name" to name.trim(),
                        "email" to email.trim().lowercase(),
                        "createdAt" to System.currentTimeMillis(),
                        "notificationPermissionStatus" to "unknown"
                    )
                )
                .await()
        }
        return RemoteAuthResult.SignedIn(user.uid)
    }

    override suspend fun sendPasswordReset(email: String) {
        requireConfigured()
        authProvider().sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun signOut(): RemoteAuthResult {
        requireConfigured()
        authProvider().signOut()
        return RemoteAuthResult.SignedOut
    }

    override suspend fun syncPendingOperation(operation: PendingOperation): RemoteSyncResult {
        requireConfigured()
        val userId = requireAuthenticatedUserId()
        firestoreProvider()
            .collection(FirebaseCollections.PendingOperations)
            .document(operation.id)
            .set(operation.toRemoteMap(userId))
            .await()
        return RemoteSyncResult.Applied
    }

    override suspend fun uploadAttachment(userId: String, attachment: Attachment): RemoteUploadResult {
        requireConfigured()
        val storagePath = FirebaseStoragePaths.attachment(userId, attachment.taskId, attachment)
        val localUri = attachment.localUriOrNull()
        if (localUri != null) {
            val metadata = storageMetadata {
                contentType = attachment.mimeType
            }
            storageProvider().reference.child(storagePath).putFile(localUri, metadata).await()
        }
        firestoreProvider()
            .collection(FirebaseCollections.Attachments)
            .document(attachment.id)
            .set(
                mapOf(
                    "id" to attachment.id,
                    "taskId" to attachment.taskId,
                    "uploadedBy" to attachment.uploadedBy,
                    "fileName" to attachment.fileName,
                    "originalFileName" to attachment.originalFileName,
                    "mimeType" to attachment.mimeType,
                    "fileSize" to attachment.fileSize,
                    "storagePath" to storagePath,
                    "createdAt" to attachment.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
        return RemoteUploadResult(storagePath = storagePath)
    }

    override suspend fun deleteAttachment(userId: String, attachment: Attachment) {
        requireConfigured()
        val storagePath = FirebaseStoragePaths.attachment(userId, attachment.taskId, attachment)
        runCatching { storageProvider().reference.child(storagePath).delete().await() }
        firestoreProvider()
            .collection(FirebaseCollections.Attachments)
            .document(attachment.id)
            .delete()
            .await()
    }

    override suspend fun registerFcmToken(userId: String, token: String) {
        requireConfigured()
        val resolvedToken = token.ifBlank { messagingProvider().token.await() }
        firestoreProvider()
            .collection(FirebaseCollections.Users)
            .document(userId)
            .collection("fcmTokens")
            .document(resolvedToken)
            .set(
                mapOf(
                    "token" to resolvedToken,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    override suspend fun createInviteLink(invite: RemoteInviteLink): RemoteInviteLink {
        requireConfigured()
        requireAuthenticatedUserId()
        firestoreProvider()
            .collection(FirebaseCollections.InviteLinks)
            .document(invite.token)
            .set(invite.toRemoteMap())
            .await()
        return invite
    }

    override suspend fun resolveInviteLink(token: String): RemoteInviteLink? {
        requireConfigured()
        val doc = firestoreProvider()
            .collection(FirebaseCollections.InviteLinks)
            .document(token)
            .get()
            .await()
        return doc.data?.toRemoteInviteLink()
    }

    override suspend fun acceptInviteLink(token: String, userId: String): RemoteInviteLink {
        requireConfigured()
        val acceptedAt = System.currentTimeMillis()
        val ref = firestoreProvider().collection(FirebaseCollections.InviteLinks).document(token)
        ref.update(mapOf("acceptedBy" to userId, "acceptedAt" to acceptedAt)).await()
        return resolveInviteLink(token) ?: error("Convite remoto nao encontrado.")
    }

    private fun requireConfigured() {
        check(isConfigured) {
            "Firebase is not configured. Add google-services.json and initialize FirebaseApp before enabling remote sync."
        }
    }

    private fun requireAuthenticatedUserId(): String {
        return authProvider().currentUser?.uid
            ?: error("Firebase user is not authenticated. Sign in before enabling remote sync.")
    }

    internal fun PendingOperation.toRemoteMap(userId: String): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "entity" to entity.name,
            "entityId" to entityId,
            "operation" to operation.name,
            "createdAt" to createdAt,
            "attempts" to attempts,
            "lastError" to lastError,
            "syncedAt" to System.currentTimeMillis()
        )
    }

    private fun RemoteInviteLink.toRemoteMap(): Map<String, Any?> = mapOf(
        "token" to token,
        "permission" to permission.name,
        "createdBy" to createdBy,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt,
        "acceptedBy" to acceptedBy,
        "acceptedAt" to acceptedAt,
        "task" to mapOf(
            "id" to task.id,
            "title" to task.title,
            "description" to task.description,
            "status" to task.status.name,
            "priority" to task.priority.name,
            "createdBy" to task.createdBy,
            "assignedTo" to task.assignedTo,
            "dueDateEpochMillis" to task.dueDateEpochMillis,
            "createdAt" to task.createdAt,
            "updatedAt" to task.updatedAt
        )
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toRemoteInviteLink(): RemoteInviteLink {
        val taskMap = this["task"] as? Map<String, Any?> ?: error("Convite remoto sem tarefa.")
        return RemoteInviteLink(
            token = this["token"] as? String ?: "",
            permission = UserPermission.valueOf(this["permission"] as? String ?: UserPermission.Viewer.name),
            createdBy = this["createdBy"] as? String ?: "",
            createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
            expiresAt = (this["expiresAt"] as? Number)?.toLong(),
            acceptedBy = this["acceptedBy"] as? String,
            acceptedAt = (this["acceptedAt"] as? Number)?.toLong(),
            task = RemoteInviteTaskSnapshot(
                id = taskMap["id"] as? String ?: "",
                title = taskMap["title"] as? String ?: "Tarefa compartilhada",
                description = taskMap["description"] as? String ?: "",
                status = TaskStatus.valueOf(taskMap["status"] as? String ?: TaskStatus.Todo.name),
                priority = TaskPriority.valueOf(taskMap["priority"] as? String ?: TaskPriority.Medium.name),
                createdBy = taskMap["createdBy"] as? String ?: "",
                assignedTo = taskMap["assignedTo"] as? String,
                dueDateEpochMillis = (taskMap["dueDateEpochMillis"] as? Number)?.toLong(),
                createdAt = (taskMap["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (taskMap["updatedAt"] as? Number)?.toLong() ?: 0L
            )
        )
    }

    private fun Attachment.localUriOrNull(): Uri? {
        val raw = storagePath.takeIf { it.isNotBlank() } ?: return null
        return when {
            raw.startsWith("content://") || raw.startsWith("file://") -> Uri.parse(raw)
            File(raw).exists() -> Uri.fromFile(File(raw))
            else -> null
        }
    }
}
