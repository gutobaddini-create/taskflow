package com.taskflow

import com.taskflow.data.remote.AttachmentSyncStep
import com.taskflow.data.remote.ConflictResolution
import com.taskflow.data.remote.RemoteSyncPolicy
import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.PendingOperationType
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteSyncPolicyTest {
    @Test
    fun attachmentCreateUploadsBinaryBeforeMetadata() {
        val operation = PendingOperation(entity = PendingEntityType.Attachment, entityId = "att-1", operation = PendingOperationType.Create)

        assertEquals(AttachmentSyncStep.UploadBinaryThenApplyMetadata, RemoteSyncPolicy.attachmentStep(operation))
    }

    @Test
    fun attachmentDeleteRemovesRemoteBinaryBeforeMetadata() {
        val operation = PendingOperation(entity = PendingEntityType.Attachment, entityId = "att-1", operation = PendingOperationType.Delete)

        assertEquals(AttachmentSyncStep.DeleteRemoteBinaryThenMetadata, RemoteSyncPolicy.attachmentStep(operation))
    }

    @Test
    fun simpleConflictResolutionUsesLatestUpdateTimestamp() {
        assertEquals(ConflictResolution.KeepLocalAndRetry, RemoteSyncPolicy.resolveUpdatedAtConflict(localUpdatedAt = 20, remoteUpdatedAt = 10))
        assertEquals(ConflictResolution.AcceptRemote, RemoteSyncPolicy.resolveUpdatedAtConflict(localUpdatedAt = 10, remoteUpdatedAt = 20))
        assertEquals(ConflictResolution.NoConflict, RemoteSyncPolicy.resolveUpdatedAtConflict(localUpdatedAt = 10, remoteUpdatedAt = 10))
        assertEquals(ConflictResolution.NoConflict, RemoteSyncPolicy.resolveUpdatedAtConflict(localUpdatedAt = 10, remoteUpdatedAt = null))
    }
}
