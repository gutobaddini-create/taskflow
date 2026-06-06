package com.taskflow

import android.Manifest
import android.os.Bundle
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.data.local.TaskFlowPreferences
import com.taskflow.data.local.TaskFlowUserPreferences
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.repository.LocalTaskFlowRepository
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.LoadingFullScreen
import com.taskflow.core.design.SegmentedControl as DesignSegmentedControl
import com.taskflow.core.design.TaskFlowButton as DesignTaskFlowButton
import com.taskflow.core.design.TaskFlowCard as DesignTaskFlowCard
import com.taskflow.core.design.TaskFlowTheme
import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.core.notifications.ReminderReceiver
import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.core.utils.attachmentType
import com.taskflow.core.utils.isAllowedAttachment
import com.taskflow.core.utils.isValidUrl
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
import com.taskflow.domain.model.*
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val OffWhite = Color(0xFFF7F8FC)
private val Text = Color(0xFF07132F)
private val Muted = Color(0xFF667085)
private val Blue = Color(0xFF2563FF)
private val Purple = Color(0xFF7C3AED)
private val Border = Color(0xFFE5E7EB)
class MainActivity : ComponentActivity() {
    private val pendingInviteToken = mutableStateOf<String?>(null)
    private val pendingTaskId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInviteToken.value = intent.inviteToken()
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
        pendingInviteToken.value = intent.inviteToken()
        pendingTaskId.value = intent.notificationTaskId()
    }
}

private fun Intent.inviteToken(): String? = data?.takeIf { it.scheme == "taskflow" && it.host == "invite" }?.lastPathSegment
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
    LaunchedEffect(inviteToken) {
        if (!inviteToken.isNullOrBlank()) screen = Screen.AcceptInvite
    }
    LaunchedEffect(notificationTaskId, tasks) {
        if (!notificationTaskId.isNullOrBlank() && tasks.any { it.id == notificationTaskId }) {
            vm.selectedTaskId = notificationTaskId
            screen = Screen.Detail
            onNotificationTaskHandled()
        }
    }
    TaskFlowTheme {
        Surface(Modifier.fillMaxSize(), color = OffWhite) {
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
                Screen.AcceptInvite -> AcceptInviteScreen(vm, inviteToken, { onInviteHandled(); screen = Screen.Home }, { onInviteHandled(); screen = Screen.Onboarding })
            }
        }
    }
}

@Composable
fun Shell(current: Screen, navigate: (Screen) -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        NavigationBar(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 14.dp).clip(RoundedCornerShape(26.dp)),
            containerColor = Color.White
        ) {
            navItems().forEach { (screen, icon) ->
                NavigationBarItem(selected = current == screen, onClick = { navigate(screen) }, icon = { Icon(icon, null) }, label = { Text(screen.label) })
            }
        }
    }
}

fun navItems() = listOf(Screen.Home to Icons.Default.CalendarToday, Screen.Spaces to Icons.Default.List, Screen.People to Icons.Default.Groups, Screen.Settings to Icons.Default.Settings)
