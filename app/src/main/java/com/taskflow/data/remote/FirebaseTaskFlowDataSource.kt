package com.taskflow.data.remote

import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.PendingOperation

class FirebaseTaskFlowDataSource(
    override val isConfigured: Boolean = false
) : RemoteTaskFlowDataSource {
    override suspend fun signIn(email: String, password: String): RemoteAuthResult {
        return requireConfigured()
    }

    override suspend fun signUp(name: String, email: String, password: String): RemoteAuthResult {
        return requireConfigured()
    }

    override suspend fun sendPasswordReset(email: String) {
        requireConfigured<Unit>()
    }

    override suspend fun signOut(): RemoteAuthResult {
        return if (isConfigured) RemoteAuthResult.SignedOut else requireConfigured()
    }

    override suspend fun syncPendingOperation(operation: PendingOperation): RemoteSyncResult {
        return requireConfigured()
    }

    override suspend fun uploadAttachment(userId: String, attachment: Attachment): RemoteUploadResult {
        return requireConfigured()
    }

    override suspend fun deleteAttachment(userId: String, attachment: Attachment) {
        requireConfigured<Unit>()
    }

    override suspend fun registerFcmToken(userId: String, token: String) {
        requireConfigured<Unit>()
    }

    private fun <T> requireConfigured(): T {
        check(isConfigured) {
            "Firebase is not configured. Add google-services.json and bind Firebase SDK implementations before enabling remote sync."
        }
        error("Firebase SDK implementation is pending project credentials.")
    }
}
