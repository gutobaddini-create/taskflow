package com.taskflow

import com.taskflow.data.remote.FirebaseCollections
import com.taskflow.data.remote.FirebaseStoragePaths
import com.taskflow.data.remote.FirebaseTaskFlowDataSource
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentType
import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.PendingOperationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseContractsTest {
    @Test
    fun collectionNamesCoverMvpEntities() {
        assertTrue(FirebaseCollections.all.containsAll(
            listOf(
                "users",
                "spaces",
                "lists",
                "tasks",
                "reminders",
                "attachments",
                "links",
                "customFields",
                "checklist",
                "comments",
                "activityLog",
                "invites",
                "inviteLinks",
                "pendingOperations"
            )
        ))
    }

    @Test
    fun pendingEntityTypesHaveRemoteCollectionsPrepared() {
        val collectionByEntity = mapOf(
            PendingEntityType.Space to FirebaseCollections.Spaces,
            PendingEntityType.List to FirebaseCollections.Lists,
            PendingEntityType.Task to FirebaseCollections.Tasks,
            PendingEntityType.Reminder to FirebaseCollections.Reminders,
            PendingEntityType.Attachment to FirebaseCollections.Attachments,
            PendingEntityType.Link to FirebaseCollections.Links,
            PendingEntityType.CustomField to FirebaseCollections.CustomFields,
            PendingEntityType.Checklist to FirebaseCollections.Checklist,
            PendingEntityType.Comment to FirebaseCollections.Comments,
            PendingEntityType.Invite to FirebaseCollections.Invites
        )

        assertEquals(PendingEntityType.entries.toSet(), collectionByEntity.keys)
        assertTrue(FirebaseCollections.all.containsAll(collectionByEntity.values))
    }

    @Test
    fun attachmentStoragePathIsPrivatePerUserAndTask() {
        val attachment = Attachment(
            id = "att-1",
            taskId = "task-1",
            uploadedBy = "user-1",
            fileName = "contrato.pdf",
            originalFileName = "contrato.pdf",
            fileType = AttachmentType.Pdf,
            mimeType = "application/pdf",
            fileSize = 128,
            storagePath = "local/contrato.pdf"
        )

        assertEquals(
            "users/user-1/tasks/task-1/attachments/att-1/contrato.pdf",
            FirebaseStoragePaths.attachment("user-1", "task-1", attachment)
        )
        assertEquals(
            "users/user-1/tasks/task-1/thumbnails/att-1.jpg",
            FirebaseStoragePaths.thumbnail("user-1", "task-1", "att-1")
        )
    }

    @Test
    fun firebaseDataSourceFailsClosedUntilConfigured() = runTest {
        val dataSource = FirebaseTaskFlowDataSource(forceConfigured = false)

        assertFalse(dataSource.isConfigured)
        val error = runCatching { dataSource.signIn("ana@taskflow.local", "secret") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("Firebase is not configured"))
    }

    @Test
    fun pendingOperationRemotePayloadIncludesAuthenticatedUserId() {
        val operation = PendingOperation(
            id = "op-1",
            entity = PendingEntityType.Task,
            entityId = "task-1",
            operation = PendingOperationType.Create,
            createdAt = 1234
        )
        val payload = with(FirebaseTaskFlowDataSource(forceConfigured = false)) {
            operation.toRemoteMap(userId = "user-1")
        }

        assertEquals("user-1", payload["userId"])
        assertEquals("op-1", payload["id"])
        assertEquals("Task", payload["entity"])
        assertEquals("task-1", payload["entityId"])
        assertEquals("Create", payload["operation"])
    }
}
