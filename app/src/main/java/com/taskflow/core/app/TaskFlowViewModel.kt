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
import com.taskflow.data.remote.AndroidConnectivityMonitor
import com.taskflow.data.remote.FirebaseTaskFlowDataSource
import com.taskflow.data.remote.RoomPendingOperationQueue
import com.taskflow.data.remote.SyncCoordinator
import com.taskflow.data.repository.LocalTaskFlowRepository
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaskFlowDatabase.get(application).dao()
    val repo = LocalTaskFlowRepository(dao, application, viewModelScope)
    private val preferencesStore = TaskFlowPreferences(application)
    private val syncCoordinator = SyncCoordinator(
        queue = RoomPendingOperationQueue(dao),
        remote = FirebaseTaskFlowDataSource(),
        connectivity = AndroidConnectivityMonitor(application)
    )
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

    fun currentUser(): User {
        val currentId = preferences.value.currentUserId
        return users.value.firstOrNull { it.id == currentId } ?: users.value.firstOrNull() ?: User(name = "Manuel", email = "manuel@taskflow.local")
    }

    fun selectedTask(): Task? = tasks.value.firstOrNull { it.id == selectedTaskId } ?: tasks.value.firstOrNull()
}
