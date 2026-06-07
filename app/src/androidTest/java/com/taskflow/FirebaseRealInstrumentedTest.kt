package com.taskflow

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.taskflow.data.remote.FirebaseCollections
import com.taskflow.data.remote.FirebaseTaskFlowDataSource
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentType
import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.PendingOperationType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FirebaseRealInstrumentedTest {
    @Test
    fun realFirebaseAuthFirestoreStorageAndFcmTokenFlow() = runBlocking {
        val suffix = System.currentTimeMillis()
        val email = "taskflow-device-smoke-$suffix@example.com"
        val password = "TaskFlow!$suffix"
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val remote = FirebaseTaskFlowDataSource()

        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = requireNotNull(authResult.user?.uid)
        val spaceId = "device-space-$suffix"
        val taskId = "device-task-$suffix"
        val operationId = "device-operation-$suffix"
        val attachmentId = "device-attachment-$suffix"
        val token = "device-fcm-token-$suffix"
        val file = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "taskflow-device-smoke-$suffix.txt")
        file.writeText("TaskFlow physical Firebase smoke $suffix")
        val attachment = Attachment(
            id = attachmentId,
            taskId = taskId,
            uploadedBy = userId,
            fileName = file.name,
            originalFileName = file.name,
            fileType = AttachmentType.Document,
            mimeType = "text/plain",
            fileSize = file.length(),
            storagePath = Uri.fromFile(file).toString()
        )

        try {
            firestore.collection(FirebaseCollections.Users).document(userId).set(
                mapOf(
                    "id" to userId,
                    "email" to email,
                    "createdAt" to suffix,
                    "notificationPermissionStatus" to "unknown"
                )
            ).await()
            firestore.collection(FirebaseCollections.Spaces).document(spaceId).set(
                mapOf(
                    "id" to spaceId,
                    "ownerId" to userId,
                    "members" to listOf(userId),
                    "name" to "TaskFlow Device Smoke",
                    "createdAt" to suffix
                )
            ).await()
            firestore.collection(FirebaseCollections.Tasks).document(taskId).set(
                mapOf(
                    "id" to taskId,
                    "spaceId" to spaceId,
                    "createdBy" to userId,
                    "assignedTo" to userId,
                    "participants" to listOf(userId),
                    "title" to "TaskFlow Device Smoke",
                    "status" to "todo",
                    "createdAt" to suffix
                )
            ).await()

            remote.syncPendingOperation(
                PendingOperation(
                    id = operationId,
                    entity = PendingEntityType.Task,
                    entityId = taskId,
                    operation = PendingOperationType.Create,
                    createdAt = suffix
                )
            )
            remote.uploadAttachment(userId, attachment)
            remote.registerFcmToken(userId, token)

            val pending = firestore.collection(FirebaseCollections.PendingOperations).document(operationId).get().await()
            val metadata = firestore.collection(FirebaseCollections.Attachments).document(attachmentId).get().await()
            val fcmToken = firestore.collection(FirebaseCollections.Users).document(userId).collection("fcmTokens").document(token).get().await()
            val storageObjects = storage.reference.child("users/$userId/tasks/$taskId/attachments/$attachmentId/${file.name}").metadata.await()

            assertFalse(pending.data.isNullOrEmpty())
            assertFalse(metadata.data.isNullOrEmpty())
            assertFalse(fcmToken.data.isNullOrEmpty())
            assertFalse(storageObjects.path.isBlank())
        } finally {
            runCatching { firestore.collection(FirebaseCollections.Users).document(userId).collection("fcmTokens").document(token).delete().await() }
            runCatching { remote.deleteAttachment(userId, attachment) }
            runCatching { firestore.collection(FirebaseCollections.PendingOperations).document(operationId).delete().await() }
            runCatching { firestore.collection(FirebaseCollections.Tasks).document(taskId).delete().await() }
            runCatching { firestore.collection(FirebaseCollections.Spaces).document(spaceId).delete().await() }
            runCatching { firestore.collection(FirebaseCollections.Users).document(userId).delete().await() }
            runCatching { auth.currentUser?.delete()?.await() }
            runCatching { file.delete() }
        }
    }
}
