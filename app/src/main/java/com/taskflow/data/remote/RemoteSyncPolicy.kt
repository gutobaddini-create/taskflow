package com.taskflow.data.remote

import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.PendingOperationType

enum class AttachmentSyncStep {
    UploadBinaryThenApplyMetadata,
    DeleteRemoteBinaryThenMetadata,
    MetadataOnly
}

enum class ConflictResolution {
    KeepLocalAndRetry,
    AcceptRemote,
    NoConflict
}

object RemoteSyncPolicy {
    fun attachmentStep(operation: PendingOperation): AttachmentSyncStep {
        if (operation.entity != PendingEntityType.Attachment) return AttachmentSyncStep.MetadataOnly
        return when (operation.operation) {
            PendingOperationType.Create,
            PendingOperationType.Update -> AttachmentSyncStep.UploadBinaryThenApplyMetadata
            PendingOperationType.Delete -> AttachmentSyncStep.DeleteRemoteBinaryThenMetadata
        }
    }

    fun resolveUpdatedAtConflict(localUpdatedAt: Long, remoteUpdatedAt: Long?): ConflictResolution {
        if (remoteUpdatedAt == null) return ConflictResolution.NoConflict
        if (localUpdatedAt == remoteUpdatedAt) return ConflictResolution.NoConflict
        return if (localUpdatedAt > remoteUpdatedAt) {
            ConflictResolution.KeepLocalAndRetry
        } else {
            ConflictResolution.AcceptRemote
        }
    }
}
