package com.taskflow.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.taskflow.data.local.TaskFlowDao
import com.taskflow.data.mapper.toDomain
import com.taskflow.data.mapper.toEntity
import com.taskflow.domain.model.PendingOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
}

class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableStateFlow(connectivityManager.currentOnlineState())
    override val isOnline: StateFlow<Boolean> = _isOnline

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = connectivityManager.currentOnlineState()
                }
            }
        )
    }

    private fun ConnectivityManager.currentOnlineState(): Boolean {
        val network = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

interface PendingOperationQueue {
    suspend fun pending(): List<PendingOperation>
    suspend fun markApplied(operationId: String)
    suspend fun markFailed(operation: PendingOperation, message: String)
}

class RoomPendingOperationQueue(
    private val dao: TaskFlowDao
) : PendingOperationQueue {
    override suspend fun pending(): List<PendingOperation> {
        return dao.pendingOperationsSnapshot().map { it.toDomain() }
    }

    override suspend fun markApplied(operationId: String) {
        dao.deletePendingOperation(operationId)
    }

    override suspend fun markFailed(operation: PendingOperation, message: String) {
        dao.upsertPendingOperations(
            listOf(
                operation
                    .copy(attempts = operation.attempts + 1, lastError = message.take(180))
                    .toEntity()
            )
        )
    }
}

data class SyncRunReport(
    val attempted: Int,
    val applied: Int,
    val failed: Int,
    val skippedReason: SyncSkippedReason? = null
)

enum class SyncSkippedReason {
    Offline,
    RemoteNotConfigured
}

class SyncCoordinator(
    private val queue: PendingOperationQueue,
    private val remote: RemoteTaskFlowDataSource,
    private val connectivity: ConnectivityMonitor
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            connectivity.isOnline
                .collect { online ->
                    if (online) syncOnce()
                }
        }
    }

    suspend fun syncOnce(): SyncRunReport {
        val pending = queue.pending()
        if (pending.isEmpty()) return SyncRunReport(attempted = 0, applied = 0, failed = 0)
        if (!connectivity.isOnline.value) {
            return SyncRunReport(attempted = 0, applied = 0, failed = 0, skippedReason = SyncSkippedReason.Offline)
        }
        if (!remote.isConfigured) {
            return SyncRunReport(attempted = 0, applied = 0, failed = 0, skippedReason = SyncSkippedReason.RemoteNotConfigured)
        }

        var applied = 0
        var failed = 0
        pending.forEach { operation ->
            when (val result = runCatching { remote.syncPendingOperation(operation) }.getOrElse { throwable ->
                queue.markFailed(operation, throwable.message ?: "Erro remoto")
                failed += 1
                null
            }) {
                RemoteSyncResult.Applied -> {
                    queue.markApplied(operation.id)
                    applied += 1
                }
                is RemoteSyncResult.Conflict -> {
                    when (RemoteSyncPolicy.resolveUpdatedAtConflict(operation.createdAt, result.remoteUpdatedAt)) {
                        ConflictResolution.AcceptRemote,
                        ConflictResolution.NoConflict -> {
                            queue.markApplied(operation.id)
                            applied += 1
                        }
                        ConflictResolution.KeepLocalAndRetry -> {
                            queue.markFailed(operation, result.message)
                            failed += 1
                        }
                    }
                }
                null -> Unit
            }
        }
        return SyncRunReport(attempted = pending.size, applied = applied, failed = failed)
    }
}
