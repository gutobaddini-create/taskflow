package com.taskflow.core.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.local.TaskFlowPreferences
import com.taskflow.data.local.TaskFlowUserPreferences
import com.taskflow.data.mapper.toEntity
import com.taskflow.data.remote.AndroidConnectivityMonitor
import com.taskflow.data.remote.FirebaseTaskFlowDataSource
import com.taskflow.data.remote.RemoteAuthResult
import com.taskflow.data.remote.RemoteInviteLink
import com.taskflow.data.remote.RemoteInviteTaskSnapshot
import com.taskflow.data.remote.RoomPendingOperationQueue
import com.taskflow.data.remote.SyncCoordinator
import com.taskflow.data.repository.LocalTaskFlowRepository
import com.taskflow.domain.model.ActivityLog
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Space
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskList
import com.taskflow.domain.model.User
import com.taskflow.domain.model.now
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaskFlowDatabase.get(application).dao()
    val repo = LocalTaskFlowRepository(dao, application, viewModelScope)
    private val preferencesStore = TaskFlowPreferences(application)
    private val syncCoordinator = SyncCoordinator(
        queue = RoomPendingOperationQueue(dao),
        remote = FirebaseTaskFlowDataSource(),
        connectivity = AndroidConnectivityMonitor(application)
    )
    private val firebaseRemote = FirebaseTaskFlowDataSource()
    val users = repo.users
    val spaces = repo.spaces
    val lists = repo.lists
    val tasks = repo.tasks
    val reminders = repo.reminders
    val attachments = repo.attachments
    val links = repo.links
    val customFields = repo.customFields
    val checklist = repo.checklist
    val comments = repo.comments
    val invites = repo.invites
    val activity = repo.activity
    val pendingOperations = repo.pendingOperations
    private val _preferences = MutableStateFlow(TaskFlowUserPreferences())
    val preferences: StateFlow<TaskFlowUserPreferences> = _preferences
    var remindersVisible by mutableStateOf(true)
        private set
    var homeFilter by mutableStateOf("Hoje")
        private set
    var selectedTaskId by mutableStateOf<String?>(null)
    var materialsTab by mutableStateOf("Anexos")
    var pendingInviteToken by mutableStateOf<String?>(null)

    init {
        syncCoordinator.start(viewModelScope)
        viewModelScope.launch {
            preferencesStore.values.collect { prefs ->
                _preferences.value = prefs
                remindersVisible = prefs.remindersVisible
                homeFilter = prefs.homeFilter
            }
        }
    }

    fun updateHomeFilter(value: String) {
        homeFilter = value
        viewModelScope.launch { preferencesStore.setHomeFilter(value) }
    }

    fun updateRemindersVisible(value: Boolean) {
        remindersVisible = value
        viewModelScope.launch {
            preferencesStore.setRemindersVisible(value)
            preferencesStore.setNotificationsEnabled(value)
        }
    }

    fun setTheme(value: String) {
        viewModelScope.launch { preferencesStore.setTheme(value) }
    }

    fun setNotificationsEnabled(value: Boolean) {
        remindersVisible = value
        viewModelScope.launch {
            preferencesStore.setNotificationsEnabled(value)
            preferencesStore.setRemindersVisible(value)
        }
    }

    fun setCurrentUserIfNeeded(userId: String) {
        if (preferences.value.currentUserId.isBlank()) {
            viewModelScope.launch { preferencesStore.setCurrentUserId(userId) }
        }
    }

    fun setCurrentUser(userId: String) {
        viewModelScope.launch { preferencesStore.setCurrentUserId(userId) }
    }

    fun logoutLocal() {
        viewModelScope.launch { preferencesStore.setCurrentUserId("") }
    }

    fun loginLocal(email: String): Boolean {
        val normalized = email.trim().lowercase()
        val user = users.value.firstOrNull { it.email.lowercase() == normalized } ?: return false
        setCurrentUser(user.id)
        return true
    }

    fun registerLocal(name: String, email: String): Boolean {
        val cleanName = name.trim()
        val normalized = email.trim().lowercase()
        if (cleanName.isBlank() || !normalized.contains("@")) return false
        val existing = users.value.firstOrNull { it.email.lowercase() == normalized }
        val user = existing ?: User(name = cleanName, email = normalized)
        repo.saveUser(user)
        setCurrentUser(user.id)
        return true
    }

    suspend fun loginFirebase(email: String, password: String): Result<Unit> {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || password.isBlank()) return Result.failure(IllegalArgumentException("Informe e-mail e senha."))
        return runCatching {
            when (val result = firebaseRemote.signIn(normalized, password)) {
                is RemoteAuthResult.SignedIn -> {
                    val user = users.value.firstOrNull { it.email.lowercase() == normalized }
                        ?: User(id = result.userId, name = normalized.substringBefore("@").ifBlank { "TaskFlow" }, email = normalized)
                    repo.saveUser(user.copy(id = result.userId, email = normalized))
                    setCurrentUser(result.userId)
                }
                RemoteAuthResult.SignedOut -> error("Firebase nao retornou usuario autenticado.")
            }
        }
    }

    suspend fun registerFirebase(name: String, email: String, password: String): Result<Unit> {
        val cleanName = name.trim()
        val normalized = email.trim().lowercase()
        if (cleanName.isBlank() || !normalized.contains("@") || password.length < 6) {
            return Result.failure(IllegalArgumentException("Informe nome, e-mail valido e senha com 6+ caracteres."))
        }
        return runCatching {
            when (val result = firebaseRemote.signUp(cleanName, normalized, password)) {
                is RemoteAuthResult.SignedIn -> {
                    repo.saveUser(User(id = result.userId, name = cleanName, email = normalized))
                    setCurrentUser(result.userId)
                }
                RemoteAuthResult.SignedOut -> error("Firebase nao retornou usuario cadastrado.")
            }
        }
    }

    fun currentUser(): User {
        val currentId = preferences.value.currentUserId
        return users.value.firstOrNull { it.id == currentId }
            ?: User(id = currentId.ifBlank { "local" }, name = "Voce", email = "")
    }

    fun selectedTask(): Task? {
        val currentId = preferences.value.currentUserId
        return tasks.value.firstOrNull { it.id == selectedTaskId && (currentId.isBlank() || it.createdBy == currentId || it.assignedTo == currentId || currentId in it.participants) }
    }

    suspend fun createRemoteInvite(invite: Invite, task: Task): Result<RemoteInviteLink> = runCatching {
        repo.createInvite(invite)
        val link = RemoteInviteLink(
            token = invite.token,
            permission = invite.permission,
            createdBy = invite.createdBy,
            createdAt = invite.createdAt,
            expiresAt = invite.expiresAt,
            task = RemoteInviteTaskSnapshot(
                id = task.id,
                title = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                createdBy = task.createdBy,
                assignedTo = task.assignedTo,
                dueDateEpochMillis = task.dueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                createdAt = task.createdAt,
                updatedAt = task.updatedAt
            )
        )
        firebaseRemote.createInviteLink(link)
    }

    suspend fun acceptRemoteInvite(token: String): Result<Task> = runCatching {
        val current = currentUser()
        check(current.id.isNotBlank() && current.id != "local") { "Entre ou crie uma conta antes de aceitar o convite." }
        val remote = firebaseRemote.resolveInviteLink(token) ?: error("Convite remoto nao encontrado.")
        val expiresAt = remote.expiresAt
        if (expiresAt != null && expiresAt < now()) error("Convite expirado.")
        val accepted = firebaseRemote.acceptInviteLink(token, current.id)
        importAcceptedInvite(accepted, current.id)
    }

    suspend fun resolveRemoteInvite(token: String): Result<RemoteInviteLink?> = runCatching {
        firebaseRemote.resolveInviteLink(token)
    }

    private suspend fun importAcceptedInvite(remote: RemoteInviteLink, userId: String): Task {
        val existingSpace = spaces.value.firstOrNull { it.name == "Compartilhadas comigo" && userId in it.members }
        val space = existingSpace ?: Space(name = "Compartilhadas comigo", ownerId = userId, members = listOf(userId))
        val existingList = lists.value.firstOrNull { it.spaceId == space.id && it.name == "Convites aceitos" }
        val list = existingList ?: TaskList(spaceId = space.id, name = "Convites aceitos", order = 0)
        val task = Task(
            id = remote.task.id,
            spaceId = space.id,
            listId = list.id,
            title = remote.task.title,
            description = remote.task.description,
            status = remote.task.status,
            priority = remote.task.priority,
            createdBy = remote.task.createdBy,
            assignedTo = remote.task.assignedTo,
            participants = listOf(remote.task.createdBy, userId).distinct(),
            dueDate = remote.task.dueDateEpochMillis?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) },
            createdAt = remote.task.createdAt,
            updatedAt = now()
        )
        val invite = Invite(taskId = task.id, createdBy = remote.createdBy, permission = remote.permission, token = remote.token, acceptedBy = userId, createdAt = remote.createdAt, expiresAt = remote.expiresAt)
        dao.upsertSpaces(listOf(space.toEntity()))
        dao.upsertLists(listOf(list.toEntity()))
        dao.upsertTasks(listOf(task.toEntity()))
        dao.upsertInvites(listOf(invite.toEntity()))
        dao.upsertActivity(listOf(ActivityLog(taskId = task.id, userId = userId, action = "Convite aceito: ${remote.permission.label}").toEntity()))
        selectedTaskId = task.id
        return task
    }
}
