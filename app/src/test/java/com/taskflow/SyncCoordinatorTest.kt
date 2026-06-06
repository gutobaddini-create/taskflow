package com.taskflow

import com.taskflow.data.remote.ConnectivityMonitor
import com.taskflow.data.remote.PendingOperationQueue
import com.taskflow.data.remote.RemoteAuthResult
import com.taskflow.data.remote.RemoteSyncResult
import com.taskflow.data.remote.RemoteTaskFlowDataSource
import com.taskflow.data.remote.RemoteUploadResult
import com.taskflow.data.remote.SyncCoordinator
import com.taskflow.data.remote.SyncSkippedReason
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.PendingEntityType
import com.taskflow.domain.model.PendingOperation
import com.taskflow.domain.model.PendingOperationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCoordinatorTest {
    @Test
    fun offlineConnectivityKeepsPendingOperationsQueued() = runBlocking {
        val operation = PendingOperation(entity = PendingEntityType.Task, entityId = "task-1", operation = PendingOperationType.Create)
        val queue = FakeQueue(listOf(operation))
        val remote = FakeRemote(isConfigured = true)
        val coordinator = SyncCoordinator(queue, remote, FakeConnectivity(online = false))

        val report = coordinator.syncOnce()

        assertEquals(SyncSkippedReason.Offline, report.skippedReason)
        assertEquals(listOf(operation), queue.pending())
        assertTrue(remote.synced.isEmpty())
    }

    @Test
    fun unconfiguredRemoteKeepsPendingOperationsQueued() = runBlocking {
        val operation = PendingOperation(entity = PendingEntityType.Task, entityId = "task-1", operation = PendingOperationType.Create)
        val queue = FakeQueue(listOf(operation))
        val remote = FakeRemote(isConfigured = false)
        val coordinator = SyncCoordinator(queue, remote, FakeConnectivity(online = true))

        val report = coordinator.syncOnce()

        assertEquals(SyncSkippedReason.RemoteNotConfigured, report.skippedReason)
        assertEquals(listOf(operation), queue.pending())
        assertTrue(remote.synced.isEmpty())
    }

    @Test
    fun onlineConfiguredRemoteAppliesAndClearsPendingOperations() = runBlocking {
        val operation = PendingOperation(entity = PendingEntityType.Task, entityId = "task-1", operation = PendingOperationType.Create)
        val queue = FakeQueue(listOf(operation))
        val remote = FakeRemote(isConfigured = true)
        val coordinator = SyncCoordinator(queue, remote, FakeConnectivity(online = true))

        val report = coordinator.syncOnce()

        assertEquals(1, report.attempted)
        assertEquals(1, report.applied)
        assertEquals(0, report.failed)
        assertTrue(queue.pending().isEmpty())
        assertEquals(listOf(operation), remote.synced)
    }

    @Test
    fun newerLocalConflictKeepsOperationForRetry() = runBlocking {
        val operation = PendingOperation(entity = PendingEntityType.Task, entityId = "task-1", operation = PendingOperationType.Update, createdAt = 20)
        val queue = FakeQueue(listOf(operation))
        val remote = FakeRemote(isConfigured = true, result = RemoteSyncResult.Conflict(remoteUpdatedAt = 10, message = "Remote changed"))
        val coordinator = SyncCoordinator(queue, remote, FakeConnectivity(online = true))

        val report = coordinator.syncOnce()

        assertEquals(1, report.failed)
        assertEquals(1, queue.pending().single().attempts)
        assertEquals("Remote changed", queue.pending().single().lastError)
    }

    private class FakeConnectivity(online: Boolean) : ConnectivityMonitor {
        override val isOnline: StateFlow<Boolean> = MutableStateFlow(online)
    }

    private class FakeQueue(initial: List<PendingOperation>) : PendingOperationQueue {
        private val operations = initial.toMutableList()

        override suspend fun pending(): List<PendingOperation> = operations.toList()

        override suspend fun markApplied(operationId: String) {
            operations.removeAll { it.id == operationId }
        }

        override suspend fun markFailed(operation: PendingOperation, message: String) {
            operations.replaceAll { current ->
                if (current.id == operation.id) current.copy(attempts = current.attempts + 1, lastError = message) else current
            }
        }
    }

    private class FakeRemote(
        override val isConfigured: Boolean,
        private val result: RemoteSyncResult = RemoteSyncResult.Applied
    ) : RemoteTaskFlowDataSource {
        val synced = mutableListOf<PendingOperation>()

        override suspend fun signIn(email: String, password: String): RemoteAuthResult = error("Not used")
        override suspend fun signUp(name: String, email: String, password: String): RemoteAuthResult = error("Not used")
        override suspend fun sendPasswordReset(email: String) = error("Not used")
        override suspend fun signOut(): RemoteAuthResult = error("Not used")
        override suspend fun uploadAttachment(userId: String, attachment: Attachment): RemoteUploadResult = error("Not used")
        override suspend fun deleteAttachment(userId: String, attachment: Attachment) = error("Not used")
        override suspend fun registerFcmToken(userId: String, token: String) = error("Not used")

        override suspend fun syncPendingOperation(operation: PendingOperation): RemoteSyncResult {
            synced += operation
            return result
        }
    }
}
