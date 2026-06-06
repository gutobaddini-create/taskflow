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

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {
    val repo = LocalTaskFlowRepository(TaskFlowDatabase.get(application).dao(), application, viewModelScope)
    private val preferencesStore = TaskFlowPreferences(application)
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

@Composable
fun TaskFlowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DesignTaskFlowCard(modifier, content)
}

@Composable
fun TaskCard(task: Task, listName: String, hasReminder: Boolean, onClick: () -> Unit) {
    TaskFlowCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(66.dp).clip(RoundedCornerShape(20.dp)).background(priorityColor(task.priority)))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(34.dp).border(2.dp, Border, CircleShape).clip(CircleShape))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Text)
                Text("${task.dueDate?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"}  -  $listName", color = Muted)
            }
            PriorityPill(task.priority)
            if (hasReminder) Icon(Icons.Default.Notifications, null, tint = Muted, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun NextReminderCard(title: String, date: LocalDateTime) {
    TaskFlowCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.Event, Purple.copy(.14f), Purple)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text("Proximo lembrete", color = Purple, fontWeight = FontWeight.Bold); Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Text); Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")), color = Muted) }
            Icon(Icons.Default.ChevronRight, null, tint = Text)
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    DesignSegmentedControl(options, selected, onSelect)
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    DesignTaskFlowButton(text, onClick, modifier, enabled)
}

@Composable
fun IconTile(icon: ImageVector, bg: Color = Color.White, tint: Color = Text) = Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
@Composable
fun IconBubble(icon: ImageVector, bg: Color = Blue.copy(.12f), tint: Color = Blue) = Box(Modifier.size(42.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
@Composable
fun SectionTitle(text: String) = Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
@Composable
fun ChipText(text: String, active: Boolean = true, modifier: Modifier = Modifier) = Text(text, color = if (active) Purple else Muted, fontWeight = FontWeight.SemiBold, modifier = modifier.clip(RoundedCornerShape(50)).background(if (active) Purple.copy(.10f) else Border.copy(.45f)).padding(horizontal = 12.dp, vertical = 8.dp))
@Composable
fun PriorityPill(priority: TaskPriority) = Text(priority.label, color = priorityColor(priority), modifier = Modifier.clip(RoundedCornerShape(50)).background(priorityColor(priority).copy(.12f)).padding(horizontal = 12.dp, vertical = 8.dp))
@Composable
fun StatusPill(status: TaskStatus) = Text(status.label, color = Blue, modifier = Modifier.clip(RoundedCornerShape(50)).background(Blue.copy(.10f)).padding(horizontal = 12.dp, vertical = 8.dp))
@Composable
fun SmallAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) = OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(10.dp)) { Icon(icon, null); Spacer(Modifier.width(6.dp)); Text(label) }
@Composable
fun TopRow(action: String, title: String, onAction: () -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onAction) { Text(action) }; Spacer(Modifier.weight(1f)); Text(title, fontWeight = FontWeight.Bold, color = Text); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(76.dp)) }
@Composable
fun InfoRow(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, color = Text, fontWeight = FontWeight.SemiBold) }
@Composable
fun MaterialRow(icon: ImageVector, title: String, subtitle: String) = TaskFlowCard(Modifier.padding(bottom = 10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(icon, Purple.copy(.10f), Purple); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Text); Text(subtitle, color = Muted, maxLines = 1) }; Icon(Icons.Default.MoreVert, null, tint = Muted) } }
fun priorityColor(priority: TaskPriority) = when (priority) { TaskPriority.High -> Color(0xFFEF4444); TaskPriority.Medium -> Blue; TaskPriority.Low -> Color(0xFF22C55E) }
