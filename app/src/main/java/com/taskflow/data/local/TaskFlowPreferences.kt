package com.taskflow.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.taskFlowDataStore by preferencesDataStore(LocalPersistencePlan.preferencesName)

data class TaskFlowUserPreferences(
    val theme: String = "Claro",
    val notificationsEnabled: Boolean = true,
    val currentUserId: String = "",
    val homeFilter: String = "Hoje",
    val remindersVisible: Boolean = true
)

class TaskFlowPreferences(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val currentUserId = stringPreferencesKey("current_user_id")
        val homeFilter = stringPreferencesKey("home_filter")
        val remindersVisible = booleanPreferencesKey("reminders_visible")
    }

    val values: Flow<TaskFlowUserPreferences> = context.taskFlowDataStore.data.map { prefs ->
        TaskFlowUserPreferences(
            theme = prefs[Keys.theme] ?: "Claro",
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: true,
            currentUserId = prefs[Keys.currentUserId] ?: "",
            homeFilter = prefs[Keys.homeFilter] ?: "Hoje",
            remindersVisible = prefs[Keys.remindersVisible] ?: true
        )
    }

    suspend fun setTheme(value: String) {
        context.taskFlowDataStore.edit { it[Keys.theme] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.taskFlowDataStore.edit { it[Keys.notificationsEnabled] = value }
    }

    suspend fun setCurrentUserId(value: String) {
        context.taskFlowDataStore.edit { it[Keys.currentUserId] = value }
    }

    suspend fun setHomeFilter(value: String) {
        context.taskFlowDataStore.edit { it[Keys.homeFilter] = value }
    }

    suspend fun setRemindersVisible(value: Boolean) {
        context.taskFlowDataStore.edit { it[Keys.remindersVisible] = value }
    }
}
