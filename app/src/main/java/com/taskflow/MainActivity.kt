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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
fun DetailScreen(vm: TaskFlowViewModel, onBack: () -> Unit, onMaterials: () -> Unit, onShare: () -> Unit, onReminder: () -> Unit) {
    val task = vm.selectedTask()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val fields by vm.customFields.collectAsState()
    val comments by vm.comments.collectAsState()
    val invites by vm.invites.collectAsState()
    val activity by vm.activity.collectAsState()
    if (task == null) {
        LoadingFullScreen("Carregando tarefa...")
        return
    }
    var editing by remember(task.id) { mutableStateOf(false) }
    var commentText by remember(task.id) { mutableStateOf("") }
    var editingCommentId by remember(task.id) { mutableStateOf<String?>(null) }
    var editingCommentText by remember(task.id) { mutableStateOf("") }
    var editTitle by remember(task.id) { mutableStateOf(task.title) }
    var editDescription by remember(task.id) { mutableStateOf(task.description) }
    var editStatus by remember(task.id) { mutableStateOf(task.status) }
    var editPriority by remember(task.id) { mutableStateOf(task.priority) }
    var confirmComplete by remember(task.id) { mutableStateOf(false) }
    var editDueDay by remember(task.id) { mutableStateOf(if (task.dueDate?.toLocalDate() == LocalDate.now().plusDays(1)) "Amanha" else "Hoje") }
    var editDueHour by remember(task.id) { mutableStateOf(task.dueDate?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "09:00") }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: users.firstOrNull() ?: vm.currentUser()
    var editAssignedTo by remember(task.id, currentUser.id) { mutableStateOf(task.assignedTo ?: currentUser.id) }
    val userOptions = users.ifEmpty { listOf(currentUser) }
    val effectivePermission = PermissionPolicy.acceptedPermission(task.id, currentUser.id, invites)
    val canViewTask = PermissionPolicy.canViewTask(task, currentUser.id, effectivePermission)
    val canEditTask = PermissionPolicy.canEditTask(task, currentUser.id, effectivePermission)
    val canComment = PermissionPolicy.canCommentOnTask(task, currentUser.id, effectivePermission)
    val assigneeName = userOptions.firstOrNull { it.id == task.assignedTo }?.name ?: "Manuel"
    val hasActiveReminders = reminders.any { it.taskId == task.id && it.isActive }
    if (confirmComplete) {
        AlertDialog(
            onDismissRequest = { confirmComplete = false },
            title = { Text("Concluir tarefa?") },
            text = { Text(if (hasActiveReminders) "Esta tarefa possui lembretes ativos. Ao concluir, os lembretes serao desativados." else "A tarefa sera marcada como concluida.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmComplete = false
                    vm.repo.completeTask(task.id)
                    onBack()
                }) { Text("Concluir") }
            },
            dismissButton = { TextButton(onClick = { confirmComplete = false }) { Text("Cancelar") } }
        )
    }
    if (!canViewTask) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                TopRow("<", "Detalhe da tarefa", onBack)
                Spacer(Modifier.height(24.dp))
                TaskFlowCard {
                    Icon(Icons.Default.Lock, null, tint = Muted, modifier = Modifier.size(42.dp))
                    Text("Sem acesso a esta tarefa", fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 12.dp))
                    Text("Solicite um convite ao responsavel para visualizar detalhes, anexos, links e campos.", color = Muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("<") }
                Spacer(Modifier.weight(1f))
                Text("Detalhe da tarefa", fontWeight = FontWeight.Bold, color = Text)
                Spacer(Modifier.weight(1f))
                if (canEditTask) {
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "Cancelar" else "Editar") }
                } else {
                    Text("Ver", color = Muted, modifier = Modifier.padding(end = 12.dp))
                }
            }
            if (editing) {
                OutlinedTextField(editTitle, { editTitle = it }, label = { Text("Titulo") }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(18.dp))
                OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(18.dp), minLines = 3)
                SectionTitle("Status")
                Segmented(TaskStatus.entries.map { it.label }, editStatus.label) { selected -> editStatus = TaskStatus.entries.first { it.label == selected } }
                SectionTitle("Prioridade")
                Segmented(TaskPriority.entries.map { it.label }, editPriority.label) { selected -> editPriority = TaskPriority.entries.first { it.label == selected } }
                SectionTitle("Prazo")
                Segmented(listOf("Hoje", "Amanha"), editDueDay) { editDueDay = it }
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("09:00", "11:00", "14:00", "16:30"), editDueHour) { editDueHour = it }
                SectionTitle("Responsavel")
                Segmented(userOptions.map { it.name }, userOptions.firstOrNull { it.id == editAssignedTo }?.name ?: vm.currentUser().name) { name ->
                    editAssignedTo = userOptions.first { it.name == name }.id
                }
                Spacer(Modifier.height(16.dp))
                GradientButton("Salvar alteracoes", {
                    if (editTitle.isNotBlank()) {
                        val date = if (editDueDay == "Amanha") LocalDate.now().plusDays(1) else LocalDate.now()
                        val done = editStatus == TaskStatus.Done
                        vm.repo.updateTask(task.copy(title = editTitle.trim(), description = editDescription, status = editStatus, priority = editPriority, assignedTo = editAssignedTo, dueDate = LocalDateTime.of(date, LocalTime.parse(editDueHour)), isCompleted = done, completedAt = if (done) task.completedAt ?: now() else null))
                        editing = false
                    }
                }, Modifier.fillMaxWidth().testTag("save-task-edits").semantics { contentDescription = "Salvar alteracoes da tarefa" }, enabled = editTitle.isNotBlank())
                TextButton(onClick = { vm.repo.deleteTask(task.id); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Excluir tarefa", color = Color(0xFFEF4444)) }
            } else {
                Text(task.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 18.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusPill(task.status); PriorityPill(task.priority) }
            }
            SectionTitle("Proximo lembrete")
            val reminderModifier = if (canEditTask) Modifier.clickable(onClick = onReminder) else Modifier
            TaskFlowCard(reminderModifier.testTag("open-reminder").semantics { contentDescription = "Configurar lembrete" }) {
                val taskReminders = reminders.filter { it.taskId == task.id }
                val nextReminder = taskReminders.mapNotNull { reminder -> ReminderEngine.nextOccurrence(reminder)?.let { reminder to it } }.minByOrNull { it.second }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(nextReminder?.first?.displaySummary() ?: "Nenhum lembrete", fontWeight = FontWeight.Bold, color = Text)
                        Text(nextReminder?.second?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Sem disparo agendado", color = Muted)
                    }
                    Switch(
                        checked = taskReminders.any { it.isActive },
                        onCheckedChange = { active -> taskReminders.forEach { vm.repo.saveReminder(it.copy(isActive = active)) } },
                        enabled = canEditTask && taskReminders.isNotEmpty()
                    )
                }
            }
            SectionTitle("Descricao")
            TaskFlowCard { Text((if (editing) editDescription else task.description).ifBlank { "Sem descricao." }, color = Muted, lineHeight = 22.sp) }
            SectionTitle("Dados principais")
            TaskFlowCard {
                InfoRow("Prazo", task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Sem prazo")
                InfoRow("Responsavel", assigneeName)
                InfoRow("Participantes", "2 pessoas")
            }
            SectionTitle("Materiais da tarefa")
            val materialsModifier = if (canViewTask) Modifier.clickable(onClick = onMaterials) else Modifier
            TaskFlowCard(materialsModifier) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("${attachments.count { it.taskId == task.id }} anexos")
                    ChipText("${links.count { it.taskId == task.id }} link")
                    ChipText("${fields.count { it.taskId == task.id }} campos")
                }
                Spacer(Modifier.height(12.dp))
                Text(attachments.firstOrNull { it.taskId == task.id }?.fileName ?: "Nenhum anexo", color = Text)
                Text(links.firstOrNull { it.taskId == task.id }?.title ?: "Nenhum link", color = Text)
            }
            SectionTitle("Comentarios")
            TaskFlowCard {
                val taskComments = comments.filter { it.taskId == task.id }
                if (taskComments.isEmpty()) {
                    Text("Nenhum comentario.", color = Muted)
                } else {
                    taskComments.forEach {
                        val isOwnComment = it.authorId == currentUser.id
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(users.firstOrNull { user -> user.id == it.authorId }?.name ?: "Usuario", fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.weight(1f))
                            if (isOwnComment && editingCommentId != it.id) {
                                TextButton(onClick = {
                                    editingCommentId = it.id
                                    editingCommentText = it.text
                                }, modifier = Modifier.semantics { contentDescription = "Editar comentario" }) { Text("Editar") }
                                TextButton(onClick = { vm.repo.deleteComment(it.id) }, modifier = Modifier.semantics { contentDescription = "Excluir comentario" }) { Text("Excluir", color = Color(0xFFEF4444)) }
                            }
                        }
                        if (editingCommentId == it.id) {
                            OutlinedTextField(editingCommentText, { editingCommentText = it }, label = { Text("Editar comentario") }, shape = RoundedCornerShape(18.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(onClick = {
                                    editingCommentId = null
                                    editingCommentText = ""
                                }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                                Button(onClick = {
                                    if (editingCommentText.isNotBlank()) {
                                        vm.repo.updateComment(it.copy(text = editingCommentText.trim()))
                                        editingCommentId = null
                                        editingCommentText = ""
                                    }
                                }, modifier = Modifier.weight(1f).semantics { contentDescription = "Salvar comentario" }, enabled = editingCommentText.isNotBlank(), shape = RoundedCornerShape(50)) { Text("Salvar") }
                            }
                        } else {
                            Text(it.text, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text(formatTimestamp(it.updatedAt), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
                OutlinedTextField(commentText, { commentText = it }, label = { Text("Novo comentario") }, shape = RoundedCornerShape(18.dp), singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = canComment)
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    if (commentText.isNotBlank()) {
                        vm.repo.addComment(Comment(taskId = task.id, authorId = currentUser.id, text = commentText.trim()))
                        commentText = ""
                    }
                }, modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Adicionar comentario" }, enabled = canComment && commentText.isNotBlank(), shape = RoundedCornerShape(50)) {
                    Text("Adicionar comentario", fontWeight = FontWeight.Bold)
                }
                if (!canComment) {
                    Text("Sem permissao para comentar.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            SectionTitle("Historico")
            TaskFlowCard {
                val taskActivity = activity.filter { it.taskId == task.id }.take(6)
                if (taskActivity.isEmpty()) {
                    Text("Nenhum evento.", color = Muted)
                } else {
                    taskActivity.forEach {
                        Text(it.action, color = Text, fontWeight = FontWeight.SemiBold)
                        Text(formatTimestamp(it.createdAt), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50)) { Text("Compartilhar") }
                GradientButton("Concluir", { confirmComplete = true }, Modifier.weight(1f), enabled = canEditTask)
            }
        }
    }
}

private fun Reminder.displaySummary(): String {
    if (type == ReminderType.OneTime || recurrenceType == RecurrenceType.None) return "Lembrete unico"
    return when (recurrenceUnit) {
        RecurrenceUnit.Days -> if (recurrenceInterval == 1) "Todo dia" else "A cada $recurrenceInterval dias"
        RecurrenceUnit.Weeks -> {
            val days = selectedWeekDays.sortedBy { it.value }.joinToString { it.short }
            (if (recurrenceInterval == 1) "Toda semana" else "A cada $recurrenceInterval semanas") + if (days.isBlank()) "" else " - $days"
        }
        RecurrenceUnit.Months -> {
            val monthly = selectedMonthDay?.let { "dia $it" } ?: monthlyRule.label.lowercase()
            (if (recurrenceInterval == 1) "Todo mes" else "A cada $recurrenceInterval meses") + " - $monthly"
        }
        RecurrenceUnit.Years -> if (recurrenceInterval == 1) "Todo ano" else "A cada $recurrenceInterval anos"
    }
}

@Composable
fun NameDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(title, initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Text, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value, { value = it }, label = { Text("Nome") }, shape = RoundedCornerShape(18.dp), singleLine = true, modifier = Modifier.testTag("name-dialog-field").semantics { contentDescription = "Campo nome" })
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }, enabled = value.isNotBlank(), modifier = Modifier.testTag("name-dialog-save").semantics { contentDescription = "Salvar nome" }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("name-dialog-cancel").semantics { contentDescription = "Cancelar nome" }) { Text("Cancelar") }
        }
    )
}

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
fun formatTimestamp(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
