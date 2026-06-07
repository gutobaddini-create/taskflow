package com.taskflow

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.BottomNavigationBar
import com.taskflow.core.design.NavigationItem
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.core.design.TaskFlowTheme
import com.taskflow.core.notifications.ReminderReceiver
import com.taskflow.core.sharing.InviteLinks
import com.taskflow.feature.auth.OnboardingScreen
import com.taskflow.feature.home.HomeScreen
import com.taskflow.feature.materials.MaterialsScreen
import com.taskflow.feature.people.PeopleScreen
import com.taskflow.feature.reminders.ReminderScreen
import com.taskflow.feature.settings.SettingsScreen
import com.taskflow.feature.sharing.AcceptInviteScreen
import com.taskflow.feature.sharing.ShareScreen
import com.taskflow.feature.spaces.SpacesScreen
import com.taskflow.feature.tasks.DetailScreen
import com.taskflow.feature.tasks.NewTaskScreen

class MainActivity : ComponentActivity() {
    private val pendingInviteToken = mutableStateOf<String?>(null)
    private val pendingTaskId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInviteToken.value = InviteLinks.tokenFromIntent(intent)
        pendingTaskId.value = intent.notificationTaskId()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        setContent {
            TaskFlowRoot(
                inviteToken = pendingInviteToken.value,
                notificationTaskId = pendingTaskId.value,
                onInviteHandled = { pendingInviteToken.value = null },
                onNotificationTaskHandled = { pendingTaskId.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingInviteToken.value = InviteLinks.tokenFromIntent(intent)
        pendingTaskId.value = intent.notificationTaskId()
    }
}

private fun Intent.notificationTaskId(): String? = getStringExtra(ReminderReceiver.EXTRA_TASK_ID)

sealed class Screen(val label: String) {
    data object Onboarding : Screen("Inicio")
    data object Home : Screen("Hoje")
    data object Spaces : Screen("Listas")
    data object People : Screen("Pessoas")
    data object Settings : Screen("Ajustes")
    data object NewTask : Screen("Nova tarefa")
    data object Detail : Screen("Detalhe")
    data object Reminder : Screen("Lembrete")
    data object Materials : Screen("Materiais")
    data object Share : Screen("Compartilhar")
    data object AcceptInvite : Screen("Convite")
}

@Composable
fun TaskFlowRoot(
    inviteToken: String? = null,
    notificationTaskId: String? = null,
    onInviteHandled: () -> Unit = {},
    onNotificationTaskHandled: () -> Unit = {},
    vm: TaskFlowViewModel = viewModel()
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    val tasks by vm.tasks.collectAsState()
    val preferences by vm.preferences.collectAsState()
    LaunchedEffect(inviteToken) {
        if (!inviteToken.isNullOrBlank()) {
            vm.pendingInviteToken = inviteToken
            screen = if (preferences.currentUserId.isBlank()) Screen.Onboarding else Screen.AcceptInvite
        }
    }
    LaunchedEffect(preferences.currentUserId, vm.pendingInviteToken) {
        if (preferences.currentUserId.isNotBlank() && !vm.pendingInviteToken.isNullOrBlank()) {
            screen = Screen.AcceptInvite
        }
    }
    LaunchedEffect(notificationTaskId, tasks) {
        if (!notificationTaskId.isNullOrBlank() && tasks.any { it.id == notificationTaskId }) {
            vm.selectedTaskId = notificationTaskId
            screen = Screen.Detail
            onNotificationTaskHandled()
        }
    }
    TaskFlowTheme {
        Surface(Modifier.fillMaxSize(), color = TaskFlowColors.OffWhite) {
            when (screen) {
                Screen.Onboarding -> OnboardingScreen(vm) { screen = Screen.Home }
                Screen.Home -> Shell(screen, { screen = it }) { HomeScreen(vm, { screen = Screen.NewTask }, { vm.selectedTaskId = it; screen = Screen.Detail }) }
                Screen.Spaces -> Shell(screen, { screen = it }) { SpacesScreen(vm, { vm.selectedTaskId = it; screen = Screen.Detail }) }
                Screen.People -> Shell(screen, { screen = it }) { PeopleScreen(vm) }
                Screen.Settings -> Shell(screen, { screen = it }) { SettingsScreen(vm) { vm.logoutLocal(); screen = Screen.Onboarding } }
                Screen.NewTask -> NewTaskScreen(vm, { screen = Screen.Home })
                Screen.Detail -> DetailScreen(vm, { screen = Screen.Home }, { screen = Screen.Materials }, { screen = Screen.Share }, { screen = Screen.Reminder })
                Screen.Reminder -> ReminderScreen(vm) { screen = Screen.Detail }
                Screen.Materials -> MaterialsScreen(vm) { screen = Screen.Detail }
                Screen.Share -> ShareScreen(vm) { screen = Screen.Detail }
                Screen.AcceptInvite -> AcceptInviteScreen(vm, inviteToken ?: vm.pendingInviteToken, { onInviteHandled(); vm.pendingInviteToken = null; screen = Screen.Home }, { onInviteHandled(); vm.pendingInviteToken = null; screen = Screen.Onboarding })
            }
        }
    }
}

@Composable
fun Shell(current: Screen, navigate: (Screen) -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        val navigationItems = navItems().map { (screen, icon) -> NavigationItem(screen.label, screen.label, icon) }
        BottomNavigationBar(
            items = navigationItems,
            selectedRoute = current.label,
            onSelect = { selected ->
                navItems().firstOrNull { (screen, _) -> screen.label == selected }?.let { (screen, _) ->
                    navigate(screen)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}

fun navItems() = listOf(Screen.Home to Icons.Default.CalendarToday, Screen.Spaces to Icons.AutoMirrored.Filled.List, Screen.People to Icons.Default.Groups, Screen.Settings to Icons.Default.Settings)
